package com.scall.phone.data

import android.content.Context
import android.provider.CallLog

/**
 * Seen-through watermark for the "new missed calls" badge.
 *
 * Comparing both timestamp and row ID avoids repeatedly counting old calls
 * when the call-log provider changes its ordering or the app restarts.
 */
internal data class MissedCallCursor(val timestamp: Long, val id: Long)

internal fun latestMissedCallCursor(calls: List<RecentEntry>): MissedCallCursor? =
    calls.asSequence()
        .filter { it.type == CallLog.Calls.MISSED_TYPE }
        .map { MissedCallCursor(it.timestamp, it.id) }
        .maxWithOrNull(compareBy<MissedCallCursor>({ it.timestamp }, { it.id }))

/**
 * Count distinct NEW missed-call events, not provider refreshes or duplicated
 * call-log rows. A few OEMs can insert the same missed event twice within a
 * second, sometimes under different _IDs. Keep all rows in History, but count
 * a matching number within 1500ms once for the new-call badge.
 */
internal fun countNewMissedCalls(calls: List<RecentEntry>, seen: MissedCallCursor): Int {
    val uniqueIds = HashSet<Long>()
    val latestByNumber = HashMap<String, Long>()
    var count = 0
    val missed = calls.asSequence()
        .filter { it.type == CallLog.Calls.MISSED_TYPE &&
            (it.timestamp > seen.timestamp ||
                (it.timestamp == seen.timestamp && it.id > seen.id)) }
        .sortedWith(compareBy<RecentEntry>({ it.timestamp }, { it.id }))
    for (entry in missed) {
        if (!uniqueIds.add(entry.id)) continue
        // Unknown/private numbers must not collapse unrelated callers together.
        val digits = entry.number.filter(Char::isDigit)
        val key = if (digits.isNotEmpty()) digits.takeLast(10)
            else "private:${entry.id}"
        val previous = latestByNumber[key]
        if (previous == null || entry.timestamp - previous > 1500L) {
            count++
        }
        latestByNumber[key] = entry.timestamp
    }
    return count
}

/**
 * A private, device-local viewed marker; never deletes or alters call-log rows.
 * On first use, historical missed calls are the initial viewed baseline rather
 * than displaying an alarming lifetime missed-call total as "new" notifications.
 */
internal class MissedCallBadgeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "s_call_missed_calls", Context.MODE_PRIVATE
    )

    fun initialize(calls: List<RecentEntry>): MissedCallCursor {
        if (prefs.getBoolean("initialized", false)) return current()
        val startingPoint = latestMissedCallCursor(calls) ?: MissedCallCursor(0L, 0L)
        prefs.edit()
            .putLong("seen_timestamp", startingPoint.timestamp)
            .putLong("seen_id", startingPoint.id)
            .putBoolean("initialized", true)
            .apply()
        return startingPoint
    }

    fun markViewed(calls: List<RecentEntry>): MissedCallCursor {
        val previous = current()
        val latest = latestMissedCallCursor(calls) ?: return previous
        val next = if (latest.timestamp > previous.timestamp ||
            (latest.timestamp == previous.timestamp && latest.id > previous.id)
        ) latest else previous
        prefs.edit()
            .putLong("seen_timestamp", next.timestamp)
            .putLong("seen_id", next.id)
            .putBoolean("initialized", true)
            .apply()
        return next
    }

    private fun current(): MissedCallCursor = MissedCallCursor(
        prefs.getLong("seen_timestamp", 0L),
        prefs.getLong("seen_id", 0L)
    )
}
