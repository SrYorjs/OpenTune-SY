package com.arturo254.opentune.ui.player.GalaxStudioPlayer

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Immutable
data class StudioSceneState(
    val isLandscape: Boolean,
    val isPlaying: Boolean,
    val progress: Float,
    val primary: Color,
    val secondary: Color,
    val queuePeekHeight: Dp,
)
