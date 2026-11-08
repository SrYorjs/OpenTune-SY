package com.arturo254.opentune.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.ShowHomeCategoryChipsKey
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.innertube.models.*
import com.arturo254.opentune.innertube.pages.HomePage
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.mochy.component.HomeTopGlow
import com.arturo254.opentune.ui.mochy.component.SmoothCornerShape
import com.arturo254.opentune.ui.mochy.component.press3D
import com.arturo254.opentune.ui.mochy.component.revealOnAppear
import com.arturo254.opentune.ui.mochy.component.rememberRevealSeenState
import com.arturo254.opentune.ui.mochy.home.HomeTrackRow
import com.arturo254.opentune.ui.mochy.home.HomeCoverCard
import com.arturo254.opentune.ui.mochy.home.HomeSectionTitle
import com.arturo254.opentune.ui.menu.*
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.viewmodels.HomeViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** Music-first home. Presentation is independent from playback and catalog state. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenSearchTab: () -> Unit = { navController.navigate(Screens.Search.route) },
    onOpenLibraryTab: () -> Unit = { navController.navigate(Screens.Library.route) },
    ambientActive: Boolean = true,
) {
    val queueTitle = stringResource(R.string.home)
    val player = LocalPlayerConnection.current
    val context = LocalContext.current
    val menus = LocalMenuState.current
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val mochyReveal = rememberRevealSeenState()
    val refresh = rememberPullToRefreshState()
    val picks by viewModel.quickPicks.collectAsStateWithLifecycle()
    val recentItems by viewModel.keepListening.collectAsStateWithLifecycle()
    val dial by viewModel.speedDialSongs.collectAsStateWithLifecycle()
    val forgotten by viewModel.forgottenFavorites.collectAsStateWithLifecycle()
    val catalog by viewModel.homePage.collectAsStateWithLifecycle()
    val chip by viewModel.selectedChip.collectAsStateWithLifecycle()
    val account by viewModel.accountImageUrl.collectAsStateWithLifecycle()
    val metadataMap by viewModel.allItemsMetadata.collectAsStateWithLifecycle()
    val trends by viewModel.globalCatalogTrending.collectAsStateWithLifecycle()
    val loading by viewModel.isLoading.collectAsStateWithLifecycle()
    val refreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val current = player?.mediaMetadata?.collectAsStateWithLifecycle()?.value
    val playing = player?.isPlaying?.collectAsStateWithLifecycle()?.value == true
    val (showChips) = rememberPreference(ShowHomeCategoryChipsKey, true)
    var shuffling by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf<String?>(null) }
    val recent =
        remember(recentItems) {
            recentItems.orEmpty().filterIsInstance<Song>().distinctBy { it.id }
        }
    val selection =
        remember(picks, recent, dial) { (picks.orEmpty() + recent + dial).distinctBy { it.id } }
    val feature = selection.firstOrNull()
    val backStack by navController.currentBackStackEntryAsState()
    val top = backStack?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(top?.value, ambientActive) {
        if (ambientActive && top?.value == true) {
            list.animateScrollToItem(0)
            backStack?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    LaunchedEffect(showChips, chip) { if (!showChips && chip != null) viewModel.toggleChip(chip) }
    BackHandler(ambientActive && chip != null) { viewModel.toggleChip(chip) }
    LaunchedEffect(catalog?.continuation, ambientActive) {
        val next = catalog?.continuation ?: return@LaunchedEffect
        if (!ambientActive) return@LaunchedEffect
        snapshotFlow {
                val info = list.layoutInfo
                info.totalItemsCount > 0 &&
                    (info.visibleItemsInfo.lastOrNull()?.index ?: -1) >= info.totalItemsCount - 4
            }
            .distinctUntilChanged()
            .filter { it }
            .collect { viewModel.loadMoreYouTubeItems(next) }
    }
    val play: (Song, List<Song>) -> Unit = { song, queue ->
        if (current?.id == song.id) player?.player?.togglePlayPause()
        else
            player?.playQueue(
                ListQueue(
                    title = queueTitle,
                    items = queue.map { it.toMediaItem() },
                    startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
                )
            )
    }
    val menu: (Song) -> Unit = { song ->
        menus.show {
            SongMenu(
                originalSong = song,
                navController = navController,
                metadata = metadataMap[song.id],
                onDismiss = menus::dismiss,
            )
        }
    }
    val shuffle: () -> Unit = {
        if (!shuffling && player != null) {
            shuffling = true
            viewModel.playGalaxShuffle(player) { started ->
                shuffling = false
                if (!started)
                    Toast.makeText(context, R.string.galax_shuffle_unavailable, Toast.LENGTH_SHORT)
                        .show()
            }
        }
    }
    val featuredRemote =
        catalog
            ?.sections
            .orEmpty()
            .asSequence()
            .flatMap { it.items.asSequence() }
            .filterIsInstance<SongItem>()
            .firstOrNull()
    val featuredSource = trends.firstOrNull()
    val resolve: (SourceVariant) -> Unit = { track ->
        if (player != null && resolving == null)
            scope.launch {
                resolving = track.sourceId
                try {
                    if (!viewModel.playSourceVariant(player, track, queueTitle))
                        Toast.makeText(
                                context,
                                R.string.catalog_source_unplayable,
                                Toast.LENGTH_SHORT,
                            )
                            .show()
                } finally {
                    resolving = null
                }
            }
    }
    val playFeature: () -> Unit = {
        when {
            feature != null -> play(feature, selection)
            featuredRemote != null -> {
                if (current?.id == featuredRemote.id) player?.player?.togglePlayPause()
                else
                    player?.playQueue(
                        YouTubeQueue(
                            featuredRemote.endpoint ?: WatchEndpoint(videoId = featuredRemote.id),
                            featuredRemote.toMediaMetadata(),
                        )
                    )
            }
            featuredSource != null -> resolve(featuredSource)
            else -> onOpenSearchTab()
        }
    }
    PulseScreenFrame(active = ambientActive) { layout, safePadding ->
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val featuredArtworks = remember(feature?.thumbnailUrl, featuredRemote?.thumbnail, featuredSource?.artworkUrl, recent) {
                listOfNotNull(
                    feature?.thumbnailUrl,
                    featuredRemote?.thumbnail,
                    recent.firstOrNull()?.thumbnailUrl,
                    featuredSource?.artworkUrl,
                ).distinct().take(2)
            }
            HomeTopGlow(
                coverUrls = featuredArtworks,
                depth = 340.dp,
                visibility = {
                    if (list.firstVisibleItemIndex != 0) 0f
                    else (1f - list.firstVisibleItemScrollOffset / 450f).coerceIn(0f, 1f)
                },
                modifier = Modifier.align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(340.dp),
            )
            LazyColumn(
            state = list,
            modifier =
                Modifier.fillMaxSize()
                    .pullToRefresh(
                        isRefreshing = refreshing,
                        state = refresh,
                        onRefresh = viewModel::refresh,
                    ),
            contentPadding = safePadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("pulse/header") {
                Box(Modifier.revealOnAppear("mochy-home-header", mochyReveal, enabled = ambientActive)) {
                    PulseHeader(account, onNavigate = { navController.navigate(it) })
                }
            }
            item("pulse/search") { PulseSearch(onOpenSearchTab) }
            item("pulse/feature") {
                Box(Modifier.revealOnAppear("mochy-home-feature", mochyReveal, enabled = ambientActive)) {
                PulseFeature(
                    title = feature?.title ?: featuredRemote?.title ?: featuredSource?.title,
                    subtitle =
                        feature?.artists?.joinToString { it.name }
                            ?: featuredRemote?.artists?.joinToString { it.name }
                            ?: featuredSource?.artists?.joinToString(),
                    artwork =
                        feature?.thumbnailUrl
                            ?: featuredRemote?.thumbnail
                            ?: featuredSource?.artworkUrl,
                    playing =
                        playing &&
                            (current?.id == feature?.id || current?.id == featuredRemote?.id),
                    onPlay = playFeature,
                    compactHeight = layout.compactHeight,
                    onMenu =
                        when {
                            feature != null -> ({ menu(feature) })
                            featuredRemote != null -> ({
                                    menus.show {
                                        YouTubeSongMenu(
                                            song = featuredRemote,
                                            navController = navController,
                                            onDismiss = menus::dismiss,
                                        )
                                    }
                                })
                            else -> null
                        },
                )
                }
            }
            item("pulse/shortcuts") {
                PulseShortcuts(
                    shuffling,
                    player != null,
                    shuffle,
                    onOpenLibraryTab,
                    onRecognize = {
                        navController.navigate(
                            com.arturo254.opentune.ui.screens.musicrecognition.MusicRecognitionRoute
                        )
                    },
                )
            }
            if (showChips && catalog?.chips?.isNotEmpty() == true)
                item("pulse/filters") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(
                            catalog?.chips.orEmpty(),
                            key = { index, _ -> "mood/$index" },
                        ) { _, item ->
                            FilterChip(
                                selected = item == chip,
                                onClick = { viewModel.toggleChip(item) },
                                shape = RoundedCornerShape(50),
                                label = { Text(item.title) },
                            )
                        }
                    }
                }
            if (recent.isNotEmpty()) {
                item("pulse/recent-heading") { HomeSectionTitle(stringResource(R.string.pulse_recent)) }
                item("pulse/recent") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(recent.take(10), key = { it.id }) { song ->
                            PulseTrackRow(
                                song,
                                current?.id == song.id,
                                playing,
                                { play(song, recent) },
                                { menu(song) },
                                Modifier.width((layout.coverWidth * 1.9f).coerceIn(240.dp, 330.dp)),
                            )
                        }
                    }
                }
            }
            if (trends.isNotEmpty() && chip == null) {
                item("pulse/trends-heading") {
                    HomeSectionTitle(
                        title = stringResource(R.string.pulse_trending),
                        label = stringResource(R.string.pulse_trending_detail),
                    )
                }
                item("pulse/trends") {
                    PulseShelfLayout { coverWidth ->
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(
                                trends.distinctBy { it.provider.value + it.sourceId }.take(16),
                                key = { it.provider.value + ":" + it.sourceId },
                            ) { track ->
                                PulseCover(
                                    coverWidth,
                                    track.title,
                                    track.artists.joinToString(),
                                    track.artworkUrl,
                                    busy = resolving == track.sourceId,
                                    onClick = { resolve(track) },
                                )
                            }
                        }
                    }
                }
            }
            if (selection.isNotEmpty()) {
                item("pulse/picks-heading") { HomeSectionTitle(stringResource(R.string.pulse_picks)) }
                itemsIndexed(
                    selection.take(6).chunked(layout.columns),
                    key = { index, _ -> "pulse/picks/$index" },
                ) { _, row ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { song ->
                            PulseTrackRow(
                                song,
                                current?.id == song.id,
                                playing,
                                { play(song, selection) },
                                { menu(song) },
                                Modifier.weight(1f),
                            )
                        }
                        repeat(layout.columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            if (dial.isNotEmpty()) {
                item("pulse/dial-heading") { HomeSectionTitle(stringResource(R.string.speed_dial)) }
                item("pulse/dial") {
                    PulseShelf(dial.distinctBy { it.id }.take(16), current?.id, playing, play, menu)
                }
            }
            if (!forgotten.isNullOrEmpty()) {
                item("pulse/favorites-heading") {
                    HomeSectionTitle(stringResource(R.string.forgotten_favorites))
                }
                item("pulse/favorites") {
                    PulseShelf(
                        forgotten.orEmpty().distinctBy { it.id }.take(16),
                        current?.id,
                        playing,
                        play,
                        menu,
                    )
                }
            }
            catalog?.sections.orEmpty().forEachIndexed { index, section ->
                item(galaxHomeSectionRowKey(index, true)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { HomeSectionTitle(title = section.title, label = section.label) }
                        if (section.endpoint?.browseId != null)
                            TextButton(
                                onClick = {
                                    section.endpoint?.browseId?.let { id ->
                                        navController.navigate(
                                            if (id == "FEmusic_moods_and_genres")
                                                Screens.MoodAndGenres.route
                                            else "browse/$id"
                                        )
                                    }
                                },
                                modifier = Modifier.padding(end = 12.dp),
                            ) {
                                Text(stringResource(R.string.pulse_explore))
                            }
                    }
                }
                item(galaxHomeSectionRowKey(index, false)) { PulseCatalog(section, navController) }
            }
            if (loading)
                item("pulse/loading") {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                }
            if (
                !loading &&
                    selection.isEmpty() &&
                    catalog?.sections.isNullOrEmpty() &&
                    trends.isEmpty()
            ) {
                item("pulse/empty") {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            stringResource(R.string.pulse_empty),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.pulse_empty_detail),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = viewModel::refresh) {
                            Text(stringResource(R.string.pulse_refresh))
                        }
                    }
                }
            }
        }
        Indicator(
            isRefreshing = refreshing,
            state = refresh,
            modifier =
                Modifier.fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .padding(top = safePadding.calculateTopPadding()),
        )
        }
    }
}

