package com.arturo254.opentune.ui.original.lyrics

/** Original mergeWordPieces semantics, using only boundaries present in the source text.
 * Keep every timed piece for the main sweep; word-based styles must not insert gaps in a word.
 * Unlike the upstream missing-space heuristic, this never guesses absent source whitespace.
 */
internal fun LyricsEntry.withOriginalWordSpacing(): LyricsEntry {
    val pieces = words?.takeIf { it.size > 1 } ?: return this
    val merged = ArrayList<WordTimestamp>()
    var searchFrom = 0
    var lastEnd = -1
    fun unspaced(char: Char): Boolean = when (Character.UnicodeScript.of(char.code)) {
        Character.UnicodeScript.HAN, Character.UnicodeScript.HIRAGANA,
        Character.UnicodeScript.KATAKANA, Character.UnicodeScript.THAI -> true
        else -> false
    }
    pieces.filter { it.text.isNotBlank() }.forEach { piece ->
        val part = piece.text.trim()
        val start = text.indexOf(part, searchFrom)
        val previous = merged.lastOrNull()
        val glued = start >= 0 && start == lastEnd && lastEnd > 0 &&
            !unspaced(text[lastEnd - 1]) && !unspaced(text[start])
        if (previous != null && glued) {
            merged[merged.lastIndex] = WordTimestamp(previous.text + part,
                previous.startTime, maxOf(previous.endTime, piece.endTime))
        } else {
            merged += piece.copy(text = part)
        }
        lastEnd = if (start < 0) -1 else start + part.length
        if (lastEnd >= 0) searchFrom = lastEnd
    }
    return if (merged.size == pieces.size) this else copy(words = merged, syllables = syllables ?: pieces)
}
