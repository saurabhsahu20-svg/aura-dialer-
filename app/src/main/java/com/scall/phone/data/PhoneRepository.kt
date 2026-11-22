package com.scall.phone.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class ContactEntry(
    val id: Long,
    val name: String,
    val number: String,
    val photo: String?,
    val favorite: Boolean,
    val allNumbers: List<String> = listOf(number),
    val nickname: String = "",
    val company: String = ""
)

internal data class RecentEntry(
    val id: Long,
    val number: String,
    val name: String,
    val type: Int,
    val timestamp: Long,
    val duration: Long,
    val photo: String?
)

/** Total comes from the provider cursor; only requested rows are materialized. */
internal data class CallHistoryPage(
    val entries: List<RecentEntry>,
    val totalCount: Int,
    val incomingCount: Int = 0,
    val outgoingCount: Int = 0,
    val missedCount: Int = 0
)


internal data class SimAccount(val handle: PhoneAccountHandle, val label: String) {
    val key: String get() = SimPreferences.accountKey(handle)
}

internal fun normalizeNumber(number: String): String = number.filter { it.isDigit() || it == '+' }

internal class PhoneRepository(private val context: Context) {
    private fun permitted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasContactsPermission() = permitted(Manifest.permission.READ_CONTACTS)
    fun hasLogsPermission() = permitted(Manifest.permission.READ_CALL_LOG)
    fun hasWriteLogsPermission() = permitted(Manifest.permission.WRITE_CALL_LOG)
    fun hasCallPermission() = permitted(Manifest.permission.CALL_PHONE)
    fun hasPhoneStatePermission() = permitted(Manifest.permission.READ_PHONE_STATE)

