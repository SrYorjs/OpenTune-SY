package com.arturo254.opentune.ui.original

import com.arturo254.opentune.lyrics.LyricsEntry
import com.arturo254.opentune.lyrics.WordTimestamp
import com.arturo254.opentune.ui.original.adapters.toOriginalMiniLine
import org.junit.Assert.*
import org.junit.Test

class OriginalMiniLyricsAdapterTest {
    @Test fun keepsSourceTimingAndSharesLiveRomanizationWithoutInventingReadings() {
        val source = LyricsEntry(1250L, "歌", listOf(WordTimestamp("歌", 1.25, 2.8)), agent = "voice-2")
        val mapped = source.toOriginalMiniLine()
        assertEquals(1250L, mapped.time)
        assertEquals("歌", mapped.text)
        assertEquals("voice-2", mapped.agent)
        assertEquals(1.25, mapped.words!!.single().startTime, 0.0)
        assertEquals(2.8, mapped.words!!.single().endTime, 0.0)
        source.romanizedTextFlow.value = "uta"
        assertEquals("uta", mapped.romanizedTextFlow.value)
        assertNull(mapped.rubyFlow.value)
        assertNull(mapped.translatedTextFlow.value)
        assertNull(mapped.syllables)
    }

    @Test fun preservesBackingVoiceAndAbsenceOfWordTiming() {
        val backing = LyricsEntry(3000L, "oh", listOf(WordTimestamp("oh", 3.0, 4.0, isBackground = true)))
        assertTrue(backing.toOriginalMiniLine().isBackground)
        val plain = LyricsEntry(5000L, "line").toOriginalMiniLine()
        assertNull(plain.words)
        assertFalse(plain.isBackground)
    }
}
