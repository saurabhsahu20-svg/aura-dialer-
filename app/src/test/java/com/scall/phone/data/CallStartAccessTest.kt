package com.scall.phone.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CallStartAccessTest {
    @Test fun notDefaultDialerNeedsRoleBeforeTryingToCall() {
        assertEquals(CallStartAccess.NEED_DEFAULT_DIALER, callStartAccess(false, true, true))
    }

    @Test fun callPermissionRequiredEvenWhenPhoneStateIsAllowed() {
        assertEquals(CallStartAccess.NEED_CALL_PERMISSION, callStartAccess(true, false, true))
    }

    @Test fun phoneStatePermissionIsOptionalWhenPlacingACall() {
        assertEquals(CallStartAccess.USE_DEFAULT_SIM, callStartAccess(true, true, false))
    }

    @Test fun simChoicesAvailableWhenAllRequiredAccessGranted() {
        assertEquals(CallStartAccess.SIM_SELECTION_AVAILABLE, callStartAccess(true, true, true))
    }
}
