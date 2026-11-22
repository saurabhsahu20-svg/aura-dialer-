package com.scall.phone.ui

/** Explicit user-initiated external messaging. Never probe account membership. */
internal enum class ExternalChatApp { WHATSAPP, TELEGRAM }

/** Only interpret an Indian national number or an explicit international number. */
internal fun chatPhoneDigits(raw: String): String? {
    val input = raw.trim()
    if (input.isBlank() || input.any { !it.isDigit() && it != '+' && it != ' ' && it != '-' && it != '(' && it != ')' }) return null
    if (input.count { it == '+' } > 1 || ('+' in input && !input.startsWith("+"))) return null
    val digits = input.filter(Char::isDigit)
    if (!input.startsWith("+") && digits.length == 10 && digits.first() in '6'..'9') return "91$digits"
    if (!input.startsWith("+") && digits.length == 11 && digits.startsWith("0") && digits[1] in '6'..'9') return "91" + digits.drop(1)
    if (!input.startsWith("+") && digits.length == 12 && digits.startsWith("91") && digits[2] in '6'..'9') return digits
    if (input.startsWith("+") && digits.length in 8..15 && digits.first() != '0') return digits
    return null
}

internal fun externalChatLink(raw: String, app: ExternalChatApp): String? {
    val digits = chatPhoneDigits(raw) ?: return null
    return when (app) {
        ExternalChatApp.WHATSAPP -> "https://wa.me/$digits"
        ExternalChatApp.TELEGRAM -> "https://t.me/+$digits"
    }
}
