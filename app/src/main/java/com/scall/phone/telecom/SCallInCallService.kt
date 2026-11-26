package com.scall.phone.telecom

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import androidx.annotation.RequiresApi
import android.telecom.InCallService
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.scall.phone.R
import com.scall.phone.data.PhoneRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Incoming calls belong to Android Telecom. Display an urgent CallStyle notification,
 * so Android can show a heads-up on an unlocked phone or a full-screen incoming
 * call surface when locked, subject to the user's notification/FSI settings.
 *
 * IMPORTANT: onCallAdded can arrive BEFORE STATE_RINGING. Always refresh when the
 * Telecom state changes; otherwise the phone may ring without any call alert UI.
 */
/** Reposting an identical CallStyle event can retrigger OEM alert animations. */
internal data class CallAlertSignature(val callIdentity: Int, val state: Int, val caller: String)

internal fun needsCallNotificationUpdate(
    previous: CallAlertSignature?, latest: CallAlertSignature
): Boolean = previous != latest

class SCallInCallService : InCallService() {
    companion object {
        private const val INCOMING_NOTIFICATION_ID = 4102
        private const val ONGOING_NOTIFICATION_ID = 4103
    }

    private var lastPostedAlert: CallAlertSignature? = null
    private val callerAnnouncer by lazy { CallerNameAnnouncer(applicationContext) }
    private val flipToSilence by lazy {
        FlipToSilenceController(this) {
            callerAnnouncer.silenceSpeech()
            silenceCurrentRingtoneIfDefaultDialer()
        }
    }

    // Android explicitly permits TelecomManager.silenceRinger() to the
    // selected default dialer as an alternative to MODIFY_PHONE_STATE.
    // Lint cannot infer this role exemption, so suppress ONLY that check
    // on this guarded call, never request the protected permission.
    @android.annotation.SuppressLint("MissingPermission")
    private fun silenceCurrentRingtoneIfDefaultDialer() {
        val telecom = getSystemService(TELECOM_SERVICE) as? TelecomManager ?: return
        if (telecom.defaultDialerPackage != packageName) return
        telecom.silenceRinger()
    }
    private val callbackByCall = linkedMapOf<Call, Call.Callback>()
    private val stateCheckHandler = Handler(Looper.getMainLooper())
    private val lookupScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val contactNames = mutableMapOf<Call, String>()
    private val pendingContactLookups = mutableSetOf<Call>()

    // Telecom's display name is often absent for ordinary SIM calls. Resolve
    // a saved contact asynchronously, never blocking the incoming call alert.
    private fun lookupIncomingContactName(call: Call) {
        if (call.state != Call.STATE_RINGING || !pendingContactLookups.add(call)) return
        val number = runCatching { call.details.handle?.schemeSpecificPart.orEmpty() }
            .getOrDefault("")
        if (number.isBlank()) return
        lookupScope.launch {
            val name = PhoneRepository(applicationContext).findContact(number)?.name
            if (!name.isNullOrBlank() && call in callbackByCall &&
                call.state == Call.STATE_RINGING) {
                contactNames[call] = name
                updateCallNotification()
            }
        }
    }

    // Some OEMs deliver onCallAdded before the final RINGING state and do
    // not immediately dispatch a state change callback. Retry briefly while
    // the original call is still owned by Android Telecom.
    private fun recheckIncomingState(call: Call) {
        for (delayMillis in listOf(120L, 350L, 800L, 1500L)) {
            stateCheckHandler.postDelayed({
                if (callbackByCall.containsKey(call)) {
                    CallBridge.refreshFromTelecom()
                    updateCallNotification()
                }
            }, delayMillis)
        }
    }

