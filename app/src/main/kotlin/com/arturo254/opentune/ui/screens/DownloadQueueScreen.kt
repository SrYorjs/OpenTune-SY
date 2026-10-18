package com.arturo254.opentune.ui.screens

import com.arturo254.opentune.ui.screens.GalaxScaffold as Scaffold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.arturo254.opentune.ui.screens.GalaxTopAppBar as TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.component.EmptyPlaceholder
import com.arturo254.opentune.viewmodels.DownloadItem
import com.arturo254.opentune.viewmodels.DownloadQueueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQueueScreen(
    navController: NavController,
    viewModel: DownloadQueueViewModel = hiltViewModel(),
) {
    val downloads by viewModel.downloads.collectAsState()
    val isPaused by viewModel.downloadsPaused.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.download_queue)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    if (downloads.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                if (isPaused) viewModel.resumeAll() else viewModel.pauseAll()
                            }
                        ) {
                            Icon(
                                painter = painterResource(if (isPaused) R.drawable.play else R.drawable.pause),
                                contentDescription = null
                            )
                        }
                        IconButton(onClick = { viewModel.removeAll() }) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyPlaceholder(
                    icon = R.drawable.downloading,
                    text = stringResource(R.string.no_active_downloads)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item("download_summary", contentType = "summary") {
                    com.arturo254.opentune.core.designsystem.theme.GalaxScreenHeading(
                        stringResource(R.string.download_queue), "${downloads.size} ${stringResource(R.string.downloading)}")
                }
                items(downloads, key = { it.download.request.id }, contentType = { "download" }) { item ->
                    DownloadQueueItem(
                        item = item,
                        onRemove = { viewModel.removeDownload(item.download.request.id) }
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
    val progress = item.download.percentDownloaded.takeIf { it >= 0f }?.div(100f)?.coerceIn(0f, 1f)
    val animatedProgress = animateFloatAsState(targetValue = progress ?: 0f, label = "downloadProgress")
    val status = when (item.download.state) {
        Download.STATE_QUEUED -> R.string.queued
        Download.STATE_DOWNLOADING -> R.string.downloading
        Download.STATE_RESTARTING -> R.string.restarting
        Download.STATE_FAILED -> R.string.failed
        Download.STATE_STOPPED -> R.string.paused
        else -> null
    }
    com.arturo254.opentune.ui.component.MenuSurfaceSection(Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            com.arturo254.opentune.core.designsystem.component.Artwork(item.song?.song?.thumbnailUrl,
                Modifier.size(56.dp), shape = com.arturo254.opentune.core.designsystem.theme.GalaxScreenStyle.artworkShape)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    status?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    progress?.let { Text("${(it * 100).toInt()}%", style = MaterialTheme.typography.labelSmall) }
                }
                if (item.download.state == Download.STATE_DOWNLOADING || item.download.state == Download.STATE_STOPPED) {
                    if (progress == null) LinearProgressIndicator(Modifier.fillMaxWidth().height(3.dp))
                    else LinearProgressIndicator(progress = { animatedProgress.value }, modifier = Modifier.fillMaxWidth().height(3.dp))
                }
            }
            IconButton(onClick = onRemove) {
                Icon(painterResource(R.drawable.close), stringResource(R.string.remove), Modifier.size(20.dp))
            }
        }
    }
}
