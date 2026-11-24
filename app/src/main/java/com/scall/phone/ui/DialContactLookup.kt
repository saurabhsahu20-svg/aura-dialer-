package com.scall.phone.ui

import com.scall.phone.data.ContactEntry
import com.scall.phone.data.contactLookupKey

/** Only an exact normalized saved number can appear as the caller name.
 * A partial digit/T9 suggestion must never be mislabelled as an exact contact.
 * Country codes remain significant, except known Indian mobile formats.
 */
internal fun exactDialContact(number: String, contacts: List<ContactEntry>): ContactEntry? {
    val key = contactLookupKey(number) ?: return null
    return contacts.firstOrNull { contact ->
        contact.name.isNotBlank() && contactLookupKey(contact.number) == key
    }
}

/** Carries the query with the result to prevent names from lagging one keystroke. */
internal data class DialContactSearchResult(
    val query: String,
    val suggestions: List<ContactEntry>,
    val exactContact: ContactEntry?
)
