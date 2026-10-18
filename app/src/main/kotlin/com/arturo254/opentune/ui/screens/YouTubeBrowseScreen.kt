package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.viewmodels.YouTubeBrowseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeBrowseScreen(navController: NavController, viewModel: YouTubeBrowseViewModel = hiltViewModel()) {
    val result by viewModel.result.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val failed by viewModel.failed.collectAsState()
    GalaxScaffold(topBar = { GalaxTopAppBar(title = { Text(result?.title ?: stringResource(R.string.explore)) },
        navigationIcon = { IconButton(onClick = { navController.navigateUp() }) {
            Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.back_button_desc))
        } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            result?.let { page ->
                item("intro", contentType = "header") {
                    GalaxMediaHeader(title = page.title ?: stringResource(R.string.explore),
                        label = stringResource(R.string.galax_discover_title),
                        artworkUrls = listOf(page.thumbnail ?: page.items.firstOrNull()?.items?.firstOrNull()?.thumbnail))
                }
            }
            if (loading) item("loading") { LinearProgressIndicator(Modifier.padding(horizontal = 24.dp).fillMaxWidth()) }
            if (failed) item("retry") { GalaxRetryPanel(onRetry = viewModel::refresh) }
            result?.items?.forEachIndexed { sectionIndex, section ->
                val entries = section.items.distinctBy { it.id }
                if (entries.isNotEmpty()) {
                    section.title?.let { title -> item("title_$sectionIndex", contentType = "heading") { GalaxSectionHeading(title) } }
                    if (entries.all { it is SongItem }) {
                        itemsIndexed(entries, key = { _, entry -> "browse:$sectionIndex:${entry.id}" }, contentType = { _, _ -> "song" }) { index, entry ->
                            GalaxRemoteItem(entry, navController, ranked = index + 1)
                        }
                    } else item("shelf_$sectionIndex", contentType = "shelf") { GalaxRemoteShelf(entries, navController) }
                }
            }
            if (!loading && !failed && result?.items.isNullOrEmpty()) item("empty") {
                Text(stringResource(R.string.no_results_found), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
