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
