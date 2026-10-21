package com.arturo254.opentune.ui.screens

import android.net.Uri
import com.arturo254.opentune.core.designsystem.theme.galaxScreenBackground
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.playback.queues.ListQueue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidSurface
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidChrome
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.SongListItem
import com.arturo254.opentune.ui.screens.library.LibraryScreen
import com.arturo254.opentune.viewmodels.HomeViewModel
import com.arturo254.opentune.viewmodels.MoodAndGenresViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

@Composable
fun GalaxMainPages(navController: NavController, pager: PagerState, onOpenSearch: () -> Unit) {
    val scope = rememberCoroutineScope()
    androidx.activity.compose.BackHandler(enabled = pager.currentPage != 0) {
        scope.launch { pager.animateScrollToPage(0) }
    }
    Box(Modifier.fillMaxSize()) {
    HorizontalPager(state = pager, key = { Screens.MainScreens[it].route }, modifier = Modifier.fillMaxSize()) { page ->
        when (page) {
            0 -> HomeScreen(
                navController,
                onOpenSearchTab = { scope.launch { pager.animateScrollToPage(1) } },
                onOpenLibraryTab = { scope.launch { pager.animateScrollToPage(2) } },
            )
            1 -> GalaxDiscoverScreen(navController, onOpenSearch)
            2 -> LibraryScreen(navController)
        }
    }
        val base = if (MaterialTheme.colorScheme.background.luminance() < .45f)
            com.arturo254.opentune.core.designsystem.theme.GalaxScreenStyle.deepSpace else MaterialTheme.colorScheme.background
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(88.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, base.copy(alpha = .94f)))))
    }

}

@Composable
fun GalaxPortal(title: String, subtitle: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    com.arturo254.opentune.core.designsystem.theme.GalaxScreenHeading(
        title, subtitle, modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    )
}

