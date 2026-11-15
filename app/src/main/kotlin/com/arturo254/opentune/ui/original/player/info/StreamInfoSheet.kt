package com.arturo254.opentune.ui.original.player.info
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.ui.original.adapters.LocalOriginalPlayerConnection
import com.arturo254.opentune.ui.original.player.menu.MenuSheetSurface
@Composable fun StreamInfoSheet(onDismiss: () -> Unit) {
 val connection = LocalOriginalPlayerConnection.current ?: return
 val metadata by connection.mediaMetadata.collectAsState()
 val format by connection.galaxConnection.currentFormat.collectAsState(initial = null)
 val id = metadata?.id ?: return
 val facts by remember(connection, id) { connection.streamFacts(id) }.collectAsState(initial = null)
 MenuSheetSurface(onDismiss, wrapHeight = true, ownGlassSource = true) { _ -> Column(Modifier.padding(20.dp)) {
  Text("Stream information")
  Text(facts?.describe(format) ?: "Format unavailable")
  format?.mimeType?.let { Text(it) }
  format?.codecs?.let { Text(it) }
 } }
}
