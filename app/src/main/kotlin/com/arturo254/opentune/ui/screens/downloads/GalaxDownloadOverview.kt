package com.arturo254.opentune.ui.screens.downloads

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.offline.Download
import com.arturo254.opentune.R
import com.arturo254.opentune.viewmodels.DownloadItem

enum class GalaxDownloadFilter {
    ALL,
    ACTIVE,
    PAUSED,
    FAILED,
}

data class GalaxDownloadSummary(
    val total: Int,
    val active: Int,
    val paused: Int,
    val failed: Int,
    val completedFraction: Float?,
)

fun summarizeDownloadQueue(items: List<DownloadItem>): GalaxDownloadSummary {
    val active = items.count {
        it.download.state == Download.STATE_DOWNLOADING ||
            it.download.state == Download.STATE_QUEUED ||
            it.download.state == Download.STATE_RESTARTING
    }
    val paused = items.count { it.download.state == Download.STATE_STOPPED }
    val failed = items.count { it.download.state == Download.STATE_FAILED }
    val reported = items.filter { it.download.percentDownloaded in 0f..100f }
    val fraction = reported.takeIf { it.isNotEmpty() }?.let { known ->
        (known.sumOf { it.download.percentDownloaded.toDouble() } / known.size / 100.0)
            .toFloat().coerceIn(0f, 1f)
    }
    return GalaxDownloadSummary(items.size, active, paused, failed, fraction)
}

fun filterDownloadQueue(items: List<DownloadItem>, filter: GalaxDownloadFilter): List<DownloadItem> =
    when (filter) {
        GalaxDownloadFilter.ALL -> items
        GalaxDownloadFilter.ACTIVE -> items.filter {
            it.download.state == Download.STATE_DOWNLOADING ||
                it.download.state == Download.STATE_QUEUED ||
                it.download.state == Download.STATE_RESTARTING
        }
        GalaxDownloadFilter.PAUSED -> items.filter {
            it.download.state == Download.STATE_STOPPED
        }
        GalaxDownloadFilter.FAILED -> items.filter {
            it.download.state == Download.STATE_FAILED
        }
    }

@Composable
fun GalaxDownloadOverview(
    summary: GalaxDownloadSummary,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val primary = colors.primary
    val secondary = colors.tertiary

    Box(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .background(
                Brush.linearGradient(
                    listOf(
                        primary.copy(alpha = .21f),
                        colors.surfaceContainer,
                        secondary.copy(alpha = .10f),
                    ),
                ),
                RoundedCornerShape(30.dp),
            ),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * .91f, size.height * .16f)
            for (index in 0..6) {
                drawCircle(
                    color = primary.copy(alpha = .07f + (index % 2) * .035f),
                    radius = size.width * (.10f + index * .075f),
                    center = center,
                    style = Stroke(width = .85.dp.toPx()),
                )
            }
        }
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.galax_downloads_collection),
                style = MaterialTheme.typography.labelSmall,
                color = primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.3.sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = summary.total.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Black,
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(R.string.galax_downloads_queue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (isPaused)
                            stringResource(R.string.galax_downloads_paused)
                        else stringResource(R.string.galax_downloads_running),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                GalaxDownloadMetric(summary.active.toString(), stringResource(R.string.galax_downloads_active))
                GalaxDownloadMetric(summary.paused.toString(), stringResource(R.string.galax_downloads_paused))
                GalaxDownloadMetric(summary.failed.toString(), stringResource(R.string.galax_downloads_failed))
            }
            summary.completedFraction?.let { fraction ->
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = primary,
                    trackColor = colors.onSurface.copy(alpha = .09f),
                )
                Text(
                    text = stringResource(R.string.galax_downloads_known_progress, (fraction * 100).toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GalaxDownloadMetric(number: String, title: String) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(number, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = scheme.onSurface)
        Text(title, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
    }
}

@Composable
fun GalaxDownloadFilters(
    selected: GalaxDownloadFilter,
    onSelected: (GalaxDownloadFilter) -> Unit,
    summary: GalaxDownloadSummary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 19.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        for (filter in GalaxDownloadFilter.entries) {
            val count = when (filter) {
                GalaxDownloadFilter.ALL -> summary.total
                GalaxDownloadFilter.ACTIVE -> summary.active
                GalaxDownloadFilter.PAUSED -> summary.paused
                GalaxDownloadFilter.FAILED -> summary.failed
            }
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = {
                    Text(
                        text = stringResource(
                            when (filter) {
                                GalaxDownloadFilter.ALL -> R.string.galax_downloads_all
                                GalaxDownloadFilter.ACTIVE -> R.string.galax_downloads_active
                                GalaxDownloadFilter.PAUSED -> R.string.galax_downloads_paused
                                GalaxDownloadFilter.FAILED -> R.string.galax_downloads_failed
                            },
                        ) + " $count",
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}
