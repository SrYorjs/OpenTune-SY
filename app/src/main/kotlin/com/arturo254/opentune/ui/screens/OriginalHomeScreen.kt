package com.arturo254.opentune.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.grid.LazyGridPrefetchStrategy
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.arturo254.opentune.ui.visual.component.HomeCompactTopBones
import com.arturo254.opentune.ui.visual.component.HomeHopInCard
import com.arturo254.opentune.ui.visual.component.HomeSectionBones
import com.arturo254.opentune.ui.visual.component.HomeSectionTitle
import com.arturo254.opentune.ui.visual.component.HomeTopGlow
import com.arturo254.opentune.ui.visual.component.HomeTrackRow
import com.arturo254.opentune.ui.visual.component.homeRowHeight
import com.arturo254.opentune.ui.visual.component.HomeVideoCard
import com.arturo254.opentune.ui.visual.component.OriginalHomeHero
import com.arturo254.opentune.ui.visual.component.OriginalHomeHeroItem
import com.arturo254.opentune.ui.visual.component.OriginalCommunityPlaylistCard
import com.arturo254.opentune.ui.visual.component.OriginalAllTimeHitsCard
import com.arturo254.opentune.ui.visual.component.rememberRevealSeenState
import com.arturo254.opentune.ui.visual.component.revealOnAppear
import com.arturo254.opentune.ui.visual.utils.SnapLayoutInfoProvider
import com.arturo254.opentune.viewmodels.HomeViewModel
import com.arturo254.opentune.ui.visual.HomeStyle
import com.arturo254.opentune.ui.visual.HomeStyleKey
import com.arturo254.opentune.utils.rememberPreference
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
    val recent by viewModel.recentlyPlayedForHome.collectAsStateWithLifecycle()
    val keepListening by viewModel.keepListening.collectAsStateWithLifecycle()
    val localPlaylists by viewModel.localPlaylistsForHome.collectAsStateWithLifecycle()
    val recentActivity by viewModel.recentActivity.collectAsStateWithLifecycle()
    val speedDial by viewModel.speedDialSongs.collectAsStateWithLifecycle()
    val forgotten by viewModel.forgottenFavorites.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayedSongsForHome.collectAsStateWithLifecycle()
    val history by viewModel.recentHistorySongs.collectAsStateWithLifecycle()
    val accountPlaylists by viewModel.accountPlaylists.collectAsStateWithLifecycle()
    val homePage by viewModel.homePage.collectAsStateWithLifecycle()
    val trends by viewModel.globalCatalogTrending.collectAsStateWithLifecycle()
    val loading by viewModel.isLoading.collectAsStateWithLifecycle()
    val accountImage by viewModel.accountImageUrl.collectAsStateWithLifecycle()
    val (homeStyleValue) = rememberPreference(HomeStyleKey, HomeStyle.MOCHYY.name)
    val homeStyle = remember(homeStyleValue) {
        runCatching { HomeStyle.valueOf(homeStyleValue) }.getOrDefault(HomeStyle.MOCHYY)
    }
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
    val heroItems = remember(songs, remoteItems, recent, keepListening, localPlaylists, accountPlaylists, latestSongId, playing) {
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
                canvasEligible = true,
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
                album = song.album?.name,
                canvasEligible = true,
            )
        }
        val localCollections = buildList<LocalItem> {
            addAll(localPlaylists.orEmpty())
            addAll(keepListening.orEmpty().filterIsInstance<Album>())
        }.distinctBy { it.id }.take(2).map { item ->
                OriginalHomeHeroItem(
                    id = "${item.javaClass.simpleName}_${item.id}",
                    title = item.title,
                    subtitle = when (item) {
                        is Album -> item.artists.joinToString(", ") { it.name }
                        is Playlist -> "${item.songCount} songs"
                        else -> ""
                    },
                    artwork = (item as? Playlist)?.thumbnails?.firstOrNull() ?: item.thumbnailUrl,
                    label = "Continue",
                    isCurrent = false,
                    isPlaying = false,
                    opensPage = true,
                    onOpen = { openLocal(item) },
                )
            }
        val playlists = accountPlaylists.orEmpty().filter { !it.thumbnail.isNullOrBlank() }.take(2).map { item ->
            OriginalHomeHeroItem(
                id = "remote_playlist_${item.id}",
                title = item.title,
                subtitle = item.author?.name.orEmpty(),
                artwork = item.thumbnail,
                label = "Your playlists",
                isCurrent = false,
                isPlaying = false,
                opensPage = true,
                onOpen = { openRemote(item) },
            )
        }
        (own + remote + localCollections + playlists).distinctBy { it.id }.take(7)
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
            when (homeStyle) {
                HomeStyle.MOCHYY -> {
                    homeSectionItem("original/hero", seen) {
                        OriginalHomeHero(
                            items = heroItems,
                            active = ambientActive,
                            scrollProgress = { collapsedScroll },
                            isLoading = loading,
                            gachaPool = songs.map { it.toMediaMetadata() },
                            currentSongId = latestSongId,
                            isPlaying = playing,
                            onGachaWinner = { winner ->
                                val index = songs.indexOfFirst { it.id == winner.id }
                                if (index >= 0 && player != null) {
                                    player.playQueue(
                                        ListQueue(
                                            title = "Home",
                                            items = songs.map { it.toMediaItem() },
                                            startIndex = index,
                                        )
                                    )
                                }
                            },
                        )
                    }
                }
                HomeStyle.LISTEN_NOW -> {
                    homeSectionItem("original/listenNow", seen) {
                        Box {
                            HomeTopGlow(
                                coverUrls = songs.mapNotNull { it.thumbnailUrl }.take(2),
                                depth = 185.dp,
                                visibility = { 1f - collapsedScroll },
                                modifier = Modifier.matchParentSize(),
                            )
                            Column {
                                Spacer(Modifier.height(WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 85.dp))
                                HomeSectionTitle("Start listening")
                                if (songs.isEmpty() && loading) {
                                    HomeCompactTopBones(isHopIn = false)
                                } else if (songs.isNotEmpty()) {
                                    OriginalHomeTrackGrid(songs.take(15)) { _, song ->
                                        HomeTrackRow(
                                            thumbnailUrl = song.thumbnailUrl,
                                            title = song.title,
                                            subtitle = song.artists.joinToString(", ") { it.name },
                                            isActive = song.id == latestSongId,
                                            isPlaying = playing,
                                            songId = song.id,
                                            onClick = { play(song, songs) },
                                            onLongClick = { menu(song) },
                                            onMoreClick = { menu(song) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                HomeStyle.HOP_IN -> {
                    homeSectionItem("original/hopIn", seen) {
                        val columns = if (LocalConfiguration.current.screenWidthDp >= 600) 3 else 2
                        val gridItems = buildList<LocalItem> {
                            addAll(recent.orEmpty())
                            addAll(keepListening.orEmpty())
                            addAll(localPlaylists.orEmpty())
                            addAll(songs)
                        }.distinctBy { it.id }.take(columns * 2)
                        Box {
                            HomeTopGlow(
                                coverUrls = gridItems.mapNotNull { it.thumbnailUrl }.take(2),
                                depth = 185.dp,
                                visibility = { 1f - collapsedScroll },
                                modifier = Modifier.matchParentSize(),
                            )
                            Column {
                                Spacer(Modifier.height(WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 85.dp))
                                HomeSectionTitle("Hop in")
                                if (gridItems.isEmpty() && loading) {
                                    HomeCompactTopBones(isHopIn = true)
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = HomeContentInset),
                                    ) {
                                        gridItems.chunked(columns).forEach { rowItems ->
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                rowItems.forEach { item ->
                                                    HomeHopInCard(
                                                        title = item.title,
                                                        subtitle = when (item) {
                                                            is Song -> item.artists.joinToString(", ") { it.name }
                                                            is Album -> item.artists.joinToString(", ") { it.name }
                                                            is Artist -> null
                                                            is Playlist -> item.songCount.toString() + " songs"
                                                        },
                                                        thumbnailUrl = (item as? Playlist)?.thumbnails?.firstOrNull() ?: item.thumbnailUrl,
                                                        isActive = item.id == latestSongId,
                                                        isPlaying = { playing },
                                                        onClick = { openLocal(item) },
                                                        onLongClick = { localMenu(item) },
                                                        sharedElementKey = when (item) {
                                                            is Album -> "album-${item.id}"
                                                            is Playlist -> "playlist-${item.id}"
                                                            else -> null
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                    )
                                                }
                                                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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
            if (!mostPlayed.isNullOrEmpty()) {
                homeSectionItem("original/mostPlayedTitle", seen) {
                    HomeSectionTitle("Most played", onClick = { navController.navigate("section_detail/most_played") })
                }
                homeSectionItem("original/mostPlayed", seen) {
                    OriginalHomeTrackGrid(mostPlayed.orEmpty().take(18)) { index, song ->
                        HomeTrackRow(
                            thumbnailUrl = song.thumbnailUrl,
                            songId = song.id,
                            title = song.title,
                            subtitle = song.artists.joinToString(", ") { it.name },
                            isActive = song.id == latestSongId,
                            isPlaying = playing,
                            rank = index + 1,
                            onClick = { play(song, mostPlayed.orEmpty()) },
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
            if (recentSongs.isNotEmpty() || !history.isNullOrEmpty()) {
                homeSectionItem("original/historyTitle", seen) {
                    HomeSectionTitle("History", onClick = { navController.navigate("history") })
                }
                homeSectionItem("original/history", seen) {
                    if (!history.isNullOrEmpty()) {
                        OriginalHomeTrackGrid(history.orEmpty().take(18)) { _, song ->
                            HomeTrackRow(
                                thumbnailUrl = song.thumbnailUrl,
                                title = song.title,
                                subtitle = song.artists.joinToString(", ") { it.name },
                                songId = song.id,
                                isActive = song.id == latestSongId,
                                isPlaying = playing,
                                onClick = { play(song, history.orEmpty()) },
                                onLongClick = { menu(song) },
                                onMoreClick = { menu(song) },
                            )
                        }
                    } else {
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
            }
            if (accountPlaylists.orEmpty().isNotEmpty() || localPlaylists.orEmpty().isNotEmpty()) {
                homeSectionItem("original/playlistsTitle", seen) {
                    HomeSectionTitle("Your playlists", onClick = onOpenLibraryTab)
                }
                homeSectionItem("original/playlists", seen) {
                    val entries = buildList<Any> {
                        addAll(localPlaylists.orEmpty())
                        addAll(accountPlaylists.orEmpty())
                    }
                    OriginalHomeCardRow("playlists", entries, seen, { item ->
                        when (item) {
                            is Playlist -> "local_${item.id}"
                            is PlaylistItem -> "remote_${item.id}"
                            else -> item.hashCode().toString()
                        }
                    }) { item ->
                        when (item) {
                            is Playlist -> {
                                if (item.thumbnails.distinct().size >= 3) {
                                    OriginalCommunityPlaylistCard(
                                        id = item.id,
                                        title = item.title,
                                        subtitle = "${item.songCount} songs",
                                        covers = item.thumbnails,
                                        onClick = { openLocal(item) },
                                    )
                                } else {
                                    HomeCoverCard(
                                        title = item.title,
                                        subtitle = "${item.songCount} songs",
                                        thumbnailUrl = item.thumbnails.firstOrNull() ?: item.thumbnailUrl,
                                        isPlaying = playing,
                                        modifier = Modifier.combinedClickable(
                                            interactionSource = null,
                                            indication = HomeCardHighlight,
                                            onClick = { openLocal(item) },
                                            onLongClick = { localMenu(item) },
                                        ),
                                        sharedElementKey = "playlist-${item.id}",
                                    )
                                }
                            }
                            is PlaylistItem -> HomeCoverCard(
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
            if (trends.isNotEmpty()) {
                homeSectionItem("original/allTimeHitsTitle", seen) {
                    HomeSectionTitle("All-time hits")
                }
                homeSectionItem("original/allTimeHits", seen) {
                    OriginalHomeCardRow("all_time_hits", trends.distinctBy { it.sourceId }.take(18), seen, { it.sourceId }) { track ->
                        OriginalAllTimeHitsCard(
                            title = track.artists.joinToString(", ").ifBlank { track.title },
                            artwork = track.artworkUrl,
                            onClick = {
                                if (player != null) scope.launch {
                                    viewModel.playSourceVariant(player, track, "Home")
                                }
                            },
                            modifier = Modifier.width(246.dp),
                        )
                    }
                }
            }
            val artists = remoteItems.filterIsInstance<ArtistItem>()
            if (artists.isNotEmpty()) {
                homeSectionItem("original/artistListTitle", seen) {
                    HomeSectionTitle("Artists")
                }
                homeSectionItem("original/artistList", seen) {
                    OriginalHomeCardRow("artist_list", artists, seen, { it.id }) { artist ->
                        HomeCoverCard(
                            title = artist.title,
                            subtitle = null,
                            thumbnailUrl = artist.thumbnail,
                            circular = true,
                            isPlaying = playing,
                            modifier = Modifier.combinedClickable(
                                interactionSource = null,
                                indication = HomeCardHighlight,
                                onClick = { openRemote(artist) },
                                onLongClick = { remoteMenu(artist) },
                            ),
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
                    val videoRow = section.title.contains("video", ignoreCase = true) ||
                        section.title.contains("concert", ignoreCase = true) ||
                        section.title.contains("live show", ignoreCase = true)
                    if (videoRow && section.items.any { it is SongItem }) {
                        OriginalHomeCardRow(
                            "homeSectionVideo/$index",
                            section.items.filterIsInstance<SongItem>().distinctBy { it.id },
                            seen,
                            { it.id },
                        ) { song ->
                            HomeVideoCard(
                                title = song.title,
                                subtitle = song.artists.joinToString(", ") { it.name },
                                videoId = song.id,
                                fallbackThumbnailUrl = song.thumbnail,
                                width = 314.dp,
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
                    } else {
                    OriginalHomeCardRow("homeSection/$index", section.items.distinctBy { it.id }, seen, { it.id }) { item ->
                        HomeCoverCard(
                            title = item.title,
                            subtitle = when (item) {
                                is SongItem -> item.artists.joinToString(", ") { it.name }
                                is AlbumItem -> item.artists.orEmpty().joinToString(", ") { it.name }
                                is ArtistItem -> null
                                is PlaylistItem -> item.author?.name
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> OriginalHomeCardRow(
    key: String,
    elements: List<T>,
    seen: com.arturo254.opentune.ui.visual.component.RevealSeenState,
    itemKey: (T) -> Any,
    content: @Composable (T) -> Unit,
) {
    LazyRow(
        state = rememberLazyListState(
            prefetchStrategy = remember { LazyListPrefetchStrategy(nestedPrefetchItemCount = 4) },
        ),
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
    val state = rememberLazyGridState(
        prefetchStrategy = remember { LazyGridPrefetchStrategy(nestedPrefetchItemCount = 6) },
    )
    val rows = min(3, elements.size)
    if (rows == 0) return
    val snap = remember(state) {
        SnapLayoutInfoProvider(lazyGridState = state, positionInLayout = { _, _ -> 0f })
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columnWidth = if (maxWidth * 0.475f >= 320.dp) maxWidth * 0.475f else maxWidth * 0.76f
        val rowSpacing = 6.dp
        LazyHorizontalGrid(
            state = state,
            rows = GridCells.Fixed(rows),
            flingBehavior = rememberSnapFlingBehavior(snap),
            contentPadding = PaddingValues(horizontal = HomeContentInset),
            horizontalArrangement = Arrangement.spacedBy(39.dp),
            verticalArrangement = Arrangement.spacedBy(rowSpacing),
            modifier = Modifier.fillMaxWidth().height(homeRowHeight() * rows + rowSpacing * (rows - 1)),
        ) {
            gridItemsIndexed(elements, key = { _, element -> element.hashCode() }) { index, element ->
                Box(Modifier.width(columnWidth)) { content(index, element) }
            }
        }
    }
}
