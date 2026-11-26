package com.scall.phone.telecom

import android.app.NotificationManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingCallChannelPriorityTest {
    @Test fun lowOrDefaultCannotReliablyShowIncomingCallBanner() {
        assertFalse(incomingCallChannelCanHeadsUp(null))
        assertFalse(incomingCallChannelCanHeadsUp(NotificationManager.IMPORTANCE_NONE))
        assertFalse(incomingCallChannelCanHeadsUp(NotificationManager.IMPORTANCE_LOW))
        assertFalse(incomingCallChannelCanHeadsUp(NotificationManager.IMPORTANCE_DEFAULT))
    }

    @Test fun highImportanceIsRequiredForCallHeadsUp() {
        assertTrue(incomingCallChannelCanHeadsUp(NotificationManager.IMPORTANCE_HIGH))
        assertTrue(incomingCallChannelCanHeadsUp(NotificationManager.IMPORTANCE_MAX))
    }

    @Test fun missingNotificationPermissionOrFullScreenAccessAreStillReported() {
        assertTrue(resolveIncomingCallAccess(true,true,false,true) ==
            IncomingCallAccess.NEED_NOTIFICATION_CHANNEL)
        assertTrue(resolveIncomingCallAccess(true,true,true,false) ==
            IncomingCallAccess.NEED_FULL_SCREEN_ACCESS)
        assertTrue(resolveIncomingCallAccess(true,true,true,true) ==
            IncomingCallAccess.READY)
    }
}