    fun hasCellularTelephony(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

    fun isDefaultDialer(): Boolean = try {
        val manager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        manager?.defaultDialerPackage == context.packageName
    } catch (_: RuntimeException) { false }

    suspend fun contacts(): List<ContactEntry> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) return@withContext emptyList()
        val byId = linkedMapOf<Long, ContactEntry>()
        val numbers = mutableMapOf<Long, MutableList<String>>()
        val nicknames = mutableMapOf<Long, String>()
        val companies = mutableMapOf<Long, String>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null, null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val name = cursor.getString(1).orEmpty().ifBlank { "Unknown" }
                    val number = cursor.getString(2).orEmpty()
                    if (number.isBlank()) continue
                    val collected = numbers.getOrPut(id) { mutableListOf() }
                    if (collected.none { normalizeNumber(it) == normalizeNumber(number) }) {
                        collected += number
                    }
                    if (id !in byId) {
                        byId[id] = ContactEntry(id, name, number,
                            cursor.getString(3), cursor.getInt(4) == 1)
                    }
                }
            }
            // Searchable nickname and company metadata are loaded once off the UI thread.
            // This never requires additional permissions or external services.
            if (byId.isNotEmpty()) {
                val nickMime = ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE
                val orgMime = ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                context.contentResolver.query(ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.Data.CONTACT_ID, ContactsContract.Data.MIMETYPE,
                        ContactsContract.Data.DATA1),
                    "${ContactsContract.Data.MIMETYPE}=? OR ${ContactsContract.Data.MIMETYPE}=?",
                    arrayOf(nickMime, orgMime), null)?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        if (id in byId) {
                            if (cursor.getString(1) == nickMime) nicknames[id] = cursor.getString(2).orEmpty()
                            else companies[id] = cursor.getString(2).orEmpty()
                        }
                    }
                }
            }
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            android.util.Log.w("SCall", "Contacts provider unavailable", error)
        }
        byId.map { (id, entry) ->
            entry.copy(allNumbers = numbers[id].orEmpty(),
                nickname = nicknames[id].orEmpty(), company = companies[id].orEmpty())
        }
    }

    suspend fun recentCalls(
        contacts: List<ContactEntry>,
        maxRows: Int = 250
    ): CallHistoryPage = withContext(Dispatchers.IO) {
        if (!hasLogsPermission()) return@withContext CallHistoryPage(emptyList(), 0)
        val pageLimit = maxRows.coerceIn(1, 10_000)
        val byNumber = contacts.flatMap { item ->
            item.allNumbers.map { normalizeNumber(it).takeLast(10) to item }
        }.toMap()
        val recent = ArrayList<RecentEntry>(pageLimit)
        var totalCount = 0
        var incomingCount = 0
        var outgoingCount = 0
        var missedCount = 0
        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION),
                null, null, "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                // Cursor count is the real Android call-log total, not the UI page size.
                totalCount = cursor.count.coerceAtLeast(0)
                while (cursor.moveToNext()) {
                    when (cursor.getInt(3)) {
                        CallLog.Calls.INCOMING_TYPE -> incomingCount++
                        CallLog.Calls.OUTGOING_TYPE -> outgoingCount++
                        CallLog.Calls.MISSED_TYPE -> missedCount++
                    }
                    if (recent.size >= pageLimit) continue
                    val number = cursor.getString(1).orEmpty()
                    val contact = byNumber[normalizeNumber(number).takeLast(10)]
                    val cachedName = cursor.getString(2).orEmpty()
                    recent += RecentEntry(
                        id = cursor.getLong(0),
                        number = number,
                        name = contact?.name ?: cachedName.ifBlank { number.ifBlank { "Private number" } },
                        type = cursor.getInt(3),
                        timestamp = cursor.getLong(4),
                        duration = cursor.getLong(5),
                        photo = contact?.photo
                    )
                }
            }
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            android.util.Log.w("SCall", "Call history provider unavailable", error)
        }
        CallHistoryPage(recent, totalCount, incomingCount, outgoingCount, missedCount)
    }

    /**
     * Deletes ONLY explicitly selected system call-log row IDs.
     * Requires the default phone role and restricted WRITE_CALL_LOG permission.
     * Returns -1 if blocked; never claim success when the system rejects the operation.
     */
    suspend fun deleteRecentCalls(ids: Set<Long>): Int = withContext(Dispatchers.IO) {
        val valid = ids.filter { it > 0 }.distinct().take(200)
        if (valid.isEmpty()) return@withContext 0
        if (!isDefaultDialer() || !hasLogsPermission() || !hasWriteLogsPermission()) {
            return@withContext -1
        }
        val where = "${CallLog.Calls._ID} IN (${valid.joinToString(",") { "?" }})"
        try {
            context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                where,
                valid.map { it.toString() }.toTypedArray()
            )
        } catch (_: SecurityException) {
            -1
        } catch (_: Exception) {
            -1
        }
    }

    suspend fun markMissedCallsRead(): Boolean = withContext(Dispatchers.IO) {
        if (!isDefaultDialer() || !hasLogsPermission() || !hasWriteLogsPermission())
            return@withContext false
        try {
            context.contentResolver.update(
                CallLog.Calls.CONTENT_URI,
                android.content.ContentValues().apply { put(CallLog.Calls.IS_READ, 1) },
                "${CallLog.Calls.TYPE}=? AND ${CallLog.Calls.IS_READ}=?",
                arrayOf(CallLog.Calls.MISSED_TYPE.toString(), "0")
            )
            true
        } catch (error: Exception) {
            android.util.Log.w("SCall", "Could not mark missed calls as read", error)
            false
        }
    }

    suspend fun findContact(number: String): ContactEntry? = withContext(Dispatchers.IO) {
        if (!hasContactsPermission() || number.isBlank()) return@withContext null
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)
        )
        try {
            context.contentResolver.query(uri,
                arrayOf(ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.PHOTO_URI), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) ContactEntry(cursor.getLong(0), cursor.getString(1) ?: number,
                    number, cursor.getString(2), false) else null
            }
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            android.util.Log.w("SCall", "Caller lookup unavailable", error)
            null
        }
    }

    /** Real Telecom call-capable accounts; never assume list order equals physical SIM slot. */
    fun availableSimAccounts(): List<SimAccount> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED) return emptyList()
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            ?: return emptyList()
        return try {
            telecom.callCapablePhoneAccounts.mapIndexed { index, handle ->
                val label = try {
                    telecom.getPhoneAccount(handle)?.label?.toString()?.takeIf { it.isNotBlank() }
                        ?: "Phone line ${index + 1}"
                } catch (_: Exception) { "Phone line ${index + 1}" }
                SimAccount(handle, label)
            }
        } catch (_: Exception) { emptyList() }
    }

    /** Active subscriptions are sorted by real SIM slot, unlike Telecom phone accounts. */
    fun availableSmsSubscriptions(): List<SmsSubscription> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED) return emptyList()
        return try {
            val manager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            manager.activeSubscriptionInfoList.orEmpty().sortedBy { it.simSlotIndex }.map { info ->
                val prefix = if (info.simSlotIndex >= 0) "SIM ${info.simSlotIndex + 1}" else "eSIM"
                val operator = info.displayName?.toString()?.takeIf { it.isNotBlank() }
                    ?: info.carrierName?.toString().orEmpty()
                SmsSubscription(info.subscriptionId, info.simSlotIndex,
                    if (operator.isBlank()) prefix else "$prefix · $operator")
            }
        } catch (_: Exception) { emptyList() }
    }

    // These specific default-subscription APIs were introduced only in API 24.
    // Android 6.0 still supports Telecom calling accounts; report the default
    // as unknown instead of crashing or guessing the wrong physical SIM.
    fun defaultVoiceSubscription(): Int = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            SubscriptionManager.getDefaultVoiceSubscriptionId()
        else SubscriptionManager.INVALID_SUBSCRIPTION_ID
    } catch (_: Exception) { SubscriptionManager.INVALID_SUBSCRIPTION_ID }

    fun defaultSmsSubscription(): Int = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            SubscriptionManager.getDefaultSmsSubscriptionId()
        else SubscriptionManager.INVALID_SUBSCRIPTION_ID
    } catch (_: Exception) { SubscriptionManager.INVALID_SUBSCRIPTION_ID }

    fun defaultDataSubscription(): Int = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            SubscriptionManager.getDefaultDataSubscriptionId()
        else SubscriptionManager.INVALID_SUBSCRIPTION_ID
    } catch (_: Exception) { SubscriptionManager.INVALID_SUBSCRIPTION_ID }

    /** Only Android Telecom places calls: never bounce ACTION_DIAL into our own Activity. */
    fun placeCall(number: String, account: PhoneAccountHandle? = null) {
        val dialedNumber = number.trim()
        require(dialedNumber.isNotBlank()) { "Enter a phone number" }
        if (!isDefaultDialer()) throw IllegalStateException("S Caller is no longer the default Phone app")
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) !=
            PackageManager.PERMISSION_GRANTED) throw SecurityException("Call permission not granted")
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            ?: throw IllegalStateException("Android Telecom is unavailable")
        val extras = Bundle().apply {
            if (account != null) putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, account)
        }
        telecom.placeCall(Uri.fromParts("tel", dialedNumber, null), extras)
    }

    fun openSms(number: String, requestedSubscriptionId: Int? = null) {
        if (number.isBlank()) return
        try {
            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null)).apply {
                // Best-effort hint only; the user's messaging app decides which SIM actually sends.
                if (requestedSubscriptionId != null) {
                    putExtra("subscription", requestedSubscriptionId)
                    putExtra("android.telephony.extra.SUBSCRIPTION_ID", requestedSubscriptionId)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) { }
    }
}

