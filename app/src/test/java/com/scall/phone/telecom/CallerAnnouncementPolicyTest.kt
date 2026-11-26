package com.scall.phone.telecom

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallerAnnouncementPolicyTest {
    @Test fun disabledOrUnknownCallerDoesNotSpeak() {
        assertFalse(shouldAnnounceSavedCaller(false, true, "Amit", false))
        assertFalse(shouldAnnounceSavedCaller(true, true, null, false))
        assertFalse(shouldAnnounceSavedCaller(true, true, "  ", false))
    }

    @Test fun onlyRingingSavedNamesSpeakOnce() {
        assertTrue(shouldAnnounceSavedCaller(true, true, "Amit", false))
        assertFalse(shouldAnnounceSavedCaller(true, false, "Amit", false))
        assertFalse(shouldAnnounceSavedCaller(true, true, "Amit", true))
    }
}
