package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.innertube.models.*
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.YouTubeGridItem
import com.arturo254.opentune.ui.component.YouTubeListItem
import com.arturo254.opentune.ui.menu.YouTubeAlbumMenu
import com.arturo254.opentune.ui.menu.YouTubeArtistMenu
import com.arturo254.opentune.ui.menu.YouTubePlaylistMenu
import com.arturo254.opentune.ui.menu.YouTubeSongMenu

@Composable
fun GalaxRemoteShelf(entries: List<YTItem>, navController: NavController, modifier: Modifier = Modifier) {
    val unique = remember(entries) { entries.distinctBy { it.id } }
    LazyRow(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(unique, key = { it.id }, contentType = { it::class.simpleName }) { entry ->
            GalaxRemoteItem(entry, navController, Modifier.width(176.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalaxRemoteItem(entry: YTItem, navController: NavController, modifier: Modifier = Modifier,
    ranked: Int? = null, onClick: (() -> Unit)? = null) {
    val connection = LocalPlayerConnection.current ?: return
    val metadata by connection.mediaMetadata.collectAsState()
    val playing by connection.isPlaying.collectAsState()
    val menu = LocalMenuState.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val active = when (entry) {
        is SongItem -> metadata?.id == entry.id
        is AlbumItem -> metadata?.album?.id == entry.id
        else -> false
    }
    val open = onClick ?: {
        when (entry) {
            is SongItem -> if (active) connection.player.togglePlayPause() else
                connection.playQueue(YouTubeQueue(endpoint = WatchEndpoint(videoId = entry.id), preloadItem = entry.toMediaMetadata()))
            is AlbumItem -> navController.navigate("album/${entry.id}")
            is ArtistItem -> navController.navigate("artist/${entry.id}")
            is PlaylistItem -> navController.navigate("online_playlist/${entry.id}")
        }
    }
    val showMenu = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menu.show {
            when (entry) {
                is SongItem -> YouTubeSongMenu(entry, navController, onDismiss = menu::dismiss)
                is AlbumItem -> YouTubeAlbumMenu(entry, navController, onDismiss = menu::dismiss)
                is ArtistItem -> YouTubeArtistMenu(entry, onDismiss = menu::dismiss)
                is PlaylistItem -> YouTubePlaylistMenu(entry, coroutineScope = scope, onDismiss = menu::dismiss)
            }
        }
    }
    if (ranked != null) Row(modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(ranked.toString().padStart(2, '0'), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(30.dp))
        YouTubeListItem(item = entry, isActive = active, isPlaying = playing,
            trailingContent = { IconButton(onClick = showMenu) {
                Icon(painterResource(R.drawable.more_vert), stringResource(R.string.more_options))
            } }, modifier = Modifier.weight(1f).combinedClickable(onClick = open, onLongClick = showMenu))
    } else YouTubeGridItem(item = entry, isActive = active, isPlaying = playing, fillMaxWidth = true,
        coroutineScope = scope, modifier = modifier.combinedClickable(onClick = open, onLongClick = showMenu))
}
