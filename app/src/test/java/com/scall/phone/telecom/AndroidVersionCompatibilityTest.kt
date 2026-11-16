package com.scall.phone.telecom

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Older Android supports direct high priority call alerts without channels. */
class AndroidVersionCompatibilityTest {
    @Test fun android6And7DoNotRequireChannels() {
        assertFalse(usesNotificationChannels(23))
        assertFalse(usesNotificationChannels(24))
        assertFalse(usesNotificationChannels(25))
    }
    @Test fun android8AndNewerUseChannels() {
        assertTrue(usesNotificationChannels(26))
        assertTrue(usesNotificationChannels(35))
    }
    @Test fun telecomRequiresAndroid6() {
        assertFalse(supportsInCallService(22))
        assertTrue(supportsInCallService(23))
        assertTrue(supportsInCallService(35))
    }
}
