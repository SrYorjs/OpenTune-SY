package com.arturo254.opentune.ui.original

import com.arturo254.opentune.lyrics.LyricsUtils
import com.arturo254.opentune.ui.original.adapters.applyOriginalReading
import com.arturo254.opentune.ui.original.adapters.hasJapaneseKana
import com.arturo254.opentune.ui.original.adapters.japaneseRubyTokens
import com.arturo254.opentune.ui.original.lyrics.LyricsEntry
import com.arturo254.opentune.ui.original.lyrics.WordTimestamp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class OriginalJapaneseRubyTest {
    @Test fun kuromojiRetainsSurfacesReadingsAndUtf16PositionsIncludingRepeatedWords() = runBlocking {
        val text = "🧪 東京で東京を見る"
        val tokens = LyricsUtils.japaneseTokenReadings(text)
        tokens.forEach { assertEquals(it.surface, text.substring(it.position, it.end)) }
        val tokyo = tokens.filter { it.surface == "東京" }
        assertEquals(listOf(3, 6), tokyo.map { it.position })
        assertTrue(tokyo.all { it.reading == "トウキョウ" })
        assertTrue(tokens.any { it.surface == "🧪" && it.reading == null })
        val ruby = japaneseRubyTokens(text)!!
        assertEquals(listOf(3, 6), ruby.filter { text.substring(it.start, it.end) == "東京" }.map { it.start })
        assertTrue(ruby.filter { text.substring(it.start, it.end) == "東京" }.all { it.text == "toukyou" })
    }

    @Test fun unknownLatinAndPunctuationNeverBecomeInventedReadings() = runBlocking {
        for (text in listOf("🧪", "XYZ", "。！？")) assertNull(japaneseRubyTokens(text))
    }

    @Test fun providerSyllablesDoNotDuplicateAWholeTokensReadingOrChangeTiming() = runBlocking {
        val syllables = listOf(WordTimestamp("東", 1.0, 1.2), WordTimestamp("京", 1.2, 1.5), WordTimestamp("へ", 1.5, 2.0))
        val entry = LyricsEntry(1000, "東京へ", words = syllables, syllables = syllables)
        applyOriginalReading(entry, japaneseBody = true, japaneseEnabled = true, koreanEnabled = true)
        assertEquals(1, entry.rubyFlow.value!!.count { it.start == 0 && it.end == 2 })
        assertSame(syllables, entry.words)
        assertSame(syllables, entry.syllables)
        assertEquals(1000L, entry.time)
        assertNull(entry.romanizedTextFlow.value)
    }

    @Test fun japaneseBodyAllowsKanjiOnlyLinesAndLeavesChineseOutsideTheFeature() = runBlocking {
        assertTrue(hasJapaneseKana("君を見ている"))
        assertFalse(hasJapaneseKana("你好世界"))
        val japanese = LyricsEntry(0, "東京")
        applyOriginalReading(japanese, japaneseBody = true, japaneseEnabled = true, koreanEnabled = true)
        assertFalse(japanese.rubyFlow.value.isNullOrEmpty())
        val chinese = LyricsEntry(0, "你好世界")
        applyOriginalReading(chinese, japaneseBody = false, japaneseEnabled = true, koreanEnabled = true)
        assertNull(chinese.rubyFlow.value)
        assertNull(chinese.romanizedTextFlow.value)
    }

    @Test fun koreanStaysAWholeLineAndDisablingPreferencesClearsPreviousReadings() = runBlocking {
        val korean = LyricsEntry(0, "안녕하세요")
        applyOriginalReading(korean, japaneseBody = true, japaneseEnabled = true, koreanEnabled = true)
        assertEquals(LyricsUtils.romanizeKorean(korean.text), korean.romanizedTextFlow.value)
        assertNull(korean.rubyFlow.value)
        applyOriginalReading(korean, japaneseBody = true, japaneseEnabled = true, koreanEnabled = false)
        assertNull(korean.romanizedTextFlow.value)
        val japanese = LyricsEntry(0, "東京へ")
        applyOriginalReading(japanese, japaneseBody = true, japaneseEnabled = true, koreanEnabled = true)
        assertFalse(japanese.rubyFlow.value.isNullOrEmpty())
        applyOriginalReading(japanese, japaneseBody = true, japaneseEnabled = false, koreanEnabled = true)
        assertNull(japanese.rubyFlow.value)
        assertNull(japanese.romanizedTextFlow.value)
    }
}
