package com.arturo254.opentune.ui.original.player.share.song
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.ui.original.lyrics.LyricsEntry
import com.arturo254.opentune.ui.original.player.share.LyricsShareSession
/** GalaxOT native text sharing. The original image/video editor is deferred to operation 12. */
@Composable internal fun LyricsSongShare(song: MediaMetadata, pick: LyricsShareSession, lines: List<LyricsEntry>, onDismiss: () -> Unit) {
 val context = LocalContext.current
 LaunchedEffect(pick) {
  try {
   val text = pick.chosen.joinToString("\n") { it.second } + "\n\n" + song.title + " ? " + song.artists.joinToString { it.name }
   context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
  } finally { onDismiss() }
 }
}
