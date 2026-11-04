package com.arturo254.opentune.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.ui.screens.search.*
import com.arturo254.opentune.viewmodels.ChartsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ChartsScreen(navController: NavController, viewModel: ChartsViewModel = hiltViewModel()) {
    val page by viewModel.chartsPage.collectAsStateWithLifecycle()
    val loading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val alternate by viewModel.catalogChart.collectAsStateWithLifecycle()
    val player = LocalPlayerConnection.current
    val queueTitle = stringResource(R.string.prism_charts)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { if (page == null) viewModel.loadCharts() }
    PrismScreenFrame { layout, padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("heading") {
                PrismHeading(
                    queueTitle,
                    stringResource(R.string.prism_dest_chart_detail),
                    eyebrow = stringResource(R.string.prism_dest_explore),
                    onBack = { navController.navigateUp() },
                )
            }
            if (loading && page == null)
                item("loading") {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                }
            if (error != null)
                item("retry") {
                    PrismStatus(
                        stringResource(R.string.prism_dest_unavailable),
                        stringResource(R.string.prism_dest_retry_detail),
                        stringResource(R.string.retry),
                        viewModel::loadCharts,
                    )
                }
            page?.sections?.forEachIndexed { sectionIndex, section ->
                val entries = section.items.distinctBy { it.javaClass.simpleName + it.id }
                if (entries.isNotEmpty()) {
                    item("heading/$sectionIndex") {
                        PrismSectionTitle(
                            when (section.title) {
                                "Trending" -> stringResource(R.string.trending)
                                "Top music videos" -> stringResource(R.string.top_music_videos)
                                else -> section.title
                            }
                        )
                    }
                    if (section.title == "Top music videos" || entries.any { it !is SongItem }) {
                        item("shelf/$sectionIndex") {
                            PrismRemoteShelf(entries, navController, layout.tileWidth)
                        }
                    } else {
                        val rowColumns = (layout.columns / 2).coerceAtLeast(1)
                        itemsIndexed(
                            entries.chunked(rowColumns),
                            key = { index, _ -> "ranking/$sectionIndex/$index" },
                        ) { rowIndex, row ->
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                row.forEachIndexed { index, entry ->
                                    PrismRemoteItem(
                                        entry,
                                        navController,
                                        Modifier.weight(1f),
                                        rank = rowIndex * rowColumns + index + 1,
                                    )
                                }
                                repeat(rowColumns - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
            if (alternate.isNotEmpty() && page?.sections.orEmpty().all { it.items.isEmpty() }) {
                item("catalog/source") {
                    PrismSectionTitle(stringResource(R.string.prism_global_chart), "Deezer")
                }
                itemsIndexed(
                    alternate.distinctBy { it.sourceId },
                    key = { _, track -> "catalog/${track.sourceId}" },
                ) { index, track ->
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                enabled = pending == null && player != null,
                                role = Role.Button,
                            ) {
                                val connection = player ?: return@clickable
                                pending = track.sourceId
                                scope.launch {
                                    try {
                                        if (
                                            !viewModel.playCatalogTrack(
                                                connection,
                                                track,
                                                queueTitle,
                                            )
                                        )
                                            Toast.makeText(
                                                    context,
                                                    R.string.catalog_source_unplayable,
                                                    Toast.LENGTH_SHORT,
                                                )
                                                .show()
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                                context,
                                                R.string.catalog_source_unplayable,
                                                Toast.LENGTH_SHORT,
                                            )
                                            .show()
                                    } finally {
                                        pending = null
                                    }
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            (index + 1).toString().padStart(2, '0'),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        PrismArtwork(
                            track.artworkUrl,
                            Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)),
                        )
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(track.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                track.artists.joinToString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (pending == track.sourceId)
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(painterResource(R.drawable.play), null, Modifier.size(20.dp))
                    }
                }
            }
            if (
                !loading &&
                    error == null &&
                    alternate.isEmpty() &&
                    page?.sections.orEmpty().all { it.items.isEmpty() }
            )
                item("empty") {
                    PrismStatus(
                        stringResource(R.string.no_results_found),
                        actionLabel = stringResource(R.string.refresh),
                        onAction = viewModel::loadCharts,
                    )
                }
            if (page?.continuation != null)
                item("more") {
                    if (loading)
                        LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                    else
                        TextButton(
                            onClick = viewModel::loadMore,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        ) {
                            Text(stringResource(R.string.galax_show_more))
                        }
                }
        }
    }
}
