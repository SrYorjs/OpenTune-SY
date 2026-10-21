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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Observatory: an open optical lens, a search console and full-width spectra. */
@Composable
internal fun NovaDiscoveryExperience(
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
    val remote = remember(recommendations) {
        recommendations.orEmpty().flatMap { it.items }.filterIsInstance<SongItem>().distinctBy { it.id }.take(12)
    }
    val local = remember(picks) { picks.orEmpty().distinctBy { it.id }.take(12) }
    val tiles = remember(genres) { genres.orEmpty().distinctBy { it.endpoint.browseId + it.title }.take(12) }
    val fallback = remember { listOf("Electrónica", "Jazz", "Pop", "Rock", "R&B", "Hip-hop") }
    val backStack by navController.currentBackStackEntryAsState()
    val top = backStack?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(top?.value, ambientActive) {
        if (ambientActive && top?.value == true) {
            list.animateScrollToItem(0)
            backStack?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    Box(Modifier.fillMaxSize()) {
        NovaBackdrop(NovaSpace.OBSERVATORY, ambientActive, Modifier.fillMaxSize())
        LazyColumn(state = list, modifier = Modifier.fillMaxSize(),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item("observatory/title") {
                Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("SIN FRONTERAS", style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Descubrir", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                    }
                    NovaIcon(R.drawable.search, "Abrir búsqueda", onOpenSearch)
                }
            }
            item("observatory/lens") { NovaEntrance { ObservatoryLens(ambientActive) } }
            item("observatory/search") {
                Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("¿A dónde te lleva la música?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                    Surface(onClick = onOpenSearch, shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(painterResource(R.drawable.search), null, tint = MaterialTheme.colorScheme.primary)
                            Text("Canciones, artistas, álbumes…", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text("TODOS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            item("observatory/destinations") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ObservatoryDestination("En tendencia", "Lo que mueve al mundo", R.drawable.trending_up, Modifier.weight(1f)) {
                        navController.navigate("charts_screen")
                    }
                    ObservatoryDestination("Recién llegado", "Nuevos lanzamientos", R.drawable.album, Modifier.weight(1f)) {
                        navController.navigate("new_release")
                    }
                }
            }
            item("observatory/genres-title") {
                NovaHeading("Sintoniza un universo", "Géneros y estados de ánimo", "Ver todo") {
                    navController.navigate(Screens.MoodAndGenres.route)
                }
            }
            itemsIndexed(tiles, key = { index, _ -> galaxDiscoveryGenreRowKey(index, fallback = false) }) { index, genre ->
                ObservatorySpectrum(genre.title, Color(genre.stripeColor), index,
                    onClick = { navController.navigate("youtube_browse/${genre.endpoint.browseId}?params=${Uri.encode(genre.endpoint.params.orEmpty())}") })
            }
            if (loading && tiles.isEmpty()) item("observatory/loading") { NovaLoading("Buscando nuevas frecuencias…") }
            if (!loading && tiles.isEmpty()) {
                itemsIndexed(fallback, key = { index, _ -> galaxDiscoveryGenreRowKey(index, fallback = true) }) { index, genre ->
                    ObservatorySpectrum(genre, MaterialTheme.colorScheme.primary, index,
                        onClick = { navController.navigate("search/${Uri.encode(genre)}") })
                }
            }
            if (failed) item("observatory/retry") {
                NovaMessage("No se pudo cargar el catálogo", "Puedes seguir buscando música o volver a intentarlo.", "Reintentar", genresModel::refresh)
            }
            if (remote.isNotEmpty() || local.isNotEmpty()) item("observatory/selection-title") {
                NovaHeading("Fuera de tu órbita", "Algo que aún no conocías")
            }
            if (remote.isNotEmpty()) {
                items(remote, key = { "observatory/remote/${it.id}" }) { song ->
                    NovaMusicRow(song.title, song.artists.joinToString { it.name }, song.thumbnail,
                        modifier = Modifier.padding(horizontal = 12.dp), active = current?.id == song.id, playing = playing,
                        onClick = {
                            if (current?.id == song.id) player?.player?.togglePlayPause()
                            else player?.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                        }, onMenu = { menus.show { YouTubeSongMenu(song = song, navController = navController, onDismiss = menus::dismiss) } })
                }
            } else {
                items(local, key = { "observatory/local/${it.id}" }) { song ->
                    NovaMusicRow(song.title, song.artists.joinToString { it.name }, song.thumbnailUrl,
                        modifier = Modifier.padding(horizontal = 12.dp), active = current?.id == song.id, playing = playing,
                        onClick = {
                            if (current?.id == song.id) player?.player?.togglePlayPause()
                            else player?.playQueue(ListQueue(title = "Descubrir", items = local.map { it.toMediaItem() },
                                startIndex = local.indexOfFirst { it.id == song.id }.coerceAtLeast(0)))
                        }, onMenu = { menus.show { SongMenu(originalSong = song, navController = navController, onDismiss = menus::dismiss) } })
                }
            }
            item("observatory/end") { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun ObservatoryLens(active: Boolean) {
    val phase = rememberNovaPhase(active)
    val colors = MaterialTheme.colorScheme
    Canvas(Modifier.fillMaxWidth().height(190.dp)) {
        val t = phase.value * (2 * PI.toFloat())
        val c = Offset(size.width * .54f, size.height * .5f)
        val r = size.height * .37f
        drawCircle(Brush.radialGradient(listOf(colors.primary.copy(alpha = .18f), Color.Transparent), c, r * 1.8f), r * 1.8f, c)
        repeat(5) { i ->
            val rx = r * (1f + i * .15f)
            val ry = r * (.43f + i * .07f)
            drawOval(colors.primary.copy(alpha = .3f - i * .045f), c - Offset(rx, ry), Size(rx * 2, ry * 2), style = Stroke(1.dp.toPx()))
        }
        drawCircle(colors.background, r * .57f, c)
        drawCircle(Brush.linearGradient(listOf(colors.primary.copy(alpha = .65f), colors.background), c - Offset(r, r), c + Offset(r, r)), r * .55f, c)
        drawArc(colors.onSurface.copy(alpha = .45f), 200f, 105f, false,
            c - Offset(r * .56f, r * .56f), Size(r * 1.12f, r * 1.12f), style = Stroke(1.dp.toPx()))
        val point = c + Offset(cos(t) * r * 1.5f, sin(t) * r * .64f)
        drawCircle(colors.primary.copy(alpha = .16f), 9.dp.toPx(), point)
        drawCircle(colors.primary, 2.5.dp.toPx(), point)
        for (side in listOf(-1, 1)) {
            val x = c.x + side * r * 2.05f
            drawLine(colors.outlineVariant, Offset(x, c.y - 5.dp.toPx()), Offset(x, c.y + 5.dp.toPx()), 1.dp.toPx())
            drawLine(colors.outlineVariant, Offset(x - 5.dp.toPx(), c.y), Offset(x + 5.dp.toPx(), c.y), 1.dp.toPx())
        }
    }
}

@Composable
private fun ObservatoryDestination(title: String, subtitle: String, icon: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
    }
}

@Composable
private fun ObservatorySpectrum(title: String, tint: Color, index: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).heightIn(min = 90.dp)
        .clip(RoundedCornerShape(12.dp)).background(colors.surfaceContainer.copy(alpha = .7f))
        .clickable(role = Role.Button, onClick = onClick)) {
        Canvas(Modifier.matchParentSize()) {
            val ink = lerp(colors.primary, tint, .35f)
            val start = size.width * .58f
            repeat(34) { bar ->
                val x = start + (size.width - start) * bar / 34f
                val wave = .12f + .68f * kotlin.math.abs(sin(bar * (.21f + index * .025f) + index))
                val h = size.height * wave
                drawLine(ink.copy(alpha = .16f + bar / 140f), Offset(x, (size.height - h) / 2),
                    Offset(x, (size.height + h) / 2), 2.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("%02d".format(index + 1), style = MaterialTheme.typography.labelSmall, color = colors.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(.7f))
            }
            Icon(painterResource(R.drawable.arrow_forward), null, modifier = Modifier.size(18.dp))
        }
    }
}

internal fun galaxDiscoveryGenreRowKey(index: Int, fallback: Boolean): String =
    (if (fallback) "discovery-fallback:" else "discovery-genre:") + index
