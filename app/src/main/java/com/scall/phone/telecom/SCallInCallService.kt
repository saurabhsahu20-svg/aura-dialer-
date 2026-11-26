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
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import androidx.annotation.RequiresApi
import android.telecom.InCallService
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.scall.phone.R

/**
 * Incoming calls belong to Android Telecom. Display an urgent CallStyle notification,
 * so Android can show a heads-up on an unlocked phone or a full-screen incoming
 * call surface when locked, subject to the user's notification/FSI settings.
 *
 * IMPORTANT: onCallAdded can arrive BEFORE STATE_RINGING. Always refresh when the
 * Telecom state changes; otherwise the phone may ring without any call alert UI.
 */
class SCallInCallService : InCallService() {
    companion object {
        private const val INCOMING_NOTIFICATION_ID = 4102
        private const val ONGOING_NOTIFICATION_ID = 4103
    }

    private val callbackByCall = linkedMapOf<Call, Call.Callback>()
    private val stateCheckHandler = Handler(Looper.getMainLooper())

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
        recheckIncomingState(call)
    }

    override fun onCallRemoved(call: Call) {
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
            // Do not post an "ongoing" notification for STATE_NEW: a later
            // update to RINGING might not trigger a fresh full-screen alert.
            // Likewise do not let a disconnected callback hide a live call.
            cancelCallNotifications()
            return
        }
        showCallNotification(calls[index])
    }

    private fun showCallNotification(call: Call) {
        val incoming = call.state == Call.STATE_RINGING
        val caller = call.details.callerDisplayName?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: call.details.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() }
            ?: "Unknown caller"
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
            // CallStyle ranks real phone calls prominently on Android 12+ and
            // provides accessible answer/decline actions on older versions.
            builder.setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    person, declineIntent, answerIntent
                )
            )
            builder.setFullScreenIntent(uiPending, true)
        } else {
            builder.setStyle(NotificationCompat.CallStyle.forOngoingCall(person, hangUpIntent))
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
