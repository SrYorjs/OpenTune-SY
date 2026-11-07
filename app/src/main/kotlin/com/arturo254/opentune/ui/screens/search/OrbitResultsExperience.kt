package com.arturo254.opentune.ui.screens.search

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult

/**
 * Órbita: resultados editoriales, navegación segmentada y pistas numeradas.
 * Convive con PrismScreenFrame para respetar los fondos GalaxOT existentes.
 */
@Composable
internal fun OrbitResultsHero(
    title: String,
    eyebrow: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 54.dp, bottomStart = 32.dp, bottomEnd = 8.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        colors.primaryContainer.copy(alpha = .64f),
                        colors.surfaceContainerHigh.copy(alpha = .62f),
                        colors.surface.copy(alpha = .45f),
                    )
                )
            )
            .padding(horizontal = 22.dp, vertical = 26.dp)
    ) {
        val compact = maxWidth / LocalDensity.current.fontScale < 350.dp
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBack != null)
                TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                    Text("←", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.close))
                }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(9.dp).clip(RoundedCornerShape(2.dp))
                        .background(colors.primary)
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    eyebrow.uppercase(),
                    color = colors.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        title,
                        style =
                            if (compact) MaterialTheme.typography.headlineMedium
                            else MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurface,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                if (!compact) {
                    Canvas(Modifier.size(78.dp)) {
                        val stroke = 1.6.dp.toPx()
                        for (i in 0..2) {
                            drawCircle(
                                color = colors.primary.copy(alpha = .50f - i * .13f),
                                radius = size.minDimension * (.20f + i * .14f),
                                style = Stroke(stroke),
                            )
                        }
                        drawCircle(colors.primary, radius = 4.dp.toPx())
                    }
                }
            }
            HorizontalDivider(color = colors.primary.copy(alpha = .30f))
        }
    }
}

@Composable
internal fun OrbitResultsCategories(
    selected: SearchResultCategory,
    onSelect: (SearchResultCategory) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(SearchResultCategory.entries, key = { it.name }) { category ->
            val label = stringResource(
                when (category) {
                    SearchResultCategory.ALL -> R.string.prism_results_all
                    SearchResultCategory.SONGS -> R.string.prism_results_songs
                    SearchResultCategory.ALBUMS -> R.string.prism_results_albums
                    SearchResultCategory.PLAYLISTS -> R.string.prism_results_playlists
                }
            )
            val active = selected == category
            Column(
                Modifier.clip(RoundedCornerShape(8.dp))
                    .background(if (active) colors.primary else colors.surfaceContainer.copy(alpha = .36f))
                    .clickable(role = Role.Tab) { onSelect(category) }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    label,
                    color = if (active) colors.onPrimary else colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * Sustituye las tarjetas apiladas por una lista editorial con número, portada
 * y controles independientes. El indicador de descarga no bloquea la navegación.
 */
@Composable
internal fun OrbitRecordingRow(
    index: Int,
    result: UnifiedTrackSearchResult,
    preferred: SourceVariant?,
    active: Boolean,
    playing: Boolean,
    resolving: Boolean,
    downloaded: Boolean,
    downloading: Boolean,
    onPlay: (() -> Unit)?,
    onVersions: () -> Unit,
    onDownload: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val selected = preferred ?: result.recommendedVariant
    val artwork = selected?.artworkUrl ?: result.variants.firstNotNullOfOrNull { it.artworkUrl }
    val quality = selected?.let(::variantQualityDescription)
    val providers = result.variants.map { it.provider }
        .distinct().take(3)
        .joinToString(" · ", transform = ::variantProviderName)
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        val compact = maxWidth / LocalDensity.current.fontScale < 340.dp
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 82.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.width(28.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        (index + 1).toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) colors.primary else colors.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Box(Modifier.size(if (compact) 48.dp else 60.dp)
                    .clip(RoundedCornerShape(7.dp))) {
                    PrismArtwork(artwork, Modifier.matchParentSize())
                    if (resolving)
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center).size(26.dp),
                            strokeWidth = 2.dp,
                        )
                }
                Column(
                    Modifier.weight(1f)
                        .clickable(enabled = onPlay != null, role = Role.Button) {
                            onPlay?.invoke()
                        }
                        .padding(vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        result.identity.title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (active) colors.primary else colors.onSurface,
                    )
                    Text(
                        result.identity.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!compact) {
                        Text(
                            listOfNotNull(
                                providers.takeIf { it.isNotBlank() },
                                quality,
                                if (downloaded) stringResource(R.string.prism_results_offline) else null,
                            ).joinToString("  ·  "),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (!resolving && onPlay != null)
                    IconButton(onClick = onPlay) {
                        Icon(
                            painterResource(if (active && playing) R.drawable.pause else R.drawable.play),
                            stringResource(if (active && playing) R.string.prism_results_pause else R.string.prism_results_play),
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                if (!compact) {
                    OrbitTrackActions(result, downloading, onVersions, onDownload)
                }
            }
            if (compact) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 40.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        listOfNotNull(
                            providers.takeIf { it.isNotBlank() },
                            quality,
                            if (downloaded) stringResource(R.string.prism_results_offline) else null,
                        ).joinToString(" · "),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    OrbitTrackActions(result, downloading, onVersions, onDownload)
                }
            }
            HorizontalDivider(
                thickness = if (active) 1.5.dp else .5.dp,
                color = if (active) colors.primary.copy(alpha = .55f)
                else colors.outlineVariant.copy(alpha = .55f),
            )
        }
    }
}

@Composable
private fun OrbitTrackActions(
    result: UnifiedTrackSearchResult,
    downloading: Boolean,
    onVersions: () -> Unit,
    onDownload: (() -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onVersions) {
            Icon(
                painterResource(R.drawable.more_horiz),
                stringResource(R.string.prism_results_versions),
                modifier = Modifier.size(20.dp),
            )
        }
        if (downloading)
            CircularProgressIndicator(Modifier.padding(12.dp).size(20.dp), strokeWidth = 2.dp)
        else if (onDownload != null)
            IconButton(onClick = onDownload) {
                Icon(
                    painterResource(R.drawable.download),
                    stringResource(R.string.prism_results_download, result.identity.title),
                    modifier = Modifier.size(20.dp),
                )
            }
    }
}
