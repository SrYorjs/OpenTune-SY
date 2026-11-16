package com.arturo254.opentune.ui.original

import com.arturo254.opentune.ui.original.lyrics.*
import org.junit.Assert.*
import org.junit.Test

class OriginalWordPiecesTest {
    @Test fun repeatedWordsUseTheirOwnTextOffsetsAndRetainEverySourceTime() {
        val pieces = listOf(WordTimestamp("ha", 1.0, 1.1), WordTimestamp("cer ", 1.1, 1.5),
            WordTimestamp("ha", 2.0, 2.2), WordTimestamp("cer", 2.2, 2.8))
        val entry = LyricsEntry(1000L, "hacer hacer", pieces).withOriginalWordSpacing()
        assertEquals(listOf("hacer", "hacer"), entry.words!!.map { it.text })
        assertEquals(listOf(1.0 to 1.5, 2.0 to 2.8), entry.words!!.map { it.startTime to it.endTime })
        assertEquals(pieces, entry.syllables)
        assertEquals("hacer hacer", entry.text)
    }

    @Test fun unspacedScriptsAndUnmatchedSourcePiecesAreNeverGuessed() {
        val han = LyricsEntry(0L, "你好", listOf(WordTimestamp("你", 0.0, 1.0), WordTimestamp("好", 1.0, 2.0)))
        assertSame(han, han.withOriginalWordSpacing())
        val missing = LyricsEntry(0L, "real text", listOf(WordTimestamp("absent", 0.0, 1.0), WordTimestamp("text", 1.0, 2.0)))
        assertSame(missing, missing.withOriginalWordSpacing())
        assertNull(missing.withOriginalWordSpacing().rubyFlow.value)
    }
}
