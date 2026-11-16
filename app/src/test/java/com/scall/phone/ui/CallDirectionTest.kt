package com.scall.phone.ui

import android.provider.CallLog
import org.junit.Assert.assertEquals
import org.junit.Test

class CallDirectionTest {
    @Test fun incomingIsRecognized() {
        assertEquals(CallDirection.INCOMING,
            classifyCallDirection(CallLog.Calls.INCOMING_TYPE))
    }
    @Test fun outgoingIsRecognized() {
        assertEquals(CallDirection.OUTGOING,
            classifyCallDirection(CallLog.Calls.OUTGOING_TYPE))
    }
    @Test fun missedIsRecognized() {
        assertEquals(CallDirection.MISSED,
            classifyCallDirection(CallLog.Calls.MISSED_TYPE))
    }
    @Test fun rejectedIsNotFalselyMarkedMissed() {
        assertEquals(CallDirection.OTHER,
            classifyCallDirection(CallLog.Calls.REJECTED_TYPE))
    }
    @Test fun unknownIsOther() {
        assertEquals(CallDirection.OTHER, classifyCallDirection(101))
    }
    @Test fun durationFormatsCorrectly() {
        assertEquals("23s", formatCallDuration(23))
        assertEquals("2m 5s", formatCallDuration(125))
        assertEquals("1h 2m", formatCallDuration(3723))
    }

    /** WCAG-like relative luminance check against white for Light Theme. */
    private fun contrastOnWhite(color: androidx.compose.ui.graphics.Color): Double {
        fun channel(value: Float): Double {
            val x = value.toDouble()
            return if (x <= .04045) x / 12.92 else Math.pow((x + .055) / 1.055, 2.4)
        }
        val lum = .2126 * channel(color.red) +
            .7152 * channel(color.green) + .0722 * channel(color.blue)
        return 1.05 / (lum + 0.05)
    }

    @Test fun lightThemeCallColorsHaveStrongWhiteContrast() {
        for (direction in listOf(CallDirection.INCOMING, CallDirection.OUTGOING, CallDirection.MISSED)) {
            val contrast = contrastOnWhite(direction.strongColor(false))
            org.junit.Assert.assertTrue("${direction.label} contrast $contrast is too low", contrast >= 6.0)
        }
    }

    @Test fun distinctSymbolsUseDistinctColorsInBothThemes() {
        val directions = listOf(CallDirection.INCOMING, CallDirection.OUTGOING, CallDirection.MISSED)
        org.junit.Assert.assertEquals(3, directions.map { it.strongColor(false) }.distinct().size)
        org.junit.Assert.assertEquals(3, directions.map { it.strongColor(true) }.distinct().size)
    }

}
