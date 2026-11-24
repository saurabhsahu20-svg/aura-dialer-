package com.scall.phone.data

/**
 * A full international number must never be matched using only its final
 * ten digits: two unrelated countries can share the same suffix.
 *
 * Indian 10-digit mobile, 0-prefixed national and +91 international mobile
 * variants intentionally resolve to the same key. All other numbers must
 * match all of their digits exactly.
 */
internal fun callLogContactKey(raw: String): String? {
    val digits = raw.filter(Char::isDigit)
    if (digits.length !in 3..15) return null
    val nationalIndia = when {
        digits.length == 10 && !raw.trimStart().startsWith("+") &&
            digits[0] in '6'..'9' -> digits
        digits.length == 11 && digits[0] == '0' &&
            digits[1] in '6'..'9' -> digits.drop(1)
        digits.length == 12 && digits.startsWith("91") &&
            digits[2] in '6'..'9' -> digits.drop(2)
        else -> null
    }
    return if (nationalIndia != null) "IN:$nationalIndia" else "FULL:$digits"
}
