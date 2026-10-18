package com.arturo254.opentune.ui.screens

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidSurface
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
    HorizontalPager(state = pager, key = { Screens.MainScreens[it].route }, modifier = Modifier.fillMaxSize()) { page ->
        when (page) {
            0 -> HomeScreen(navController)
            1 -> GalaxDiscoverScreen(navController, onOpenSearch)
            2 -> LibraryScreen(navController)
        }
    }
}

/** A source-only nebula and sibling optical panel: never capture its own glass. */
@Composable
fun GalaxPortal(title: String, subtitle: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val backdrop = rememberLayerBackdrop()
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.tertiary
    val background = MaterialTheme.colorScheme.surfaceContainerLow
    val motion = OpenTuneDesignSystem.galaxMotion
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(motion.reducedMotion) {
        if (!motion.reducedMotion) while (true) {
            withFrameMillis { phase = (it % 24000L) / 24000f }
        }
    }
    Box(modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth().height(184.dp).clip(RoundedCornerShape(32.dp))) {
        Canvas(Modifier.matchParentSize().layerBackdrop(backdrop)) {
            drawRect(background)
            val x = size.width * (.65f + .12f * kotlin.math.sin(phase * 6.283f))
            drawCircle(Brush.radialGradient(listOf(primary.copy(alpha = .40f), Color.Transparent), Offset(x, size.height * .2f), size.width * .65f), size.width * .65f, Offset(x, size.height * .2f))
            drawCircle(Brush.radialGradient(listOf(secondary.copy(alpha = .24f), Color.Transparent), Offset(size.width * .12f, size.height), size.width * .5f), size.width * .5f, Offset(size.width * .12f, size.height))
            repeat(23) { i ->
                drawCircle(primary.copy(alpha = .2f + (i % 4) * .12f), if (i % 5 == 0) 2.2f else 1.2f,
                    Offset(size.width * ((i * 37 % 101) / 101f), size.height * ((i * 19 % 97) / 97f)))
            }
            drawArc(primary.copy(alpha = .18f), -35f + phase * 12f, 230f, false,
                topLeft = Offset(size.width * .49f, -size.height * .40f), size = androidx.compose.ui.geometry.Size(size.height * 1.8f, size.height * 1.8f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.3f))
        }
        GalaxLiquidSurface(backdrop = backdrop, shapeOverride = RoundedCornerShape(32.dp),
            modifier = Modifier.matchParentSize().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
            Column(Modifier.align(Alignment.BottomStart).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("G A L A X O T", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun GalaxDiscoverScreen(navController: NavController, onOpenSearch: () -> Unit,
    genresModel: MoodAndGenresViewModel = hiltViewModel(), home: HomeViewModel = hiltViewModel()) {
    val genres by genresModel.moodAndGenres.collectAsState()
    val recommendations by home.quickPicks.collectAsState()
    val similar by home.similarRecommendations.collectAsState()
    val discoveries = remember(similar) { similar.orEmpty().flatMap { it.items }
        .filterIsInstance<com.arturo254.opentune.innertube.models.SongItem>().distinctBy { it.id }.take(6) }
    val player = LocalPlayerConnection.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item("discover_header") {
            GalaxPortal(stringResource(R.string.galax_discover_title), stringResource(R.string.galax_discover_subtitle))
        }
        item("search_field") {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().heightIn(min = 60.dp).clickable(onClick = onOpenSearch)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(painterResource(R.drawable.search), null)
                    Text(stringResource(R.string.galax_search_hint), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item("featured_genres") {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val featured = genres.orEmpty().take(4)
                if (featured.isEmpty()) {
                    // Useful offline/cold-start discovery; no invented personalization.
                    listOf("Pop", "Rock", "Hip-hop", "Electrónica").chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEachIndexed { index, title -> MoodAndGenresButton(title, if (index == 0) 0xFF4064AD else 0xFF7554A4,
                                { navController.navigate("search/${Uri.encode(title)}") }, Modifier.weight(1f)) }
                        }
                    }
                } else featured.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { genre -> MoodAndGenresButton(genre.title, genre.stripeColor,
                            { navController.navigate("youtube_browse/${genre.endpoint.browseId}?params=${Uri.encode(genre.endpoint.params.orEmpty())}") }, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item("for_you") { Text(stringResource(R.string.galax_discover_for_you), Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.titleLarge) }
        if (recommendations.isNullOrEmpty() && discoveries.isEmpty()) {
            item("start_listening") { Text(stringResource(R.string.galax_discover_empty), Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(discoveries, key = { "new:${it.id}" }) { song ->
            com.arturo254.opentune.ui.component.YouTubeListItem(item = song,
                modifier = Modifier.padding(horizontal = 12.dp).animateItem().clickable {
                    player?.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                })
        }
        items(if (discoveries.isEmpty()) recommendations.orEmpty().distinctBy { it.id }.take(6) else emptyList(), key = { "discovery:${it.id}" }) { song ->
            SongListItem(song = song, modifier = Modifier.padding(horizontal = 12.dp).animateItem().clickable {
                player?.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
            })
        }
        item("all_genres") { Text(stringResource(R.string.mood_and_genres), Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.titleLarge) }
        items(genres.orEmpty().drop(4).chunked(2)) { row ->
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { genre -> MoodAndGenresButton(genre.title, genre.stripeColor,
                    { navController.navigate("youtube_browse/${genre.endpoint.browseId}?params=${Uri.encode(genre.endpoint.params.orEmpty())}") }, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
