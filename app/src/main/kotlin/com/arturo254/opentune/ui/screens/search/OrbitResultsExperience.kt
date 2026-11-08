package com.arturo254.opentune.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import com.arturo254.opentune.ui.mochy.component.RoundedHighlight
import com.arturo254.opentune.ui.mochy.search.SearchArtwork
import com.arturo254.opentune.ui.mochy.search.SearchCategoryRow
import com.arturo254.opentune.ui.mochy.search.SearchColors
import com.arturo254.opentune.ui.mochy.search.SearchHorizontalPadding
import com.arturo254.opentune.ui.mochy.search.SearchRule
import com.arturo254.opentune.ui.mochy.search.SearchSectionHeader

@Composable
internal fun OrbitResultsHero(
    title: String,
    eyebrow: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(SearchColors.page(true))
            .padding(top = 20.dp, bottom = 3.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 13.dp)) {
                Text("‹  " + stringResource(R.string.close), color = SearchColors.Primary)
            }
        }
        Text(
            text = eyebrow.uppercase(),
            modifier = Modifier.padding(horizontal = SearchHorizontalPadding),
            color = SearchColors.Secondary,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 2.sp,
        )
        SearchSectionHeader(title = title, ruleAbove = false, large = true)
        Text(
            text = subtitle,
            modifier = Modifier.padding(horizontal = SearchHorizontalPadding),
            color = SearchColors.Secondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
        )
        Spacer(Modifier.size(5.dp))
    }
}

@Composable
internal fun OrbitResultsCategories(
    selected: SearchResultCategory,
    onSelect: (SearchResultCategory) -> Unit,
) {
    val categories = SearchResultCategory.entries
    val labels = categories.map { category ->
        stringResource(
            when (category) {
                SearchResultCategory.ALL -> R.string.prism_results_all
                SearchResultCategory.SONGS -> R.string.prism_results_songs
                SearchResultCategory.PLAYLISTS -> R.string.prism_results_playlists
                SearchResultCategory.ALBUMS -> R.string.prism_results_albums
            }
        )
    }
    SearchCategoryRow(
        categories = labels,
        selectedIndex = categories.indexOf(selected).coerceAtLeast(0),
        onSelect = { index -> categories.getOrNull(index)?.let(onSelect) },
    )
}

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
    val thumbnail = preferred?.artworkUrl
        ?: result.recommendedVariant?.artworkUrl
        ?: result.variants.firstNotNullOfOrNull { it.artworkUrl }
    val indication = remember { RoundedHighlight(cornerRadius = 100.dp, spillX = (-10).dp) }
    val interaction = remember { MutableInteractionSource() }
    val subtitle = result.identity.artists.joinToString { it.name }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable(
                    enabled = onPlay != null,
                    interactionSource = interaction,
                    indication = indication,
                    onClick = { onPlay?.invoke() },
                )
                .padding(horizontal = SearchHorizontalPadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                SearchArtwork(url = thumbnail, size = 46.dp)
                if (resolving) {
                    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = result.identity.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (active) Color(0xFF7FD1FF) else SearchColors.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = SearchColors.Secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (downloaded) {
                    Text(
                        stringResource(R.string.prism_results_offline),
                        color = Color(0xFF7FD1FF),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            if (resolving) {
                CircularProgressIndicator(
                    Modifier.size(21.dp),
                    strokeWidth = 1.8.dp,
                    color = SearchColors.Primary,
                )
            } else if (onPlay != null) {
                IconButton(onClick = onPlay, modifier = Modifier.size(35.dp)) {
                    Icon(
                        painter = painterResource(
                            if (active && playing) R.drawable.pause else R.drawable.play
                        ),
                        contentDescription = stringResource(
                            if (active && playing) R.string.prism_results_pause else R.string.prism_results_play
                        ),
                        tint = SearchColors.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (downloading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(7.dp).size(18.dp),
                    color = SearchColors.Secondary,
                    strokeWidth = 2.dp,
                )
            } else if (onDownload != null) {
                IconButton(onClick = onDownload, modifier = Modifier.size(34.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = stringResource(R.string.prism_results_download, result.identity.title),
                        tint = SearchColors.Secondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        SearchRule()
    }
}
