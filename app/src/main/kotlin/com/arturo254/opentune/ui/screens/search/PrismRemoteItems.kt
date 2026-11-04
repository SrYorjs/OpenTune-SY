package com.arturo254.opentune.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.innertube.models.*
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.*

/** The catalogue keeps its native actions while sharing Search's visual language. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PrismRemoteItem(
    entry: YTItem,
    navController: NavController,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    compact: Boolean = false,
) {
    val connection = LocalPlayerConnection.current
    val metadata = connection?.mediaMetadata?.collectAsStateWithLifecycle()?.value
    val playing = connection?.isPlaying?.collectAsStateWithLifecycle()?.value == true
    val menu = LocalMenuState.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val active =
        when (entry) {
            is SongItem -> metadata?.id == entry.id
            is AlbumItem -> metadata?.album?.id == entry.id
            else -> false
        }
    val open: () -> Unit = {
        when (entry) {
            is SongItem -> {
                if (active) connection?.player?.togglePlayPause()
                else
                    connection?.playQueue(
                        YouTubeQueue(
                            endpoint = entry.endpoint ?: WatchEndpoint(videoId = entry.id),
                            preloadItem = entry.toMediaMetadata(),
                        )
                    )
            }
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
                is PlaylistItem ->
                    YouTubePlaylistMenu(entry, coroutineScope = scope, onDismiss = menu::dismiss)
            }
        }
    }
    val detail =
        when (entry) {
            is SongItem -> entry.artists.joinToString { it.name }
            is AlbumItem -> entry.artists.orEmpty().joinToString { it.name }
            is ArtistItem -> stringResource(R.string.artists)
            is PlaylistItem -> entry.author?.name ?: stringResource(R.string.playlists)
        }
    val artworkShape = if (entry is ArtistItem) CircleShape else RoundedCornerShape(20.dp)
    if (compact || rank != null) {
        Row(
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceContainer.copy(
                        alpha = if (active) .9f else .48f
                    )
                )
                .combinedClickable(
                    role = Role.Button,
                    onClick = open,
                    onLongClick = showMenu,
                    onLongClickLabel = stringResource(R.string.more_options),
                )
                .padding(start = 12.dp, end = 2.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (rank != null)
                Text(
                    rank.toString().padStart(2, '0'),
                    Modifier.widthIn(min = 22.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            Box(Modifier.size(56.dp).clip(artworkShape)) {
                PrismArtwork(entry.thumbnail, Modifier.fillMaxSize())
                if (active && playing) PrismPlaybackMark(Modifier.align(Alignment.BottomEnd))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = showMenu) {
                Icon(painterResource(R.drawable.more_vert), stringResource(R.string.more_options))
            }
        }
    } else {
        Column(
            modifier.combinedClickable(
                role = Role.Button,
                onClick = open,
                onLongClick = showMenu,
                onLongClickLabel = stringResource(R.string.more_options),
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(artworkShape)) {
                PrismArtwork(entry.thumbnail, Modifier.fillMaxSize())
                IconButton(onClick = showMenu, modifier = Modifier.align(Alignment.TopEnd)) {
                    Box(
                        Modifier.size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = .88f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.more_vert),
                            stringResource(R.string.more_options),
                            Modifier.size(20.dp),
                        )
                    }
                }
                if (active && playing)
                    PrismPlaybackMark(Modifier.align(Alignment.BottomEnd).padding(10.dp))
            }
            Text(
                entry.title,
                Modifier.padding(horizontal = 2.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                detail,
                Modifier.padding(horizontal = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PrismPlaybackMark(modifier: Modifier) {
    Box(
        modifier.size(28.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.pause),
            stringResource(R.string.prism_results_pause),
            Modifier.size(17.dp),
            tint = MaterialTheme.colorScheme.onTertiary,
        )
    }
}

@Composable
internal fun PrismRemoteShelf(entries: List<YTItem>, navController: NavController, tileWidth: Dp) {
    LazyRow(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(
            entries.distinctBy { it.javaClass.simpleName + it.id },
            key = { it.javaClass.simpleName + ":" + it.id },
        ) { entry ->
            PrismRemoteItem(entry, navController, Modifier.width(tileWidth))
        }
    }
}
