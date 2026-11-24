package com.scall.phone.ui

/**
 * Shared pure predicate for asynchronous Contacts/Favorites search.
 * Keep punctuation-friendly matching while also accepting digits from
 * numbers formatted with spaces, dashes and parentheses.
 */
internal fun matchesContactSearch(name: String, number: String, search: String): Boolean {
    val query = search.trim()
    if (query.isEmpty()) return true
    if (name.contains(query, ignoreCase = true) ||
        number.contains(query, ignoreCase = true)) return true
    val digits = query.filter(Char::isDigit)
    if (digits.isEmpty()) return false
    return number.filter(Char::isDigit).contains(digits)
}
