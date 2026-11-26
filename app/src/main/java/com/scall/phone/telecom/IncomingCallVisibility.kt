package com.scall.phone.telecom

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.scall.phone.R

/**
 * Android 14+ allows users to disable Full Screen Intent independently of
 * notifications. Do not attempt to bypass this user/system choice.
 */
internal enum class IncomingCallAccess {
    NEED_DEFAULT_DIALER,
    NEED_NOTIFICATION_PERMISSION,
    NEED_NOTIFICATION_CHANNEL,
    NEED_FULL_SCREEN_ACCESS,
    READY
}

internal fun resolveIncomingCallAccess(
    defaultDialer: Boolean,
    notificationsAllowed: Boolean,
    channelAllowed: Boolean,
    fullScreenAllowed: Boolean
): IncomingCallAccess = when {
    !defaultDialer -> IncomingCallAccess.NEED_DEFAULT_DIALER
    !notificationsAllowed -> IncomingCallAccess.NEED_NOTIFICATION_PERMISSION
    !channelAllowed -> IncomingCallAccess.NEED_NOTIFICATION_CHANNEL
    !fullScreenAllowed -> IncomingCallAccess.NEED_FULL_SCREEN_ACCESS
    else -> IncomingCallAccess.READY
}

internal object IncomingCallVisibility {
    const val CHANNEL_ID = "s_call_phone_calls"

    /** Create the notification channel on initial app launch, not just when
     * Telecom first binds to the service. This makes disabled call alerts
     * visible in Settings before the next missed call.
     * Existing user's channel importance is never raised or overridden.
     */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.incoming_calls_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.incoming_calls_description)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    fun access(context: Context, defaultDialer: Boolean): IncomingCallAccess {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val permitted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val notificationAllowed = permitted && NotificationManagerCompat.from(context)
            .areNotificationsEnabled()
        // Android 6 and 7 do not have notification channels at all.
        val channelAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            (notificationManager.getNotificationChannel(CHANNEL_ID)?.importance?.let {
                // IMPORTANCE_LOW/DEFAULT may allow sound/vibration but not
                // a heads-up call banner. Ringing calls need HIGH or above.
                it >= NotificationManager.IMPORTANCE_HIGH
            } == true)
        val fullScreenAllowed = Build.VERSION.SDK_INT < 34 ||
            notificationManager.canUseFullScreenIntent()
        return resolveIncomingCallAccess(
            defaultDialer,
            notificationAllowed,
            channelAllowed,
            fullScreenAllowed
        )
    }

    fun openSettings(context: Context, access: IncomingCallAccess) {
        val intent = when (access) {
            IncomingCallAccess.NEED_FULL_SCREEN_ACCESS -> if (Build.VERSION.SDK_INT >= 34) {
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }

            IncomingCallAccess.NEED_NOTIFICATION_CHANNEL ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
                    }
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                }

            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            }
        }
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
