package com.scall.phone.ui

/**
 * Explicit chat links. Phone-number account registration is not detectable
 * through Android Contacts/CallLog; only WhatsApp/Telegram can resolve it.
 * Never transmit a contact number until the user taps a chat option.
 */
internal enum class ExternalChatApp { WHATSAPP, TELEGRAM }

/**
 * Use E.164-like digits. 10-digit Indian numbers and 0-prefixed 11-digit
 * national numbers can safely receive country code 91. Other countries
 * require the '+' international prefix: we never guess a country code.
 */
internal fun chatPhoneDigits(raw: String): String? {
    val value = raw.trim()
    if (value.isBlank()) return null
    if (value.any { !it.isDigit() && it != '+' && it != ' ' && it != '-' &&
            it != '(' && it != ')' }) return null
    val digits = value.filter(Char::isDigit)
    if (digits.length == 10 && !value.startsWith("+")) return "91$digits"
    if (digits.length == 11 && digits.startsWith("0") && !value.startsWith("+"))
        return "91" + digits.drop(1)
    if (digits.length == 12 && digits.startsWith("91") && !value.startsWith("+"))
        return digits
    if (value.startsWith("+") && value.count { it == '+' } == 1 &&
        digits.length in 8..15 && digits.first() != '0') return digits
    return null
}

internal fun externalChatLink(raw: String, app: ExternalChatApp): String? {
    val digits = chatPhoneDigits(raw) ?: return null
    return when (app) {
        ExternalChatApp.WHATSAPP -> "https://wa.me/$digits"
        ExternalChatApp.TELEGRAM -> "https://t.me/+$digits"
    }
}
