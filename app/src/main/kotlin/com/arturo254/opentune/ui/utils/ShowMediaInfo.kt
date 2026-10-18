package com.arturo254.opentune.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.component.Artwork
import com.arturo254.opentune.core.designsystem.theme.GalaxDestinationTheme
import com.arturo254.opentune.core.designsystem.theme.GalaxScreenStyle
import com.arturo254.opentune.core.designsystem.theme.galaxScreenBackground
import com.arturo254.opentune.db.entities.FormatEntity
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.MediaInfo
import com.arturo254.opentune.ui.component.MenuSurfaceSection
import com.arturo254.opentune.ui.component.NewMenuItem
import kotlinx.coroutines.CancellationException
import java.text.NumberFormat

private data class MediaDetail(val key: String, val label: String, val value: String, val icon: Int)

@Composable
fun ShowMediaInfo(videoId: String) {
    if (videoId.isBlank()) return
    val database = LocalDatabase.current
    val connection = LocalPlayerConnection.current
    val context = LocalContext.current
    var info by remember(videoId) { mutableStateOf<MediaInfo?>(null) }
    var song by remember(videoId) { mutableStateOf<Song?>(null) }
    var format by remember(videoId) { mutableStateOf<FormatEntity?>(null) }
    var loading by remember(videoId) { mutableStateOf(true) }
    var attempt by remember(videoId) { mutableIntStateOf(0) }
    LaunchedEffect(videoId, attempt) {
        loading = true
        try {
            info = YouTube.getMediaInfo(videoId).getOrNull()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            info = null
        } finally {
            loading = false
        }
    }
    LaunchedEffect(videoId) { database.song(videoId).collect { song = it } }
    LaunchedEffect(videoId) { database.format(videoId).collect { format = it } }
    val unknown = stringResource(R.string.unknown)
    val details = buildList {
        add(MediaDetail("id", stringResource(R.string.media_id), videoId, R.drawable.tag))
        format?.let { current ->
            add(MediaDetail("itag", "Itag", current.itag.toString(), R.drawable.code))
            add(MediaDetail("mime", stringResource(R.string.mime_type), current.mimeType ?: unknown, R.drawable.memory))
            add(MediaDetail("codec", stringResource(R.string.codecs), current.codecs ?: unknown, R.drawable.tune))
            add(MediaDetail("rate", stringResource(R.string.bitrate), current.bitrate?.let { "${it / 1000} Kbps" } ?: unknown, R.drawable.graphic_eq))
            add(MediaDetail("sample", stringResource(R.string.sample_rate), current.sampleRate?.let { "$it Hz" } ?: unknown, R.drawable.equalizer))
            add(MediaDetail("size", stringResource(R.string.file_size), current.contentLength?.let { Formatter.formatShortFileSize(context, it) } ?: unknown, R.drawable.folder))
        }
        connection?.let { add(MediaDetail("volume", stringResource(R.string.volume), "${(it.player.volume * 100).toInt()}%", R.drawable.volume_up)) }
    }
    fun copy(value: String) {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("GalaxOS", value))
        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
    }
    GalaxDestinationTheme {
        LazyColumn(modifier = Modifier.fillMaxSize().galaxScreenBackground().windowInsetsPadding(WindowInsets.systemBars),
            contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item("heading", contentType = "heading") {
                Text(stringResource(R.string.details), style = MaterialTheme.typography.headlineLarge)
            }
            item("track", contentType = "track") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Artwork(song?.song?.thumbnailUrl, Modifier.size(88.dp), shape = GalaxScreenStyle.artworkShape)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(song?.title ?: info?.title ?: unknown, style = MaterialTheme.typography.titleLarge,
                            maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(song?.artists?.joinToString { it.name } ?: info?.author ?: unknown,
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(details, key = { it.key }, contentType = { "detail" }) { detail ->
                MenuSurfaceSection {
                    NewMenuItem(headlineContent = { Text(detail.value, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(detail.label) },
                        leadingContent = { Icon(painterResource(detail.icon), null) }, onClick = { copy(detail.value) })
                }
            }
            item("information", contentType = "heading") {
                Text(stringResource(R.string.information), style = MaterialTheme.typography.titleLarge)
            }
            if (loading) item("loading", contentType = "status") {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else if (info == null) item("retry", contentType = "status") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.no_results_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.retry)) }
                }
            }
            info?.let { remote ->
                item("description", contentType = "description") {
                    SelectionContainer {
                        Text(remote.description.orEmpty(), style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item("statistics", contentType = "statistics") {
                    val stats = listOf(
                        stringResource(R.string.subscribers) to remote.subscribers.orEmpty().ifBlank { unknown },
                        stringResource(R.string.views) to formatMediaCount(remote.viewCount).ifBlank { unknown },
                        stringResource(R.string.likes) to formatMediaCount(remote.like).ifBlank { unknown },
                        stringResource(R.string.dislikes) to formatMediaCount(remote.dislike).ifBlank { unknown },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        stats.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { (label, value) ->
                                    MenuSurfaceSection(Modifier.weight(1f)) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(value, style = MaterialTheme.typography.titleLarge)
                                            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMediaCount(value: Any?): String {
    val text = value?.toString().orEmpty()
    return text.toLongOrNull()?.let { NumberFormat.getIntegerInstance().format(it) } ?: text
}
