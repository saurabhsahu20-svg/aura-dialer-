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
}
