package com.scall.phone.ui

import com.scall.phone.data.ContactEntry

/**
 * Only show a saved contact once a sufficiently complete phone number matches.
 * Handles Android-formatted numbers, +91 country code, and additional numbers
 * on the same contact. Never show a prefix/substring match as a full contact.
 */
internal fun findDialedContact(dialed: String, contacts: List<ContactEntry>): ContactEntry? {
    val input = dialed.filter(Char::isDigit)
    if (input.length < 7 || input.length > 15) return null

    fun matches(saved: String): Boolean {
        val digits = saved.filter(Char::isDigit)
        if (digits == input) return true
        // Match common India country-code and national display variants,
        // while avoiding a false match on short extensions or partial input.
        if (input.length >= 10 && digits.length >= 10) {
            val inputNational = input.takeLast(10)
            val savedNational = digits.takeLast(10)
            val supportedInput = input.length == 10 || input.length == 12 && input.startsWith("91")
                || input.length == 11 && input.startsWith("0")
            val supportedSaved = digits.length == 10 || digits.length == 12 && digits.startsWith("91")
                || digits.length == 11 && digits.startsWith("0")
            return supportedInput && supportedSaved && inputNational == savedNational
        }
        return false
    }

    return contacts.firstOrNull { contact ->
        (contact.allNumbers + contact.number).any(::matches)
    }
}


/** A suggestion always refers to a specific saved number, not just the primary contact. */
internal data class DialContactSuggestion(val contact: ContactEntry, val number: String)

/**
 * Search saved phone numbers as digits arrive. Prefix matches first, then
 * other substring matches; no country-code punctuation affects results.
 * Results are capped for predictable performance on large contact lists.
 */
internal fun findDialContactSuggestions(
    typed: String, contacts: List<ContactEntry>, limit: Int = 30
): List<DialContactSuggestion> {
    val query = typed.filter(Char::isDigit)
    if (query.length < 3 || query.length > 15 || limit <= 0) return emptyList()
    // Cap candidates per rank: avoid sorting thousands of matches on each tap.
    val cap = limit.coerceAtMost(40)
    val buckets = Array(4) { ArrayList<DialContactSuggestion>(cap) }
    for (contact in contacts) {
        val all = (contact.allNumbers + contact.number).distinctBy { it.filter(Char::isDigit) }
        for (saved in all) {
            val digits = saved.filter(Char::isDigit)
            if (digits.length < query.length || !digits.contains(query)) continue
            val national = if (digits.length == 12 && digits.startsWith("91")) digits.drop(2)
                else if (digits.length == 11 && digits.startsWith("0")) digits.drop(1)
                else digits
            val rank = when {
                national == query || digits == query -> 0
                national.startsWith(query) -> 1
                digits.startsWith(query) -> 2
                else -> 3
            }
            if (buckets[rank].size < cap) {
                buckets[rank].add(DialContactSuggestion(contact, saved))
            }
        }
    }
    return buckets.asSequence()
        .flatMap { bucket -> bucket.sortedBy { it.contact.name.lowercase() }.asSequence() }
        .take(cap)
        .toList()
}
