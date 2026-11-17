package com.scall.phone.telecom

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
        private const val NOTIFICATION_ID = 4102
    }

    private val callbackByCall = linkedMapOf<Call, Call.Callback>()

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
        MissedCallBadges.refresh(this)
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
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            MissedCallBadges.refresh(applicationContext)
        }, 2000L)
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
        try {
            CallBridge.unbind(this)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
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
        val primary = callbackByCall.keys.firstOrNull { it.state == Call.STATE_RINGING }
            ?: callbackByCall.keys.firstOrNull { it.state == Call.STATE_ACTIVE }
            ?: callbackByCall.keys.firstOrNull()
        if (primary == null || primary.state == Call.STATE_DISCONNECTED) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
            return
        }
        showCallNotification(primary)
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
            .setOnlyAlertOnce(!incoming)
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
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(NOTIFICATION_ID, builder.build())
            } catch (_: SecurityException) {
                // Surface permissions/status in the app's Incoming call display settings.
            }
        }
    }

    private fun actionPending(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            this, requestCode,
            Intent(this, CallActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
