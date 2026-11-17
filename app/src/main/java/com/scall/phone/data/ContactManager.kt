package com.scall.phone.data

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class ContactValue(val value: String = "", val label: String = "Mobile", val rowId: Long = 0L)
internal data class ContactAccount(val name: String?, val type: String?, val label: String)
internal data class ContactGroup(val id: Long, val title: String)

internal data class ManagedContact(
    val id: Long = 0L, val rawId: Long = 0L,
    val first: String = "", val middle: String = "", val last: String = "",
    val nickname: String = "", val phones: List<ContactValue> = listOf(ContactValue()),
    val emails: List<ContactValue> = emptyList(),
    val company: String = "", val jobTitle: String = "", val department: String = "",
    val notes: String = "", val birthday: String = "", val anniversary: String = "",
    val street: String = "", val city: String = "", val state: String = "",
    val postcode: String = "", val country: String = "",
    val website: String = "", val relationship: String = "",
    val favorite: Boolean = false, val photo: String? = null
) {
    val fullName: String get() = listOf(first, middle, last)
        .filter { it.isNotBlank() }.joinToString(" ").trim()
}

/** Android ContactsContract is the source of truth; no invented/synced contact records. */
private typealias DataRow = ContactsContract.Data
private typealias RawRow = ContactsContract.RawContacts
private typealias PersonRow = ContactsContract.Contacts
private typealias PhoneRow = ContactsContract.CommonDataKinds.Phone
private typealias EmailRow = ContactsContract.CommonDataKinds.Email
private typealias NameRow = ContactsContract.CommonDataKinds.StructuredName

internal class ContactManager(private val context: Context) {
    private val resolver = context.contentResolver

