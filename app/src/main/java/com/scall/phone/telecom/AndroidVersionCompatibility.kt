package com.scall.phone.telecom

/** Android Telecom in-call UI is available from Android 6.0 (API 23). */
internal fun supportsInCallService(sdk: Int): Boolean = sdk >= 23

/** NotificationChannel is available only from Android 8.0 (API 26). */
internal fun usesNotificationChannels(sdk: Int): Boolean = sdk >= 26
