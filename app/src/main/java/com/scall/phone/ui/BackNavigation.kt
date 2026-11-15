package com.scall.phone.ui

/** Small, saved back-stack for Compose destinations, without replacing Android's root Back. */
internal fun pushPageHistory(history: List<String>, current: String, destination: String): List<String> =
    if (current == destination) history else (history + current).takeLast(16)

/** Empty history returns the root Recents screen. */
internal fun popPageHistory(history: List<String>): Pair<String, List<String>> =
    (history.lastOrNull() ?: "Recents") to history.dropLast(1)
