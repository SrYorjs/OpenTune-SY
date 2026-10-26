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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.ShowHomeCategoryChipsKey
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
import kotlin.math.sin

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
    val recent = remember(recentItems) { recentItems.orEmpty().filterIsInstance<Song>().distinctBy { it.id } }
    val selection = remember(picks, recent, dial) {
        (picks.orEmpty() + recent + dial).distinctBy { it.id }
    }
    val backStack by navController.currentBackStackEntryAsState()
    val top = backStack?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(top?.value, ambientActive) {
        if (ambientActive && top?.value == true) {
            list.animateScrollToItem(0)
            backStack?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    LaunchedEffect(showChips, chip) {
        if (!showChips && chip != null) viewModel.toggleChip(chip)
    }
    BackHandler(ambientActive && chip != null) { viewModel.toggleChip(chip) }
    LaunchedEffect(catalog?.continuation, ambientActive) {
        val next = catalog?.continuation ?: return@LaunchedEffect
        if (!ambientActive) return@LaunchedEffect
        snapshotFlow {
            val info = list.layoutInfo
            info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: -1) >= info.totalItemsCount - 4
        }.distinctUntilChanged().filter { it }.collect { viewModel.loadMoreYouTubeItems(next) }
    }
    val play: (Song, List<Song>) -> Unit = { song, queue ->
        if (current?.id == song.id) player?.player?.togglePlayPause()
        else player?.playQueue(
            ListQueue(
                title = context.getString(R.string.home),
                items = queue.map { it.toMediaItem() },
                startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
            ),
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
                if (!started) Toast.makeText(context, R.string.galax_shuffle_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val palette = asterPalette()
    val phase = rememberAsterPhase(ambientActive)
    Box(Modifier.fillMaxSize()) {
        AsterBackdrop(AsterScene.SESSION, phase, Modifier.fillMaxSize())
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize().pullToRefresh(
                isRefreshing = refreshing,
                state = refresh,
                onRefresh = viewModel::refresh,
            ),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item("session/masthead") {
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        AsterEyebrow("GALAXOT  /  INICIO")
                        Text(
                            "A tu ritmo.",
                            style = MaterialTheme.typography.headlineLarge,
                            letterSpacing = (-1.2).sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    AsterIcon(R.drawable.search, "Buscar música", onOpenSearchTab)
                    IconButton(onClick = { navController.navigate("account") }) {
                        AsterArtwork(account, Modifier.size(34.dp), circular = true)
                    }
                }
            }
            item("session/feature") {
                AsterEntrance {
                    SessionFeature(
                        songs = selection.take(3),
                        phase = phase,
                        currentId = current?.id,
                        playing = playing,
                        onPlay = { play(it, selection) },
                        onMenu = menu,
                        onDiscover = onOpenSearchTab,
                    )
                }
            }
            item("session/launchpad") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SessionAction(
                        title = "Sin plan.\nSolo música.",
                        label = if (shuffling) "PREPARANDO" else "MEZCLA PERSONAL",
                        icon = R.drawable.shuffle,
                        accent = palette.violet,
                        modifier = Modifier.weight(1f),
                        enabled = !shuffling && player != null,
                        onClick = shuffle,
                    )
                    SessionAction(
                        title = "Lo que\nse queda.",
                        label = "TU BIBLIOTECA",
                        icon = R.drawable.album,
                        accent = palette.ice,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenLibraryTab,
                    )
                }
            }
            if (recent.isNotEmpty()) {
                item("session/recent-heading") {
                    AsterHeading("Una vez más", "Retoma tu música")
                }
                item("session/recent") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(recent.take(8), key = { it.id }) { song ->
                            SessionResume(
                                song = song,
                                active = current?.id == song.id,
                                playing = playing,
                                onPlay = { play(song, recent) },
                                onMenu = { menu(song) },
                            )
                        }
                    }
                }
            }
            if (showChips && catalog?.chips?.isNotEmpty() == true) {
                item("session/moods") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(catalog?.chips.orEmpty(), key = { index, _ -> "mood/$index" }) { _, mood ->
                            AsterChoice(mood.title, mood == chip, { viewModel.toggleChip(mood) })
                        }
                    }
                }
            }
            if (selection.isNotEmpty()) {
                item("session/selection-heading") {
                    AsterHeading("Tu frecuencia", "Hecho para ti")
                }
                item("session/selection") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        itemsIndexed(selection.take(12), key = { _, song -> song.id }) { index, song ->
                            AsterRecordCard(
                                title = song.title,
                                subtitle = song.artists.joinToString { it.name },
                                artwork = song.thumbnailUrl,
                                onClick = { play(song, selection) },
                                onMenu = { menu(song) },
                                active = current?.id == song.id,
                                playing = playing,
                                index = index + 1,
                            )
                        }
                    }
                }
            }
            item("session/recognition") {
                SessionRecognition {
                    navController.navigate(com.arturo254.opentune.ui.screens.musicrecognition.MusicRecognitionRoute)
                }
            }
            if (dial.isNotEmpty()) {
                item("session/dial-heading") {
                    AsterHeading(stringResource(R.string.speed_dial), "Acceso directo")
                }
                itemsIndexed(dial.distinctBy { it.id }.take(6), key = { _, song -> "session/dial/${song.id}" }) { index, song ->
                    AsterMusicRow(
                        title = song.title,
                        subtitle = song.artists.joinToString { it.name },
                        artwork = song.thumbnailUrl,
                        onClick = { play(song, dial) },
                        onMenu = { menu(song) },
                        active = current?.id == song.id,
                        playing = playing,
                        number = index + 1,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
            if (!forgotten.isNullOrEmpty()) {
                item("session/rediscover-heading") {
                    AsterHeading("Seguían aquí", stringResource(R.string.forgotten_favorites))
                }
                item("session/rediscover") {
                    SessionShelf(forgotten.orEmpty().distinctBy { it.id }.take(16), current?.id, playing, play, menu)
                }
            }
            if (trends.isNotEmpty() && chip == null) {
                item("session/global-heading") {
                    AsterHeading("El mundo suena", "En tendencia", "Explorar") {
                        navController.navigate("charts_screen")
                    }
                }
                item("session/global") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(
                            trends.distinctBy { it.provider.value + ":" + it.sourceId }.take(16),
                            key = { it.provider.value + ":" + it.sourceId },
                        ) { track ->
                            AsterRecordCard(
                                title = track.title,
                                subtitle = track.artists.joinToString(),
                                artwork = track.artworkUrl,
                                busy = resolving == track.sourceId,
                                onClick = {
                                    if (player != null && resolving == null) scope.launch {
                                        resolving = track.sourceId
                                        try {
                                            if (!viewModel.playSourceVariant(player, track, "Tendencias")) {
                                                Toast.makeText(context, R.string.catalog_source_unplayable, Toast.LENGTH_SHORT).show()
                                            }
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
            catalog?.sections.orEmpty().forEachIndexed { index, section ->
                item(galaxHomeSectionRowKey(index, true)) {
                    AsterHeading(
                        title = section.title,
                        detail = section.label,
                        action = if (section.endpoint?.browseId != null) "Explorar" else null,
                        onAction = {
                            section.endpoint?.browseId?.let { id ->
                                navController.navigate(if (id == "FEmusic_moods_and_genres") Screens.MoodAndGenres.route else "browse/$id")
                            }
                        },
                    )
                }
                item(galaxHomeSectionRowKey(index, false)) {
                    SessionCatalog(section, navController)
                }
            }
            if (loading) item("session/loading") { AsterLoading("Afinando tus recomendaciones…") }
            if (!loading && selection.isEmpty() && catalog?.sections.isNullOrEmpty() && trends.isEmpty()) {
                item("session/empty") {
                    AsterMessage(
                        "Tu primera órbita",
                        "Encuentra una canción que te guste. Tu espacio musical empieza contigo.",
                        "Actualizar recomendaciones",
                        viewModel::refresh,
                    )
                }
            }
            item("session/end") { Spacer(Modifier.height(28.dp)) }
        }
        Indicator(
            isRefreshing = refreshing,
            state = refresh,
            modifier = Modifier.align(Alignment.TopCenter).padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}

@Composable
private fun SessionFeature(
    songs: List<Song>,
    phase: State<Float>,
    currentId: String?,
    playing: Boolean,
    onPlay: (Song) -> Unit,
    onMenu: (Song) -> Unit,
    onDiscover: () -> Unit,
) {
    val palette = asterPalette()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = songs.firstOrNull { it.id == selectedId } ?: songs.firstOrNull()
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(palette.panel.copy(alpha = .8f))
            .border(1.dp, palette.line, RoundedCornerShape(28.dp)),
    ) {
        Box(Modifier.fillMaxWidth().height(244.dp)) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(Brush.radialGradient(listOf(palette.violet.copy(alpha = .2f), Color.Transparent), center, size.width * .75f))
                drawAsterGlyph(1, palette.violet, phase.value)
                drawLine(palette.line, Offset(20.dp.toPx(), size.height * .86f), Offset(size.width - 20.dp.toPx(), size.height * .86f), 1.dp.toPx())
            }
            AsterEyebrow("SESIÓN PERSONAL", Modifier.align(Alignment.TopStart).padding(20.dp))
            Box(
                Modifier.align(Alignment.Center).padding(top = 24.dp).size(170.dp)
                    .graphicsLayer {
                        rotationZ = -8f
                        translationY = sin(phase.value * 6.283f) * 4.dp.toPx()
                    }
                    .background(palette.violet.copy(alpha = .2f), RoundedCornerShape(14.dp))
                    .border(1.dp, palette.violet.copy(alpha = .22f), RoundedCornerShape(14.dp)),
            )
            AsterArtwork(
                selected?.thumbnailUrl,
                Modifier.align(Alignment.Center).padding(top = 24.dp).size(170.dp)
                    .graphicsLayer {
                        rotationZ = 5f
                        translationY = sin(phase.value * 6.283f + .4f) * 4.dp.toPx()
                    },
            )
            Text(
                "${(songs.indexOf(selected) + 1).coerceAtLeast(1).toString().padStart(2, '0')} / ${songs.size.coerceAtLeast(1).toString().padStart(2, '0')}",
                modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted,
            )
        }
        Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                selected?.title ?: "Dale play\na tu universo.",
                style = MaterialTheme.typography.headlineMedium,
                letterSpacing = (-.8).sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                selected?.artists?.joinToString { it.name } ?: "Un espacio para perderte y encontrarte en la música.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isPlaying = selected != null && selected.id == currentId && playing
                Button(
                    onClick = { if (selected == null) onDiscover() else onPlay(selected) },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.violet, contentColor = palette.base),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
                ) {
                    Icon(painterResource(if (isPlaying) R.drawable.pause else R.drawable.play), null, Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (selected == null) "Descubrir" else if (isPlaying) "Pausar" else "Escuchar")
                }
                Spacer(Modifier.weight(1f))
                if (selected != null) AsterIcon(R.drawable.more_vert, "Opciones de ${selected.title}", { onMenu(selected) })
            }
            if (songs.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    songs.forEachIndexed { index, song ->
                        AsterChoice(
                            text = (index + 1).toString().padStart(2, '0'),
                            selected = song.id == selected?.id,
                            onClick = { selectedId = song.id },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionAction(
    title: String,
    label: String,
    icon: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val palette = asterPalette()
    Column(
        modifier.clip(RoundedCornerShape(22.dp))
            .background(accent.copy(alpha = .075f))
            .border(1.dp, accent.copy(alpha = .18f), RoundedCornerShape(22.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(icon), null, Modifier.size(23.dp), tint = accent)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = palette.muted, letterSpacing = .7.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionResume(
    song: Song,
    active: Boolean,
    playing: Boolean,
    onPlay: () -> Unit,
    onMenu: () -> Unit,
) {
    val palette = asterPalette()
    Row(
        modifier = Modifier.width(278.dp).clip(RoundedCornerShape(16.dp))
            .background(palette.panel.copy(alpha = .65f))
            .combinedClickable(role = Role.Button, onClick = onPlay, onLongClick = onMenu)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsterArtwork(song.thumbnailUrl, Modifier.size(54.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artists.joinToString { it.name }, style = MaterialTheme.typography.bodySmall, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AsterPlaybackMark(active && playing)
    }
}

@Composable
private fun SessionRecognition(onClick: () -> Unit) {
    val palette = asterPalette()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, palette.line, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Canvas(Modifier.size(34.dp)) {
            repeat(7) { bar ->
                val height = size.height * (.25f + .65f * kotlin.math.abs(sin(bar * 1.2f)))
                val x = size.width * (bar + .5f) / 7
                drawLine(palette.ice, Offset(x, (size.height - height) / 2), Offset(x, (size.height + height) / 2), 2.dp.toPx(), StrokeCap.Round)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("¿Qué está sonando?", style = MaterialTheme.typography.titleSmall)
            Text("Identifica la canción a tu alrededor", style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }
        Icon(painterResource(R.drawable.arrow_forward), null, Modifier.size(18.dp), tint = palette.ice)
    }
}

@Composable
private fun SessionShelf(
    songs: List<Song>,
    current: String?,
    playing: Boolean,
    play: (Song, List<Song>) -> Unit,
    menu: (Song) -> Unit,
) {
    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(songs, key = { it.id }) { song ->
            AsterRecordCard(
                title = song.title,
                subtitle = song.artists.joinToString { it.name },
                artwork = song.thumbnailUrl,
                active = current == song.id,
                playing = playing,
                onClick = { play(song, songs) },
                onMenu = { menu(song) },
            )
        }
    }
}

@Composable
private fun SessionCatalog(section: HomePage.Section, nav: NavController) {
    val player = LocalPlayerConnection.current
    val menus = LocalMenuState.current
    val scope = rememberCoroutineScope()
    val current = player?.mediaMetadata?.collectAsStateWithLifecycle()?.value
    val playing = player?.isPlaying?.collectAsStateWithLifecycle()?.value == true
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(
            section.items.distinctBy { it.javaClass.simpleName + ":" + it.id },
            key = { it.javaClass.simpleName + ":" + it.id },
        ) { item ->
            val subtitle = when (item) {
                is SongItem -> item.artists.joinToString { it.name }
                is AlbumItem -> item.artists.orEmpty().joinToString { it.name }
                is ArtistItem -> "Artista"
                is PlaylistItem -> item.author?.name.orEmpty()
            }
            AsterRecordCard(
                title = item.title,
                subtitle = subtitle,
                artwork = item.thumbnail,
                circular = item is ArtistItem,
                active = item is SongItem && current?.id == item.id,
                playing = playing,
                onClick = {
                    when (item) {
                        is SongItem -> {
                            if (current?.id == item.id) {
                                player?.player?.togglePlayPause()
                            } else {
                                player?.playQueue(
                                    YouTubeQueue(
                                        item.endpoint ?: WatchEndpoint(videoId = item.id),
                                        item.toMediaMetadata(),
                                    ),
                                )
                            }
                        }
                        is AlbumItem -> nav.navigate("album/${item.id}")
                        is ArtistItem -> nav.navigate("artist/${item.id}")
                        is PlaylistItem -> nav.navigate("online_playlist/${item.id}")
                    }
                },
                onMenu = {
                    menus.show {
                        when (item) {
                            is SongItem -> YouTubeSongMenu(
                                song = item,
                                navController = nav,
                                onDismiss = menus::dismiss,
                            )
                            is AlbumItem -> YouTubeAlbumMenu(
                                albumItem = item,
                                navController = nav,
                                onDismiss = menus::dismiss,
                            )
                            is ArtistItem -> YouTubeArtistMenu(
                                artist = item,
                                onDismiss = menus::dismiss,
                            )
                            is PlaylistItem -> YouTubePlaylistMenu(
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

internal fun galaxHomeSectionRowKey(index: Int, heading: Boolean): String =
    (if (heading) "home-section-title:" else "home-section-content:") + index
