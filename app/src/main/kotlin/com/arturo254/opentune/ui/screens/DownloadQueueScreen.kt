package com.arturo254.opentune.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.component.EmptyPlaceholder
import com.arturo254.opentune.ui.component.MenuSurfaceSection
import com.arturo254.opentune.ui.screens.downloads.GalaxDownloadFilter
import com.arturo254.opentune.ui.screens.downloads.GalaxDownloadFilters
import com.arturo254.opentune.ui.screens.downloads.GalaxDownloadOverview
import com.arturo254.opentune.ui.screens.downloads.filterDownloadQueue
import com.arturo254.opentune.ui.screens.downloads.summarizeDownloadQueue
import com.arturo254.opentune.viewmodels.DownloadItem
import com.arturo254.opentune.viewmodels.DownloadQueueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQueueScreen(
    navController: NavController,
    viewModel: DownloadQueueViewModel = hiltViewModel(),
) {
    val downloads by viewModel.downloads.collectAsState()
    val paused by viewModel.downloadsPaused.collectAsState()
    val summary = remember(downloads) { summarizeDownloadQueue(downloads) }
    var filter by rememberSaveable { mutableStateOf(GalaxDownloadFilter.ALL) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val shown = remember(downloads, filter) { filterDownloadQueue(downloads, filter) }

    if (confirmClear && downloads.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.galax_downloads_clear_confirm)) },
            text = { Text(stringResource(R.string.galax_downloads_clear_detail)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        viewModel.removeAll()
                    },
                ) {
                    Text(stringResource(R.string.galax_downloads_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            },
        )
    }

    GalaxScaffold(
        topBar = {
            GalaxTopAppBar(
                title = { Text(stringResource(R.string.download_queue)) },
                navigationIcon = {
                    IconButton(onClick = navController::popBackStack) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (downloads.isNotEmpty()) {
                        IconButton(
                            onClick = { if (paused) viewModel.resumeAll() else viewModel.pauseAll() },
                        ) {
                            Icon(
                                painter = painterResource(if (paused) R.drawable.play else R.drawable.pause),
                                contentDescription = stringResource(
                                    if (paused) R.string.galax_downloads_resume
                                    else R.string.galax_downloads_pause,
                                ),
                            )
                        }
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = stringResource(R.string.galax_downloads_clear),
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        if (downloads.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                EmptyPlaceholder(
                    icon = R.drawable.downloading,
                    text = stringResource(R.string.no_active_downloads),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                item("galax_download_overview", contentType = "overview") {
                    GalaxDownloadOverview(summary = summary, isPaused = paused)
                }
                item("galax_download_filters", contentType = "filters") {
                    GalaxDownloadFilters(
                        selected = filter,
                        onSelected = { filter = it },
                        summary = summary,
                    )
                }
                if (shown.isEmpty()) {
                    item("galax_download_no_match") {
                        Box(
                            Modifier.fillMaxWidth().padding(top = 52.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(R.string.galax_downloads_filter_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                items(
                    items = shown,
                    key = { it.download.request.id },
                    contentType = { "download" },
                ) { item ->
                    DownloadQueueItem(
                        item = item,
                        onRemove = { viewModel.removeDownload(item.download.request.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadQueueItem(
    item: DownloadItem,
    onRemove: () -> Unit,
) {
    val progress = item.download.percentDownloaded
        .takeIf { it in 0f..100f }?.div(100f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress ?: 0f,
        label = "downloadProgress",
    )
    val status = when (item.download.state) {
        Download.STATE_QUEUED -> R.string.queued
        Download.STATE_DOWNLOADING -> R.string.downloading
        Download.STATE_RESTARTING -> R.string.restarting
        Download.STATE_FAILED -> R.string.failed
        Download.STATE_STOPPED -> R.string.paused
        else -> null
    }
    val scheme = MaterialTheme.colorScheme
    MenuSurfaceSection(Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            com.arturo254.opentune.core.designsystem.component.Artwork(
                item.song?.song?.thumbnailUrl,
                Modifier.size(60.dp),
                shape = RoundedCornerShape(16.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    status?.let {
                        Text(
                            stringResource(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.download.state == Download.STATE_FAILED) scheme.error
                                else scheme.onSurfaceVariant,
                        )
                    }
                    progress?.let {
                        Text(
                            "${(it * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.primary,
                        )
                    }
                }
                if (
                    item.download.state == Download.STATE_DOWNLOADING ||
                    item.download.state == Download.STATE_STOPPED ||
                    item.download.state == Download.STATE_RESTARTING
                ) {
                    if (progress == null) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = scheme.primary,
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = scheme.primary,
                            trackColor = scheme.onSurface.copy(alpha = .10f),
                        )
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.remove),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
