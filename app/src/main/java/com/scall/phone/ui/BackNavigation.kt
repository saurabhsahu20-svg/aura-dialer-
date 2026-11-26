package com.scall.phone.ui

/** Bottom tabs are sibling destinations, never a chronological back stack. */
internal val primaryCallTabs: Set<String> =
    setOf("Recents", "Contacts", "Keypad", "Favorites")

internal fun isPrimaryCallTab(destination: String): Boolean =
    destination in primaryCallTabs

/**
 * Switching among bottom tabs always clears page history. Only subpages
 * (Settings, SIM Manager, etc.) store their direct parent for one-level Back.
 * Cap nested history so an errant link cannot grow an unbounded stack.
 */
internal fun pushPageHistory(
    history: List<String>, current: String, destination: String
): List<String> {
    if (current == destination) return history
    if (isPrimaryCallTab(destination)) return emptyList()
    val previous = if (isPrimaryCallTab(current)) emptyList() else history
    return (previous + current).takeLast(8)
}

/**
 * Back from any bottom tab returns to the root Recents, without replaying the
 * user's previous tab selections. Nested pages return to the immediate parent.
 */
internal fun backPage(history: List<String>, current: String): Pair<String, List<String>> =
    if (isPrimaryCallTab(current)) "Recents" to emptyList()
    else popPageHistory(history)

/** Missing/empty history falls back to Recents safely. */
internal fun popPageHistory(history: List<String>): Pair<String, List<String>> =
    (history.lastOrNull() ?: "Recents") to history.dropLast(1)
