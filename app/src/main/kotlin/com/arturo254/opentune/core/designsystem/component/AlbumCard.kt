package com.arturo254.opentune.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.theme.shapes
import com.arturo254.opentune.core.designsystem.theme.spacing
import com.arturo254.opentune.core.designsystem.theme.typography

/** Album grid tile without assumptions about streaming providers or storage. */
@Composable
fun AlbumCard(
    title: String,
    artist: String,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val typography = OpenTuneDesignSystem.typography
    val spacing = OpenTuneDesignSystem.spacing
    val shapes = OpenTuneDesignSystem.shapes
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Artwork(
            imageUrl = artworkUrl,
            shape = shapes.gridArtwork,
            placeholderIcon = R.drawable.album,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Spacer(Modifier.height(spacing.sm))
        Text(
            text = title,
            style = typography.songTitle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = artist,
            style = typography.songSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
