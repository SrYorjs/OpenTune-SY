package com.arturo254.opentune.ui.original.player
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.arturo254.opentune.constants.LyricsRomanizeJapaneseKey
import com.arturo254.opentune.constants.LyricsRomanizeKoreanKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.LyricsEntity
import com.arturo254.opentune.ui.original.lyrics.LyricsEntry
import com.arturo254.opentune.lyrics.LyricsUtils as GalaxLyrics
import com.arturo254.opentune.utils.dataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
/** Real existing whole-line romanization; per-word ruby and parallel translation remain unavailable in C1. */
@Composable internal fun LyricsReadings(lines: List<LyricsEntry>, mediaId: String?, database: MusicDatabase, entity: LyricsEntity?) {
 val context = LocalContext.current
 LaunchedEffect(lines, mediaId, entity) {
  context.dataStore.data.collectLatest { preferences ->
   withContext(Dispatchers.Default) { lines.forEach { entry ->
    entry.romanizedTextFlow.value = when {
     preferences[LyricsRomanizeJapaneseKey] == true && GalaxLyrics.isJapanese(entry.text) && !GalaxLyrics.isChinese(entry.text) -> GalaxLyrics.romanizeJapanese(entry.text)
     preferences[LyricsRomanizeKoreanKey] == true && GalaxLyrics.isKorean(entry.text) -> GalaxLyrics.romanizeKorean(entry.text)
     else -> null
    }
   }}
  }
 }
}