    fun canRead() = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
        PackageManager.PERMISSION_GRANTED
    fun canWrite() = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) ==
        PackageManager.PERMISSION_GRANTED

    /** Read existing Android provider groups without modifying synced account groups. */
    suspend fun groups(): List<ContactGroup> = withContext(Dispatchers.IO) {
        if (!canRead()) return@withContext emptyList()
        val result = mutableListOf<ContactGroup>()
        try {
            resolver.query(ContactsContract.Groups.CONTENT_URI,
                arrayOf(ContactsContract.Groups._ID, ContactsContract.Groups.TITLE),
                "${ContactsContract.Groups.DELETED}=0", null,
                "${ContactsContract.Groups.TITLE} COLLATE NOCASE ASC")?.use { c ->
                while (c.moveToNext()) {
                    val title = c.getString(1).orEmpty()
                    if (title.isNotBlank()) result += ContactGroup(c.getLong(0), title)
                }
            }
        } catch (_: SecurityException) { }
        result.distinctBy { it.id }
    }

    suspend fun members(groupId: Long): Set<Long> = withContext(Dispatchers.IO) {
        if (!canRead() || groupId <= 0) return@withContext emptySet()
        val result = mutableSetOf<Long>()
        resolver.query(ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.CONTACT_ID),
            "${ContactsContract.Data.MIMETYPE}=? AND ${ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID}=?",
            arrayOf(ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE, groupId.toString()),
            null)?.use { c ->
            while (c.moveToNext()) result += c.getLong(0)
        }
        result
    }

    suspend fun accounts(): List<ContactAccount> = withContext(Dispatchers.IO) {
        if (!canRead()) return@withContext emptyList()
        val accounts = linkedSetOf<Pair<String?, String?>>(null to null)
        try {
            resolver.query(RawRow.CONTENT_URI, arrayOf(RawRow.ACCOUNT_NAME, RawRow.ACCOUNT_TYPE),
                "${RawRow.DELETED}=0", null, null)?.use { c ->
                while (c.moveToNext()) accounts += c.getString(0) to c.getString(1)
            }
        } catch (_: SecurityException) { }
        accounts.map { (n, t) -> ContactAccount(n, t, n ?: "Device (local)") }
    }

    /**
     * Edit one raw record within an aggregated contact. Never replace siblings
     * from other accounts or remove unsupported fields.
     */
    suspend fun load(contactId: Long): ManagedContact? = withContext(Dispatchers.IO) {
        if (!canRead() || contactId <= 0) return@withContext null
        val rawId = resolver.query(RawRow.CONTENT_URI, arrayOf(RawRow._ID),
            "${RawRow.CONTACT_ID}=? AND ${RawRow.DELETED}=0",
            arrayOf(contactId.toString()), "${RawRow._ID} ASC")?.use {
                if (it.moveToFirst()) it.getLong(0) else null
            } ?: return@withContext null
        var result = ManagedContact(id = contactId, rawId = rawId, phones = emptyList())
        val phones = mutableListOf<ContactValue>()
        val emails = mutableListOf<ContactValue>()
        resolver.query(DataRow.CONTENT_URI, arrayOf(DataRow._ID, DataRow.MIMETYPE,
            DataRow.DATA1, DataRow.DATA2, DataRow.DATA3, DataRow.DATA4, DataRow.DATA5,
            DataRow.DATA6, DataRow.DATA7, DataRow.DATA8, DataRow.DATA9, DataRow.DATA10),
            "${DataRow.RAW_CONTACT_ID}=?", arrayOf(rawId.toString()), null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                fun v(index: Int) = c.getString(index).orEmpty()
                when (v(1)) {
                    NameRow.CONTENT_ITEM_TYPE -> result = result.copy(first = v(3), middle = v(6), last = v(4))
                    PhoneRow.CONTENT_ITEM_TYPE -> phones += ContactValue(v(2),
                        phoneLabel(v(3), v(4)), id)
                    EmailRow.CONTENT_ITEM_TYPE -> emails += ContactValue(v(2),
                        emailLabel(v(3), v(4)), id)
                    ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE ->
                        result = result.copy(nickname = v(2))
                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE ->
                        result = result.copy(company = v(2), jobTitle = v(5), department = v(6))
                    ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE ->
                        result = result.copy(notes = v(2))
                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE ->
                        result = result.copy(website = v(2))
                    ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE ->
                        result = result.copy(relationship = v(2))
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE ->
                        result = result.copy(street = v(5), city = v(8), state = v(9),
                            postcode = v(10), country = v(11))
                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE ->
                        when (v(3).toIntOrNull()) {
                            ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY ->
                                result = result.copy(birthday = v(2))
                            ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY ->
                                result = result.copy(anniversary = v(2))
                        }
                }
            }
        }
        resolver.query(PersonRow.CONTENT_URI,
            arrayOf(PersonRow.DISPLAY_NAME_PRIMARY, PersonRow.STARRED, PersonRow.PHOTO_URI),
            "${PersonRow._ID}=?", arrayOf(contactId.toString()), null)?.use { c ->
            if (c.moveToFirst()) {
                if (result.fullName.isBlank()) result = result.copy(first = c.getString(0).orEmpty())
                result = result.copy(favorite = c.getInt(1) != 0, photo = c.getString(2))
            }
        }
        result.copy(phones = phones.ifEmpty { listOf(ContactValue()) }, emails = emails)
    }

    /** Aggregated numbers may live in several synced raw PersonRow. */
    suspend fun numbers(contactId: Long): List<ContactValue> = withContext(Dispatchers.IO) {
        if (!canRead()) return@withContext emptyList()
        val found = mutableListOf<ContactValue>()
        resolver.query(PhoneRow.CONTENT_URI, arrayOf(PhoneRow.NUMBER, PhoneRow.TYPE, PhoneRow.LABEL),
            "${PhoneRow.CONTACT_ID}=?", arrayOf(contactId.toString()), null)?.use { c ->
            while (c.moveToNext()) found += ContactValue(c.getString(0).orEmpty(),
                phoneLabel(c.getString(1), c.getString(2)))
        }
        found.filter { it.value.isNotBlank() }.distinctBy { normalizeNumber(it.value) }
    }

    suspend fun duplicates(number: String, exceptId: Long = 0): List<String> = withContext(Dispatchers.IO) {
        if (!canRead() || number.filter(Char::isDigit).length < 7) return@withContext emptyList()
        val found = linkedSetOf<String>()
        val lookup = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        resolver.query(lookup, arrayOf(ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { c ->
            while (c.moveToNext()) if (c.getLong(0) != exceptId)
                found += c.getString(1).orEmpty().ifBlank { "Another contact" }
        }
        found.toList()
    }

    suspend fun save(person: ManagedContact, account: ContactAccount? = null,
                     pickedPhoto: Uri? = null, removePhoto: Boolean = false): Long =
        withContext(Dispatchers.IO) {
            if (!canWrite()) throw SecurityException("Allow Contacts editing in Android settings")
            require(person.fullName.isNotBlank()) { "Enter a contact name" }
            require(person.phones.any { it.value.count(Char::isDigit) >= 3 }) {
                "Enter a valid phone number"
            }
            val creating = person.id == 0L
            if (!creating) require(person.rawId > 0L) { "The contact is no longer available" }
            val ops = arrayListOf<ContentProviderOperation>()
            if (creating) ops += ContentProviderOperation.newInsert(RawRow.CONTENT_URI)
                .withValue(RawRow.ACCOUNT_NAME, account?.name)
                .withValue(RawRow.ACCOUNT_TYPE, account?.type).build()
            val existing = mutableMapOf<String, MutableList<Long>>()
            if (!creating) resolver.query(DataRow.CONTENT_URI, arrayOf(DataRow._ID, DataRow.MIMETYPE),
                "${DataRow.RAW_CONTACT_ID}=?", arrayOf(person.rawId.toString()), null)?.use { c ->
                while (c.moveToNext()) existing.getOrPut(c.getString(1).orEmpty()) {
                    mutableListOf()
                }.add(c.getLong(0))
            }
            fun ins(mime: String, values: Map<String, Any?>): ContentProviderOperation {
                val builder = ContentProviderOperation.newInsert(DataRow.CONTENT_URI)
                    .withValue(DataRow.MIMETYPE, mime)
                if (creating) builder.withValueBackReference(DataRow.RAW_CONTACT_ID, 0)
                else builder.withValue(DataRow.RAW_CONTACT_ID, person.rawId)
                values.forEach { (k, v) -> builder.withValue(k, v) }
                return builder.build()
            }
            fun upd(id: Long, values: Map<String, Any?>): ContentProviderOperation {
                val builder = ContentProviderOperation.newUpdate(DataRow.CONTENT_URI)
                    .withSelection("${DataRow._ID}=? AND ${DataRow.RAW_CONTACT_ID}=?",
                        arrayOf(id.toString(), person.rawId.toString()))
                values.forEach { (k, v) -> builder.withValue(k, v) }
                return builder.build()
            }
            fun delete(id: Long) = ContentProviderOperation.newDelete(DataRow.CONTENT_URI)
                .withSelection("${DataRow._ID}=? AND ${DataRow.RAW_CONTACT_ID}=?",
                    arrayOf(id.toString(), person.rawId.toString())).build()
            fun single(mime: String, values: Map<String, Any?>, enabled: Boolean) {
                val id = existing[mime]?.firstOrNull()
                if (enabled) ops += if (id == null) ins(mime, values) else upd(id, values)
                else if (id != null) ops += delete(id)
            }
            single(NameRow.CONTENT_ITEM_TYPE, mapOf(NameRow.GIVEN_NAME to person.first.trim(),
                NameRow.MIDDLE_NAME to person.middle.trim(), NameRow.FAMILY_NAME to person.last.trim()), true)
            fun repeated(mime: String, rows: List<ContactValue>, typeKey: String,
                         labelKey: String, type: (String) -> Int) {
                val retained = mutableSetOf<Long>()
                rows.filter { it.value.isNotBlank() }.forEach { row ->
                    val kind = type(row.label)
                    val values = mapOf(DataRow.DATA1 to row.value.trim(), typeKey to kind,
                        labelKey to if (kind == 0) row.label else null)
                    val id = row.rowId.takeIf { it in existing[mime].orEmpty() }
                    if (id == null) ops += ins(mime, values)
                    else { retained += id; ops += upd(id, values) }
                }
                // Deletion is scoped to explicitly removed rows of the edited raw only.
                existing[mime].orEmpty().filterNot { it in retained }.forEach { ops += delete(it) }
            }
            repeated(PhoneRow.CONTENT_ITEM_TYPE, person.phones, PhoneRow.TYPE, PhoneRow.LABEL, ::phoneType)
            repeated(EmailRow.CONTENT_ITEM_TYPE, person.emails, EmailRow.TYPE, EmailRow.LABEL, ::emailType)
            single(ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE, mapOf(ContactsContract.CommonDataKinds.Nickname.NAME to person.nickname.trim()), person.nickname.isNotBlank())
            single(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE, mapOf(ContactsContract.CommonDataKinds.Organization.COMPANY to person.company.trim(),
                ContactsContract.CommonDataKinds.Organization.TITLE to person.jobTitle.trim(), ContactsContract.CommonDataKinds.Organization.DEPARTMENT to person.department.trim()),
                listOf(person.company, person.jobTitle, person.department).any { it.isNotBlank() })
            single(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE, mapOf(ContactsContract.CommonDataKinds.Note.NOTE to person.notes.trim()), person.notes.isNotBlank())
            single(ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE, mapOf(ContactsContract.CommonDataKinds.Website.URL to person.website.trim()), person.website.isNotBlank())
            single(ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE, mapOf(ContactsContract.CommonDataKinds.Relation.NAME to person.relationship.trim(),
                ContactsContract.CommonDataKinds.Relation.TYPE to ContactsContract.CommonDataKinds.Relation.TYPE_CUSTOM), person.relationship.isNotBlank())
            single(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE, mapOf(ContactsContract.CommonDataKinds.StructuredPostal.STREET to person.street.trim(),
                ContactsContract.CommonDataKinds.StructuredPostal.CITY to person.city.trim(), ContactsContract.CommonDataKinds.StructuredPostal.REGION to person.state.trim(),
                ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE to person.postcode.trim(), ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY to person.country.trim(),
                ContactsContract.CommonDataKinds.StructuredPostal.TYPE to ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME),
                listOf(person.street,person.city,person.state,person.postcode,person.country).any { it.isNotBlank() })
            // Query event types so a birthday edit never silently overwrites an anniversary.
            fun putDate(type: Int, value: String) {
                val matched = existing[ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE].orEmpty().firstOrNull { row ->
                    resolver.query(DataRow.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Event.TYPE), "${DataRow._ID}=?",
                        arrayOf(row.toString()), null)?.use { c ->
                        c.moveToFirst() && c.getInt(0) == type
                    } ?: false
                }
                if (value.isNotBlank()) {
                    val values = mapOf(ContactsContract.CommonDataKinds.Event.START_DATE to value.trim(), ContactsContract.CommonDataKinds.Event.TYPE to type)
                    ops += if (matched == null) ins(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE, values) else upd(matched, values)
                } else if (matched != null) ops += delete(matched)
            }
            putDate(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY, person.birthday)
            putDate(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY, person.anniversary)
            if (pickedPhoto != null) {
                single(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE,
                    mapOf(ContactsContract.CommonDataKinds.Photo.PHOTO to photoJpeg(pickedPhoto)), true)
            } else if (removePhoto) {
                single(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE, emptyMap(), false)
            }
            val saved = resolver.applyBatch(ContactsContract.AUTHORITY, ops)
            val contactId = if (creating) {
                val newRawId = saved.firstOrNull()?.uri?.lastPathSegment?.toLongOrNull()
                    ?: throw IllegalStateException("Contact saved but ID unavailable")
                resolver.query(RawRow.CONTENT_URI, arrayOf(RawRow.CONTACT_ID),
                    "${RawRow._ID}=?", arrayOf(newRawId.toString()), null)?.use { c ->
                    if (c.moveToFirst()) c.getLong(0) else 0L
                } ?: 0L
            } else person.id
            if (contactId > 0 && person.favorite) setFavoriteDirect(contactId, true)
            contactId
        }

    suspend fun favorite(contactId: Long, starred: Boolean) = withContext(Dispatchers.IO) {
        if (!canWrite()) false else setFavoriteDirect(contactId, starred)
    }
    private fun setFavoriteDirect(id: Long, starred: Boolean): Boolean {
        val values = ContentValues().apply { put(PersonRow.STARRED, if (starred) 1 else 0) }
        return resolver.update(Uri.withAppendedPath(PersonRow.CONTENT_URI, id.toString()), values, null, null) > 0
    }
    suspend fun delete(contactId: Long) = withContext(Dispatchers.IO) {
        if (!canWrite()) false else resolver.delete(
            Uri.withAppendedPath(PersonRow.CONTENT_URI, contactId.toString()), null, null) > 0
    }
    fun vcardUri(contactId: Long): Uri? {
        if (!canRead()) return null
        val key = resolver.query(Uri.withAppendedPath(PersonRow.CONTENT_URI, contactId.toString()),
            arrayOf(PersonRow.LOOKUP_KEY), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: return null
        return Uri.withAppendedPath(PersonRow.CONTENT_VCARD_URI, Uri.encode(key))
    }

    /** Downsample large SAF images before inserting into the Android contact provider. */
    private fun photoJpeg(uri: Uri): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalArgumentException("Unsupported contact photo")
        }
        var sample = 1
        while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalArgumentException("Could not read contact photo")
        return try {
            ByteArrayOutputStream().use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)) {
                    throw IllegalStateException("Photo compression failed")
                }
                out.toByteArray()
            }
        } finally { bitmap.recycle() }
    }

    /**
     * Export a user-selected text VCF through Android's Storage Access Framework.
     * Includes supported names, numbers, emails, dates and notes; photos, synced
     * account metadata and group membership remain managed by Android Contacts.
     */
    suspend fun exportBasicVcard(contactIds: List<Long>, target: Uri): Int =
        withContext(Dispatchers.IO) {
            if (!canRead()) throw SecurityException("Contacts access is required")
            fun escape(text: String): String = text.replace("\\", "\\\\")
                .replace("\n", "\\n").replace(";", "\\;").replace(",", "\\,")
            var exported = 0
            val stream = resolver.openOutputStream(target, "wt")
                ?: throw IllegalStateException("Cannot open the selected backup destination")
            stream.bufferedWriter(Charsets.UTF_8).use { output ->
                contactIds.distinct().forEach { id ->
                    val card = load(id) ?: return@forEach
                    val allPhones = numbers(id)
                    if (card.fullName.isBlank() || allPhones.isEmpty()) return@forEach
                    output.append("BEGIN:VCARD\r\nVERSION:3.0\r\n")
                    output.append("N:${escape(card.last)};${escape(card.first)};${escape(card.middle)};;\r\n")
                    output.append("FN:${escape(card.fullName)}\r\n")
                    allPhones.forEach { item ->
                        output.append("TEL;TYPE=${if (item.label.equals("Work", true)) "WORK" else "CELL"}:${escape(item.value)}\r\n")
                    }
                    card.emails.filter { it.value.isNotBlank() }.forEach { item ->
                        output.append("EMAIL:${escape(item.value)}\r\n")
                    }
                    if (card.company.isNotBlank()) output.append("ORG:${escape(card.company)}\r\n")
                    if (card.jobTitle.isNotBlank()) output.append("TITLE:${escape(card.jobTitle)}\r\n")
                    if (card.birthday.isNotBlank()) output.append("BDAY:${escape(card.birthday)}\r\n")
                    if (card.notes.isNotBlank()) output.append("NOTE:${escape(card.notes)}\r\n")
                    output.append("END:VCARD\r\n")
                    exported++
                }
            }
            exported
        }

    private fun phoneType(label: String) = when (label.lowercase()) {
        "mobile" -> PhoneRow.TYPE_MOBILE
        "home" -> PhoneRow.TYPE_HOME
        "work" -> PhoneRow.TYPE_WORK
        "other" -> PhoneRow.TYPE_OTHER
        else -> PhoneRow.TYPE_CUSTOM
    }
    private fun emailType(label: String) = when (label.lowercase()) {
        "personal", "home" -> EmailRow.TYPE_HOME
        "work" -> EmailRow.TYPE_WORK
        "other" -> EmailRow.TYPE_OTHER
        else -> EmailRow.TYPE_CUSTOM
    }
    private fun phoneLabel(type: String?, custom: String?) = when (type?.toIntOrNull()) {
        PhoneRow.TYPE_MOBILE -> "Mobile"
        PhoneRow.TYPE_HOME -> "Home"
        PhoneRow.TYPE_WORK -> "Work"
        PhoneRow.TYPE_OTHER -> "Other"
        else -> custom?.takeIf { it.isNotBlank() } ?: "Other"
    }
    private fun emailLabel(type: String?, custom: String?) = when (type?.toIntOrNull()) {
        EmailRow.TYPE_HOME -> "Personal"
        EmailRow.TYPE_WORK -> "Work"
        EmailRow.TYPE_OTHER -> "Other"
        else -> custom?.takeIf { it.isNotBlank() } ?: "Other"
    }
}
