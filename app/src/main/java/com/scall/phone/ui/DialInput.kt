package com.scall.phone.ui

/** Preserve only characters accepted by Android's dialer and cap user input length. */
internal fun cleanDialNumber(text: String): String =
    text.filter { it.isDigit() || it in "+*#(),; -" }.take(40)

/** On-screen digits, including a long press on zero for plus. */
internal fun appendDialKey(current: String, key: String): String =
    cleanDialNumber(current + key)
