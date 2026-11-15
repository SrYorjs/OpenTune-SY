package com.arturo254.opentune.ui.original.player.menu
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import com.arturo254.opentune.ui.original.adapters.LocalOriginalPlayerConnection
import com.arturo254.opentune.viewmodels.LyricsMenuViewModel
import com.arturo254.opentune.utils.dataStore
import kotlinx.coroutines.launch
@Composable private fun BackendSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
 MenuSheetSurface(onDismiss, wrapHeight = true, ownGlassSource = true) { _ ->
  Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
 }
}
@Composable fun DeviceOutputSheet(onDismiss: () -> Unit) { BackendSheet(onDismiss) { Text("Device output unavailable") } }
@Composable fun LyricsTranslationSheet(onDismiss: () -> Unit) { BackendSheet(onDismiss) { Text("Parallel translation unavailable") } }
@Composable fun LyricsProviderSheet(onDismiss: () -> Unit) {
 val connection = LocalOriginalPlayerConnection.current ?: return
 val metadata by connection.mediaMetadata.collectAsState()
 val vm: LyricsMenuViewModel = hiltViewModel()
 val results by vm.results.collectAsState()
 val loading by vm.isLoading.collectAsState()
 LaunchedEffect(metadata?.id) { metadata?.let { vm.search(it.id, it.title, it.artists.joinToString { a -> a.name }, it.duration) } }
 DisposableEffect(vm) { onDispose { vm.cancelSearch() } }
 BackendSheet(onDismiss) {
  Text("Lyrics provider")
  Text("Saved source attribution unavailable")
  if (loading) CircularProgressIndicator()
  results.forEach { result -> TextButton(onClick = { metadata?.let { vm.updateLyrics(it, result.lyrics) }; onDismiss() }) { Text(result.providerName) } }
  if (!loading && results.isEmpty()) Text("No results")
 }
}
@Composable fun LyricsEditorSheet(onDismiss: () -> Unit) {
 val connection = LocalOriginalPlayerConnection.current ?: return
 val metadata by connection.mediaMetadata.collectAsState()
 val entity by connection.currentLyrics.collectAsState(initial = null)
 val vm: LyricsMenuViewModel = hiltViewModel()
 var edited by remember(entity?.id, entity?.lyrics) { mutableStateOf(entity?.lyrics.orEmpty()) }
 BackendSheet(onDismiss) {
  Text("Edit lyrics")
  OutlinedTextField(value = edited, onValueChange = { edited = it }, modifier = Modifier.fillMaxWidth())
  TextButton(enabled = metadata != null, onClick = { metadata?.let { vm.updateLyrics(it, edited) }; onDismiss() }) { Text("Save") }
 }
}
@Composable fun LyricsOffsetSheet(onDismiss: () -> Unit) {
 val connection = LocalOriginalPlayerConnection.current ?: return
 val offset by connection.lyricsOffsetMs.collectAsState()
 val store = LocalContext.current.dataStore
 val scope = rememberCoroutineScope()
 BackendSheet(onDismiss) {
  Text("Lyrics offset: " + offset + " ms")
  Row { listOf(-100, 100).forEach { step -> TextButton(onClick = { scope.launch { store.edit { it[com.arturo254.opentune.constants.LyricsSyncOffsetKey] = offset + step } } }) { Text(if (step > 0) "+100 ms" else "-100 ms") } } }
 }
}
@Composable fun SongDetailsSheet(onDismiss: () -> Unit, onOpenArtist: (String) -> Unit, onOpenAlbum: (String) -> Unit) {
 val connection = LocalOriginalPlayerConnection.current ?: return
 val metadata by connection.mediaMetadata.collectAsState()
 val song by connection.currentSong.collectAsState(initial = null)
 BackendSheet(onDismiss) {
  metadata?.let { current ->
   Text(current.title)
   current.artists.forEach { artist -> if (artist.id != null) TextButton(onClick = { onOpenArtist(artist.id) }) { Text(artist.name) } else Text(artist.name) }
   val album = current.album
   if (album != null) TextButton(onClick = { onOpenAlbum(album.id) }) { Text(album.title) }
   else song?.album?.let { dbAlbum -> TextButton(onClick = { onOpenAlbum(dbAlbum.id) }) { Text(dbAlbum.title) } }
  }
 }
}
