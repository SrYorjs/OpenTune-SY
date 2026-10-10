package com.arturo254.opentune.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.theme.sizes
import com.arturo254.opentune.core.designsystem.theme.spacing
import com.arturo254.opentune.core.designsystem.theme.typography

/** Stateless, source-neutral song row; queue/playback behavior is supplied by the caller. */
@Composable
fun SongRow(
    title: String,
    artist: String,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    metadata: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val spacing = OpenTuneDesignSystem.spacing
    val sizes = OpenTuneDesignSystem.sizes
    val typography = OpenTuneDesignSystem.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(spacing.mediaRowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            imageUrl = artworkUrl,
            modifier = Modifier.size(sizes.listThumbnailSize),
        )
        Spacer(Modifier.width(spacing.mediaRowArtworkGap))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.mediaRowTextGap),
        ) {
            Text(
                text = title,
                style = typography.songTitle,
                color =
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = typography.songSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!metadata.isNullOrBlank()) {
                Text(
                    text = metadata,
                    style = typography.metadata,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing()
    }
}
