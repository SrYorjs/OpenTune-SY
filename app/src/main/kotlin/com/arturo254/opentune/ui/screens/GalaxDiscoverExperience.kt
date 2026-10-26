package com.arturo254.opentune.ui.screens

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.menu.YouTubeSongMenu
import com.arturo254.opentune.viewmodels.HomeViewModel
import com.arturo254.opentune.viewmodels.MoodAndGenresViewModel

@Composable
internal fun AsterDiscoveryExperience(
    navController: NavController,
    onOpenSearch: () -> Unit,
    genresModel: MoodAndGenresViewModel,
    home: HomeViewModel,
    ambientActive: Boolean,
) {
    val genres by genresModel.moodAndGenres.collectAsStateWithLifecycle()
    val loading by genresModel.isLoading.collectAsStateWithLifecycle()
    val failed by genresModel.failed.collectAsStateWithLifecycle()
    val picks by home.quickPicks.collectAsStateWithLifecycle()
    val recommendations by home.similarRecommendations.collectAsStateWithLifecycle()
    val player = LocalPlayerConnection.current
    val current = player?.mediaMetadata?.collectAsStateWithLifecycle()?.value
    val playing = player?.isPlaying?.collectAsStateWithLifecycle()?.value == true
    val menus = LocalMenuState.current
    val list = rememberLazyListState()
    val palette = asterPalette()
    val phase = rememberAsterPhase(ambientActive)
    val remote = remember(recommendations) {
        recommendations.orEmpty().flatMap { it.items }.filterIsInstance<SongItem>().distinctBy { it.id }.take(12)
    }
    val local = remember(picks) { picks.orEmpty().distinctBy { it.id }.take(12) }
    val tiles = remember(genres) {
        genres.orEmpty().distinctBy { it.endpoint.browseId + ":" + it.title }.take(12)
    }
    val fallback = remember {
        listOf("Electrónica", "Jazz", "Pop", "Rock", "R&B", "Hip-hop", "Clásica", "Ambient")
    }
    val backStack by navController.currentBackStackEntryAsState()
    val top = backStack?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(top?.value, ambientActive) {
        if (ambientActive && top?.value == true) {
            list.animateScrollToItem(0)
            backStack?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    Box(Modifier.fillMaxSize()) {
        AsterBackdrop(AsterScene.EXPLORE, phase, Modifier.fillMaxSize())
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item("explore/heading") {
                Column(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsterEyebrow("DESCUBRIR", Modifier.weight(1f))
                        Box(Modifier.size(6.dp).background(palette.ice, CircleShape))
                        Spacer(Modifier.width(7.dp))
                        Text("SIN LÍMITES", style = MaterialTheme.typography.labelSmall, color = palette.muted, letterSpacing = 1.sp)
                    }
                    Text(
                        "Sal de\ntu órbita.",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-2).sp,
                        lineHeight = 50.sp,
                    )
                    Text(
                        "Otro sonido. Otra forma de sentir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.muted,
                    )
                }
            }
            item("explore/search") {
                AsterEntrance { ExploreSearchConsole(onOpenSearch) }
            }
            item("explore/destinations") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ExploreDestination(
                        title = "En tendencia",
                        subtitle = "El pulso del mundo",
                        icon = R.drawable.trending_up,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("charts_screen") },
                    )
                    ExploreDestination(
                        title = "Estrenos",
                        subtitle = "Antes que nadie",
                        icon = R.drawable.album,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("new_release") },
                    )
                }
            }
            item("explore/map-title") {
                AsterHeading("Elige tu destino", "Mapa de sonidos", "Todos") {
                    navController.navigate(Screens.MoodAndGenres.route)
                }
            }
            if (loading && tiles.isEmpty()) {
                item("explore/loading") { AsterLoading("Abriendo nuevos caminos…") }
            }
            itemsIndexed(
                tiles.chunked(2),
                key = { index, _ -> galaxDiscoveryGenreRowKey(index, fallback = false) },
            ) { row, pair ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    pair.forEachIndexed { column, genre ->
                        val index = row * 2 + column
                        ExploreGenre(
                            title = genre.title,
                            index = index,
                            phase = phase,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                navController.navigate("youtube_browse/${genre.endpoint.browseId}?params=${Uri.encode(genre.endpoint.params.orEmpty())}")
                            },
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (!loading && tiles.isEmpty()) {
                itemsIndexed(
                    fallback.chunked(2),
                    key = { index, _ -> galaxDiscoveryGenreRowKey(index, fallback = true) },
                ) { row, pair ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        pair.forEachIndexed { column, genre ->
                            ExploreGenre(
                                title = genre,
                                index = row * 2 + column,
                                phase = phase,
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigate("search/${Uri.encode(genre)}") },
                            )
                        }
                    }
                }
            }
            if (failed) {
                item("explore/retry") {
                    AsterMessage(
                        "El mapa está tardando",
                        "Puedes buscar directamente o volver a cargar los géneros.",
                        "Reintentar",
                        genresModel::refresh,
                    )
                }
            }
            if (remote.isNotEmpty() || local.isNotEmpty()) {
                item("explore/radio-heading") {
                    AsterHeading("Conexiones inesperadas", "Dale una oportunidad")
                }
                item("explore/radio") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (remote.isNotEmpty()) {
                            items(remote, key = { "remote/${it.id}" }) { song ->
                                ExploreRadio(
                                    title = song.title,
                                    artist = song.artists.joinToString { it.name },
                                    artwork = song.thumbnail,
                                    active = current?.id == song.id,
                                    playing = playing,
                                    onPlay = {
                                        if (current?.id == song.id) player?.player?.togglePlayPause()
                                        else player?.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                    },
                                    onMenu = {
                                        menus.show {
                                            YouTubeSongMenu(song = song, navController = navController, onDismiss = menus::dismiss)
                                        }
                                    },
                                )
                            }
                        } else {
                            items(local, key = { "local/${it.id}" }) { song ->
                                ExploreRadio(
                                    title = song.title,
                                    artist = song.artists.joinToString { it.name },
                                    artwork = song.thumbnailUrl,
                                    active = current?.id == song.id,
                                    playing = playing,
                                    onPlay = {
                                        if (current?.id == song.id) player?.player?.togglePlayPause()
                                        else player?.playQueue(
                                            ListQueue(
                                                title = "Descubrir",
                                                items = local.map { it.toMediaItem() },
                                                startIndex = local.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
                                            ),
                                        )
                                    },
                                    onMenu = {
                                        menus.show {
                                            SongMenu(originalSong = song, navController = navController, onDismiss = menus::dismiss)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
            item("explore/invitation") {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(color = palette.line)
                    Text("Siempre hay algo\nque aún no escuchaste.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Light)
                    TextButton(onClick = onOpenSearch) {
                        Text("Encuentra tu próxima canción", color = palette.ice)
                        Spacer(Modifier.width(10.dp))
                        Icon(painterResource(R.drawable.arrow_forward), null, Modifier.size(18.dp), tint = palette.ice)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreSearchConsole(onClick: () -> Unit) {
    val palette = asterPalette()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(palette.ink)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(painterResource(R.drawable.search), null, Modifier.size(28.dp), tint = palette.base)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("¿Qué quieres escuchar?", style = MaterialTheme.typography.titleMedium, color = palette.base, fontWeight = FontWeight.SemiBold)
            Text("Canciones, artistas, álbumes", style = MaterialTheme.typography.bodySmall, color = palette.base.copy(alpha = .75f))
        }
        Icon(painterResource(R.drawable.arrow_forward), null, Modifier.size(20.dp), tint = palette.base)
    }
}

@Composable
private fun ExploreDestination(
    title: String,
    subtitle: String,
    icon: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val palette = asterPalette()
    Column(
        modifier.clip(RoundedCornerShape(18.dp))
            .background(palette.panel.copy(alpha = .7f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painterResource(icon), null, Modifier.size(22.dp), tint = palette.ice)
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.muted)
    }
}

@Composable
private fun ExploreGenre(
    title: String,
    index: Int,
    phase: State<Float>,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val palette = asterPalette()
    val accent = when (index % 3) {
        0 -> palette.violet
        1 -> palette.ice
        else -> palette.rose
    }
    Column(
        modifier.clip(RoundedCornerShape(22.dp))
            .background(accent.copy(alpha = .08f))
            .border(1.dp, accent.copy(alpha = .18f), RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(108.dp)) {
            Canvas(Modifier.matchParentSize()) { drawAsterGlyph(index, accent, phase.value) }
            Text(
                (index + 1).toString().padStart(2, '0'),
                modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(start = 16.dp, end = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Icon(painterResource(R.drawable.arrow_forward), null, Modifier.size(16.dp), tint = accent)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExploreRadio(
    title: String,
    artist: String,
    artwork: String?,
    active: Boolean,
    playing: Boolean,
    onPlay: () -> Unit,
    onMenu: () -> Unit,
) {
    val palette = asterPalette()
    Column(
        Modifier.width(206.dp).clip(RoundedCornerShape(26.dp))
            .background(palette.panel)
            .combinedClickable(role = Role.Button, onClick = onPlay, onLongClick = onMenu)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().border(1.dp, palette.line, CircleShape))
            Box(Modifier.fillMaxSize(.88f).border(1.dp, palette.violet.copy(alpha = .25f), CircleShape))
            AsterArtwork(artwork, Modifier.fillMaxSize(.74f), circular = true)
            Box(
                Modifier.align(Alignment.BottomEnd).size(40.dp).background(palette.base, CircleShape),
                contentAlignment = Alignment.Center,
            ) { AsterPlaybackMark(active && playing) }
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(artist, style = MaterialTheme.typography.bodySmall, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("EXPLORAR SONIDO", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, letterSpacing = .6.sp, color = palette.ice)
            AsterIcon(R.drawable.more_vert, "Opciones de $title", onMenu)
        }
    }
}

internal fun galaxDiscoveryGenreRowKey(index: Int, fallback: Boolean): String =
    (if (fallback) "discovery-fallback:" else "discovery-genre:") + index