@Composable
fun GalaxDiscoverScreen(
    navController: NavController,
    onOpenSearch: () -> Unit,
    genresModel: MoodAndGenresViewModel = hiltViewModel(),
    home: HomeViewModel = hiltViewModel(),
) {
    val genres by genresModel.moodAndGenres.collectAsState()
    val genresLoading by genresModel.isLoading.collectAsState()
    val genresFailed by genresModel.failed.collectAsState()
    val similar by home.similarRecommendations.collectAsState()
    val recommendations by home.quickPicks.collectAsState()
    val discoveries = remember(similar) {
        similar.orEmpty().flatMap { it.items }
            .filterIsInstance<com.arturo254.opentune.innertube.models.SongItem>()
            .distinctBy { it.id }.take(8)
    }
    val player = LocalPlayerConnection.current
    val active by player?.mediaMetadata?.collectAsState() ?: remember {
        mutableStateOf<com.arturo254.opentune.models.MediaMetadata?>(null)
    }
    val playing by player?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    LazyColumn(
        modifier = Modifier.fillMaxSize().galaxScreenBackground(),
        contentPadding = insets,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item("search_hero") {
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                    .height(226.dp).clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(listOf(
                            Color(0xFF0C1D35),
                            MaterialTheme.colorScheme.primary.copy(alpha = .36f),
                            Color(0xFF090F20),
                        ))
                    ),
            ) {
                com.arturo254.opentune.core.designsystem.component.GalaxOrbitField(
                    modifier = Modifier.matchParentSize(),
                    active = true,
                    intensity = .38f,
                )
                Column(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(23.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text("Buscar", style = MaterialTheme.typography.headlineLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = (-1.2).sp, color = Color.White)
                    Row(
                        Modifier.fillMaxWidth().height(55.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = .13f))
                            .galaxLiquidChrome(RoundedCornerShape(18.dp), .8f)
                            .clickable(onClick = onOpenSearch).padding(horizontal = 17.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(painterResource(R.drawable.search), null, Modifier.size(22.dp),
                            tint = Color.White.copy(alpha = .9f))
                        Text("Artistas, canciones, álbumes",
                            color = Color.White.copy(alpha = .72f),
                            style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
        }
        item("links_title") { GalaxDiscoverCleanHeading("Explorar") }
        item("links") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GalaxDiscoverQuickLink(
                    "Éxitos", R.drawable.trending_up, Modifier.weight(1f)
                ) { navController.navigate("charts_screen") }
                GalaxDiscoverQuickLink(
                    "Novedades", R.drawable.album, Modifier.weight(1f)
                ) { navController.navigate("new_release") }
            }
        }
        item("genres_title") { GalaxDiscoverCleanHeading(stringResource(R.string.mood_and_genres)) }
        val featured = genres.orEmpty()
            .distinctBy { it.endpoint.browseId + ":" + it.title }
            .take(6)
        if (featured.isEmpty() && !genresLoading) {
            item("genre_suggestions") {
                val defaults = listOf("Pop", "Rock", "Hip-hop", "Electrónica")
                Column(Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    defaults.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEachIndexed { index, title ->
                                GalaxDiscoverGenreTile(
                                    title, if (index == 0) 0xFF4680C1 else 0xFF7465AA,
                                    Modifier.weight(1f)
                                ) { navController.navigate("search/${Uri.encode(title)}") }
                            }
                        }
                    }
                }
            }
        }
        if (featured.isNotEmpty()) {
            itemsIndexed(featured.chunked(2), key = { index, _ ->
                "galax_genre_pair_$" + "index"
            }) { _, row ->
                Row(Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { genre ->
                        GalaxDiscoverGenreTile(
                            genre.title, genre.stripeColor, Modifier.weight(1f)
                        ) {
                            navController.navigate(
                                "youtube_browse/${genre.endpoint.browseId}?params=${Uri.encode(genre.endpoint.params.orEmpty())}"
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (genresLoading) {
            item("genres_loading") {
                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
        }
        if (genresFailed) {
            item("genres_retry") {
                TextButton(
                    onClick = genresModel::refresh,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) { Text(stringResource(R.string.retry)) }
            }
        }
        if (discoveries.isNotEmpty() || !recommendations.isNullOrEmpty()) {
            item("selection_title") { GalaxDiscoverCleanHeading("Para ti") }
        }
        if (discoveries.isNotEmpty()) {
            items(discoveries, key = { "discovery:${it.id}" }) { track ->
                GalaxDiscoverTrackRow(
                    track = track,
                    isPlaying = active?.id == track.id && playing,
                ) {
                    if (active?.id == track.id) {
                        player?.player?.let { p ->
                            p.togglePlayPause()
                        }
                    } else {
                        player?.playQueue(YouTubeQueue.radio(track.toMediaMetadata()))
                    }
                }
            }
        } else {
            items(recommendations.orEmpty().distinctBy { it.id }.take(6),
                key = { "recommend:${it.id}" }) { song ->
                GalaxDiscoverLocalRow(
                    song = song,
                    isPlaying = active?.id == song.id && playing,
                ) {
                    if (active?.id == song.id) {
                        player?.player?.let { p ->
                            p.togglePlayPause()
                        }
                    } else {
                        player?.playQueue(com.arturo254.opentune.playback.queues.ListQueue(
                            title = "Para ti",
                            items = listOf(song.toMediaItem()),
                        ))
                    }
                }
            }
        }
        if (genres.orEmpty().size > 6) {
            item("genres_more") {
                TextButton(
                    onClick = { navController.navigate(Screens.MoodAndGenres.route) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) { Text("Todos los géneros") }
            }
        }
        item("end") { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun GalaxDiscoverCleanHeading(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 23.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        letterSpacing = (-.5).sp,
    )
}

@Composable
private fun GalaxDiscoverQuickLink(
    title: String, icon: Int, modifier: Modifier = Modifier, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(21.dp)
    Row(
        modifier.height(73.dp).clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .galaxLiquidChrome(shape, .68f)
            .clickable(onClick = onClick).padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(icon), null, Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}

@Composable
private fun GalaxDiscoverGenreTile(
    title: String, swatch: Long, modifier: Modifier = Modifier, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(23.dp)
    val accent = Color(swatch)
    Box(
        modifier.height(115.dp).clip(shape)
            .background(Brush.linearGradient(
                listOf(accent.copy(alpha = .88f),
                    MaterialTheme.colorScheme.surfaceContainerHigh, Color(0xFF101B31))
            ))
            .clickable(onClick = onClick),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val origin = Offset(size.width * .87f, size.height * .12f)
            for (i in 0..2) {
                drawCircle(
                    Color.White.copy(alpha = .11f), radius = size.width * (.19f + i * .16f),
                    center = origin,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                )
            }
        }
        Text(title,
            modifier = Modifier.align(Alignment.BottomStart).padding(17.dp),
            color = Color.White, maxLines = 2,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

@Composable
private fun GalaxDiscoverTrackRow(
    track: com.arturo254.opentune.innertube.models.SongItem,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(17.dp))
            .clickable(onClick = onClick).padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        coil3.compose.AsyncImage(
            track.thumbnail, null,
            Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
        Column(Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.bodyLarge,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                color = if (isPlaying) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface)
            Text(track.artists.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Icon(painterResource(if (isPlaying) R.drawable.pause else R.drawable.play), null,
            Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GalaxDiscoverLocalRow(
    song: com.arturo254.opentune.db.entities.Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(17.dp))
            .clickable(onClick = onClick).padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        coil3.compose.AsyncImage(
            song.thumbnailUrl, null,
            Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                color = if (isPlaying) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface)
            Text(song.artists.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Icon(painterResource(if (isPlaying) R.drawable.pause else R.drawable.play), null,
            Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun GalaxCollectionTile(title: String, icon: Int, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val backdrop = rememberLayerBackdrop()
    val surface = MaterialTheme.colorScheme.surfaceContainer
    Box(modifier.heightIn(min = 124.dp).clip(RoundedCornerShape(26.dp))) {
        Canvas(Modifier.matchParentSize().layerBackdrop(backdrop)) {
            drawRect(surface)
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .45f), Color.Transparent),
                Offset(size.width, 0f), size.width), size.width, Offset(size.width, 0f))
            drawCircle(accent.copy(alpha = .22f), size.width * .4f, Offset(size.width, 0f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        }
        GalaxLiquidSurface(backdrop = backdrop, shapeOverride = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            }
        }
    }
}
