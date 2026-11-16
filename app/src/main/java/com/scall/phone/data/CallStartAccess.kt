package com.scall.phone.data

/** Keep call permissions distinct from optional SIM-selection access. */
internal enum class CallStartAccess {
    NEED_DEFAULT_DIALER,
    NEED_CALL_PERMISSION,
    USE_DEFAULT_SIM,
    SIM_SELECTION_AVAILABLE
}

internal fun callStartAccess(
    defaultDialer: Boolean,
    canPlaceCall: Boolean,
    canReadPhoneState: Boolean
): CallStartAccess = when {
    !defaultDialer -> CallStartAccess.NEED_DEFAULT_DIALER
    !canPlaceCall -> CallStartAccess.NEED_CALL_PERMISSION
    !canReadPhoneState -> CallStartAccess.USE_DEFAULT_SIM
    else -> CallStartAccess.SIM_SELECTION_AVAILABLE
}
