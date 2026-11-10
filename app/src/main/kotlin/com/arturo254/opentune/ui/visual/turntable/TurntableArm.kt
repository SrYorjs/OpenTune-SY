package com.arturo254.opentune.ui.visual.turntable

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.arturo254.opentune.utils.makeTimeString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val PivotX = 0.895f
private const val PivotY = 0.125f

private val Record = discGeometryFor(PlayerCoverStyle.TURNTABLE)
private val RecordX = Record.centerX
private val RecordY = Record.centerYByWidth ?: Record.centerY
private val RecordRadius = Record.diameter / 2f

private fun rad(degrees: Float) = Math.toRadians(degrees.toDouble()).toFloat()
private fun deg(radians: Float) = Math.toDegrees(radians.toDouble()).toFloat()

private val StylusReach = hypot(ArmLength + ArmStylusAlong * cos(rad(ArmHeadBend)), ArmStylusAlong * sin(rad(ArmHeadBend)))
private val StylusDegrees = deg(atan2(ArmStylusAlong * sin(rad(ArmHeadBend)), ArmLength + ArmStylusAlong * cos(rad(ArmHeadBend))))

private fun tubeDegreesFor(fraction: Float): Float {
    val vx = RecordX - PivotX
    val vy = RecordY - PivotY
    val v = hypot(vx, vy)
    val r = fraction * RecordRadius
    val turn = acos(((StylusReach * StylusReach + v * v - r * r) / (2f * StylusReach * v)).coerceIn(-1f, 1f))
    return deg(atan2(vy, vx)) - deg(turn) - StylusDegrees
}

private val StartDegrees = tubeDegreesFor(0.94f)
private val EndDegrees = tubeDegreesFor(Record.art + 0.075f)

private const val ArcRadius = 1.03f

private const val PeekMs = 3_000L
private const val LabelGapDegrees = 3.5f

@Composable
fun TurntableArm(
    
    active: Boolean,
    interactive: Boolean,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    mediaId: String,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    
    awayProvider: () -> Float = { 0f },
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val angle = remember { Animatable(StartDegrees) }
    val held = remember { Animatable(0f) }
    val dragging = remember { booleanArrayOf(false) }
    val scrubMs = remember { mutableLongStateOf(-1L) }
    val peek = remember { Animatable(0f) }
    val peekGrow = remember { Animatable(0f) }

    LaunchedEffect(active, isPlaying, mediaId) {
        if (!active || !isPlaying) return@LaunchedEffect
        if (peek.value < 0.05f) peekGrow.snapTo(0f)
        launch { peek.animateTo(1f, tween(240)) }
        peekGrow.animateTo(
            1f,
            tween(760, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)),
        )
        delay(PeekMs - 760L)
        peek.animateTo(0f, tween(420))
    }

    LaunchedEffect(active, positionMs, durationMs) {
        if (!active || dragging[0]) return@LaunchedEffect
        val target = angleFor(positionMs, durationMs)
        if (abs(target - angle.value) > 1.5f) {
            angle.animateTo(target, spring(dampingRatio = 0.9f, stiffness = 120f))
        } else if (!angle.isRunning) {
            angle.snapTo(target)
        }
    }

    val measurer = rememberTextMeasurer(cacheSize = 16)
    val labelStyle = remember {
        TextStyle(
            fontFamily = com.arturo254.opentune.ui.visual.theme.InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = Color.White,
        )
    }
    val currentDuration by rememberUpdatedState(durationMs)
    val currentOnSeek by rememberUpdatedState(onSeek)

    val softShadow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val density = LocalDensity.current
    val shadowBlur = remember(density, softShadow) {
        if (softShadow) with(density) { BlurEffect(5.dp.toPx(), 5.dp.toPx(), TileMode.Clamp) } else null
    }

    Box(
        modifier = modifier.then(
            if (!interactive) Modifier else Modifier.pointerInput(Unit) {
                val grabPx = 26.dp.toPx()
                awaitEachGesture {
                    val w = size.width.toFloat()
                    val pivot = Offset(w * PivotX, w * PivotY)
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!onArm(down.position, pivot, angle.value, w, grabPx)) return@awaitEachGesture
                    val duration = currentDuration.takeIf { it > 0L } ?: return@awaitEachGesture
                    down.consume()
                    dragging[0] = true
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch { angle.stop() }
                    scope.launch { held.animateTo(1f, tween(180)) }
                    val grabbed = angle.value - degreesOf(down.position, pivot)
                    var lastTick = -1L
                    scrubMs.longValue = msFor(angle.value, duration)

                    drag(down.id) { change ->
                        change.consume()
                        val next = (degreesOf(change.position, pivot) + grabbed).coerceIn(StartDegrees, EndDegrees)
                        scope.launch { angle.snapTo(next) }
                        val ms = msFor(next, duration)
                        scrubMs.longValue = ms
                        val tick = ms / 10_000L
                        if (tick != lastTick) {
                            if (lastTick >= 0) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastTick = tick
                        }
                    }

                    currentOnSeek(scrubMs.longValue.coerceIn(0L, duration))
                    dragging[0] = false
                    scope.launch {
                        delay(450)
                        held.animateTo(0f, tween(320))
                        if (!dragging[0]) scrubMs.longValue = -1L
                    }
                }
            }
        )
    ) {
        Spacer(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    turnAboutPivot(angle.value)
                    val lift = maxOf(held.value, (awayProvider() * 2.5f).coerceAtMost(0.6f))
                    val shift = size.width * (0.012f + 0.024f * lift)
                    translationX = shift * 0.55f
                    translationY = shift * 1.1f
                    alpha = (if (softShadow) 0.8f else 0.42f) - 0.2f * lift
                    renderEffect = shadowBlur
                }
                .drawBehind { drawTonearm(pivotIn(size), StartDegrees, size.width, silhouette = true) }
        )

        Spacer(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    turnAboutPivot(angle.value)
                    val s = 1f + 0.012f * maxOf(held.value, (awayProvider() * 2.5f).coerceAtMost(0.6f))
                    scaleX = s
                    scaleY = s
                }
                .drawBehind { drawTonearm(pivotIn(size), StartDegrees, size.width, silhouette = false) }
        )

        Spacer(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val lift = maxOf(held.value, peek.value)
                    if (lift > 0.001f) {
                        drawTimeArc(
                            pivot = pivotIn(size),
                            degrees = angle.value,
                            w = size.width,
                            alpha = lift,
                            grow = if (held.value >= peek.value) 1f else peekGrow.value,
                            positionMs = scrubMs.longValue,
                            durationMs = durationMs,
                            measurer = measurer,
                            labelStyle = labelStyle,

                        )
                    }
                }
        )
    }
}

