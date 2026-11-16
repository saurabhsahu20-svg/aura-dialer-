package com.scall.phone.telecom

import org.junit.Assert.assertEquals
import org.junit.Test

class IncomingCallAccessTest {
    @Test fun defaultDialerMustBeEnabled() {
        assertEquals(
            IncomingCallAccess.NEED_DEFAULT_DIALER,
            resolveIncomingCallAccess(false, true, true, true)
        )
    }

    @Test fun notificationPermissionMustBeAllowed() {
        assertEquals(
            IncomingCallAccess.NEED_NOTIFICATION_PERMISSION,
            resolveIncomingCallAccess(true, false, true, true)
        )
    }

    @Test fun disabledCallChannelRequiresSettings() {
        assertEquals(
            IncomingCallAccess.NEED_NOTIFICATION_CHANNEL,
            resolveIncomingCallAccess(true, true, false, true)
        )
    }

    @Test fun android14FullScreenAccessRequiresUserApproval() {
        assertEquals(
            IncomingCallAccess.NEED_FULL_SCREEN_ACCESS,
            resolveIncomingCallAccess(true, true, true, false)
        )
    }

    @Test fun allPermissionsReady() {
        assertEquals(
            IncomingCallAccess.READY,
            resolveIncomingCallAccess(true, true, true, true)
        )
    }
}
