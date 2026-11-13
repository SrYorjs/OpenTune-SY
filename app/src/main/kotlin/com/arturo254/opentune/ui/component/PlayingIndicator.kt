package com.arturo254.opentune.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.ui.original.component.PlayingIndicator as OriginalPlayingIndicator
import com.arturo254.opentune.ui.original.component.PlayingIndicatorBox as OriginalPlayingIndicatorBox

@Composable
fun PlayingIndicator(
    color: Color,
    modifier: Modifier = Modifier,
    bars: Int = 5,
    barWidth: Dp = 2.5.dp,
    spacing: Dp = 2.dp,
) = OriginalPlayingIndicator(
    color = color,
    modifier = modifier,
    bars = bars,
    barWidth = barWidth,
    spacing = spacing,
)

@Composable
fun PlayingIndicatorBox(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    playWhenReady: Boolean,
    color: Color = Color.White,
) = OriginalPlayingIndicatorBox(
    modifier = modifier,
    isActive = isActive,
    playWhenReady = playWhenReady,
    color = color,
)
