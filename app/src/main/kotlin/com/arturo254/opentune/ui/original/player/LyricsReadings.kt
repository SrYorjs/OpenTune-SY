package com.arturo254.opentune.ui.original.player
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.arturo254.opentune.constants.LyricsRomanizeJapaneseKey
import com.arturo254.opentune.constants.LyricsRomanizeKoreanKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.LyricsEntity
import com.arturo254.opentune.ui.original.lyrics.LyricsEntry
import com.arturo254.opentune.ui.original.adapters.applyOriginalReading
import com.arturo254.opentune.ui.original.adapters.hasJapaneseKana
import com.arturo254.opentune.utils.dataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
@Composable internal fun LyricsReadings(lines: List<LyricsEntry>, mediaId: String?, database: MusicDatabase, entity: LyricsEntity?) {
 val context = LocalContext.current
 LaunchedEffect(lines, mediaId, entity) {
  val japaneseBody = lines.any { hasJapaneseKana(it.text) }
  context.dataStore.data.collectLatest { preferences ->
   withContext(Dispatchers.Default) { lines.forEach { entry ->
    applyOriginalReading(entry, japaneseBody, preferences[LyricsRomanizeJapaneseKey] != false, preferences[LyricsRomanizeKoreanKey] != false)
   }}
  }
 }
}
