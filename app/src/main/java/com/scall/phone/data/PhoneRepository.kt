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
    val number: String,
    val name: String,
    val type: Int,
    val timestamp: Long,
    val duration: Long,
    val photo: String?
)

internal data class SimAccount(val handle: PhoneAccountHandle, val label: String)

internal fun normalizeNumber(number: String): String = number.filter { it.isDigit() || it == '+' }

internal class PhoneRepository(private val context: Context) {
    private fun permitted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasContactsPermission() = permitted(Manifest.permission.READ_CONTACTS)
    fun hasLogsPermission() = permitted(Manifest.permission.READ_CALL_LOG)
    fun hasCallPermission() = permitted(Manifest.permission.CALL_PHONE)

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
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION),
                null, null, "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext() && recent.size < 200) {
                    val number = cursor.getString(0).orEmpty()
                    val contact = byNumber[normalizeNumber(number).takeLast(10)]
                    val cachedName = cursor.getString(1).orEmpty()
                    recent += RecentEntry(
                        number = number,
                        name = contact?.name ?: cachedName.ifBlank { number.ifBlank { "Private number" } },
                        type = cursor.getInt(2),
                        timestamp = cursor.getLong(3),
                        duration = cursor.getLong(4),
                        photo = contact?.photo
                    )
                }
            }
        } catch (_: SecurityException) { }
        recent
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

    fun availableSimAccounts(): List<SimAccount> {
        if (!isDefaultDialer()) return emptyList()
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        return try {
            telecom.callCapablePhoneAccounts.mapIndexed { index, handle ->
                val label = try {
                    telecom.getPhoneAccount(handle)?.label?.toString() ?: "SIM ${index + 1}"
                } catch (_: SecurityException) { "SIM ${index + 1}" }
                SimAccount(handle, label)
            }
        } catch (_: SecurityException) { emptyList() }
    }

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

    fun openSms(number: String) {
        if (number.isBlank()) return
        try {
            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) { }
    }
}

internal class PhotoStore(private val context: Context) {
    private val storage = context.getSharedPreferences("caller_photos", Context.MODE_PRIVATE)
    private fun key(number: String) = "photo:${normalizeNumber(number).takeLast(15)}"
    fun photoFor(number: String): String? = storage.getString(key(number), null)
    fun setPhoto(number: String, uri: Uri) {
        storage.edit().putString(key(number), uri.toString()).apply()
    }
    fun removePhoto(number: String) { storage.edit().remove(key(number)).apply() }
}
