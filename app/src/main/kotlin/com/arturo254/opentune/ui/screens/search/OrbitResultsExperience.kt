package com.arturo254.opentune.ui.screens.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * ÓRBITA / Refined search. The content is the hero, not the glass container.
 * Subtle layered highlights inherit the album-reactive Material scheme and the
 * Prism atmosphere; no new optical renderer, provider badges or version picker.
 */
@Composable
internal fun OrbitResultsHero(
    title: String,
    eyebrow: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("‹", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.close))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(colors.primary))
            Spacer(Modifier.width(9.dp))
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.weight(1f))
            // Small kinetic-inspired motif; deliberately static while scrolling.
            Canvas(Modifier.size(27.dp)) {
                val center = center
                drawCircle(colors.primary.copy(alpha = .32f), style = Stroke(1.dp.toPx()))
                drawCircle(colors.primary.copy(alpha = .57f), radius = size.minDimension * .29f, style = Stroke(1.dp.toPx()))
                drawCircle(colors.primary, radius = 2.5.dp.toPx(), center = center)
            }
        }
        Text(
            title,
            color = colors.onSurface,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            lineHeight = 38.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            subtitle,
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
        )
        Box(
            Modifier.fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(
                    listOf(colors.primary.copy(alpha = .52f), colors.outlineVariant.copy(alpha = .36f), Color.Transparent)
                )
            )
        )
    }
}

@Composable
internal fun OrbitResultsCategories(
    selected: SearchResultCategory,
    onSelect: (SearchResultCategory) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        items(SearchResultCategory.entries, key = { it.name }) { category ->
            val label = stringResource(when (category) {
                SearchResultCategory.ALL -> R.string.prism_results_all
                SearchResultCategory.SONGS -> R.string.prism_results_songs
                SearchResultCategory.PLAYLISTS -> R.string.prism_results_playlists
                SearchResultCategory.ALBUMS -> R.string.prism_results_albums
            })
            val active = selected == category
            val surface by animateColorAsState(
                if (active) colors.primary.copy(alpha = .15f) else colors.surfaceContainer.copy(alpha = .35f),
                animationSpec = spring(dampingRatio = .88f, stiffness = 450f),
                label = "Orbit category surface",
            )
            val textColor by animateColorAsState(
                if (active) colors.primary else colors.onSurfaceVariant,
                label = "Orbit category text",
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(surface)
                    .border(
                        1.dp,
                        if (active) colors.primary.copy(alpha = .38f)
                        else colors.outlineVariant.copy(alpha = .20f),
                        RoundedCornerShape(100.dp),
                    )
                    .clickable(role = Role.Tab) { onSelect(category) }
                    .padding(horizontal = 17.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = textColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

/**
 * A single-line, artwork-first music library row. All actionable controls have
 * dedicated hit targets; the source is resolved silently and automatically.
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
    onDownload: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val displayed = preferred ?: result.recommendedVariant
    val artwork = displayed?.artworkUrl ?: result.variants.firstNotNullOfOrNull { it.artworkUrl }
    val activeTint by animateColorAsState(
        targetValue = if (active) colors.primary.copy(alpha = .12f) else Color.Transparent,
        label = "Orbit active recording",
    )
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = 13.dp)) {
        val compact = maxWidth / LocalDensity.current.fontScale < 350.dp
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(17.dp))
                .background(activeTint)
                .heightIn(min = 73.dp)
                .padding(start = 7.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (!compact) {
                Text(
                    (index + 1).toString().padStart(2, '0'),
                    modifier = Modifier.width(27.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) colors.primary else colors.onSurfaceVariant.copy(alpha = .63f),
                    fontWeight = FontWeight.Medium,
                )
            }
            Box(
                Modifier.size(if (compact) 49.dp else 55.dp)
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                PrismArtwork(artwork, Modifier.matchParentSize())
                if (resolving) {
                    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .30f)))
                    CircularProgressIndicator(
                        Modifier.align(Alignment.Center).size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                }
            }
            Column(
                Modifier.weight(1f).clickable(enabled = onPlay != null, role = Role.Button) {
                    onPlay?.invoke()
                }.padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    result.identity.title,
                    color = if (active) colors.primary else colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    result.identity.artists.joinToString { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (downloaded) {
                    Text(
                        stringResource(R.string.prism_results_offline),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                        maxLines = 1,
                    )
                }
            }
            if (onPlay != null) {
                IconButton(onClick = onPlay, modifier = Modifier.size(39.dp)) {
                    Box(
                        Modifier.size(35.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = if (active) .18f else .09f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(if (active && playing) R.drawable.pause else R.drawable.play),
                            stringResource(if (active && playing) R.string.prism_results_pause else R.string.prism_results_play),
                            tint = colors.primary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
            if (downloading) {
                Box(Modifier.size(37.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
                }
            } else if (onDownload != null) {
                IconButton(onClick = onDownload, modifier = Modifier.size(37.dp)) {
                    Icon(
                        painterResource(R.drawable.download),
                        stringResource(R.string.prism_results_download, result.identity.title),
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 15.dp),
            color = colors.outlineVariant.copy(alpha = if (active) .11f else .25f),
            thickness = .5.dp,
        )
    }
}
