package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.arturo254.opentune.viewmodels.BrowseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(navController: NavController, scrollBehavior: TopAppBarScrollBehavior, browseId: String?,
    viewModel: BrowseViewModel = hiltViewModel()) {
    val title by viewModel.title.collectAsState()
    val entries by viewModel.items.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val failed by viewModel.failed.collectAsState()
    GalaxScaffold(topBar = { GalaxTopAppBar(title = { Text(title ?: stringResource(R.string.explore)) },
        scrollBehavior = scrollBehavior, navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.back_button_desc))
            }
        }) }) { padding ->
        LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp,
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item("intro", span = { GridItemSpan(maxLineSpan) }) {
                GalaxScreenHeading(title ?: stringResource(R.string.explore), stringResource(R.string.galax_discover_subtitle))
            }
            if (loading) item("loading", span = { GridItemSpan(maxLineSpan) }) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (failed) item("retry", span = { GridItemSpan(maxLineSpan) }) { GalaxRetryPanel(onRetry = viewModel::refresh) }
            if (!loading && !failed && entries.isNullOrEmpty()) item("empty", span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.no_results_found), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(entries.orEmpty(), key = { it.id }, contentType = { it::class.simpleName }) { entry ->
                GalaxRemoteItem(entry, navController, Modifier.fillMaxWidth())
            }
        }
    }
}
