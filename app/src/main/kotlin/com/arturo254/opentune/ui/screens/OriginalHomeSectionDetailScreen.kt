package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.db.entities.LocalItem
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.AlbumMenu
import com.arturo254.opentune.ui.menu.ArtistMenu
import com.arturo254.opentune.ui.menu.PlaylistMenu
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.visual.component.HomeContentInset
import com.arturo254.opentune.ui.visual.component.HomeLabelStyle
import com.arturo254.opentune.ui.visual.component.HomeTitleStyle
import com.arturo254.opentune.ui.visual.component.HomeTrackRow
import com.arturo254.opentune.ui.visual.component.rememberRevealSeenState
import com.arturo254.opentune.ui.visual.component.revealOnAppear
import com.arturo254.opentune.viewmodels.HomeViewModel

@Composable
fun OriginalHomeSectionDetailScreen(
    navController: NavController,
    sectionId: String,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val speedDial by viewModel.speedDialSongs.collectAsState()
    val quickPicks by viewModel.quickPicks.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val mostPlayed by viewModel.mostPlayedSongsForHome.collectAsState()
    val forgotten by viewModel.forgottenFavorites.collectAsState()
    val recentHistory by viewModel.recentHistorySongs.collectAsState()
    val player = LocalPlayerConnection.current
    val playing by player?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val metadata by player?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }
    val menus = LocalMenuState.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val seen = rememberRevealSeenState()
    val listState = rememberLazyListState()
    var query by remember(sectionId) { mutableStateOf("") }

    val title = when (sectionId) {
        "speed_dial" -> "Speed dial"
        "quick_picks" -> "Quick picks"
        "forgotten_favorites" -> "Don't forget these songs"
        "recently_played" -> "Recently played"
        "most_played" -> "Most played"
        "history" -> "History"
        else -> "Your music"
    }
    val items: List<LocalItem> = when (sectionId) {
        "speed_dial" -> speedDial
        "quick_picks" -> quickPicks.orEmpty()
        "forgotten_favorites" -> forgotten.orEmpty()
        "recently_played" -> keepListening.orEmpty()
        "most_played" -> mostPlayed.orEmpty()
        "history" -> recentHistory.orEmpty()
        else -> emptyList()
    }
    val visible = remember(items, query) {
        items.filter { item ->
            item.title.contains(query, ignoreCase = true) ||
                when (item) {
                    is Song -> item.artists.any { it.name.contains(query, ignoreCase = true) }
                    is Album -> item.artists.any { it.name.contains(query, ignoreCase = true) }
                    is Artist -> false
                    is Playlist -> false
                }
        }
    }
    val localSongs = remember(items) { items.filterIsInstance<Song>() }
    val isCurrentQueue = metadata?.id in localSongs.map { it.id }
    val playAll: (Song?, Boolean) -> Unit = { start, shuffle ->
        val ordered = if (shuffle) localSongs.shuffled() else localSongs
        if (ordered.isNotEmpty()) {
            player?.playQueue(
                ListQueue(
                    title = title,
                    items = ordered.map { it.toMediaItem() },
                    startIndex = ordered.indexOfFirst { it.id == start?.id }.coerceAtLeast(0),
                )
            )
        }
    }
    val open: (LocalItem) -> Unit = { item ->
        when (item) {
            is Song -> {
                if (metadata?.id == item.id) player?.player?.togglePlayPause()
                else playAll(item, false)
            }
            is Album -> navController.navigate("album/${item.id}")
            is Artist -> navController.navigate("artist/${item.id}")
            is Playlist -> navController.navigate(
                if (item.id == "liked") "auto_playlist/liked" else "local_playlist/${item.id}"
            )
        }
    }
    val showMenu: (LocalItem) -> Unit = { item ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menus.show {
            when (item) {
                is Song -> SongMenu(originalSong = item, navController = navController, onDismiss = menus::dismiss)
                is Album -> AlbumMenu(originalAlbum = item, navController = navController, onDismiss = menus::dismiss)
                is Artist -> ArtistMenu(originalArtist = item, coroutineScope = scope, onDismiss = menus::dismiss)
                is Playlist -> PlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = menus::dismiss)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 24.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = title,
                style = HomeTitleStyle,
                color = Color.White,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = HomeContentInset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${items.size} items",
                style = HomeLabelStyle,
                color = Color.White.copy(alpha = 0.57f),
            )
            Row {
                if (localSongs.isNotEmpty()) {
                    TextButton(onClick = { playAll(null, true) }) { Text("Shuffle") }
                    TextButton(onClick = {
                        if (isCurrentQueue) player?.player?.togglePlayPause()
                        else playAll(null, false)
                    }) { Text(if (isCurrentQueue && playing) "Pause" else "Play") }
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search in $title") },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = HomeContentInset, vertical = 12.dp),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = HomeContentInset,
                end = HomeContentInset,
                bottom = 160.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            itemsIndexed(
                items = visible,
                key = { index, item -> "${item.javaClass.simpleName}_${item.id}_$index" },
            ) { index, item ->
                val subtitle = when (item) {
                    is Song -> item.artists.joinToString(", ") { it.name }
                    is Album -> item.artists.joinToString(", ") { it.name }
                    is Artist -> "Artist"
                    is Playlist -> "${item.songCount} songs"
                }
                HomeTrackRow(
                    thumbnailUrl = (item as? Playlist)?.thumbnails?.firstOrNull() ?: item.thumbnailUrl,
                    title = item.title,
                    subtitle = subtitle,
                    songId = (item as? Song)?.id,
                    coverShape = if (item is Artist) CircleShape else RoundedCornerShape(9.dp),
                    isActive = item.id == metadata?.id,
                    isPlaying = playing,
                    onClick = { open(item) },
                    onLongClick = { showMenu(item) },
                    onMoreClick = { showMenu(item) },
                    modifier = Modifier.revealOnAppear(
                        key = "${sectionId}_${item.id}",
                        seenState = seen,
                        delayMillis = minOf(index, 8) * 28,
                    ),
                )
            }
        }
    }
}
