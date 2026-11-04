package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidEffect
import com.arturo254.opentune.innertube.models.AlbumReleaseType
import com.arturo254.opentune.ui.screens.search.*
import com.arturo254.opentune.viewmodels.NewReleaseUiState
import com.arturo254.opentune.viewmodels.NewReleaseViewModel
import kotlinx.coroutines.launch

/** An adaptive release catalogue: no fixed carousel masks or duplicated featured albums. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReleaseScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: NewReleaseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val releases by viewModel.newReleaseAlbums.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val entries =
        remember(releases, selected) {
            releases
                .distinctBy { it.id }
                .filter { selected == null || it.releaseType.name == selected }
        }
    val categories =
        listOf(
            null to stringResource(R.string.prism_dest_all_releases),
            AlbumReleaseType.ALBUM.name to stringResource(R.string.albums),
            AlbumReleaseType.SINGLE.name to stringResource(R.string.singles),
            AlbumReleaseType.EP.name to stringResource(R.string.prism_dest_eps),
        )
    PrismScreenFrame { layout, padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            state = list,
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item("heading") {
                PrismHeading(
                    stringResource(R.string.new_release_albums),
                    stringResource(R.string.prism_dest_release_detail),
                    eyebrow = stringResource(R.string.prism_dest_explore),
                    onBack = { navController.navigateUp() },
                )
            }
            item("filters") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(categories, key = { it.first ?: "all" }) { (type, label) ->
                        FilterChip(
                            selected = selected == type,
                            onClick = {
                                selected = type
                                scope.launch { list.animateScrollToItem(0) }
                            },
                            label = { Text(label) },
                            shape = RoundedCornerShape(50),
                        )
                    }
                }
            }
            if (state is NewReleaseUiState.Loading)
                item("loading") {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                }
            if (state is NewReleaseUiState.Error)
                item("retry") {
                    PrismStatus(
                        stringResource(R.string.prism_dest_unavailable),
                        stringResource(R.string.prism_dest_retry_detail),
                        stringResource(R.string.retry),
                        viewModel::retry,
                    )
                }
            if (state is NewReleaseUiState.Empty)
                item("empty") {
                    PrismStatus(
                        stringResource(R.string.prism_dest_empty_releases),
                        actionLabel = stringResource(R.string.refresh),
                        onAction = viewModel::retry,
                    )
                }
            if (state is NewReleaseUiState.Success && entries.isEmpty())
                item("empty/category") {
                    PrismStatus(stringResource(R.string.prism_dest_no_category))
                }
            if (entries.isNotEmpty())
                item("count") {
                    Text(
                        stringResource(R.string.prism_dest_count, entries.size),
                        Modifier.padding(horizontal = 24.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            itemsIndexed(
                entries.chunked(layout.columns),
                key = { index, _ -> "releases/$selected/$index" },
            ) { _, row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    row.forEach { album ->
                        PrismRemoteItem(album, navController, Modifier.weight(1f))
                    }
                    repeat(layout.columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
