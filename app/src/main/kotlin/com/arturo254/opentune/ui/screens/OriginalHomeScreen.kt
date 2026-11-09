package com.arturo254.opentune.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.db.entities.LocalItem
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.innertube.models.AlbumItem
import com.arturo254.opentune.innertube.models.ArtistItem
import com.arturo254.opentune.innertube.models.PlaylistItem
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.innertube.models.WatchEndpoint
import com.arturo254.opentune.innertube.models.YTItem
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.AlbumMenu
import com.arturo254.opentune.ui.menu.ArtistMenu
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.menu.YouTubeAlbumMenu
import com.arturo254.opentune.ui.menu.YouTubeArtistMenu
import com.arturo254.opentune.ui.menu.YouTubePlaylistMenu
import com.arturo254.opentune.ui.menu.YouTubeSongMenu
import com.arturo254.opentune.ui.visual.component.HomeCardHighlight
import com.arturo254.opentune.ui.visual.component.HomeCardSpacing
import com.arturo254.opentune.ui.visual.component.HomeContentInset
import com.arturo254.opentune.ui.visual.component.HomeCoverCard
import com.arturo254.opentune.ui.visual.component.HomeHopInCard
import com.arturo254.opentune.ui.visual.component.HomeSectionBones
import com.arturo254.opentune.ui.visual.component.HomeSectionTitle
import com.arturo254.opentune.ui.visual.component.HomeTopGlow
import com.arturo254.opentune.ui.visual.component.HomeTrackRow
import com.arturo254.opentune.ui.visual.component.OriginalHomeHero
import com.arturo254.opentune.ui.visual.component.OriginalHomeHeroItem
import com.arturo254.opentune.ui.visual.component.rememberRevealSeenState
import com.arturo254.opentune.ui.visual.component.revealOnAppear
import com.arturo254.opentune.ui.visual.utils.SnapLayoutInfoProvider
import com.arturo254.opentune.viewmodels.HomeViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.min

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OriginalHomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenSearchTab: () -> Unit,
    onOpenLibraryTab: () -> Unit,
    ambientActive: Boolean,
) {
    val player = LocalPlayerConnection.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val seen = rememberRevealSeenState()
    val quickPicks by viewModel.quickPicks.collectAsStateWithLifecycle()
    val recent by viewModel.keepListening.collectAsStateWithLifecycle()
    val recentActivity by viewModel.recentActivity.collectAsStateWithLifecycle()
    val speedDial by viewModel.speedDialSongs.collectAsStateWithLifecycle()
    val forgotten by viewModel.forgottenFavorites.collectAsStateWithLifecycle()
    val accountPlaylists by viewModel.accountPlaylists.collectAsStateWithLifecycle()
    val homePage by viewModel.homePage.collectAsStateWithLifecycle()
    val trends by viewModel.globalCatalogTrending.collectAsStateWithLifecycle()
    val loading by viewModel.isLoading.collectAsStateWithLifecycle()
    val accountImage by viewModel.accountImageUrl.collectAsStateWithLifecycle()
    val metadataMap by viewModel.allItemsMetadata.collectAsStateWithLifecycle()
    val current by player?.mediaMetadata?.collectAsStateWithLifecycle()
        ?: remember { androidx.compose.runtime.mutableStateOf(null) }
    val playing by player?.isPlaying?.collectAsStateWithLifecycle()
        ?: remember { androidx.compose.runtime.mutableStateOf(false) }
    val songs = remember(quickPicks, recent, speedDial) {
        (quickPicks.orEmpty() + recent.orEmpty().filterIsInstance<Song>() + speedDial)
            .distinctBy { it.id }
    }
    val recentSongs = remember(recentActivity) {
        recentActivity.orEmpty().filterIsInstance<SongItem>()
    }
    val latestSongId = current?.id
    val play: (Song, List<Song>) -> Unit = { song, queue ->
        if (latestSongId == song.id) {
            player?.player?.togglePlayPause()
        } else {
            player?.playQueue(
                ListQueue(
                    title = "Home",
                    items = queue.map { it.toMediaItem() },
                    startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
                )
            )
        }
    }
    val menu: (Song) -> Unit = { song ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menuState.show {
            SongMenu(
                originalSong = song,
                navController = navController,
                metadata = metadataMap[song.id],
                onDismiss = menuState::dismiss,
            )
        }
    }
    val playRemote: (SongItem) -> Unit = { song ->
        if (latestSongId == song.id) player?.player?.togglePlayPause()
        else player?.playQueue(
            YouTubeQueue(
                song.endpoint ?: WatchEndpoint(videoId = song.id),
                song.toMediaMetadata(),
            )
        )
    }
    val openRemote: (YTItem) -> Unit = { item ->
        when (item) {
            is SongItem -> playRemote(item)
            is AlbumItem -> navController.navigate("album/${item.id}")
            is ArtistItem -> navController.navigate("artist/${item.id}")
            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
        }
    }
    val remoteMenu: (YTItem) -> Unit = { item ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menuState.show {
            when (item) {
                is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = menuState::dismiss)
            }
        }
    }
    val localMenu: (LocalItem) -> Unit = { item ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menuState.show {
            when (item) {
                is Song -> SongMenu(originalSong = item, navController = navController, metadata = metadataMap[item.id], onDismiss = menuState::dismiss)
                is Album -> AlbumMenu(originalAlbum = item, navController = navController, onDismiss = menuState::dismiss)
                is Artist -> ArtistMenu(originalArtist = item, coroutineScope = scope, onDismiss = menuState::dismiss)
                is Playlist -> {}
            }
        }
    }
    val openLocal: (LocalItem) -> Unit = { item ->
        when (item) {
            is Song -> play(item, songs.ifEmpty { listOf(item) })
            is Album -> navController.navigate("album/${item.id}")
            is Artist -> navController.navigate("artist/${item.id}")
            is Playlist -> navController.navigate(if (item.id == "liked") "auto_playlist/liked" else "local_playlist/${item.id}")
        }
    }
    val remoteItems = remember(homePage) {
        homePage?.sections.orEmpty().flatMap { it.items }.distinctBy { it.id }
    }
    val heroItems = remember(songs, remoteItems, latestSongId, playing) {
        val own = songs.take(3).map { song ->
            OriginalHomeHeroItem(
                id = song.id,
                title = song.title,
                subtitle = song.artists.joinToString(", ") { it.name },
                artwork = song.thumbnailUrl,
                label = if (latestSongId == song.id) "Now playing" else "Recently played",
                isCurrent = latestSongId == song.id,
                isPlaying = playing,
                onOpen = { play(song, songs) },
            )
        }
        val remote = remoteItems.filterIsInstance<SongItem>().take(3).map { song ->
            OriginalHomeHeroItem(
                id = song.id,
                title = song.title,
                subtitle = song.artists.joinToString(", ") { it.name },
                artwork = song.thumbnail,
                label = "You may also like",
                isCurrent = latestSongId == song.id,
                isPlaying = playing,
                onOpen = { playRemote(song) },
            )
        }
        (own + remote).distinctBy { it.id }.take(5)
    }
    val topEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = topEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(scrollToTop?.value, ambientActive) {
        if (ambientActive && scrollToTop?.value == true) {
            listState.animateScrollToItem(0)
            topEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    LaunchedEffect(homePage?.continuation, ambientActive) {
        val continuation = homePage?.continuation ?: return@LaunchedEffect
        if (!ambientActive) return@LaunchedEffect
        snapshotFlow {
            val layout = listState.layoutInfo
            layout.totalItemsCount > 0 &&
                (layout.visibleItemsInfo.lastOrNull()?.index ?: -1) >= layout.totalItemsCount - 3
        }.distinctUntilChanged().filter { it }.collect {
            viewModel.loadMoreYouTubeItems(continuation)
        }
    }
    val collapsedScroll by remember(listState) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0)
                (listState.firstVisibleItemScrollOffset / 550f).coerceIn(0f, 1f)
            else 1f
        }
    }
    val albumCovers = remember(songs) { songs.mapNotNull { it.thumbnailUrl }.take(2) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 156.dp),
            verticalArrangement = Arrangement.spacedBy(29.dp),
        ) {
            homeSectionItem("original/hero", seen) {
                OriginalHomeHero(
                    items = heroItems,
                    active = ambientActive,
                    scrollProgress = { collapsedScroll },
                )
            }
            if (recent.orEmpty().isNotEmpty()) {
                homeSectionItem("original/recentTitle", seen) {
                    HomeSectionTitle("Recently played", onClick = { navController.navigate("section_detail/recently_played") })
                }
                homeSectionItem("original/recent", seen) {
                    OriginalHomeCardRow("recent", recent.orEmpty(), seen, { it.id }) { item ->
                        HomeCoverCard(
                            title = item.title,
                            subtitle = when (item) {
                                is Song -> item.artists.joinToString(", ") { it.name }
                                is Album -> item.artists.joinToString(", ") { it.name }
                                is Playlist -> item.songCount.toString() + " songs"
                                is Artist -> null
                            },
                            thumbnailUrl = (item as? Playlist)?.thumbnails?.firstOrNull() ?: item.thumbnailUrl,
                            circular = item is Artist,
                            isActive = item.id == latestSongId,
                            isPlaying = playing,
                            sharedElementKey = when (item) {
                                is Album -> "album-${item.id}"
                                is Playlist -> "playlist-${item.id}"
                                else -> null
                            },
                            modifier = Modifier.combinedClickable(
                                interactionSource = null,
                                indication = HomeCardHighlight,
                                onClick = { openLocal(item) },
                                onLongClick = { localMenu(item) },
                            ),
                        )
                    }
                }
            }
            if (songs.isNotEmpty()) {
                homeSectionItem("original/mostPlayedTitle", seen) {
                    HomeSectionTitle("Most played", onClick = { navController.navigate("section_detail/most_played") })
                }
                homeSectionItem("original/mostPlayed", seen) {
                    OriginalHomeTrackGrid(songs.take(18)) { index, song ->
                        HomeTrackRow(
                            thumbnailUrl = song.thumbnailUrl,
                            songId = song.id,
                            title = song.title,
                            subtitle = song.artists.joinToString(", ") { it.name },
                            isActive = song.id == latestSongId,
                            isPlaying = playing,
                            rank = index + 1,
                            onClick = { play(song, songs) },
                            onLongClick = { menu(song) },
                            onMoreClick = { menu(song) },
                        )
                    }
                }
            }
            if (speedDial.isNotEmpty()) {
                homeSectionItem("original/dialTitle", seen) {
                    HomeSectionTitle("Speed dial", onClick = { navController.navigate("section_detail/speed_dial") })
                }
                homeSectionItem("original/dial", seen) {
                    OriginalHomeCardRow("speed_dial", speedDial.distinctBy { it.id }, seen, { it.id }) { song ->
                        HomeCoverCard(
                            title = song.title,
                            subtitle = song.artists.joinToString(", ") { it.name },
                            thumbnailUrl = song.thumbnailUrl,
                            isActive = song.id == latestSongId,
                            isPlaying = playing,
                            modifier = Modifier.combinedClickable(
                                interactionSource = null,
                                indication = HomeCardHighlight,
                                onClick = { play(song, speedDial) },
                                onLongClick = { menu(song) },
                            ),
                        )
                    }
                }
            }
            if (recentSongs.isNotEmpty()) {
                homeSectionItem("original/historyTitle", seen) {
                    HomeSectionTitle("History", onClick = { navController.navigate("history") })
                }
                homeSectionItem("original/history", seen) {
                    OriginalHomeCardRow("history", recentSongs, seen, { it.id }) { song ->
                        HomeCoverCard(
                            title = song.title,
                            subtitle = song.artists.joinToString(", ") { it.name },
                            thumbnailUrl = song.thumbnail,
                            isActive = song.id == latestSongId,
                            isPlaying = playing,
                            modifier = Modifier.combinedClickable(
                                interactionSource = null,
                                indication = HomeCardHighlight,
                                onClick = { playRemote(song) },
                                onLongClick = { remoteMenu(song) },
                            ),
                        )
                    }
                }
            }
            if (accountPlaylists.orEmpty().isNotEmpty()) {
                homeSectionItem("original/playlistsTitle", seen) {
                    HomeSectionTitle("Your playlists", onClick = onOpenLibraryTab)
                }
                homeSectionItem("original/playlists", seen) {
                    OriginalHomeCardRow("playlists", accountPlaylists.orEmpty(), seen, { it.id }) { item ->
                        HomeCoverCard(
                            title = item.title,
                            subtitle = item.author?.name,
                            thumbnailUrl = item.thumbnail,
                            isPlaying = playing,
                            modifier = Modifier.combinedClickable(
                                interactionSource = null,
                                indication = HomeCardHighlight,
                                onClick = { openRemote(item) },
                                onLongClick = { remoteMenu(item) },
                            ),
                            sharedElementKey = "playlist-${item.id}",
                        )
                    }
                }
            }
            if (!forgotten.isNullOrEmpty()) {
                homeSectionItem("original/forgottenTitle", seen) {
                    HomeSectionTitle("Don't forget these songs", onClick = { navController.navigate("section_detail/forgotten_favorites") })
                }
                homeSectionItem("original/forgotten", seen) {
                    OriginalHomeTrackGrid(forgotten.orEmpty().take(18)) { _, song ->
                        HomeTrackRow(
                            thumbnailUrl = song.thumbnailUrl,
                            songId = song.id,
                            title = song.title,
                            subtitle = song.artists.joinToString(", ") { it.name },
                            isActive = song.id == latestSongId,
                            isPlaying = playing,
                            onClick = { play(song, forgotten.orEmpty()) },
                            onLongClick = { menu(song) },
                            onMoreClick = { menu(song) },
                        )
                    }
                }
            }
            homePage?.sections.orEmpty().forEachIndexed { index, section ->
                homeSectionItem("original/sectionTitle/$index", seen) {
                    HomeSectionTitle(
                        title = section.title,
                        label = section.label,
                        onClick = section.endpoint?.browseId?.let { id ->
                            { navController.navigate(if (id == "FEmusic_moods_and_genres") Screens.MoodAndGenres.route else "browse/$id") }
                        },
                    )
                }
                homeSectionItem("original/section/$index", seen) {
                    OriginalHomeCardRow("homeSection/$index", section.items.distinctBy { it.id }, seen, { it.id }) { item ->
                        HomeCoverCard(
                            title = item.title,
                            subtitle = when (item) {
                                is SongItem -> item.artists.joinToString(", ") { it.name }
                                is AlbumItem -> item.artists.orEmpty().joinToString(", ") { it.name }
                                is ArtistItem -> null
                                is PlaylistItem -> item.author?.name,
                            },
                            thumbnailUrl = item.thumbnail,
                            circular = item is ArtistItem,
                            isPlaying = playing,
                            sharedElementKey = when (item) {
                                is AlbumItem -> "album-${item.id}"
                                is PlaylistItem -> "playlist-${item.id}"
                                else -> null
                            },
                            modifier = Modifier.combinedClickable(
                                interactionSource = null,
                                indication = HomeCardHighlight,
                                onClick = { openRemote(item) },
                                onLongClick = { remoteMenu(item) },
                            ),
                        )
                    }
                }
            }
            if (loading) {
                item("original/bones") {
                    HomeSectionBones()
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(
                    top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 14.dp,
                    start = HomeContentInset,
                    end = 30.dp,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(24.dp))
            Box(
                modifier = Modifier.size(36.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = { navController.navigate("account") },
                        onLongClick = onOpenSearchTab,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!accountImage.isNullOrBlank()) {
                    AsyncImage(
                        model = accountImage,
                        contentDescription = "Account",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = "Account",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(23.dp),
                    )
                }
            }
        }
    }
}

