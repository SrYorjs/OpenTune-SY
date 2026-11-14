package com.arturo254.opentune.ui.original.player
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.CubicBezierEasing
internal const val LyricsRecenterDurationMs = 950
internal val LyricsRecenterEasing = CubicBezierEasing(0.5f, 0.45f, 0f, 1f)
internal val PlayerCardCorner = 28.dp
@Immutable
internal class PlayerCardLook(
    val corner: Dp,
    val frontFill: Float,
    val frontBorder: Float,
    val backFill: Float,
    val backBorder: Float,
    val borderWidth: Dp,
)

// the classic's glassy card, the one behind peeking over it
internal val ClassicCardLook = PlayerCardLook(PlayerCardCorner, 0.11f, 0.20f, 0.06f, 0.10f, 1.dp)

// the new flagship's faint 4% fill and 12% outline, layered and sized like the classic's, the card behind as much fainter as the classic's is
internal val FlagshipCardLook = PlayerCardLook(PlayerCardCorner, 0.04f, 0.12f, 0.02f, 0.06f, 1.35.dp)
