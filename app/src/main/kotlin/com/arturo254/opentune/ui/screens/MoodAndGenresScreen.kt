package com.arturo254.opentune.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.screens.search.*
import com.arturo254.opentune.viewmodels.MoodAndGenresViewModel

@Composable
fun MoodAndGenresScreen(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val genres by viewModel.moodAndGenres.collectAsStateWithLifecycle()
    val loading by viewModel.isLoading.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val grid = rememberLazyGridState()
    val backStack by navController.currentBackStackEntryAsState()
    val top = backStack?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(top?.value) {
        if (top?.value == true) {
            grid.animateScrollToItem(0)
            backStack?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    val entries =
        remember(genres) {
            genres.orEmpty().distinctBy {
                Triple(it.title, it.endpoint.browseId, it.endpoint.params)
            }
        }
    PrismScreenFrame { layout, padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(layout.columns),
            state = grid,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("heading", span = { GridItemSpan(maxLineSpan) }) {
                PrismHeading(
                    stringResource(R.string.mood_and_genres),
                    stringResource(R.string.prism_dest_genres_detail),
                    eyebrow = stringResource(R.string.prism_dest_explore),
                    onBack = { navController.navigateUp() },
                )
            }
            if (loading)
                item("loading", span = { GridItemSpan(maxLineSpan) }) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                }
            if (failed)
                item("retry", span = { GridItemSpan(maxLineSpan) }) {
                    PrismStatus(
                        stringResource(R.string.prism_dest_unavailable),
                        stringResource(R.string.prism_dest_retry_detail),
                        stringResource(R.string.retry),
                        viewModel::refresh,
                    )
                }
            if (!loading && !failed && entries.isEmpty())
                item("empty", span = { GridItemSpan(maxLineSpan) }) {
                    PrismStatus(
                        stringResource(R.string.no_results_found),
                        actionLabel = stringResource(R.string.refresh),
                        onAction = viewModel::refresh,
                    )
                }
            itemsIndexed(
                entries,
                key = { _, entry ->
                    "${entry.title}:${entry.endpoint.browseId}:${entry.endpoint.params}"
                },
            ) { index, genre ->
                val column = index % layout.columns
                PrismGenreTile(
                    genre.title,
                    Color(genre.stripeColor),
                    index,
                    onClick = {
                        navController.navigate(
                            "youtube_browse/${genre.endpoint.browseId}?params=${Uri.encode(genre.endpoint.params.orEmpty())}"
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(
                                start = if (column == 0) 24.dp else 0.dp,
                                end = if (column == layout.columns - 1) 24.dp else 0.dp,
                            ),
                )
            }
        }
    }
}

/** Kept for the existing Explore route; it shares the new natural-height genre tile. */
@Composable
fun MoodAndGenresButton(
    title: String,
    stripeColor: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PrismGenreTile(
        title,
        Color(stripeColor),
        title.hashCode().and(Int.MAX_VALUE) % 12,
        onClick,
        modifier,
    )
}
