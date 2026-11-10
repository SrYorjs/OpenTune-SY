package com.arturo254.opentune.ui.visual.player

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Immutable
data class OriginalCoverPlacement(
    val scale: Float,
    val translationX: Dp,
    val translationY: Dp,
)

fun originalCoverPlacement(
    progress: Float,
    fullWidth: Dp,
    fullCenterX: Dp,
    fullCenterY: Dp,
    miniCenterX: Dp,
    miniCenterY: Dp,
    growLate: Boolean,
): OriginalCoverPlacement {
    val p = progress.coerceIn(0f, 1f)
    val grow = if (growLate) smoothstep((p - 0.7f) / 0.3f) else p
    val travel = if (growLate) 1f - (1f - p) * (1f - p) else p
    val width = 48.dp + (fullWidth - 48.dp) * grow
    return OriginalCoverPlacement(
        scale = width.value / fullWidth.value.coerceAtLeast(1f),
        translationX = (miniCenterX - fullCenterX) * (1f - travel),
        translationY = (miniCenterY - fullCenterY) * (1f - travel),
    )
}

private fun smoothstep(value: Float): Float {
    val v = value.coerceIn(0f, 1f)
    return v * v * (3f - 2f * v)
}
