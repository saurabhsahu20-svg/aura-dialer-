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
import android.telecom.InCallService
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.scall.phone.R

/** The platform binds this service only when S call holds the default phone role. */
class SCallInCallService : InCallService() {
    companion object {
        private const val CHANNEL_ID = "s_call_phone_calls"
        private const val NOTIFICATION_ID = 4102
    }

    override fun onCreate() {
        super.onCreate()
        CallBridge.bind(this)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_ID, getString(R.string.incoming_calls_channel), NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.incoming_calls_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        })
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallBridge.addCall(call)
        showCallNotification(call)
    }

    override fun onCallRemoved(call: Call) {
        CallBridge.removeCall(call)
        if (CallBridge.current.value == null) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
        }
        super.onCallRemoved(call)
    }

    override fun onBringToForeground(showDialpad: Boolean) {
        super.onBringToForeground(showDialpad)
        // Telecom explicitly requests that we show the active call UI.
        startActivity(CallUiActivity.createIntent(this))
    }

    override fun onCanAddCallChanged(canAddCall: Boolean) {
        super.onCanAddCallChanged(canAddCall)
        CallBridge.refresh()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        CallBridge.refresh()
    }

    override fun onDestroy() {
        CallBridge.unbind(this)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    private fun showCallNotification(call: Call) {
        val uiIntent = CallUiActivity.createIntent(this)
        val uiPending = PendingIntent.getActivity(
            this, 110, uiIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val incoming = call.state == Call.STATE_RINGING
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (incoming) "Incoming call" else "Call in progress")
            .setContentText(call.details.callerDisplayName?.toString()
                ?: call.details.handle?.schemeSpecificPart ?: "Phone call")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(uiPending)
            .setOngoing(true)
            .setOnlyAlertOnce(!incoming)
        if (incoming) {
            builder.setFullScreenIntent(uiPending, true)
            builder.addAction(0, "Decline", actionPending("REJECT", 111))
            builder.addAction(0, "Answer", actionPending("ANSWER", 112))
        } else {
            builder.addAction(0, "End call", actionPending("END", 113))
        }
        val permissionGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (permissionGranted) {
            try {
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(NOTIFICATION_ID, builder.build())
            } catch (_: SecurityException) { /* Permission can be revoked at any moment. */ }
        }
    }

    private fun actionPending(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            this, requestCode,
            Intent(this, CallActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
