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
import com.arturo254.opentune.viewmodels.AccountViewModel
import com.arturo254.opentune.viewmodels.AccountContentType
import com.arturo254.opentune.ui.component.ChipsRow
import com.arturo254.opentune.innertube.models.YTItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(navController: NavController, scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AccountViewModel = hiltViewModel()) {
    val playlists by viewModel.playlists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val selected by viewModel.selectedContentType.collectAsState()
    val entries: List<YTItem>? = when (selected) {
        AccountContentType.PLAYLISTS -> playlists
        AccountContentType.ALBUMS -> albums
        AccountContentType.ARTISTS -> artists
    }
    val title = stringResource(R.string.account)
    val loading by viewModel.isLoading.collectAsState()
    val failed by viewModel.failed.collectAsState()
    GalaxScaffold(topBar = { GalaxTopAppBar(title = { Text(title) },
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
                GalaxScreenHeading(title, stringResource(R.string.library))
            }
            item("filters", span = { GridItemSpan(maxLineSpan) }) {
                ChipsRow(chips = listOf(
                    AccountContentType.PLAYLISTS to stringResource(R.string.filter_playlists),
                    AccountContentType.ALBUMS to stringResource(R.string.filter_albums),
                    AccountContentType.ARTISTS to stringResource(R.string.filter_artists)),
                    currentValue = selected, onValueUpdate = viewModel::setSelectedContentType)
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
