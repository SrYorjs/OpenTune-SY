package com.arturo254.opentune.ui.visual.studio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.media3.common.Player
import com.arturo254.opentune.ui.player.actions.PlayerActions
import com.arturo254.opentune.ui.player.state.PlayerState
import com.arturo254.opentune.ui.visual.player.AnimatedPlayGlyph
import com.arturo254.opentune.ui.visual.player.AnimatedSkipGlyph
import com.arturo254.opentune.ui.visual.player.SkipCycleState
import kotlinx.coroutines.launch

@Immutable
data class OriginalStyleTones(
    val played: Color,
    val remaining: Color,
    val ink: Color = Color(0xFFF1ECEC),
    val inkSoft: Color = Color(0xFFF1ECEC).copy(alpha = 0.62f),
    val chip: Color = Color.White.copy(alpha = 0.22f),
)

@Composable
fun rememberOriginalStyleTones(seed: Color): OriginalStyleTones = remember(seed) {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)
    val saturation = (hsl[1] * 0.42f).coerceIn(0.06f, 0.24f)
    fun tone(lightness: Float) =
        Color(ColorUtils.HSLToColor(floatArrayOf(hsl[0], saturation, lightness)))
    OriginalStyleTones(played = tone(0.45f), remaining = tone(0.30f))
}

fun Modifier.originalShadedTone(color: Color): Modifier = drawBehind {
    drawRect(color)
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
            center = Offset(size.width * 0.15f, size.height * 0.05f),
            radius = size.maxDimension * 0.9f,
        )
    )
    drawRect(
        Brush.verticalGradient(
            0.55f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.16f),
        )
    )
}

fun Modifier.originalProgressSplit(
    tones: OriginalStyleTones,
    vertical: Boolean,
    boundary: () -> Float,
): Modifier = drawBehind {
    drawOriginalSplit(tones, vertical, boundary())
}

private fun DrawScope.drawOriginalSplit(
    tones: OriginalStyleTones,
    vertical: Boolean,
    boundary: Float,
) {
    val b = boundary.coerceIn(0f, 1f)
    if (vertical) {
        val y = size.height * b
        drawRect(tones.played, size = size.copy(height = y))
        drawRect(tones.remaining, topLeft = Offset(0f, y), size = size.copy(height = size.height - y))
        drawRect(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                endY = y.coerceAtLeast(1f),
            ),
            size = size.copy(height = y),
        )
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent),
                startY = y,
                endY = y + size.height * 0.18f,
            ),
            topLeft = Offset(0f, y),
            size = size.copy(height = size.height * 0.18f),
        )
        drawRect(
            Color.White.copy(alpha = 0.10f),
            topLeft = Offset(0f, y - 1f),
            size = size.copy(height = 1.5f),
        )
    } else {
        val x = size.width * b
        drawRect(tones.played, size = size.copy(width = x))
        drawRect(tones.remaining, topLeft = Offset(x, 0f), size = size.copy(width = size.width - x))
        drawRect(
            Brush.horizontalGradient(
                listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                endX = x.coerceAtLeast(1f),
            ),
            size = size.copy(width = x),
        )
        drawRect(
            Brush.horizontalGradient(
                listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent),
                startX = x,
                endX = x + size.width * 0.14f,
            ),
            topLeft = Offset(x, 0f),
            size = size.copy(width = size.width * 0.14f),
        )
        drawRect(
            Color.White.copy(alpha = 0.10f),
            topLeft = Offset(x - 1f, 0f),
            size = size.copy(width = 1.5f),
        )
    }
}

@Composable
private fun OriginalStyleCircle(
    size: Dp,
    tones: OriginalStyleTones,
    enabled: Boolean,
    onClick: () -> Unit,
    glyph: @Composable (Color) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = if (pressed) 0.6f else 0.45f,
            stiffness = if (pressed) 900f else 500f,
        ),
        label = "originalStylePress",
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = press.value
                scaleY = press.value
                alpha = if (enabled) 1f else 0.4f
            }
            .size(size)
            .background(tones.chip, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        glyph(tones.ink)
    }
}

@Composable
fun OriginalStyleTransportCircles(
    state: PlayerState,
    actions: PlayerActions,
    tones: OriginalStyleTones,
    vertical: Boolean = false,
    size: Dp = 30.dp,
    spacing: Dp = 8.dp,
) {
    val scope = rememberCoroutineScope()
    val previous = remember { SkipCycleState() }
    val next = remember { SkipCycleState() }
    val buttons: @Composable () -> Unit = {
        OriginalStyleCircle(
            size = size,
            tones = tones,
            enabled = state.canSkipPrevious,
            onClick = {
                scope.launch { previous.step() }
                actions.previous()
            },
        ) { tint ->
            AnimatedSkipGlyph(cycle = previous, backwards = true, color = tint, width = size * 0.46f)
        }
        OriginalStyleCircle(
            size = size,
            tones = tones,
            enabled = true,
            onClick = actions::playPause,
        ) { tint ->
            AnimatedPlayGlyph(playing = state.isPlaying, color = tint, height = size * 0.52f)
        }
        OriginalStyleCircle(
            size = size,
            tones = tones,
            enabled = state.canSkipNext,
            onClick = {
                scope.launch { next.step() }
                actions.next()
            },
        ) { tint ->
            AnimatedSkipGlyph(cycle = next, backwards = false, color = tint, width = size * 0.46f)
        }
    }
    if (vertical) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) { buttons() }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) { buttons() }
    }
}
