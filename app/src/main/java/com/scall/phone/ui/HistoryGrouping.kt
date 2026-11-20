package com.scall.phone.ui

import com.scall.phone.data.RecentEntry
import java.util.Calendar

/**
 * Groups only adjacent events from the same exact phone number, same Android
 * call type, and the same LOCAL calendar day. Never silently drops log rows.
 * Selection/deletion still operates on original record IDs.
 */
internal data class HistoryGroup(val entries: List<RecentEntry>) {
    val latest: RecentEntry get() = entries.first()
    val count: Int get() = entries.size
}

internal fun groupConsecutiveHistory(calls: List<RecentEntry>): List<HistoryGroup> {
    if (calls.isEmpty()) return emptyList()
    val groups = ArrayList<HistoryGroup>(calls.size)
    val batch = ArrayList<RecentEntry>()
    fun dateKey(millis: Long): Triple<Int, Int, Int> =
        Calendar.getInstance().apply { timeInMillis = millis }
            .let { Triple(it.get(Calendar.YEAR), it.get(Calendar.DAY_OF_YEAR), it.get(Calendar.ERA)) }
    fun flush() {
        if (batch.isNotEmpty()) {
            groups.add(HistoryGroup(batch.toList()))
            batch.clear()
        }
    }
    for (call in calls) {
        val previous = batch.firstOrNull()
        if (previous != null && (
                previous.number.filter(Char::isDigit) != call.number.filter(Char::isDigit) ||
                previous.number.isBlank() || call.number.isBlank() ||
                previous.type != call.type ||
                dateKey(previous.timestamp) != dateKey(call.timestamp)
            )) flush()
        batch.add(call)
    }
    flush()
    return groups
}