@Composable
private fun PulseTrackRow(
    song: Song,
    active: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeTrackRow(
        thumbnailUrl = song.thumbnailUrl,
        title = song.title,
        subtitle = song.artists.joinToString { it.name },
        isActive = active,
        isPlaying = playing,
        onClick = onClick,
        onLongClick = onMenu,
        onMoreClick = onMenu,
        modifier = modifier,
        songId = song.id,
    )
}

@Composable
private fun PulseShelf(
    songs: List<Song>,
    current: String?,
    playing: Boolean,
    play: (Song, List<Song>) -> Unit,
    menu: (Song) -> Unit,
) {
    PulseShelfLayout { coverWidth ->
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(songs, key = { it.id }) { song ->
                PulseCover(
                    coverWidth,
                    song.title,
                    song.artists.joinToString { it.name },
                    song.thumbnailUrl,
                    false,
                    onClick = { play(song, songs) },
                    onMenu = { menu(song) },
                    selected = current == song.id && playing,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PulseCover(
    coverWidth: androidx.compose.ui.unit.Dp,
    title: String,
    subtitle: String,
    artwork: String?,
    busy: Boolean,
    onClick: () -> Unit,
    onMenu: (() -> Unit)? = null,
    selected: Boolean = false,
    circular: Boolean = false,
) {
    Column(
        modifier = Modifier.width(coverWidth).combinedClickable(
            role = Role.Button,
            onClick = onClick,
            onLongClick = onMenu,
            onLongClickLabel = stringResource(R.string.pulse_options),
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HomeCoverCard(
            title = title,
            subtitle = subtitle,
            thumbnailUrl = artwork,
            circular = circular,
            isActive = selected,
            isPlaying = selected && !busy,
        )
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PulseCatalog(section: HomePage.Section, nav: NavController) {
    val player = LocalPlayerConnection.current
    val menus = LocalMenuState.current
    val scope = rememberCoroutineScope()
    PulseShelfLayout { coverWidth ->
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                section.items.distinctBy { it.javaClass.simpleName + it.id },
                key = { it.javaClass.simpleName + ":" + it.id },
            ) { item ->
                PulseCover(
                    coverWidth,
                    item.title,
                    when (item) {
                        is SongItem -> item.artists.joinToString { it.name }
                        is AlbumItem -> item.artists.orEmpty().joinToString { it.name }
                        is ArtistItem -> "Artista"
                        is PlaylistItem -> item.author?.name.orEmpty()
                    },
                    item.thumbnail,
                    false,
                    circular = item is ArtistItem,
                    onClick = {
                        when (item) {
                            is SongItem -> {
                                if (player?.mediaMetadata?.value?.id == item.id)
                                    player?.player?.togglePlayPause()
                                else
                                    player?.playQueue(
                                        YouTubeQueue(
                                            item.endpoint ?: WatchEndpoint(videoId = item.id),
                                            item.toMediaMetadata(),
                                        )
                                    )
                            }
                            is AlbumItem -> nav.navigate("album/${item.id}")
                            is ArtistItem -> nav.navigate("artist/${item.id}")
                            is PlaylistItem -> nav.navigate("online_playlist/${item.id}")
                        }
                    },
                    onMenu = {
                        menus.show {
                            when (item) {
                                is SongItem ->
                                    YouTubeSongMenu(
                                        song = item,
                                        navController = nav,
                                        onDismiss = menus::dismiss,
                                    )
                                is AlbumItem ->
                                    YouTubeAlbumMenu(
                                        albumItem = item,
                                        navController = nav,
                                        onDismiss = menus::dismiss,
                                    )
                                is ArtistItem ->
                                    YouTubeArtistMenu(artist = item, onDismiss = menus::dismiss)
                                is PlaylistItem ->
                                    YouTubePlaylistMenu(
                                        playlist = item,
                                        coroutineScope = scope,
                                        onDismiss = menus::dismiss,
                                    )
                            }
                        }
                    },
                )
            }
        }
    }
}

internal fun galaxHomeSectionRowKey(index: Int, heading: Boolean): String =
    (if (heading) "home-section-title:" else "home-section-content:") + index
