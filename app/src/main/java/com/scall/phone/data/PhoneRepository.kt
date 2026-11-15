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
    val favorite: Boolean
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

    fun isDefaultDialer(): Boolean {
        val manager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        return manager.defaultDialerPackage == context.packageName
    }

    suspend fun contacts(): List<ContactEntry> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) return@withContext emptyList()
        val list = mutableListOf<ContactEntry>()
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
                val seen = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val name = cursor.getString(1).orEmpty().ifBlank { "Unknown" }
                    val number = cursor.getString(2).orEmpty()
                    if (number.isBlank()) continue
                    val uniqueKey = "$id:${normalizeNumber(number)}"
                    if (!seen.add(uniqueKey)) continue
                    list += ContactEntry(id, name, number, cursor.getString(3), cursor.getInt(4) == 1)
                }
            }
        } catch (_: SecurityException) { /* User can revoke access at any time. */ }
        list
    }

    suspend fun recentCalls(contacts: List<ContactEntry>): List<RecentEntry> = withContext(Dispatchers.IO) {
        if (!hasLogsPermission()) return@withContext emptyList()
        val byNumber = contacts.associateBy { normalizeNumber(it.number).takeLast(10) }
        val recent = mutableListOf<RecentEntry>()
        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION),
                null, null, "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext() && recent.size < 200) {
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
        } catch (_: SecurityException) { }
        recent
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
        } catch (_: SecurityException) { null }
    }

    /** Real Telecom call-capable accounts; never assume list order equals physical SIM slot. */
    fun availableSimAccounts(): List<SimAccount> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED) return emptyList()
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
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

    fun defaultVoiceSubscription(): Int = try {
        SubscriptionManager.getDefaultVoiceSubscriptionId()
    } catch (_: Exception) { SubscriptionManager.INVALID_SUBSCRIPTION_ID }

    fun defaultSmsSubscription(): Int = try {
        SubscriptionManager.getDefaultSmsSubscriptionId()
    } catch (_: Exception) { SubscriptionManager.INVALID_SUBSCRIPTION_ID }

    fun defaultDataSubscription(): Int = try {
        SubscriptionManager.getDefaultDataSubscriptionId()
    } catch (_: Exception) { SubscriptionManager.INVALID_SUBSCRIPTION_ID }

    /** Always use TelecomManager for calls from the default dialer. */
    fun placeCall(number: String, account: PhoneAccountHandle? = null) {
        if (number.isBlank()) return
        val uri = Uri.fromParts("tel", number, null)
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        if (isDefaultDialer() && hasCallPermission()) {
            val extras = Bundle().apply {
                if (account != null) putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, account)
            }
            try {
                telecom.placeCall(uri, extras)
                return
            } catch (error: SecurityException) {
                // Do not issue ACTION_DIAL here: when S call is the default it
                // would resolve back to this same activity.
                throw error
            }
        }
        context.startActivity(Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
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

internal class PhotoStore(private val context: Context) {
    private val storage = context.getSharedPreferences("caller_photos", Context.MODE_PRIVATE)
    private fun id(number: String) = normalizeNumber(number).takeLast(15)
    private fun key(number: String) = "photo:${id(number)}"

    fun photoFor(number: String): String? = storage.getString(key(number), null)

    fun styleFor(number: String): CallerPhotoStyle {
        val id = id(number)
        return CallerPhotoStyle(
            zoom = storage.getFloat("zoom:$id", 1f).coerceIn(1f, 2.4f),
            horizontal = storage.getFloat("pan_x:$id", 0f).coerceIn(-0.45f, 0.45f),
            vertical = storage.getFloat("pan_y:$id", 0f).coerceIn(-0.45f, 0.45f),
            dim = storage.getFloat("dim:$id", 0.34f).coerceIn(0.14f, 0.64f)
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
    fun title(row: RecentEntry): String? =
        preferences.getString("title:${key(row)}", null)?.takeIf { it.isNotBlank() }
    fun note(row: RecentEntry): String =
        preferences.getString("note:${key(row)}", "").orEmpty()

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
