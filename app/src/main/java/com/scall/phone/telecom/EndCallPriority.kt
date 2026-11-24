package com.scall.phone.telecom

import android.telecom.Call

/** Prefer the active conversation over a waiting incoming call for the red End button. */
internal fun endCallPriority(state: Int): Int = when (state) {
    Call.STATE_ACTIVE -> 0
    Call.STATE_DIALING, Call.STATE_CONNECTING -> 1
    Call.STATE_HOLDING -> 2
    Call.STATE_RINGING -> 3
    Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> 99
    else -> 4
}