/** Settings are expressed relative to the preview bounds so they adapt to every screen size. */
internal data class CallerPhotoStyle(
    val zoom: Float = 1f,
    val horizontal: Float = 0f,
    val vertical: Float = 0f,
    val dim: Float = 0.34f
)

/** Reject invalid floats before passing them to Compose graphics layers. */
internal fun boundedPhotoValue(value: Float, fallback: Float, minimum: Float, maximum: Float): Float =
    (if (value.isFinite()) value else fallback).coerceIn(minimum, maximum)

internal class PhotoStore(private val context: Context) {
    private val storage = context.getSharedPreferences("caller_photos", Context.MODE_PRIVATE)
    private fun id(number: String) = normalizeNumber(number).takeLast(15)
    private fun key(number: String) = "photo:${id(number)}"

    fun photoFor(number: String): String? = try {
        storage.getString(key(number), null)
    } catch (_: ClassCastException) {
        // Some old installs can contain preferences written with the wrong type.
        null
    }

    private fun safeFloat(key: String, fallback: Float): Float = try {
        storage.getFloat(key, fallback)
    } catch (_: ClassCastException) {
        fallback
    }

    fun styleFor(number: String): CallerPhotoStyle {
        val id = id(number)
        return CallerPhotoStyle(
            zoom = boundedPhotoValue(safeFloat("zoom:$id", 1f), 1f, 1f, 2.4f),
            horizontal = boundedPhotoValue(safeFloat("pan_x:$id", 0f), 0f, -0.45f, 0.45f),
            vertical = boundedPhotoValue(safeFloat("pan_y:$id", 0f), 0f, -0.45f, 0.45f),
            dim = boundedPhotoValue(safeFloat("dim:$id", 0.34f), 0.34f, 0.14f, 0.64f)
        )
    }

