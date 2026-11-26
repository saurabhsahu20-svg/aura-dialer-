package com.scall.phone.telecom

import android.telecom.Call

/**
 * Incoming calls must not be posted as "ongoing" while Telecom is still
 * initializing the Call. Posting the wrong type under the same notification
 * ID can prevent the later incoming FullScreenIntent from surfacing.
 */
internal enum class VisibleCallAlert { INCOMING, ONGOING, NOT_READY }

internal fun visibleCallAlert(state: Int): VisibleCallAlert = when (state) {
    Call.STATE_RINGING -> VisibleCallAlert.INCOMING
    Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_CONNECTING,
    Call.STATE_HOLDING -> VisibleCallAlert.ONGOING
    else -> VisibleCallAlert.NOT_READY
}

/** Ringing calls always override an existing ongoing call notification. */
internal fun primaryCallNotificationIndex(states: List<Int>): Int? {
    val incoming = states.indexOfFirst { visibleCallAlert(it) == VisibleCallAlert.INCOMING }
    if (incoming >= 0) return incoming
    val ongoing = states.indexOfFirst { visibleCallAlert(it) == VisibleCallAlert.ONGOING }
    return ongoing.takeIf { it >= 0 }
}