    private fun cancelCallNotifications() {
        lastPostedAlert = null
        val notifications = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notifications.cancel(INCOMING_NOTIFICATION_ID)
        notifications.cancel(ONGOING_NOTIFICATION_ID)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            CallBridge.bind(this)
        } catch (error: RuntimeException) {
            android.util.Log.e("SCallInCallService", "Telecom binding not ready", error)
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // Notification channels were introduced in Android 8.0. Android 6/7
        // use CATEGORY_CALL + PRIORITY_MAX through NotificationCompat instead.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                manager.createNotificationChannel(
                    NotificationChannel(
                        IncomingCallVisibility.CHANNEL_ID,
                        getString(R.string.incoming_calls_channel),
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = getString(R.string.incoming_calls_description)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        // Telecom provides the ringtone; avoid playing it twice.
                        setSound(null, null)
                        enableVibration(false)
                    }
                )
            } catch (error: RuntimeException) {
                android.util.Log.e("SCallInCallService", "Unable to set up call alerts", error)
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        try {
            CallBridge.addCall(call)
        } catch (error: RuntimeException) {
            android.util.Log.e("SCallInCallService", "Call bridge not ready", error)
        }
        if (!callbackByCall.containsKey(call)) {
            val callback = object : Call.Callback() {
                override fun onStateChanged(call: Call, state: Int) {
                    if (state == Call.STATE_RINGING) lookupIncomingContactName(call)
                    else callerAnnouncer.stopForCall(call)
                    updateCallNotification()
                }

                override fun onDetailsChanged(call: Call, details: Call.Details) {
                    updateCallNotification()
                }
            }
            try {
                call.registerCallback(callback)
                callbackByCall[call] = callback
            } catch (error: RuntimeException) {
                android.util.Log.e("SCallInCallService", "Failed to monitor call changes", error)
            }
        }
        updateCallNotification()
        lookupIncomingContactName(call)
        recheckIncomingState(call)
    }

    override fun onCallRemoved(call: Call) {
        callerAnnouncer.onCallRemoved(call)
        contactNames.remove(call)
        pendingContactLookups.remove(call)
        callbackByCall.remove(call)?.let { callback ->
            try {
                call.unregisterCallback(callback)
            } catch (error: RuntimeException) {
                android.util.Log.w("SCallInCallService", "Call observer already removed", error)
            }
        }
        try {
            CallBridge.removeCall(call)
        } catch (error: RuntimeException) {
            android.util.Log.w("SCallInCallService", "Unable to remove disconnected call", error)
        }
        updateCallNotification()
        super.onCallRemoved(call)
    }

    override fun onBringToForeground(showDialpad: Boolean) {
        super.onBringToForeground(showDialpad)
        CallBridge.refreshFromTelecom()
        // Only foreground Telecom request launches the Activity directly.
        // Incoming calls launched in background use Android's full-screen intent.
        try {
            startActivity(CallUiActivity.createIntent(this))
        } catch (error: RuntimeException) {
            // Android may block background activity launches; the call notification remains.
            android.util.Log.w("SCallInCallService", "Unable to bring call to foreground", error)
        }
    }

    override fun onCanAddCallChanged(canAddCall: Boolean) {
        super.onCanAddCallChanged(canAddCall)
        CallBridge.refresh()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        CallBridge.refresh()
    }

    // Android 14+ reports the actual endpoint and any newly connected devices.
    // Never force a route: Telecom must approve the selection.
    @RequiresApi(34)
    override fun onAvailableCallEndpointsChanged(availableEndpoints: List<CallEndpoint>) {
        super.onAvailableCallEndpointsChanged(availableEndpoints)
        CallBridge.updateAvailableEndpoints(availableEndpoints)
    }

    @RequiresApi(34)
    override fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
        super.onCallEndpointChanged(callEndpoint)
        CallBridge.updateSelectedEndpoint(callEndpoint)
    }

    @RequiresApi(34)
    override fun onMuteStateChanged(isMuted: Boolean) {
        super.onMuteStateChanged(isMuted)
        CallBridge.updateMutedState(isMuted)
    }

    override fun onDestroy() {
        callbackByCall.toMap().forEach { (call, callback) ->
            try {
                call.unregisterCallback(callback)
            } catch (error: RuntimeException) {
                android.util.Log.w("SCallInCallService", "Observer cleanup failed", error)
            }
        }
        callbackByCall.clear()
        contactNames.clear()
        pendingContactLookups.clear()
        lookupScope.cancel()
        callerAnnouncer.close()
        flipToSilence.stop()
        stateCheckHandler.removeCallbacksAndMessages(null)
        try {
            CallBridge.unbind(this)
            cancelCallNotifications()
        } catch (error: RuntimeException) {
            android.util.Log.w("SCallInCallService", "Call service cleanup failed", error)
        }
        super.onDestroy()
    }

    private fun updateCallNotification() {
        try {
            updateCallNotificationSafely()
        } catch (error: RuntimeException) {
            // Notification channel/CallStyle errors must not tear down an active call.
            android.util.Log.e("SCallInCallService", "Call notification failed", error)
        }
    }

    private fun updateCallNotificationSafely() {
        val calls = callbackByCall.keys.toList()
        val index = primaryCallNotificationIndex(calls.map { it.state })
        if (index == null) {
            flipToSilence.stop()
            // Do not post an "ongoing" notification for STATE_NEW: a later
            // update to RINGING might not trigger a fresh full-screen alert.
            // Likewise do not let a disconnected callback hide a live call.
            cancelCallNotifications()
            return
        }
        flipToSilence.update(
            calls[index],
            getSharedPreferences("s_call_settings", MODE_PRIVATE)
                .getBoolean("flip_silence", false)
        )
        showCallNotification(calls[index])
    }

    private fun showCallNotification(call: Call) {
        val incoming = call.state == Call.STATE_RINGING
        callerAnnouncer.onIncoming(
            call,
            contactNames[call],
            getSharedPreferences("s_call_settings", MODE_PRIVATE)
                .getBoolean("announce_caller", false)
        )
        val caller = contactNames[call]
            ?: call.details.callerDisplayName?.toString()
                ?.takeIf { it.isNotBlank() }
            ?: call.details.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() }
            ?: "Unknown caller"
        val signature = CallAlertSignature(System.identityHashCode(call), call.state, caller)
        // The state rechecker may call this several times for one ringing
        // event. Post only on a genuine state/identity/name change.
        if (!needsCallNotificationUpdate(lastPostedAlert, signature)) return
        val uiPending = PendingIntent.getActivity(
            this,
            110,
            CallUiActivity.createIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Direct Activity PendingIntent: Android 12+ blocks BroadcastReceiver
        // notification trampolines. The in-call Activity handles answering and
        // immediately renders the real Telecom call state.
        val answerIntent = PendingIntent.getActivity(
            this, 112, CallUiActivity.createAnswerIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val declineIntent = actionPending("REJECT", 111)
        val hangUpIntent = actionPending("END", 113)
        val person = Person.Builder().setName(caller).setImportant(true).build()

        val builder = NotificationCompat.Builder(this, IncomingCallVisibility.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle(if (incoming) "Incoming call • $caller" else "Call in progress")
            .setContentText(if (incoming) "Answer or decline" else caller)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(uiPending)
            .setOngoing(true)
            // New incoming calls get their own notification ID, so their
            // first RINGING notification is a fresh heads-up / FSI.
            // State/name refreshes must not restart repeated alerts.
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)

        if (incoming) {
            // RINGING must show who is calling and Answer/Reject, even when an
            // OEM's CallStyle implementation fails. Keep the FSI on both paths.
            try {
                builder.setStyle(
                    NotificationCompat.CallStyle.forIncomingCall(
                        person, declineIntent, answerIntent
                    )
                )
            } catch (error: RuntimeException) {
                android.util.Log.w("SCallInCallService",
                    "Incoming CallStyle not supported; using action notification", error)
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel, "Decline", declineIntent
                )
                builder.addAction(android.R.drawable.sym_action_call, "Answer", answerIntent)
            }
            builder.setFullScreenIntent(uiPending, true)
        } else {
            try {
                builder.setStyle(NotificationCompat.CallStyle.forOngoingCall(person, hangUpIntent))
            } catch (error: RuntimeException) {
                android.util.Log.w("SCallInCallService", "Ongoing CallStyle not supported", error)
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "End", hangUpIntent)
            }
        }

        val permissionGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (permissionGranted) {
            try {
                val notifications = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                val notificationId = if (incoming) INCOMING_NOTIFICATION_ID
                    else ONGOING_NOTIFICATION_ID
                val previousId = if (incoming) ONGOING_NOTIFICATION_ID
                    else INCOMING_NOTIFICATION_ID
                // Switching ongoing -> ringing must create a fresh incoming
                // notification, instead of mutating an already-visible one.
                notifications.cancel(previousId)
                notifications.notify(notificationId, builder.build())
                lastPostedAlert = signature
            } catch (error: SecurityException) {
                android.util.Log.w("SCallInCallService", "Call alerts permission blocked", error)
            }
        } else {
            android.util.Log.w("SCallInCallService",
                "Incoming call not shown: enable Notifications and Full-screen alerts for S Caller")
        }
    }

    private fun actionPending(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            this, requestCode,
            Intent(this, CallActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
