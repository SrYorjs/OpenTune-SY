package com.arturo254.opentune.ui.original.adapters

import com.arturo254.opentune.constants.LyricsRomanizeJapaneseKey
import com.arturo254.opentune.constants.LyricsRomanizeKoreanKey
import com.arturo254.opentune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.arturo254.opentune.lyrics.LyricsUtils as GalaxLyrics
import com.arturo254.opentune.ui.original.lyrics.LyricsEntry
import com.arturo254.opentune.ui.original.lyrics.WordTimestamp
import com.arturo254.opentune.utils.dataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import me.bush.translator.Language
import me.bush.translator.Translator

data class OriginalMiniLyricsState(val mediaId: String?, val loaded: Boolean, val lines: List<LyricsEntry>)

internal fun com.arturo254.opentune.lyrics.LyricsEntry.toOriginalMiniLine() = LyricsEntry(
    time = time, text = text,
    words = words?.map { WordTimestamp(it.text, it.startTime, it.endTime) },
    agent = agent, romanizedTextFlow = romanizedTextFlow,
    isBackground = words?.takeIf { it.isNotEmpty() }?.all { it.isBackground } == true,
)

@OptIn(ExperimentalCoroutinesApi::class)
fun OriginalPlayerConnection.miniLyrics(translationLanguage: StateFlow<Language?>): Flow<OriginalMiniLyricsState> =
    mediaMetadata.flatMapLatest { metadata ->
        combine(currentLyrics, service.dataStore.data, translationLanguage) { lyrics, preferences, language ->
            Triple(lyrics?.takeIf { it.id == metadata?.id }, preferences, language)
        }.transformLatest { (entity, preferences, language) ->
            val raw = entity?.lyrics?.trim()?.takeUnless { it == LYRICS_NOT_FOUND }
            val lines = withContext(Dispatchers.Default) {
                if (raw.isNullOrBlank()) emptyList() else if (raw.startsWith("[" ) || raw.startsWith("<")) {
                    GalaxLyrics.parseLyrics(raw).map { it.toOriginalMiniLine() }
                } else listOf(LyricsEntry(0L, raw))
            }
            emit(OriginalMiniLyricsState(metadata?.id, metadata == null || entity != null, lines))
            val translator = if (language != null) Translator() else null
            for (entry in lines) {
                try {
                    val romanized = withContext(Dispatchers.Default) {
                        when {
                            preferences[LyricsRomanizeJapaneseKey] == true && GalaxLyrics.isJapanese(entry.text) && !GalaxLyrics.isChinese(entry.text) -> GalaxLyrics.romanizeJapanese(entry.text)
                            preferences[LyricsRomanizeKoreanKey] == true && GalaxLyrics.isKorean(entry.text) -> GalaxLyrics.romanizeKorean(entry.text)
                            else -> null
                        }
                    }
                    entry.romanizedTextFlow.value = romanized
                    if (translator != null && language != null && entry.text.isNotBlank()) {
                        entry.translatedTextFlow.value = withContext(Dispatchers.IO) {
                            translator.translateBlocking(entry.text, language).translatedText
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    com.arturo254.opentune.utils.reportException(failure)
                }
            }
        }
    }
