package com.scall.phone.ui

import android.provider.CallLog
import com.scall.phone.data.RecentEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class CallDetailsTest {
    private fun call(
        id: Long, number: String, whenMillis: Long,
        type: Int = CallLog.Calls.INCOMING_TYPE, duration: Long = 0L
    ) = RecentEntry(
        id = id, number = number, name = "Saved contact", type = type,
        timestamp = whenMillis, duration = duration, photo = null
    )

    @Test fun nationalAndIndianCountryCodeHaveCompleteSharedTimeline() {
        val selected = call(2, "98765 43210", 2000, CallLog.Calls.OUTGOING_TYPE, 45L)
        val calls = listOf(
            call(1, "+91 98765 43210", 1000),
            selected,
            call(3, "09876543210", 3000, CallLog.Calls.MISSED_TYPE),
            call(4, "8876543210", 4000),
            call(5, "+44 98765 43210", 5000)
        )
        assertEquals(listOf(3L, 2L, 1L),
            callsForSameNumber(calls, selected).map { it.id })
    }

    @Test fun privateAndUnknownNumbersMustNotBeMerged() {
        val a = call(1, "", 1000)
        val b = call(2, "", 2000)
        assertEquals(listOf(a), callsForSameNumber(listOf(a, b), a))
    }

    @Test fun differentInternationalPrefixesAreDifferentCallers() {
        val uk = call(1, "+44 1234567890", 1000)
        val france = call(2, "+33 1234567890", 2000)
        assertEquals(listOf(uk), callsForSameNumber(listOf(uk, france), uk))
    }

    @Test fun typesClearlyDistinguishIncomingOutgoingAndMissed() {
        assertEquals("Incoming call", preciseCallTypeLabel(CallLog.Calls.INCOMING_TYPE))
        assertEquals("Outgoing call", preciseCallTypeLabel(CallLog.Calls.OUTGOING_TYPE))
        assertEquals("Missed call", preciseCallTypeLabel(CallLog.Calls.MISSED_TYPE))
        assertEquals("Rejected call", preciseCallTypeLabel(CallLog.Calls.REJECTED_TYPE))
        assertEquals("Blocked call", preciseCallTypeLabel(CallLog.Calls.BLOCKED_TYPE))
    }

    @Test fun estimatedEndUsesDurationSecondsNotMinutes() {
        assertEquals(101000L, estimatedCallEndMillis(1000L, 100L))
        assertNull(estimatedCallEndMillis(1000L, 0L))
        assertNull(estimatedCallEndMillis(1000L, -1L))
        assertNull(estimatedCallEndMillis(Long.MAX_VALUE - 1, 500L))
    }

    @Test fun timestampsShowFullDateAndSecondsInDeviceTimezone() {
        val result = formatCallLogDateTime(
            0L, Locale.US, TimeZone.getTimeZone("GMT")
        )
        assertEquals("Thu, 01 Jan 1970 • 12:00:00 AM", result)
    }

    @Test fun repeatedCallsAppearAsSeparateTimelineEvents() {
        val first = call(3, "9822222222", 8000, CallLog.Calls.MISSED_TYPE)
        val second = call(4, "9822222222", 9000, CallLog.Calls.OUTGOING_TYPE, 120L)
        val related = callsForSameNumber(listOf(first, second), first)
        assertEquals(2, related.size)
        assertEquals(listOf(4L, 3L), related.map { it.id })
    }
}