private fun LazyListScope.homeSectionItem(
    key: String,
    seen: com.arturo254.opentune.ui.visual.component.RevealSeenState,
    content: @Composable () -> Unit,
) {
    item(key = key) {
        Box(modifier = Modifier.revealOnAppear(key, seen, cascade = true)) {
            content()
        }
    }
}

@Composable
private fun <T> OriginalHomeCardRow(
    key: String,
    elements: List<T>,
    seen: com.arturo254.opentune.ui.visual.component.RevealSeenState,
    itemKey: (T) -> Any,
    content: @Composable (T) -> Unit,
) {
    LazyRow(
        state = rememberLazyListState(),
        contentPadding = PaddingValues(horizontal = HomeContentInset),
        horizontalArrangement = Arrangement.spacedBy(HomeCardSpacing),
    ) {
        itemsIndexed(elements, key = { _, element -> itemKey(element) }) { index, element ->
            Box(
                modifier = Modifier.revealOnAppear(
                    key = "$key/${itemKey(element)}",
                    seenState = seen,
                    delayMillis = min(index, 4) * 60,
                ),
            ) {
                content(element)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> OriginalHomeTrackGrid(
    elements: List<T>,
    content: @Composable (Int, T) -> Unit,
) {
    val state = rememberLazyGridState()
    val rows = min(3, elements.size)
    if (rows == 0) return
    val snap = remember(state) {
        SnapLayoutInfoProvider(lazyGridState = state, positionInLayout = { _, _ -> 0f })
    }
    LazyHorizontalGrid(
        state = state,
        rows = GridCells.Fixed(rows),
        flingBehavior = rememberSnapFlingBehavior(snap),
        contentPadding = PaddingValues(horizontal = HomeContentInset),
        horizontalArrangement = Arrangement.spacedBy(39.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().height((54.dp + 8.dp) * rows),
    ) {
        gridItemsIndexed(elements, key = { _, element -> element.hashCode() }) { index, element ->
            Box(Modifier.width(295.dp)) { content(index, element) }
        }
    }
}
