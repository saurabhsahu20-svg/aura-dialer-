package com.scall.phone.data

import android.provider.CallLog
import org.junit.Assert.assertEquals
import org.junit.Test

class MissedCallBadgeTest {
    private fun call(
        id: Long, whenMillis: Long, type: Int = CallLog.Calls.MISSED_TYPE
    ) = RecentEntry(
        id = id, number = "9876543210", name = "Contact",
        type = type, timestamp = whenMillis, duration = 0L, photo = null
    )

    @Test fun lifetimeHistoryIsNotANewMissedAlert() {
        val old = listOf(call(1, 1000L), call(2, 2000L), call(3, 3000L))
        val baseline = latestMissedCallCursor(old)!!
        assertEquals(0, countNewMissedCalls(old, baseline))
    }

    @Test fun countsOnlyCallsAfterViewedMarker() {
        val seen = MissedCallCursor(2000L, 2L)
        val calls = listOf(call(1, 1000L), call(2, 2000L), call(3, 3000L))
        assertEquals(1, countNewMissedCalls(calls, seen))
        assertEquals(0, countNewMissedCalls(calls, latestMissedCallCursor(calls)!!))
    }

    @Test fun ignoresNewIncomingOutgoingAndRejectedCalls() {
        val seen = MissedCallCursor(2000L, 2L)
        val calls = listOf(
            call(3, 3000L, CallLog.Calls.INCOMING_TYPE),
            call(4, 4000L, CallLog.Calls.OUTGOING_TYPE),
            call(5, 5000L, CallLog.Calls.REJECTED_TYPE)
        )
        assertEquals(0, countNewMissedCalls(calls, seen))
    }

    @Test fun handlesTiedTimestampsWithUniqueCallLogIds() {
        val seen = MissedCallCursor(2000L, 2L)
        assertEquals(
            1, countNewMissedCalls(listOf(call(1, 2000L), call(2, 2000L),
                call(3, 2000L)), seen)
        )
    }

    @Test fun emptyCallLogHasNoPhantomMissedCalls() {
        assertEquals(null, latestMissedCallCursor(emptyList()))
        assertEquals(0, countNewMissedCalls(emptyList(), MissedCallCursor(0, 0)))
    }

    @Test fun latestMissedMarkerIgnoresNewerAnsweredCall() {
        val calls = listOf(call(2, 2500L), call(4, 4000L, CallLog.Calls.INCOMING_TYPE))
        assertEquals(MissedCallCursor(2500L, 2L), latestMissedCallCursor(calls))
    }

    @Test fun deletingHistoricalRowsCannotReopenViewedNotifications() {
        val seen = MissedCallCursor(8000L, 20L)
        assertEquals(0, countNewMissedCalls(listOf(call(5, 2000L)), seen))
    }
}
