package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.theme.GalaxScreenHeading
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.viewmodels.ChartsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(navController: NavController, viewModel: ChartsViewModel = hiltViewModel()) {
    val page by viewModel.chartsPage.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val list = rememberLazyListState()
    LaunchedEffect(Unit) { if (page == null) viewModel.loadCharts() }
    GalaxScaffold(topBar = {
        GalaxTopAppBar(title = { Text(stringResource(R.string.charts)) }, navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.back_button_desc))
            }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), state = list,
            contentPadding = PaddingValues(bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item("intro") { GalaxScreenHeading(stringResource(R.string.charts), stringResource(R.string.trending)) }
            if (page == null && loading) item("loading") { LinearProgressIndicator(Modifier.padding(horizontal = 24.dp).fillMaxWidth()) }
            if (error != null) item("retry") { GalaxRetryPanel(onRetry = viewModel::loadCharts) }
            page?.sections?.forEachIndexed { sectionIndex, section ->
                val entries = section.items.distinctBy { it.id }
                if (entries.isNotEmpty()) {
                    item("title_$sectionIndex", contentType = "heading") {
                        GalaxSectionHeading(when (section.title) {
                            "Trending" -> stringResource(R.string.trending)
                            "Top music videos" -> stringResource(R.string.top_music_videos)
                            else -> section.title
                        })
                    }
                    if (section.title == "Top music videos" || entries.any { it !is SongItem }) {
                        item("shelf_$sectionIndex", contentType = "shelf") { GalaxRemoteShelf(entries, navController) }
                    } else itemsIndexed(entries, key = { _, entry -> "chart:$sectionIndex:${entry.id}" }, contentType = { _, _ -> "ranked_song" }) { rank, entry ->
                        GalaxRemoteItem(entry, navController, ranked = rank + 1)
                    }
                }
            }
            if (page?.continuation != null) item("more", contentType = "status") {
                if (loading) LinearProgressIndicator(Modifier.padding(horizontal = 24.dp).fillMaxWidth())
                else TextButton(onClick = viewModel::loadMore, modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(stringResource(R.string.galax_show_more))
                }
            }
        }
    }
}
