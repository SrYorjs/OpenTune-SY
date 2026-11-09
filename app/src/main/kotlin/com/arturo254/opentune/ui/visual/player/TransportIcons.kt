package com.arturo254.opentune.ui.visual.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import com.arturo254.opentune.R

private const val SquashLow = 0.5f

@Immutable
internal class TransportShapes(
    val play: Path,
    val playSize: Size,
    val skip: Path,
    val skipSize: Size,
    
    val skipStep: Float,
    val pairWidth: Float,
    
    val playShift: Float,
)

@Composable
internal fun rememberTransportShapes(small: Boolean = false): TransportShapes {
    val play = ImageVector.vectorResource(if (small) R.drawable.transport_play_small else R.drawable.transport_play)
    val skip = ImageVector.vectorResource(if (small) R.drawable.transport_skip_small else R.drawable.transport_skip)
    return remember(play, skip, small) {
        TransportShapes(
            play = play.toFlatPath(),
            playSize = Size(play.viewportWidth, play.viewportHeight),
            skip = skip.toFlatPath(),
            skipSize = Size(skip.viewportWidth, skip.viewportHeight),
            skipStep = if (small) 45f else 61f,
            pairWidth = if (small) 116.22f else 157.32f,
            playShift = if (small) 9.05f else 12.88f,
        )
    }
}

private fun ImageVector.toFlatPath(): Path {
    val out = Path()
    fun walk(group: VectorGroup, dx: Float, dy: Float) {
        for (i in 0 until group.size) {
            when (val node = group[i]) {
                is VectorGroup -> walk(node, dx + node.translationX, dy + node.translationY)
                is VectorPath -> out.addPath(node.pathData.toPath(), Offset(dx, dy))
            }
        }
    }
    walk(root, 0f, 0f)
    return out
}

@Stable
internal class PlayGlyphState(playing: Boolean) {
    val squash = Animatable(1f)
    var showsPause by mutableStateOf(playing)
        private set

    suspend fun swapTo(playing: Boolean) {
        if (playing == showsPause && squash.value == 1f) return
        squash.animateTo(SquashLow, tween(durationMillis = 110, easing = FastOutLinearInEasing))
        showsPause = playing
        squash.animateTo(1f, spring(dampingRatio = 0.34f, stiffness = 420f))
    }
}

@Composable
internal fun rememberPlayGlyph(playing: Boolean): PlayGlyphState {
    val state = remember { PlayGlyphState(playing) }
    LaunchedEffect(playing) { state.swapTo(playing) }
    return state
}

internal fun DrawScope.drawPlayGlyph(
    shapes: TransportShapes,
    glyph: PlayGlyphState,
    unit: Float,
    color: Color,
) {
    val s = glyph.squash.value
    val cx = size.width / 2f
    val cy = size.height / 2f
    withTransform({ scale(s, s, Offset(cx, cy)) }) {
        if (glyph.showsPause) {
            drawPause(cx, cy, shapes.playSize.height * unit, color)
        } else {
            val w = shapes.playSize.width * unit
            val h = shapes.playSize.height * unit
            withTransform({
                translate(cx + shapes.playShift * unit - w / 2f, cy - h / 2f)
                scale(unit, unit, Offset.Zero)
            }) {
                drawPath(shapes.play, color)
            }
        }
    }
}

private fun DrawScope.drawPause(cx: Float, cy: Float, playHeight: Float, color: Color) {
    val barW = playHeight * 0.29f
    val barH = playHeight * 0.86f
    val gap = playHeight * 0.145f
    val radius = CornerRadius(barW / 2f)
    drawRoundRect(color, Offset(cx - gap / 2f - barW, cy - barH / 2f), Size(barW, barH), radius)
    drawRoundRect(color, Offset(cx + gap / 2f, cy - barH / 2f), Size(barW, barH), radius)
}

@Stable
internal class SkipCycleState {
    val position = Animatable(0f)
    var target by mutableIntStateOf(0)
        private set

    suspend fun step() {
        target += 1
        position.animateTo(target.toFloat(), spring(dampingRatio = 0.6f, stiffness = 320f))
    }

    
    fun phase(): Float {
        val f = position.value - (target - 1)
        return if (f >= 0f) f else position.value - kotlin.math.floor(position.value)
    }
}

internal fun DrawScope.drawSkipGlyph(
    shapes: TransportShapes,
    phase: Float,
    unit: Float,
    color: Color,
    backwards: Boolean,
) {
    val w = shapes.skipSize.width * unit
    val h = shapes.skipSize.height * unit
    val step = shapes.skipStep * unit
    val left = size.width / 2f - shapes.pairWidth * unit / 2f
    val top = size.height / 2f - h / 2f
    val f = phase.coerceAtLeast(0f)
    val gone = f.coerceAtMost(1f)
    withTransform({ if (backwards) scale(-1f, 1f, Offset(size.width / 2f, size.height / 2f)) }) {
        
        skipTriangle(shapes, left, top, w, h, unit, scale = f, alpha = (f * 2.5f).coerceIn(0f, 1f), color = color)
        
        skipTriangle(shapes, left + step * f, top, w, h, unit, scale = 1f, alpha = 1f, color = color)
        
        if (gone < 1f) {
            skipTriangle(shapes, left + step + step * 0.8f * gone, top, w, h, unit, scale = 1f - gone, alpha = 1f - gone, color = color)
        }
    }
}

private fun DrawScope.skipTriangle(
    shapes: TransportShapes,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    unit: Float,
    scale: Float,
    alpha: Float,
    color: Color,
) {
    if (scale <= 0.001f || alpha <= 0.001f) return
    withTransform({
        scale(scale, scale, Offset(x + w / 2f, y + h / 2f))
        translate(x, y)
        scale(unit, unit, Offset.Zero)
    }) {
        drawPath(shapes.skip, color, alpha = alpha)
    }
}

@Composable
internal fun AnimatedPlayGlyph(playing: Boolean, color: Color, height: Dp, modifier: Modifier = Modifier) {
    val shapes = rememberTransportShapes()
    val glyph = rememberPlayGlyph(playing)
    Spacer(
        modifier = modifier
            .size(height)
            .drawBehind { drawPlayGlyph(shapes, glyph, size.height / shapes.playSize.height, color) },
    )
}

@Composable
internal fun AnimatedSkipGlyph(cycle: SkipCycleState, backwards: Boolean, color: Color, width: Dp, modifier: Modifier = Modifier) {
    val shapes = rememberTransportShapes()
    Spacer(
        modifier = modifier
            .size(width, width * (shapes.skipSize.height / shapes.pairWidth))
            .drawBehind { drawSkipGlyph(shapes, cycle.phase(), size.width / shapes.pairWidth, color, backwards) },
    )
}
