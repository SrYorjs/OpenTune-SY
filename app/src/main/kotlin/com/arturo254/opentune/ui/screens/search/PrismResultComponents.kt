package com.arturo254.opentune.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidEffect
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult

@Composable
internal fun PrismResultCategories(
    selected: SearchResultCategory,
    onSelect: (SearchResultCategory) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(SearchResultCategory.entries, key = { it.name }) { category ->
            val title =
                stringResource(
                    when (category) {
                        SearchResultCategory.ALL -> R.string.prism_results_all
                        SearchResultCategory.SONGS -> R.string.prism_results_songs
                        SearchResultCategory.ALBUMS -> R.string.prism_results_albums
                        SearchResultCategory.PLAYLISTS -> R.string.prism_results_playlists
                    }
                )
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(title, modifier = Modifier.padding(vertical = 4.dp)) },
                shape = RoundedCornerShape(50),
                leadingIcon =
                    if (category == selected)
                        ({ Icon(painterResource(R.drawable.check), null, Modifier.size(16.dp)) })
                    else null,
            )
        }
    }
}

@Composable
internal fun PrismResultLoading(label: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun PrismSourceNotice(
    sources: List<Pair<String, String>>,
    expanded: Boolean,
    onExpand: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        Modifier.padding(horizontal = 24.dp)
            .fillMaxWidth()
            .galaxLiquidEffect(shapeOverride = RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onExpand, contentPadding = PaddingValues(0.dp)) {
            Text(
                stringResource(R.string.prism_results_sources),
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                painterResource(if (expanded) R.drawable.expand_less else R.drawable.expand_more),
                null,
            )
        }
        Text(
            stringResource(R.string.prism_results_sources_detail, sources.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (expanded) {
            sources.forEach { (name, detail) ->
                Text(name, style = MaterialTheme.typography.labelLarge)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onRetry) { Text(stringResource(R.string.prism_results_retry)) }
        }
    }
}

@Composable
internal fun PrismCollectionHeader(
    title: String,
    subtitle: String?,
    artwork: String?,
    description: String? = null,
    onPlay: (() -> Unit)? = null,
) {
    BoxWithConstraints(Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
        val compact = maxWidth / LocalDensity.current.fontScale < 420.dp
        val picture: @Composable () -> Unit = {
            PrismArtwork(
                artwork,
                Modifier.size(if (compact) 84.dp else 128.dp).clip(RoundedCornerShape(22.dp)),
            )
        }
        val copy: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (!subtitle.isNullOrBlank())
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                if (!description.isNullOrBlank())
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                if (onPlay != null)
                    FilledTonalButton(onClick = onPlay, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(painterResource(R.drawable.play), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.prism_results_play_all))
                    }
            }
        }
        if (compact)
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                picture()
                copy()
            }
        else
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                picture()
                Box(Modifier.weight(1f)) { copy() }
            }
    }
}

/** Music and actions have separate touch targets; labels grow with accessibility text. */
@Composable
internal fun PrismRecordingRow(
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
    val display = preferred ?: result.recommendedVariant
    val artwork = display?.artworkUrl ?: result.variants.firstNotNullOfOrNull { it.artworkUrl }
    val providers =
        (result.variants + listOfNotNull(preferred))
            .map { it.provider }
            .distinct()
            .joinToString(" · ", transform = ::variantProviderName)
    val quality = display?.let(::variantQualityDescription)
    BoxWithConstraints(modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
        val wide = maxWidth / LocalDensity.current.fontScale >= 600.dp
        val stackActions = maxWidth / LocalDensity.current.fontScale < 340.dp
        val actionContent: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onVersions, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.prism_results_versions))
                }
                if (downloading)
                    CircularProgressIndicator(
                        Modifier.padding(14.dp).size(20.dp),
                        strokeWidth = 2.dp,
                    )
                else if (onDownload != null)
                    IconButton(onClick = onDownload) {
                        Icon(
                            painterResource(R.drawable.download),
                            stringResource(R.string.prism_results_download, result.identity.title),
                            Modifier.size(20.dp),
                        )
                    }
            }
        }
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    if (active) colors.primaryContainer.copy(alpha = .58f)
                    else colors.surfaceContainer.copy(alpha = .42f)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    Modifier.weight(1f)
                        .clickable(enabled = onPlay != null, role = Role.Button) {
                            onPlay?.invoke()
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))) {
                        PrismArtwork(artwork, Modifier.fillMaxSize())
                        if (resolving)
                            CircularProgressIndicator(
                                Modifier.align(Alignment.Center).size(25.dp),
                                strokeWidth = 2.dp,
                            )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            result.identity.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) colors.primary else colors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            result.identity.artists.joinToString { it.name },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (quality != null)
                            Text(
                                quality,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                            )
                    }
                }
                if (!resolving && onPlay != null)
                    IconButton(onClick = onPlay) {
                        Icon(
                            painterResource(
                                if (active && playing) R.drawable.pause else R.drawable.play
                            ),
                            stringResource(
                                if (active && playing) R.string.prism_results_pause
                                else R.string.prism_results_play
                            ),
                            Modifier.size(22.dp),
                            tint = colors.primary,
                        )
                    }
                if (wide) actionContent()
            }
            val details: @Composable () -> Unit = {
                Column(
                    Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        providers,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (downloaded || downloading || display?.isCatalogOnly() == true)
                        Text(
                            stringResource(
                                when {
                                    downloaded -> R.string.prism_results_offline
                                    downloading -> R.string.prism_results_downloading
                                    else -> R.string.prism_results_resolving
                                }
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary,
                        )
                }
            }
            if (stackActions) {
                details()
                actionContent()
            } else
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.weight(1f)) { details() }
                    if (!wide) actionContent()
                }
        }
    }
}