    /** Keep existing image URI when only its crop and contrast settings are changed. */
    fun save(number: String, image: Uri?, style: CallerPhotoStyle) {
        val id = id(number)
        storage.edit().apply {
            if (image != null) putString(key(number), image.toString())
            putFloat("zoom:$id", style.zoom.coerceIn(1f, 2.4f))
            putFloat("pan_x:$id", style.horizontal.coerceIn(-0.45f, 0.45f))
            putFloat("pan_y:$id", style.vertical.coerceIn(-0.45f, 0.45f))
            putFloat("dim:$id", style.dim.coerceIn(0.14f, 0.64f))
        }.apply()
    }

    fun removePhoto(number: String) {
        val id = id(number)
        storage.edit().remove(key(number)).remove("zoom:$id")
            .remove("pan_x:$id").remove("pan_y:$id").remove("dim:$id").apply()
    }
}

/**
 * Display-only per-call customization. The phone provider's number, date,
 * duration and call type remain untouched. Stored only on this device.
 */
internal class CallHistoryStore(context: Context) {
    private val preferences = context.getSharedPreferences("s_call_history_labels", Context.MODE_PRIVATE)

    private fun key(row: RecentEntry): String = "${row.id}:${row.timestamp}"
    fun title(row: RecentEntry): String? = try {
        preferences.getString("title:${key(row)}", null)?.takeIf { it.isNotBlank() }
    } catch (_: ClassCastException) { null }

    fun note(row: RecentEntry): String = try {
        preferences.getString("note:${key(row)}", "").orEmpty()
    } catch (_: ClassCastException) { "" }

    fun save(row: RecentEntry, title: String, note: String) {
        preferences.edit().apply {
            val cleanTitle = title.trim().take(70)
            val cleanNote = note.trim().take(250)
            if (cleanTitle.isBlank()) remove("title:${key(row)}")
            else putString("title:${key(row)}", cleanTitle)
            if (cleanNote.isBlank()) remove("note:${key(row)}")
            else putString("note:${key(row)}", cleanNote)
        }.apply()
    }

    fun remove(row: RecentEntry) {
        preferences.edit().remove("title:${key(row)}")
            .remove("note:${key(row)}").apply()
    }
}