private fun pivotIn(size: Size) = Offset(size.width * PivotX, size.width * PivotY)

@Composable
fun TurntableBase(modifier: Modifier = Modifier) {
    Spacer(modifier.drawBehind { drawArmBase(size.width) })
}

private fun GraphicsLayerScope.turnAboutPivot(degrees: Float) {
    transformOrigin = TransformOrigin(PivotX, if (size.height > 0f) PivotY * size.width / size.height else 0f)
    rotationZ = degrees - StartDegrees
}

private fun angleFor(position: Long, duration: Long): Float {
    if (duration <= 0L) return StartDegrees
    val f = (position.toFloat() / duration).coerceIn(0f, 1f)
    return StartDegrees + (EndDegrees - StartDegrees) * f
}

private fun msFor(degrees: Float, duration: Long): Long {
    val f = ((degrees - StartDegrees) / (EndDegrees - StartDegrees)).coerceIn(0f, 1f)
    return (duration * f).toLong()
}

private fun unit(degrees: Float): Offset {
    val r = rad(degrees)
    return Offset(cos(r), sin(r))
}

private fun degreesOf(p: Offset, pivot: Offset) = deg(atan2(p.y - pivot.y, p.x - pivot.x))

private fun onArm(p: Offset, pivot: Offset, degrees: Float, w: Float, slop: Float): Boolean {
    val u = unit(degrees)
    val joint = pivot + u * (w * ArmLength)
    val head = joint + unit(degrees + ArmHeadBend) * (w * ArmHeadLength * 0.5f)
    if (hypot(p.x - head.x, p.y - head.y) < w * ArmHeadLength * 0.5f + slop * 0.5f) return true
    val rel = p - pivot
    val along = rel.x * u.x + rel.y * u.y
    if (along < w * 0.12f || along > w * ArmLength) return false
    return abs(rel.y * u.x - rel.x * u.y) < slop
}

private fun DrawScope.drawTimeArc(
    pivot: Offset,
    degrees: Float,
    w: Float,
    alpha: Float,
    
    grow: Float = 1f,
    positionMs: Long,
    durationMs: Long,
    measurer: androidx.compose.ui.text.TextMeasurer,
    labelStyle: TextStyle,
    failureProgress: Float = -1f,
    failureLabel: String? = null,
) {
    val radius = w * ArcRadius
    val stroke = 5.dp.toPx()
    val topLeft = pivot - Offset(radius, radius)
    val arcSize = Size(radius * 2f, radius * 2f)
    val start = StartDegrees + StylusDegrees
    val reach = (EndDegrees - StartDegrees) * grow.coerceIn(0f, 1f)
    drawArc(
        color = Color.White.copy(alpha = 0.32f * alpha),
        startAngle = start,
        sweepAngle = reach.coerceAtLeast(0.01f),
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(stroke, cap = StrokeCap.Round),
    )
    drawArc(
        color = Color.White.copy(alpha = alpha),
        startAngle = start,
        sweepAngle = (degrees - StartDegrees).coerceIn(0.01f, reach.coerceAtLeast(0.01f)),
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(stroke, cap = StrokeCap.Round),
    )
    if (failureProgress > 0f) {
        val total = EndDegrees - StartDegrees
        val red = total * failureProgress.coerceAtMost(1f)
        drawArc(
            color = Color(0xFFE87172).copy(alpha = alpha),
            startAngle = start + total - red,
            sweepAngle = red.coerceAtLeast(0.01f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
    if (durationMs <= 0L) return
    val shown = if (positionMs >= 0L) positionMs else msFor(degrees, durationMs)
    if (failureLabel != null) {
        timeLabel(
            failureLabel, pivot, start - LabelGapDegrees, radius, alpha, measurer,
            labelStyle.copy(color = Color(0xFFE87172)),
        )
    } else {
        timeLabel(makeTimeString(shown), pivot, start - LabelGapDegrees, radius, alpha, measurer, labelStyle)
    }
    val endAlpha = alpha * ((grow - 0.7f) / 0.3f).coerceIn(0f, 1f)
    if (endAlpha > 0.001f) {
        timeLabel(makeTimeString(durationMs), pivot, EndDegrees + StylusDegrees + LabelGapDegrees, radius, endAlpha, measurer, labelStyle)
    }
}

private fun DrawScope.timeLabel(
    text: String,
    pivot: Offset,
    degrees: Float,
    radius: Float,
    alpha: Float,
    measurer: androidx.compose.ui.text.TextMeasurer,
    style: TextStyle,
) {
    val layout = measurer.measure(text, style.copy(color = style.color.copy(alpha = alpha)))
    val at = pivot + unit(degrees) * radius
    rotate(degrees - 90f, at) {
        drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
    }
}
