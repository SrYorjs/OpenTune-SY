package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.arturo254.opentune.R
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.ui.screens.search.*
import com.arturo254.opentune.viewmodels.YouTubeBrowseViewModel

@Composable
fun YouTubeBrowseScreen(
    navController: NavController,
    viewModel: YouTubeBrowseViewModel = hiltViewModel(),
) {
    val result by viewModel.result.collectAsStateWithLifecycle()
    val loading by viewModel.isLoading.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    PrismScreenFrame { layout, padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item("heading") {
                PrismHeading(
                    result?.title ?: stringResource(R.string.explore),
                    stringResource(R.string.prism_dest_collection_detail),
                    eyebrow = stringResource(R.string.prism_dest_catalogue),
                    onBack = { navController.navigateUp() },
                )
            }
            if (loading)
                item("loading") {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                }
            if (failed)
                item("retry") {
                    PrismStatus(
                        stringResource(R.string.prism_dest_unavailable),
                        stringResource(R.string.prism_dest_retry_detail),
                        stringResource(R.string.retry),
                        viewModel::refresh,
                    )
                }
            result?.items?.forEachIndexed { sectionIndex, section ->
                val entries = section.items.distinctBy { it.javaClass.simpleName + it.id }
                if (entries.isNotEmpty()) {
                    section.title
                        ?.takeIf { it.isNotBlank() }
                        ?.let { title ->
                            item("heading/$sectionIndex") { PrismSectionTitle(title) }
                        }
                    if (entries.all { it is SongItem }) {
                        val rowColumns = (layout.columns / 2).coerceAtLeast(1)
                        itemsIndexed(
                            entries.chunked(rowColumns),
                            key = { index, _ -> "songs/$sectionIndex/$index" },
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
                    } else
                        item("shelf/$sectionIndex") {
                            PrismRemoteShelf(entries, navController, layout.tileWidth)
                        }
                }
            }
            if (!loading && !failed && result?.items.orEmpty().all { it.items.isEmpty() })
                item("empty") {
                    PrismStatus(
                        stringResource(R.string.no_results_found),
                        actionLabel = stringResource(R.string.refresh),
                        onAction = viewModel::refresh,
                    )
                }
        }
    }
}
