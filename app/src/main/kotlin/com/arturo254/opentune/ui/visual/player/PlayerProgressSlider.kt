package com.arturo254.opentune.ui.visual.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import com.arturo254.opentune.utils.makeTimeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sign

private fun originalRubberBand(over: Float, reach: Float): Float {
    if (over == 0f) return 0f
    val distance = abs(over)
    return sign(over) * (1f - 1f / (distance * 0.55f / reach + 1f)) * reach
}

@Composable
fun OriginalPlayerProgressSlider(
    player: Player,
    mediaId: String?,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    timeLeft: Boolean = false,
    onSeekBarPlaced: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    onEndTimePlaced: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    seekBarModifier: Modifier = Modifier,
    endTimeModifier: Modifier = Modifier,
) {
    val currentPosition by rememberUpdatedState(positionMs)
    val currentDuration by rememberUpdatedState(durationMs)
    val currentPlaying by rememberUpdatedState(isPlaying)
    val durationKnown = durationMs > 0L && durationMs != C.TIME_UNSET
    val rangeEnd = if (durationKnown) durationMs.toFloat() else 0f
    var sliderPosition by remember(mediaId) { mutableStateOf<Long?>(null) }
    var pendingSeek by remember(mediaId) { mutableStateOf<Long?>(null) }
    val sliderState = remember(mediaId, rangeEnd) {
        SliderState(valueRange = 0f..rangeEnd).also {
            it.value = (sliderPosition ?: pendingSeek ?: currentPosition).toFloat().coerceIn(0f, rangeEnd)
        }
    }
    LaunchedEffect(active, currentPosition, rangeEnd, sliderState) {
        if (!active) return@LaunchedEffect
        pendingSeek?.let { if (abs(currentPosition - it) < SeekLandedMs) pendingSeek = null }
        if (sliderPosition == null) sliderState.value =
            (pendingSeek ?: currentPosition).toFloat().coerceIn(0f, rangeEnd)
    }
    LaunchedEffect(pendingSeek) {
        if (pendingSeek != null) {
            delay(SeekGiveUpMs)
            pendingSeek = null
        }
    }
    sliderState.onValueChange = { value ->
        val target = value.toLong().coerceAtLeast(0L)
        sliderPosition = target
        sliderState.value = target.toFloat()
    }
    sliderState.onValueChangeFinished = {
        sliderPosition?.let { target ->
            onSeek(target)
            pendingSeek = target
        }
        sliderPosition = null
        sliderState.value = (pendingSeek ?: currentPosition).toFloat().coerceIn(0f, rangeEnd)
    }

    val loadingTrack = isBuffering && (!durationKnown || positionMs < LoadingStartWindowMs)
    val showBuffering by rememberSettled(isBuffering)
    val showLoading by rememberSettled(loadingTrack)
    val elapsedText = remember(sliderPosition, pendingSeek, positionMs) {
        mutableStateOf(makeTimeString(sliderPosition ?: pendingSeek ?: positionMs))
    }
    val endText = remember(durationMs, positionMs, sliderPosition, pendingSeek, timeLeft, showLoading) {
        mutableStateOf(
            if (!durationKnown || showLoading) "" else
                if (timeLeft) "-" + makeTimeString(
                    (durationMs - (sliderPosition ?: pendingSeek ?: positionMs)).coerceAtLeast(0L)
                )
                else makeTimeString(durationMs)
        )
    }
    val clock = remember(player) { PlaybackClock({ player }) }
    val fill = remember(mediaId) { SeekFill() }
    LaunchedEffect(active, mediaId, fill, clock) {
        if (!active) return@LaunchedEffect
        fill.follow(
            target = {
                val duration = currentDuration
                if (duration <= 0L || duration == C.TIME_UNSET) 0f else
                    ((sliderPosition ?: pendingSeek ?: clock()).toFloat() / duration).coerceIn(0f, 1f)
            },
            moving = { currentPlaying || sliderPosition != null },
        )
    }

    val trackInteraction = remember { MutableInteractionSource() }
    val dragged = trackInteraction.collectIsDraggedAsState()
    val trackDragged by dragged
    val trackPressed by trackInteraction.collectIsPressedAsState()
    val stretch = remember { mutableFloatStateOf(0f) }
    val stretchScope = rememberCoroutineScope()
    val stretchSettle = remember { arrayOfNulls<Job>(1) }
    val held = trackDragged || trackPressed
    val expand = animateFloatAsState(
        targetValue = if (held) 1f else 0f,
        animationSpec = SeekBarExpandSpring,
        label = "playerSeekExpand",
    )
    val timeColor = remember(expand) {
        ColorProducer {
            Color.White.copy(alpha = TimeRestAlpha + (1f - TimeRestAlpha) * expand.value)
        }
    }
    Row(modifier = modifier.fillMaxWidth().height(28.dp)) {
        Slider(
            state = sliderState,
            interactionSource = trackInteraction,
            thumb = { Spacer(Modifier.size(0.dp)) },
            track = {
                SeekBarTrack(
                    style = SeekBarStyle.DEFAULT,
                    fractionProvider = { fill.fraction },
                    activeColor = Color.White,
                    inactiveColor = Color.White.copy(alpha = 0.24f),
                    animateWave = isPlaying,
                    active = held,
                    loading = showBuffering,
                    clockRunning = active,
                    expandProvider = { expand.value },
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(horizontal = 36.dp)
                .onSizeChanged { fill.widthPx = it.width.toFloat() }
                .then(onSeekBarPlaced?.let { Modifier.onGloballyPositioned(it) } ?: Modifier)
                .seekStretch(stretch, dragged, stretchSettle, stretchScope)
                .then(seekBarModifier),
        )
    }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
    ) {
        val style = MaterialTheme.typography.labelMedium
        val rest = Color.White.copy(alpha = TimeRestAlpha)
        AnimatedContent(
            targetState = if (showLoading) LoadingKey else null,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "seekTimeLoading",
            modifier = Modifier.timeLift(expand, pivotX = 0f) { minOf(stretch.floatValue, 0f) },
        ) { state ->
            if (state == LoadingKey) LoadingLabel(style = style, color = rest)
            else TimeText(text = elapsedText, style = style, color = timeColor)
        }
        TimeText(
            text = endText,
            style = style,
            color = timeColor,
            modifier = (onEndTimePlaced?.let { Modifier.onGloballyPositioned(it) } ?: Modifier)
                .then(endTimeModifier)
                .timeLift(expand, pivotX = 1f) { maxOf(stretch.floatValue, 0f) },
        )
    }
}

@Composable
private fun TimeText(text: State<String>, style: TextStyle, color: ColorProducer, modifier: Modifier = Modifier) {
    BasicText(
        text = text.value,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

private fun Modifier.timeLift(expand: State<Float>, pivotX: Float, stretched: () -> Float): Modifier = graphicsLayer {
    val open = expand.value
    translationX = stretched()
    translationY = TimeDrop.toPx() * open
    val s = 1f + TimeGrow * open
    scaleX = s
    scaleY = s
    transformOrigin = TransformOrigin(pivotX, 0.5f)
}

private val SeekStretchMax = 26.dp
private val SeekStretchBack = spring<Float>(dampingRatio = 0.45f, stiffness = 420f)

private fun Modifier.seekStretch(
    stretch: MutableFloatState,
    dragged: State<Boolean>,
    settle: Array<Job?>,
    scope: CoroutineScope,
): Modifier = this
    .pointerInput(Unit) {
        val reach = SeekStretchMax.toPx()
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            settle[0]?.cancel()
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull() ?: break
                if (!change.pressed) break
                val x = change.position.x
                val over = when {
                    !dragged.value -> 0f
                    x > size.width -> x - size.width
                    x < 0f -> x
                    else -> 0f
                }
                stretch.floatValue = originalRubberBand(over, reach)
            }
            val from = stretch.floatValue
            if (from != 0f) {
                settle[0] = scope.launch {
                    animate(from, 0f, animationSpec = SeekStretchBack) { value, _ -> stretch.floatValue = value }
                }
            }
        }
    }
    .graphicsLayer {
        val s = stretch.floatValue
        if (s == 0f || size.width <= 0f) return@graphicsLayer
        val pull = (abs(s) / SeekStretchMax.toPx()).coerceIn(0f, 1f)
        scaleX = (size.width + abs(s)) / size.width
        scaleY = 1f - 0.18f * pull
        transformOrigin = TransformOrigin(if (s > 0f) 0f else 1f, 0.5f)
    }

private val TimeDrop = 4.dp
private const val TimeGrow = 0.1f
private const val TimeRestAlpha = 0.85f

private const val SeekLandedMs = 1_500L

private const val SeekGiveUpMs = 1_500L

private const val FillOmega = 50f

@Stable
private class SeekFill {
    var fraction by mutableFloatStateOf(0f)
        private set

    var widthPx = 0f

    private var shown = 0f
    private var velocity = 0f

    suspend fun follow(target: () -> Float, moving: () -> Boolean) {
        shown = target()
        velocity = 0f
        fraction = shown
        while (true) {
            snapshotFlow { moving() || abs(target() - shown) > SettledFraction }.first { it }
            var lastNanos = 0L
            while (true) {
                val settled = withFrameNanos { now ->
                    val dt = if (lastNanos == 0L) 1f / 120f else ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    lastNanos = now
                    val to = target()
                    step(to, dt)
                    !moving() && abs(to - shown) < SettledFraction && abs(velocity) < SettledSpeed
                }
                if (settled) {
                    shown = target()
                    velocity = 0f
                    fraction = shown
                    break
                }
            }
        }
    }

    private fun step(to: Float, dt: Float) {
        val y = shown - to
        val e = exp(-FillOmega * dt)
        val tmp = (velocity + FillOmega * y) * dt
        shown = to + (y + tmp) * e
        velocity = (velocity - FillOmega * tmp) * e
        val minStep = if (widthPx > 0f) 0.25f / widthPx else 0f
        if (abs(shown - fraction) >= minStep) fraction = shown
    }
}

private const val SettledFraction = 0.0002f
private const val SettledSpeed = 0.002f

private const val LoadingKey = "\u0000loading"
private const val FailureKey = "\u0000failure"

private const val LoadingStartWindowMs = 1_500L

private const val LoadingShowDelayMs = 200L

@Composable
private fun rememberSettled(value: Boolean): State<Boolean> = produceState(initialValue = false, value) {
    if (value) {
        delay(LoadingShowDelayMs)
        this.value = true
    } else {
        this.value = false
    }
}

@Composable
private fun LoadingLabel(style: TextStyle, color: Color) {
    val transition = rememberInfiniteTransition(label = "loadingLabel")
    Row(verticalAlignment = Alignment.Bottom) {
        Text(text = "Loading", style = style, color = color, maxLines = 1)
        repeat(3) { index ->
            val lit by transition.animateFloat(
                initialValue = 0.15f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 520),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 170),
                ),
                label = "loadingDot$index",
            )
            Text(
                text = ".",
                style = style,
                color = color,
                maxLines = 1,
                modifier = Modifier.graphicsLayer { alpha = lit },
            )
        }
    }
}
