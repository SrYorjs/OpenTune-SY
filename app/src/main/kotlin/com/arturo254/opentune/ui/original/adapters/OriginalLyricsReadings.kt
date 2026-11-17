package com.arturo254.opentune.ui.original.adapters

import com.arturo254.opentune.lyrics.LyricsUtils
import com.arturo254.opentune.ui.original.lyrics.LyricsEntry
import com.arturo254.opentune.ui.original.lyrics.RubyToken

internal fun hasJapaneseKana(text: String): Boolean = text.codePoints().anyMatch {
    Character.UnicodeScript.of(it) in setOf(Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA)
}

internal suspend fun japaneseRubyTokens(text: String): List<RubyToken>? {
    val tokens = LyricsUtils.japaneseTokenReadings(text)
    return tokens.mapIndexedNotNull { index, token ->
        val reading = token.reading ?: return@mapIndexedNotNull null
        if (token.position < 0 || token.end > text.length || text.substring(token.position, token.end) != token.surface) return@mapIndexedNotNull null
        if (!token.surface.codePoints().anyMatch {
            Character.UnicodeScript.of(it) in setOf(Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA, Character.UnicodeScript.HAN)
        }) return@mapIndexedNotNull null
        val romanized = LyricsUtils.katakanaToRomaji(reading, tokens.getOrNull(index + 1)?.reading).trim()
        romanized.takeIf { it.isNotBlank() && it != token.surface }?.let { RubyToken(token.position, token.end, it) }
    }.takeIf { it.isNotEmpty() }
}

internal suspend fun applyOriginalReading(entry: LyricsEntry, japaneseBody: Boolean, japaneseEnabled: Boolean, koreanEnabled: Boolean) {
    val japanese = japaneseEnabled && japaneseBody && !LyricsUtils.isKorean(entry.text)
    entry.rubyFlow.value = if (japanese) japaneseRubyTokens(entry.text) else null
    entry.romanizedTextFlow.value = if (koreanEnabled && LyricsUtils.isKorean(entry.text)) {
        LyricsUtils.romanizeKorean(entry.text)
    } else null
}
