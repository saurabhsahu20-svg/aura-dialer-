package com.scall.phone.ui

import android.provider.CallLog

/**
 * One unambiguous symbol for each actual Android call-log event.
 * Do not classify rejected, blocked or unknown calls as outgoing/missed.
 */
internal enum class CallDirection(val label: String) {
    INCOMING("Incoming"),
    OUTGOING("Outgoing"),
    MISSED("Missed"),
    OTHER("Other")
}

internal fun classifyCallDirection(type: Int): CallDirection = when (type) {
    CallLog.Calls.INCOMING_TYPE -> CallDirection.INCOMING
    CallLog.Calls.OUTGOING_TYPE -> CallDirection.OUTGOING
    CallLog.Calls.MISSED_TYPE -> CallDirection.MISSED
    else -> CallDirection.OTHER
}

/** System CallLog duration is in seconds. */
internal fun formatCallDuration(seconds: Long): String {
    if (seconds <= 0) return "0s"
    val mins = seconds / 60
    val secs = seconds % 60
    val hours = mins / 60
    return when {
        hours > 0 -> "${hours}h ${mins % 60}m"
        mins > 0 -> "${mins}m ${secs}s"
        else -> "${secs}s"
    }
}
