package com.scall.phone.ui

import android.provider.CallLog
import com.scall.phone.data.RecentEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class HistoryGroupingTest {
    private fun atDay(day: Int, hour: Int): Long =
        Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, day, hour, 10, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun call(id: Long, number: String, type: Int, day: Int = 20, hour: Int = 13) =
        RecentEntry(id, number, number, type, atDay(day, hour), 12L, null)

    @Test fun consecutiveSameDirectionAndDayGroupWithoutDroppingRows() {
        val input = listOf(
            call(4, "+919876543210", CallLog.Calls.INCOMING_TYPE),
            call(3, "+919876543210", CallLog.Calls.INCOMING_TYPE),
            call(2, "9999999999", CallLog.Calls.MISSED_TYPE),
            call(1, "+919876543210", CallLog.Calls.INCOMING_TYPE)
        )
        val grouped = groupConsecutiveHistory(input)
        assertEquals(listOf(2, 1, 1), grouped.map { it.count })
        assertEquals(listOf(4L, 3L, 2L, 1L), grouped.flatMap { it.entries }.map { it.id })
    }

    @Test fun differentDirectionsAndDaysStaySeparate() {
        val input = listOf(
            call(5, "8085808586", CallLog.Calls.OUTGOING_TYPE, 20),
            call(4, "8085808586", CallLog.Calls.MISSED_TYPE, 20),
            call(3, "8085808586", CallLog.Calls.MISSED_TYPE, 19),
            call(2, "8085808586", CallLog.Calls.MISSED_TYPE, 19)
        )
        assertEquals(listOf(1,1,2), groupConsecutiveHistory(input).map { it.count })
    }

    @Test fun privateNumbersCannotAccidentallyMerge() {
        assertEquals(2, groupConsecutiveHistory(listOf(
            call(2, "", CallLog.Calls.MISSED_TYPE),
            call(1, "", CallLog.Calls.MISSED_TYPE)
        )).size)
    }

    @Test fun emptyHistoryYieldsNoGroups() {
        assertEquals(emptyList<HistoryGroup>(), groupConsecutiveHistory(emptyList()))
    }
}
