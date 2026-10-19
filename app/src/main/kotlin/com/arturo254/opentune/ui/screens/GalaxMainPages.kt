package com.arturo254.opentune.ui.screens

import android.net.Uri
import com.arturo254.opentune.core.designsystem.theme.galaxScreenBackground
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
import androidx.compose.ui.graphics.luminance
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
            0 -> HomeScreen(navController)
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
fun GalaxDiscoverScreen(navController: NavController, onOpenSearch: () -> Unit,
    genresModel: MoodAndGenresViewModel = hiltViewModel(), home: HomeViewModel = hiltViewModel()) {
    val genres by genresModel.moodAndGenres.collectAsState()
    val genresLoading by genresModel.isLoading.collectAsState()
    val genresFailed by genresModel.failed.collectAsState()
    val recommendations by home.quickPicks.collectAsState()
    val similar by home.similarRecommendations.collectAsState()
    val discoveries = remember(similar) { similar.orEmpty().flatMap { it.items }
        .filterIsInstance<com.arturo254.opentune.innertube.models.SongItem>().distinctBy { it.id }.take(6) }
    val player = LocalPlayerConnection.current
    LazyColumn(Modifier.fillMaxSize().galaxScreenBackground(), contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item("discover_header") {
            GalaxDiscoverPortal(
                title = stringResource(R.string.galax_discover_title),
                subtitle = stringResource(R.string.galax_discover_subtitle),
                searchHint = stringResource(R.string.galax_search_hint),
                onSearch = onOpenSearch,
            )
        }
        item("featured_genres_heading") {
            GalaxDiscoverEditorialHeading(
                title = stringResource(R.string.mood_and_genres),
                subtitle = "Escoge un universo musical para empezar.",
                number = "01",
            )
        }
        item("featured_genres") {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val featured = genres.orEmpty().take(4)
                if (featured.isEmpty()) {

                    listOf("Pop", "Rock", "Hip-hop", "Electrónica").chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEachIndexed { index, title -> GalaxGenreSurface(title, if (index == 0) 0xFF4064AD else 0xFF7554A4,
                                { navController.navigate("search/${Uri.encode(title)}") }, Modifier.weight(1f)) }
                        }
                    }
                } else featured.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { genre -> GalaxGenreSurface(genre.title, genre.stripeColor,
                            { navController.navigate("youtube_browse/${genre.endpoint.browseId}?params=${Uri.encode(genre.endpoint.params.orEmpty())}") }, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item("for_you") {
            GalaxDiscoverEditorialHeading(
                title = stringResource(R.string.galax_discover_for_you),
                subtitle = "Señales musicales elegidas para tu próxima escucha.",
                number = "02",
            )
        }
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
        item("all_genres") {
            GalaxDiscoverEditorialHeading(
                title = "Más constelaciones",
                subtitle = "Explora todas las categorías y descubre nuevos artistas.",
                number = "03",
            )
        }
        if (genresLoading) item("genres_loading") {
            LinearProgressIndicator(Modifier.padding(horizontal = 24.dp).fillMaxWidth())
        }
        if (genresFailed) item("genres_retry") {
            TextButton(onClick = genresModel::refresh, modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(stringResource(R.string.retry))
            }
        }
        items(genres.orEmpty().drop(4).chunked(2)) { row ->
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { genre -> GalaxGenreSurface(genre.title, genre.stripeColor,
                    { navController.navigate("youtube_browse/${genre.endpoint.browseId}?params=${Uri.encode(genre.endpoint.params.orEmpty())}") }, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
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
