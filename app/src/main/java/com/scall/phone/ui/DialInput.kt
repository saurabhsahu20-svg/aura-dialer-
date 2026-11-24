package com.scall.phone.ui

/** Preserve only characters accepted by Android's dialer and cap user input length. */
internal fun cleanDialNumber(text: String): String =
    text.filter { it.isDigit() || it in "+*#(),; -" }.trim().take(40)

/**
 * Track distinct Android ACTION_DIAL deliveries, including two requests for the
 * exact same phone number. Wrap safely if the Activity survives a very long time.
 */
internal fun nextExternalDialRequestId(previous: Int): Int =
    if (previous >= Int.MAX_VALUE || previous < 0) 1 else previous + 1

/** On-screen digits, including a long press on zero for plus. */
internal fun appendDialKey(current: String, key: String): String =
    cleanDialNumber(current + key)

/**
 * Responsive four-row keypad: fill available phone height with near-square
 * number keys; preserve at least 72dp high targets on compact displays.
 * The scrollable key area handles any vertical overflow from contact matches.
 */
internal fun adaptiveDialRowHeight(availableHeightDp: Float, suggestionCount: Int): Float {
    if (!availableHeightDp.isFinite() || availableHeightDp <= 0f) return 72f
    val matches = suggestionCount.coerceIn(0, 3)
    val suggestionSpace = if (matches == 0) 0f else 30f + matches * 54f
    return ((availableHeightDp - suggestionSpace - 12f) / 4f).coerceIn(72f, 136f)
}

/**
 * A dial pad's number-key height must never depend on search results.
 * Contact suggestions are shown on demand in a dialog, not inserted
 * above the keys, so keys cannot jump under the user's finger.
 */
internal fun stableDialRowHeight(availableHeightDp: Float): Float {
    if (!availableHeightDp.isFinite() || availableHeightDp <= 0f) return 52f
    return (availableHeightDp / 4f).coerceIn(52f, 90f)
}
