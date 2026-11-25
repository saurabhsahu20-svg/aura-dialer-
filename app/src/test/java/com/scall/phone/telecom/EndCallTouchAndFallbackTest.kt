package com.scall.phone.telecom

import android.telecom.Call
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndCallTouchAndFallbackTest {
    @Test fun deliberateTouchInsideRedHangupButtonIsRecognized() {
        val red = Rect(120f, 650f, 230f, 760f)
        assertTrue(isHangUpButtonTouch(170f, 700f, red))
        assertFalse(isHangUpButtonTouch(90f, 700f, red))
        assertFalse(isHangUpButtonTouch(170f, 600f, red))
        assertFalse(isHangUpButtonTouch(170f, 700f, null))
    }

    @Test fun fallbackOnlyTargetsOneRealOngoingCallWithPermission() {
        assertTrue(canSafelyTryTelecomEndFallback(Call.STATE_ACTIVE, 1, true))
        assertTrue(canSafelyTryTelecomEndFallback(Call.STATE_DIALING, 1, true))
        assertFalse(canSafelyTryTelecomEndFallback(Call.STATE_ACTIVE, 2, true))
        assertFalse(canSafelyTryTelecomEndFallback(Call.STATE_ACTIVE, 0, true))
        assertFalse(canSafelyTryTelecomEndFallback(Call.STATE_ACTIVE, 1, false))
        assertFalse(canSafelyTryTelecomEndFallback(Call.STATE_RINGING, 1, true))
        assertFalse(canSafelyTryTelecomEndFallback(Call.STATE_DISCONNECTED, 1, true))
    }
}
