package com.scall.phone.telecom

import android.telecom.Call

/**
 * Priority for End Call when Telecom supplies multiple simultaneous calls.
 * Active call takes precedence over a new waiting call, ensuring the red
 * end button hangs up the conversation rather than a waiting caller.
 */
internal fun endCallPriority(state: Int): Int = when (state) {
    Call.STATE_ACTIVE -> 0
    Call.STATE_DIALING, Call.STATE_CONNECTING -> 1
    Call.STATE_HOLDING -> 2
    Call.STATE_RINGING -> 3
    Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> 99
    else -> 4
}
