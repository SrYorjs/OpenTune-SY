package com.arturo254.opentune.ui.screens.library

internal fun <T> moveLibraryEntry(
    entries: MutableList<T>,
    fromKey: Any,
    toKey: Any,
    key: (T) -> Any,
): Pair<Int, Int>? {
    val from = entries.indexOfFirst { key(it) == fromKey }
    val to = entries.indexOfFirst { key(it) == toKey }
    if (from < 0 || to < 0 || from == to) return null
    entries.add(to, entries.removeAt(from))
    return from to to
}
