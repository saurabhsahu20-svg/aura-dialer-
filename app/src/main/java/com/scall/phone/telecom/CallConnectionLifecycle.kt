package com.scall.phone.telecom

/** Reference identity, not equality, prevents a stale service from clearing the new call. */
internal fun isCurrentTelecomBinding(current: Any?, detaching: Any): Boolean =
    current === detaching
