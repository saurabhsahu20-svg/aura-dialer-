package com.scall.phone.ui

import android.provider.CallLog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scall.phone.data.CallHistoryStore
import com.scall.phone.data.RecentEntry
import com.scall.phone.data.contactLookupKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Call-log DATE is the event's recorded timestamp; it is not guaranteed to
 * be the exact second the remote party picked up a ringing cellular call.
 */
internal fun formatCallLogDateTime(
    timestamp: Long,
    locale: Locale = Locale.getDefault(),
    zone: TimeZone = TimeZone.getDefault()
): String = SimpleDateFormat("EEE, dd MMM yyyy • hh:mm:ss a", locale).apply {
    timeZone = zone
}.format(Date(timestamp))

/**
 * Estimated end = recorded event timestamp + provider duration.
 * Android CallLog duration is seconds; the provider does not supply the
 * exact separate connected-at / ended-at timestamps for regular calls.
 */
internal fun estimatedCallEndMillis(timestamp: Long, seconds: Long): Long? {
    if (timestamp < 0L || seconds <= 0L || seconds > Long.MAX_VALUE / 1000L) return null
    val delta = seconds * 1000L
    if (timestamp > Long.MAX_VALUE - delta) return null
    return timestamp + delta
}

internal fun preciseCallTypeLabel(type: Int): String = when (type) {
    CallLog.Calls.INCOMING_TYPE -> "Incoming call"
    CallLog.Calls.OUTGOING_TYPE -> "Outgoing call"
    CallLog.Calls.MISSED_TYPE -> "Missed call"
    CallLog.Calls.REJECTED_TYPE -> "Rejected call"
    CallLog.Calls.BLOCKED_TYPE -> "Blocked call"
    CallLog.Calls.VOICEMAIL_TYPE -> "Voicemail"
    CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> "Answered on another device"
    else -> "Other call"
}

/**
 * Group by the whole normalized phone number, not just the last few digits.
 * Unknown/private calls have no dependable identity and must never be
 * combined as if they came from the same person.
 */
internal fun callsForSameNumber(calls: List<RecentEntry>, selected: RecentEntry): List<RecentEntry> {
    val key = contactLookupKey(selected.number) ?: return listOf(selected)
    return calls.asSequence()
        .filter { contactLookupKey(it.number) == key }
        .sortedWith(compareByDescending<RecentEntry> { it.timestamp }.thenByDescending { it.id })
        .toList()
}

@Composable
internal fun CallDetailsScreen(
    selected: RecentEntry,
    relatedCalls: List<RecentEntry>,
    note: String,
    title: String?,
    photo: String?,
    onSelectCall: (RecentEntry) -> Unit,
    onCall: (String) -> Unit,
    onMessage: (String) -> Unit
) {
    val locale = Locale.getDefault()
    val zone = TimeZone.getDefault()
    val history = remember(relatedCalls, selected.id) {
        relatedCalls.sortedWith(compareByDescending<RecentEntry> { it.timestamp }
            .thenByDescending { it.id })
    }
    val inCount = remember(history) {
        history.count { it.type == CallLog.Calls.INCOMING_TYPE }
    }
    val outCount = remember(history) {
        history.count { it.type == CallLog.Calls.OUTGOING_TYPE }
    }
    val missedCount = remember(history) {
        history.count { it.type == CallLog.Calls.MISSED_TYPE }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ContactAvatar(title?.ifBlank { selected.name } ?: selected.name, photo, size = 72.dp)
                Spacer(Modifier.height(12.dp))
                Text(title?.ifBlank { selected.name } ?: selected.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Text(selected.number.ifBlank { "Private / unknown number" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { onCall(selected.number) },
                        enabled = selected.number.isNotBlank(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Call")
                    }
                    OutlinedButton(
                        onClick = { onMessage(selected.number) },
                        enabled = selected.number.isNotBlank(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Message, contentDescription = null,
                            modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Message")
                    }
                }
            }
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CallDirectionSymbol(classifyCallDirection(selected.type),
                            modifier = Modifier.size(22.dp),
                            description = preciseCallTypeLabel(selected.type))
                        Text(preciseCallTypeLabel(selected.type),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                    }
                    CallDetailsField("Date and time", formatCallLogDateTime(selected.timestamp, locale, zone))
                    CallDetailsField("Call duration",
                        if (selected.duration <= 0L) "0 seconds / not recorded"
                        else formatCallDuration(selected.duration))
                    estimatedCallEndMillis(selected.timestamp, selected.duration)?.let { approximateEnd ->
                        CallDetailsField("Approximate end time",
                            formatCallLogDateTime(approximateEnd, locale, zone))
                    }
                    if (note.isNotBlank()) CallDetailsField("Your call note", note)
                    Text(
                        "Times come from Android's call history. End time is estimated from " +
                            "the recorded time and duration; exact answer/end moments may differ.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Column(Modifier.padding(top = 8.dp, bottom = 3.dp)) {
                Text("Complete call history • ${history.size}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Text("$inCount incoming  ·  $outCount outgoing  ·  $missedCount missed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        items(history, key = { it.id }) { entry ->
            val current = entry.id == selected.id
            Surface(
                modifier = Modifier.fillMaxWidth()
                    .clickable(enabled = !current) { onSelectCall(entry) },
                shape = RoundedCornerShape(15.dp),
                color = if (current) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.padding(13.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CallDirectionSymbol(classifyCallDirection(entry.type),
                        modifier = Modifier.size(24.dp),
                        description = preciseCallTypeLabel(entry.type))
                    Column(Modifier.weight(1f)) {
                        Text(preciseCallTypeLabel(entry.type),
                            fontWeight = if (current) FontWeight.Bold else FontWeight.SemiBold)
                        Text(formatCallLogDateTime(entry.timestamp, locale, zone),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (entry.duration > 0L) formatCallDuration(entry.duration)
                            else "0 seconds / not recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (current) Text("Selected", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun CallDetailsField(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold)
    }
}
