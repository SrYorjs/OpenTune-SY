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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.ShowHomeCategoryChipsKey
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidEffect
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.innertube.models.*
import com.arturo254.opentune.innertube.pages.HomePage
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.*
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.viewmodels.HomeViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** Orbit: a fluid galactic home backed by the existing music and playback state. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenSearchTab: () -> Unit = { navController.navigate(Screens.Search.route) },
    onOpenLibraryTab: () -> Unit = { navController.navigate(Screens.Library.route) },
    ambientActive: Boolean = true,
) {
    val player = LocalPlayerConnection.current
    val context = LocalContext.current
    val menus = LocalMenuState.current
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
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
                    title = context.getString(R.string.home),
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
    OrbitScreenFrame(active = ambientActive) { layout, safePadding ->
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item("orbit/masthead") {
                OrbitMasthead(
                    account,
                    onNavigate = { navController.navigate(it) },
                    onSearch = onOpenSearchTab,
                )
            }
            item("orbit/opening") {
                OrbitOpening(
                    feature,
                    layout,
                    current?.id == feature?.id && playing,
                    onPlay = {
                        if (feature != null) play(feature, selection) else onOpenSearchTab()
                    },
                    onMenu = { feature?.let(menu) },
                )
            }
            item("orbit/actions") {
                OrbitActions(
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
                item("orbit/moods") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        itemsIndexed(
                            catalog?.chips.orEmpty(),
                            key = { index, _ -> "mood/$index" },
                        ) { _, item ->
                            FilterChip(
                                selected = item == chip,
                                onClick = { viewModel.toggleChip(item) },
                                label = { Text(item.title) },
                            )
                        }
                    }
                }
            if (recent.isNotEmpty()) {
                item("orbit/resume-title") { NovaHeading("Vuelve a tu ritmo", "Tu última escucha") }
                item("orbit/resume") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(recent.take(12), key = { it.id }) { song ->
                            Row(
                                Modifier.width(
                                        (layout.artworkDp.dp + 120.dp).coerceIn(220.dp, 320.dp)
                                    )
                                    .galaxLiquidEffect(shapeOverride = RoundedCornerShape(24.dp))
                            ) {
                                NovaMusicRow(
                                    song.title,
                                    song.artists.joinToString { it.name },
                                    song.thumbnailUrl,
                                    onClick = { play(song, recent) },
                                    active = current?.id == song.id,
                                    playing = playing,
                                    onMenu = { menu(song) },
                                )
                            }
                        }
                    }
                }
            }
            if (selection.isNotEmpty()) {
                item("orbit/selection-title") {
                    NovaHeading("Una buena compañía", "Selección personal")
                }
                itemsIndexed(
                    selection.take(12).chunked(layout.columns),
                    key = { index, _ -> "orbit/selection/$index" },
                ) { _, row ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { song ->
                            Box(Modifier.weight(1f)) {
                                NovaMusicRow(
                                    song.title,
                                    song.artists.joinToString { it.name },
                                    song.thumbnailUrl,
                                    onClick = { play(song, selection) },
                                    active = current?.id == song.id,
                                    playing = playing,
                                    onMenu = { menu(song) },
                                )
                            }
                        }
                        repeat(layout.columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            if (dial.isNotEmpty()) {
                item("orbit/dial-title") {
                    NovaHeading(stringResource(R.string.speed_dial), "Siempre cerca")
                }
                item("orbit/dial") {
                    OrbitShelf(dial.distinctBy { it.id }.take(16), current?.id, playing, play, menu)
                }
            }
            if (!forgotten.isNullOrEmpty()) {
                item("orbit/forgotten-title") {
                    NovaHeading(stringResource(R.string.forgotten_favorites), "Vuelve a descubrir")
                }
                item("orbit/forgotten") {
                    OrbitShelf(
                        forgotten.orEmpty().distinctBy { it.id }.take(16),
                        current?.id,
                        playing,
                        play,
                        menu,
                    )
                }
            }
            if (trends.isNotEmpty() && chip == null) {
                item("orbit/world-title") { NovaHeading("Al otro lado del mundo", "Tendencias") }
                item("orbit/world") {
                    OrbitShelfLayout { coverWidth ->
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(
                                trends.distinctBy { it.provider.value + it.sourceId }.take(16),
                                key = { it.provider.value + ":" + it.sourceId },
                            ) { track ->
                                OrbitCover(
                                    coverWidth,
                                    track.title,
                                    track.artists.joinToString(),
                                    track.artworkUrl,
                                    resolving == track.sourceId,
                                    onClick = {
                                        if (player != null && resolving == null)
                                            scope.launch {
                                                resolving = track.sourceId
                                                try {
                                                    if (
                                                        !viewModel.playSourceVariant(
                                                            player,
                                                            track,
                                                            "Tendencias",
                                                        )
                                                    )
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
                                    },
                                )
                            }
                        }
                    }
                }
            }
            catalog?.sections.orEmpty().forEachIndexed { index, section ->
                item(galaxHomeSectionRowKey(index, true)) {
                    NovaHeading(
                        section.title,
                        section.label,
                        if (section.endpoint?.browseId != null) "Explorar" else null,
                        onAction = {
                            section.endpoint?.browseId?.let { id ->
                                navController.navigate(
                                    if (id == "FEmusic_moods_and_genres")
                                        Screens.MoodAndGenres.route
                                    else "browse/$id"
                                )
                            }
                        },
                    )
                }
                item(galaxHomeSectionRowKey(index, false)) { OrbitCatalog(section, navController) }
            }
            if (loading) item("orbit/loading") { NovaLoading("Preparando tu música…") }
            if (
                !loading &&
                    selection.isEmpty() &&
                    catalog?.sections.isNullOrEmpty() &&
                    trends.isEmpty()
            ) {
                item("orbit/empty") {
                    NovaMessage(
                        "Un universo por escuchar",
                        "Busca tu primera canción o actualiza tus recomendaciones.",
                        "Actualizar",
                        viewModel::refresh,
                    )
                }
            }
            item("orbit/end") { Spacer(Modifier.height(32.dp)) }
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

@Composable
private fun OrbitShelf(
    songs: List<Song>,
    current: String?,
    playing: Boolean,
    play: (Song, List<Song>) -> Unit,
    menu: (Song) -> Unit,
) {
    OrbitShelfLayout { coverWidth ->
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(songs, key = { it.id }) { song ->
                OrbitCover(
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
private fun OrbitCover(
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
        Modifier.width(coverWidth).combinedClickable(onClick = onClick, onLongClick = onMenu),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            NovaArtwork(artwork, Modifier.fillMaxWidth().aspectRatio(1f), circular)
            if (busy)
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center).size(28.dp),
                    strokeWidth = 2.dp,
                )
            if (selected)
                Box(
                    Modifier.align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.pause),
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
        }
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OrbitCatalog(section: HomePage.Section, nav: NavController) {
    val player = LocalPlayerConnection.current
    val menus = LocalMenuState.current
    val scope = rememberCoroutineScope()
    OrbitShelfLayout { coverWidth ->
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                section.items.distinctBy { it.javaClass.simpleName + it.id },
                key = { it.javaClass.simpleName + ":" + it.id },
            ) { item ->
                OrbitCover(
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
