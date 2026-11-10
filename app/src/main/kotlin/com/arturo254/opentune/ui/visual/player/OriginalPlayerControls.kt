package com.arturo254.opentune.ui.visual.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import com.arturo254.opentune.R
import kotlinx.coroutines.launch

private class OriginalHeldSpeed(private val player: Player) {
    private var saved: PlaybackParameters? = null
    fun start(factor: Float) {
        val original = saved ?: player.playbackParameters.also { saved = it }
        player.playbackParameters = PlaybackParameters(
            (original.speed * factor).coerceIn(0.25f, 4f),
            original.pitch,
        )
    }
    fun end() {
        val original = saved ?: return
        saved = null
        player.playbackParameters = original
    }
}

@Composable
fun OriginalPlayerTransportRow(
    player: Player,
    isPlaying: Boolean,
    playbackState: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    flagshipGlyph: Boolean = true,
) {
    val hold = remember(player) { OriginalHeldSpeed(player) }
    DisposableEffect(hold) { onDispose { hold.end() } }
    BoxWithConstraints(modifier = modifier.fillMaxWidth().padding(horizontal = 36.dp)) {
        val buttonSize = (maxWidth * 0.21f).coerceIn(64.dp, 96.dp)
        val iconSize = buttonSize * 0.73f
        val gap = (maxWidth * 0.09f).coerceIn(22.dp, 40.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedPressScaleSkipButton(
                icon = R.drawable.avd_skip_previous,
                tint = tint,
                onClick = onPrevious,
                modifier = Modifier.size(buttonSize),
                iconSize = iconSize,
                enabled = canSkipPrevious,
                onHoldStart = { hold.start(0.5f) },
                onHoldEnd = hold::end,
                holdLabel = "0.5×",
                holdPulseMs = 900L,
                flagshipGlyph = flagshipGlyph,
            )
            AnimatedPressScalePlayPauseButton(
                isPlaying = isPlaying,
                playbackState = playbackState,
                tint = tint,
                onClick = onPlayPause,
                modifier = Modifier.size(buttonSize),
                iconSize = iconSize,
                flagshipGlyph = flagshipGlyph,
            )
            AnimatedPressScaleSkipButton(
                icon = R.drawable.avd_skip_next,
                tint = tint,
                onClick = onNext,
                modifier = Modifier.size(buttonSize),
                iconSize = iconSize,
                enabled = canSkipNext,
                onHoldStart = { hold.start(2f) },
                onHoldEnd = hold::end,
                holdLabel = "2×",
                holdPulseMs = 420L,
                flagshipGlyph = flagshipGlyph,
            )
        }
    }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
internal fun AnimatedPressScaleSkipButton(
    icon: Int,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp,
    containerColor: Color = Color.Transparent,
    enabled: Boolean = true,
    
    onHoldStart: (() -> Unit)? = null,
    onHoldEnd: (() -> Unit)? = null,
    
    holdLabel: String? = null,
    
    holdPulseMs: Long = 600L,
    flagshipGlyph: Boolean = false,
) {
    var trigger by remember { mutableIntStateOf(0) }
    val cycle = remember { SkipCycleState() }
    val scope = rememberCoroutineScope()
    var holding by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.54f, stiffness = 720f),
        label = "pressScaleIconButton"
    )
    LaunchedEffect(holding) {
        while (holding) {
            trigger++
            if (flagshipGlyph) launch { cycle.step() }
            kotlinx.coroutines.delay(holdPulseMs)
        }
    }
    val badge by animateFloatAsState(
        targetValue = if (holding) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 520f),
        label = "holdBadge",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.36f
            }

            .then(
                if (containerColor == Color.Transparent) {
                    Modifier
                } else {
                    Modifier.clip(RoundedCornerShape(50)).background(containerColor)
                }
            )
            .tapOrHold(
                enabled = enabled,
                onTap = {
                    trigger++
                    if (flagshipGlyph) scope.launch { cycle.step() }
                    onClick()
                },
                onHoldStart = onHoldStart?.let { start ->
                    {
                        holding = true
                        start()
                    }
                },
                onHoldEnd = {
                    holding = false
                    onHoldEnd?.invoke()
                },
                interactionSource = interactionSource,
            )
    ) {
        if (holdLabel != null && badge > 0.01f) {
            androidx.compose.material3.Text(
                text = holdLabel,
                color = Color.Black,
                fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        alpha = badge.coerceIn(0f, 1f)
                        val s = 0.6f + 0.4f * badge
                        scaleX = s
                        scaleY = s
                        translationY = -(10.dp.toPx() + 12.dp.toPx() * badge)
                    }
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(horizontal = 9.dp, vertical = 2.dp),
            )
        }
        if (flagshipGlyph) {
            AnimatedSkipGlyph(cycle = cycle, backwards = icon == R.drawable.avd_skip_previous, color = tint, width = iconSize * SkipPairWidth)
            return@Box
        }
        val avd = AnimatedImageVector.animatedVectorResource(icon)
        key(trigger) {
            var atEnd by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                if (trigger > 0) atEnd = true
            }
            val painter = rememberAnimatedVectorPainter(avd, atEnd)
            Image(
                painter = painter,
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),

                modifier = Modifier

                    .requiredWidth(iconSize * SkipIconWidthFactor)
                    .aspectRatio(SkipIconAspect)
            )
        }
    }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
internal fun AnimatedPressScalePlayPauseButton(
    isPlaying: Boolean,
    playbackState: Int,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp,
    containerColor: Color = Color.Transparent,
    enabled: Boolean = true,
    flagshipGlyph: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.54f, stiffness = 720f),
        label = "pressScaleIconButton"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.36f
            }
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick
            )
    ) {
        if (playbackState == Player.STATE_ENDED) {
            Image(
                painter = painterResource(R.drawable.replay),
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier.size(iconSize)
            )
        } else if (flagshipGlyph) {
            AnimatedPlayGlyph(playing = isPlaying, color = tint, height = iconSize * PlayTriangleHeight)
        } else {
            val avd = AnimatedImageVector.animatedVectorResource(R.drawable.avd_play_to_pause)
            val painter = rememberAnimatedVectorPainter(avd, atEnd = isPlaying)
            Image(
                painter = painter,
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

private const val SkipIconAspect = 100f / 64f

private const val SkipIconWidthFactor = 2f

private const val SkipPairWidth = 0.96f
private const val PlayTriangleHeight = SkipPairWidth * (131.26f / 157.32f)
