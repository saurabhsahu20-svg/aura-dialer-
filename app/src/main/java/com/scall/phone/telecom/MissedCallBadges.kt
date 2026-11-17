package com.scall.phone.telecom

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CallLog
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.scall.phone.MainActivity
import com.scall.phone.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The Android call log, not an incrementing preference counter, is the source of truth.
 * This avoids duplicates after process death, multiple Telecom callbacks and restarts.
 * Launcher badge shape/colour are controlled by the user's launcher.
 */
internal object MissedCallBadges {
    private const val CHANNEL = "scall_missed_calls"
    private const val NOTIFICATION = 4103
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableCount = MutableStateFlow(0)
    val unreadCount = mutableCount.asStateFlow()

    fun refresh(context: Context) {
        val app = context.applicationContext
        scope.launch {
            if (ContextCompat.checkSelfPermission(app, Manifest.permission.READ_CALL_LOG) !=
                PackageManager.PERMISSION_GRANTED) {
                mutableCount.value = 0
                clearNotification(app)
                return@launch
            }
            // READ_CALL_LOG access and the default dialer role are intentionally checked
            // independently: Android or the user can revoke either at any time.
            try {
                val count = app.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(CallLog.Calls._ID),
                    "${CallLog.Calls.TYPE}=? AND ${CallLog.Calls.IS_READ}=?",
                    arrayOf(CallLog.Calls.MISSED_TYPE.toString(), "0"),
                    null
                )?.use { cursor -> cursor.count } ?: return@launch
                mutableCount.value = count
                publish(app, count)
            } catch (error: Exception) {
                Log.w("SCallBadges", "Missed call count temporarily unavailable", error)
                // Do not replace a known count with a guessed one.
            }
        }
    }

    private fun clearNotification(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
            ?.cancel(NOTIFICATION)
    }

    private fun publish(context: Context, count: Int) {
        if (count <= 0) {
            clearNotification(context)
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED) return
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL, "Missed calls", NotificationManager.IMPORTANCE_DEFAULT)
                        .apply { setShowBadge(true) }
                )
            }
            val launch = PendingIntent.getActivity(
                context, 140, Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_call)
                .setContentTitle(if (count == 1) "1 missed call" else "$count missed calls")
                .setContentText("Open S Caller to review missed calls")
                .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
                .setContentIntent(launch)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setNumber(count)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .build()
            manager.notify(NOTIFICATION, notification)
        } catch (error: Exception) {
            Log.w("SCallBadges", "Missed call notification not available", error)
        }
    }
}
