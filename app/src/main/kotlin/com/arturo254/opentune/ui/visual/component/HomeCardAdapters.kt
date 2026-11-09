package com.arturo254.opentune.ui.visual.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.arturo254.opentune.ui.component.ItemThumbnail as ExistingItemThumbnail
import com.arturo254.opentune.ui.component.PlayingIndicatorBox as ExistingPlayingIndicatorBox

@Composable
internal fun ItemThumbnail(
    thumbnailUrl: String?,
    isActive: Boolean,
    isPlaying: () -> Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    ExistingItemThumbnail(
        thumbnailUrl = thumbnailUrl,
        isActive = isActive,
        isPlaying = isPlaying(),
        shape = shape,
        modifier = modifier,
    )
}

@Composable
internal fun PlayingIndicatorBox(
    isActive: Boolean,
    playWhenReady: () -> Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    ExistingPlayingIndicatorBox(
        isActive = isActive,
        playWhenReady = playWhenReady(),
        modifier = modifier,
    )
}
