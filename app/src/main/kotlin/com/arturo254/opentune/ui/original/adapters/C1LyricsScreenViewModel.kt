package com.arturo254.opentune.ui.original.adapters
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.LyricsEntity
import com.arturo254.opentune.lyrics.LyricsHelper
import com.arturo254.opentune.models.MediaMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
@HiltViewModel
class C1LyricsScreenViewModel @Inject constructor(val database: MusicDatabase, private val helper: LyricsHelper): ViewModel() {
 private val pending = mutableSetOf<String>()
 fun ensureLyricsLoaded(metadata: MediaMetadata) {
  if (!pending.add(metadata.id)) return
  viewModelScope.launch {
   try { withContext(Dispatchers.IO) {
    if (database.lyrics(metadata.id).first() == null) {
     val raw = helper.getLyrics(metadata)
     database.query { upsert(LyricsEntity(metadata.id, raw)) }
    }
   }} catch (cancelled: CancellationException) { throw cancelled }
   catch (failure: Exception) { com.arturo254.opentune.utils.reportException(failure) }
   finally { pending.remove(metadata.id) }
  }
 }
}
