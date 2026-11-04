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
import com.arturo254.opentune.ui.screens.search.*
import com.arturo254.opentune.viewmodels.BrowseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    browseId: String?,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val catalogue by viewModel.items.collectAsStateWithLifecycle()
    val loading by viewModel.isLoading.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val entries =
        remember(catalogue) { catalogue.orEmpty().distinctBy { it.javaClass.simpleName + it.id } }
    PrismScreenFrame { layout, padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item("heading") {
                PrismHeading(
                    title ?: stringResource(R.string.explore),
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
            if (!loading && !failed && entries.isEmpty())
                item("empty") {
                    PrismStatus(
                        stringResource(R.string.no_results_found),
                        actionLabel = stringResource(R.string.refresh),
                        onAction = viewModel::refresh,
                    )
                }
            itemsIndexed(
                entries.chunked(layout.columns),
                key = { index, _ -> "collection/$index" },
            ) { _, row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    row.forEach { entry ->
                        PrismRemoteItem(entry, navController, Modifier.weight(1f))
                    }
                    repeat(layout.columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
