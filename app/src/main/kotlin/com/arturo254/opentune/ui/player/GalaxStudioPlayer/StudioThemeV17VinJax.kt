package com.arturo254.opentune.ui.player.GalaxStudioPlayer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * V17 / VinJax — an astronomical listening room built around a mechanical record.
 *
 * This file owns presentation only. Playback, seeking, menus, lyrics and queue
 * remain in the supplied controls slot. No second player or progress clock is
 * created here: the tonearm follows the host's normalized playback position.
 *
 * Layer order:
 * 1. Opaque night background and restrained distant stars.
 * 2. Stationary platter, feet, bearing and orbital edge lighting.
 * 3. Rotating black record and circular album label.
 * 4. Stationary light reflections and spindle.
 * 5. Rigid tonearm, cartridge, cue mechanism and pivot.
 *
 * Public entry points deliberately retain the original signatures. All helpers
 * are private so replacing this one source does not affect any other theme.
 */
@Composable
fun VinJaxPlayerScene(
    isLandscape: Boolean,
    isPlaying: Boolean,
    progress: Float,
    accent: Color,
    queuePeekHeight: Dp,
    artwork: @Composable BoxScope.() -> Unit,
    controls: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val visible = rememberVinJaxForeground()
    val rotation = rememberVinJaxRotation(isPlaying, reducedMotion, visible)
    val palette = remember(accent) { VinJaxPalette.from(accent) }
    val bottom = if (queuePeekHeight.value.isFinite()) {
        queuePeekHeight.coerceAtLeast(0.dp)
    } else {
        0.dp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.night),
    ) {
        VinJaxSky(palette, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                    ),
                ),
        ) {
            if (isLandscape) {
                VinJaxLandscape(
                    progress = progress,
                    isPlaying = isPlaying,
                    rotation = { rotation.value },
                    palette = palette,
                    queuePeekHeight = bottom,
                    artwork = artwork,
                    controls = controls,
                )
            } else {
                VinJaxPortrait(
                    progress = progress,
                    isPlaying = isPlaying,
                    rotation = { rotation.value },
                    palette = palette,
                    queuePeekHeight = bottom,
                    artwork = artwork,
                    controls = controls,
                )
            }
        }
    }
}

/** Standalone compatibility entry point used by previews and existing callers. */
@Composable
fun VinJaxTurntable(
    accent: Color,
    progress: Float,
    isPlaying: Boolean,
    rotation: Float,
    artwork: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = remember(accent) { VinJaxPalette.from(accent) }
    VinJaxInstrument(
        palette = palette,
        progress = progress,
        isPlaying = isPlaying,
        rotation = { rotation },
        artwork = artwork,
        modifier = modifier,
    )
}

private object VinJaxDimensions {
    const val CENTER_X = .455f
    const val CENTER_Y = .535f
    const val RECORD_RADIUS = .382f
    const val LABEL_RADIUS = .129f
    const val PIVOT_X = .854f
    const val PIVOT_Y = .173f
    const val ARM_LENGTH = .492f
    const val OUTER_GROOVE = .947f
    const val INNER_GROOVE = .452f
    const val REVOLUTION_MS = 1818
    const val PROGRESS_SMOOTHING_MS = 240
    const val CUE_MS = 280
    const val STAR_COUNT = 43
}

private data class VinJaxPalette(
    val night: Color,
    val blue: Color,
    val ice: Color,
    val violet: Color,
    val metal: Color,
    val mutedMetal: Color,
) {
    companion object {
        fun from(accent: Color): VinJaxPalette {
            // Keep the galaxy blue even for orange/green covers. Album color is
            // a secondary light source, never a replacement for readable metal.
            val safeAccent = if (accent == Color.Unspecified) {
                Color(0xFF6F9EFF)
            } else {
                accent.copy(alpha = 1f)
            }
            return VinJaxPalette(
                night = Color(0xFF050810),
                blue = lerp(Color(0xFF6399FF), safeAccent, .16f),
                ice = Color(0xFFCCE7FF),
                violet = lerp(Color(0xFF7271CE), safeAccent, .12f),
                metal = Color(0xFFD4DFEB),
                mutedMetal = Color(0xFF566478),
            )
        }
    }
}

/** Stop decorative GPU work when the activity is no longer in the foreground. */
@Composable
private fun rememberVinJaxForeground(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return foreground
}

/**
 * One bounded phase, shared by the label and record. Canceling an animation
 * preserves its last angle, so pause/resume cannot snap the label back to zero.
 * Read this State only in a draw/layer callback, not in the scene composition.
 */
@Composable
private fun rememberVinJaxRotation(
    isPlaying: Boolean,
    reducedMotion: Boolean,
    foreground: Boolean,
): State<Float> {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying, reducedMotion, foreground) {
        if (!foreground) return@LaunchedEffect
        while (isPlaying && !reducedMotion) {
            val context = currentCoroutineContext()
            if (!context.isActive) break
            // A zero system duration scale must not produce a busy snap loop.
            if ((context[MotionDurationScale]?.scaleFactor ?: 1f) <= 0f) break
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(
                    durationMillis = VinJaxDimensions.REVOLUTION_MS,
                    easing = LinearEasing,
                ),
            )
            rotation.snapTo(rotation.value % 360f)
        }
    }
    return rotation.asState()
}

@Composable
private fun VinJaxPortrait(
    progress: Float,
    isPlaying: Boolean,
    rotation: () -> Float,
    palette: VinJaxPalette,
    queuePeekHeight: Dp,
    artwork: @Composable BoxScope.() -> Unit,
    controls: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = queuePeekHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val edge = min(maxWidth.value - 16f, maxHeight.value - 8f)
                .coerceIn(0f, 560f).dp
            if (edge > 0.dp) {
                VinJaxInstrument(
                    palette = palette,
                    progress = progress,
                    isPlaying = isPlaying,
                    rotation = rotation,
                    artwork = artwork,
                    modifier = Modifier.size(edge),
                )
            }
        }
        // The host measures the real transport first; the record takes the
        // remaining space. Large text never forces controls under the queue.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            controls()
        }
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun VinJaxLandscape(
    progress: Float,
    isPlaying: Boolean,
    rotation: () -> Float,
    palette: VinJaxPalette,
    queuePeekHeight: Dp,
    artwork: @Composable BoxScope.() -> Unit,
    controls: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 12.dp,
                end = 12.dp,
                bottom = queuePeekHeight + 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            val edge = min(maxWidth.value, maxHeight.value - 4f)
                .coerceIn(0f, 560f).dp
            if (edge > 0.dp) {
                VinJaxInstrument(
                    palette = palette,
                    progress = progress,
                    isPlaying = isPlaying,
                    rotation = rotation,
                    artwork = artwork,
                    modifier = Modifier.size(edge),
                )
            }
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            controls()
        }
    }
}

private data class VinJaxStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
)

/** Deterministic field; no random allocations or particle simulation per frame. */
private val vinJaxStars = List(VinJaxDimensions.STAR_COUNT) { index ->
    val seed = index + 1
    VinJaxStar(
        x = ((seed * 73) % 997) / 997f,
        y = ((seed * 139) % 991) / 991f,
        radius = .45f + (seed % 3) * .24f,
        alpha = .12f + (seed % 5) * .045f,
    )
}

@Composable
private fun VinJaxSky(palette: VinJaxPalette, modifier: Modifier) {
    Box(
        modifier.drawWithCache {
            val blueCloud = Brush.radialGradient(
                colors = listOf(
                    palette.blue.copy(alpha = .13f),
                    palette.blue.copy(alpha = .035f),
                    Color.Transparent,
                ),
                center = Offset(size.width * .28f, size.height * .28f),
                radius = size.maxDimension * .63f,
            )
            val violetCloud = Brush.radialGradient(
                colors = listOf(palette.violet.copy(alpha = .065f), Color.Transparent),
                center = Offset(size.width * .96f, size.height * .47f),
                radius = size.maxDimension * .45f,
            )
            val lowerShade = Brush.verticalGradient(
                0f to Color.Transparent,
                .6f to palette.night.copy(alpha = .12f),
                1f to palette.night.copy(alpha = .94f),
            )
            onDrawBehind {
                drawRect(blueCloud)
                drawRect(violetCloud)
                vinJaxStars.forEach { star ->
                    val point = Offset(star.x * size.width, star.y * size.height)
                    drawCircle(
                        color = palette.ice.copy(alpha = star.alpha),
                        radius = star.radius.dp.toPx(),
                        center = point,
                    )
                }
                drawRect(lowerShade)
            }
        },
    )
}

@Composable
private fun VinJaxInstrument(
    palette: VinJaxPalette,
    progress: Float,
    isPlaying: Boolean,
    rotation: () -> Float,
    artwork: @Composable BoxScope.() -> Unit,
    modifier: Modifier,
) {
    val reducedMotion = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val safeProgress = if (progress.isFinite()) progress.coerceIn(0f,1f) else 0f
    val needlePosition = animateFloatAsState(
        targetValue = safeProgress,
        animationSpec = if (reducedMotion) snap() else tween(
            durationMillis = VinJaxDimensions.PROGRESS_SMOOTHING_MS,
            easing = LinearEasing,
        ),
        label = "vinJaxNeedleProgress",
    )
    val lifted = animateFloatAsState(
        targetValue = if(isPlaying) 0f else 1f,
        animationSpec = if (reducedMotion) snap() else tween(VinJaxDimensions.CUE_MS),
        label = "vinJaxNeedleLift",
    )

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // Square local coordinate system also protects external preview callers
        // that supply a rectangular modifier to the public entry point.
        val edge = min(maxWidth.value, maxHeight.value).coerceAtLeast(0f).dp
        if (edge > 0.dp) {
            Box(Modifier.size(edge)) {
                VinJaxPlinth(palette, Modifier.fillMaxSize())
                VinJaxRecord(
                    palette = palette,
                    edge = edge,
                    rotation = rotation,
                    reducedMotion = reducedMotion,
                    artwork = artwork,
                )
                Canvas(Modifier.fillMaxSize()) {
                    drawVinJaxRecordLight(palette)
                    drawVinJaxSpindle(palette)
                }
                Canvas(Modifier.fillMaxSize()) {
                    val geometry = VinJaxArmGeometry.at(needlePosition.value)
                    drawVinJaxCueRest(palette)
                    drawVinJaxTonearm(geometry, lifted.value, palette)
                    drawVinJaxPivot(palette)
                    drawVinJaxPower(isPlaying, palette)
                }
            }
        }
    }
}

/** All coordinates are relative to the same square as the record and label. */
private fun DrawScope.vinJaxPoint(x: Float, y: Float): Offset =
    Offset(size.minDimension * x, size.minDimension * y)

private fun DrawScope.vinJaxCenter(): Offset =
    vinJaxPoint(VinJaxDimensions.CENTER_X, VinJaxDimensions.CENTER_Y)

private fun DrawScope.vinJaxRadius(): Float =
    size.minDimension * VinJaxDimensions.RECORD_RADIUS

@Composable
private fun VinJaxPlinth(palette: VinJaxPalette, modifier: Modifier) {
    Canvas(modifier) {
        val c = vinJaxCenter()
        val r = vinJaxRadius()
        drawVinJaxAura(c, r, palette)
        drawVinJaxFeet(palette)
        drawVinJaxPlatter(c, r, palette)
        drawVinJaxOrbit(c, r, palette)
    }
}

private fun DrawScope.drawVinJaxAura(c: Offset, r: Float, palette: VinJaxPalette) {
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.Transparent,
            .72f to palette.blue.copy(alpha = .015f),
            .84f to palette.blue.copy(alpha = .09f),
            1f to Color.Transparent,
            center = c,
            radius = r * 1.24f,
        ),
        radius = r * 1.24f,
        center = c,
    )
    drawCircle(
        color = Color.Black.copy(alpha = .38f),
        radius = r * 1.045f,
        center = c + Offset(0f, r * .027f),
    )
}

private fun DrawScope.drawVinJaxFeet(palette: VinJaxPalette) {
    val unit = size.minDimension
    for (x in listOf(.19f, .72f)) {
        val foot = vinJaxPoint(x, .841f)
        drawCircle(Color(0xFF02040A), unit * .031f, foot)
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(palette.mutedMetal.copy(alpha = .6f), Color(0xFF080D17)),
                startY = foot.y - unit * .025f,
                endY = foot.y + unit * .025f,
            ),
            radius = unit * .025f,
            center = foot,
        )
        drawCircle(
            color = palette.ice.copy(alpha = .1f),
            radius = unit * .025f,
            center = foot,
            style = Stroke(unit * .0012f),
        )
    }
}

private fun DrawScope.drawVinJaxPlatter(c: Offset, r: Float, palette: VinJaxPalette) {
    drawCircle(
        brush = Brush.linearGradient(
            0f to Color(0xFF263446),
            .25f to Color(0xFF0A101C),
            .52f to Color(0xFF53627A),
            .7f to Color(0xFF121B2B),
            1f to Color(0xFF080D16),
            start = c - Offset(r, r),
            end = c + Offset(r, r),
        ),
        radius = r * 1.035f,
        center = c,
    )
    drawCircle(
        color = palette.ice.copy(alpha = .22f),
        radius = r * 1.033f,
        center = c,
        style = Stroke(r * .003f),
    )
    drawCircle(
        color = Color(0xFF03060C),
        radius = r * 1.018f,
        center = c,
    )
    drawCircle(
        color = palette.blue.copy(alpha = .28f),
        radius = r * 1.013f,
        center = c,
        style = Stroke(r * .003f),
    )
    // Fixed machining marks; these belong to the platter, not the recording.
    repeat(96) { index ->
        val angle = index * 2.0 * PI / 96.0
        val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
        drawLine(
            color = palette.ice.copy(alpha = if (index % 4 == 0) .25f else .08f),
            start = c + direction * (r * 1.021f),
            end = c + direction * (r * 1.028f),
            strokeWidth = r * .002f,
        )
    }
}

private fun DrawScope.drawVinJaxOrbit(c: Offset, r: Float, palette: VinJaxPalette) {
    val orbitRadius = r * 1.09f
    val bounds = Size(orbitRadius * 2f, orbitRadius * 2f)
    val topLeft = c - Offset(orbitRadius, orbitRadius)
    drawArc(
        color = palette.blue.copy(alpha = .16f),
        startAngle = 42f,
        sweepAngle = 102f,
        useCenter = false,
        topLeft = topLeft,
        size = bounds,
        style = Stroke(r * .002f, cap = StrokeCap.Round),
    )
    drawArc(
        color = palette.ice.copy(alpha = .13f),
        startAngle = 182f,
        sweepAngle = 46f,
        useCenter = false,
        topLeft = topLeft,
        size = bounds,
        style = Stroke(r * .002f, cap = StrokeCap.Round),
    )
    val angle = 144.0 * PI / 180.0
    val endpoint = c + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * orbitRadius
    drawCircle(palette.blue.copy(alpha = .65f), r * .006f, endpoint)
}

@Composable
private fun BoxScope.VinJaxRecord(
    palette: VinJaxPalette,
    edge: Dp,
    rotation: () -> Float,
    reducedMotion: Boolean,
    artwork: @Composable BoxScope.() -> Unit,
) {
    val diameter = edge * (VinJaxDimensions.RECORD_RADIUS * 2f)
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .size(diameter)
            .graphicsLayer {
                translationX = edge.toPx() * (VinJaxDimensions.CENTER_X - .5f)
                translationY = edge.toPx() * (VinJaxDimensions.CENTER_Y - .5f)
                val suppliedRotation = rotation()
                val rotation = if (reducedMotion || !suppliedRotation.isFinite()) {
                    0f
                } else {
                    suppliedRotation % 360f
                }
                rotationZ=rotation
            }
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawVinJaxRecordBody(palette)
            drawVinJaxGrooves()
            drawVinJaxLeadIn()
            drawVinJaxDeadWax(palette)
        }
        // A real black label substrate remains visible for missing/loading art.
        Box(
            modifier = Modifier
                .size(edge * (VinJaxDimensions.LABEL_RADIUS * 2f))
                .clip(CircleShape)
                .background(Color(0xFF142238)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawVinJaxLabelFallback(palette)
            }
            Box(
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentAlignment = Alignment.Center,
                content=artwork,
            )
        }
    }
}

private fun DrawScope.drawVinJaxRecordBody(palette: VinJaxPalette) {
    val r = size.minDimension * .5f
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFF141821),
            .32f to Color(0xFF080B11),
            .36f to Color(0xFF161A21),
            .54f to Color(0xFF080B10),
            .75f to Color(0xFF151920),
            .96f to Color(0xFF070A0F),
            1f to Color(0xFF202631),
            center = center,
            radius = r,
        ),
        radius = r,
        center = center,
    )
    drawCircle(
        color = palette.ice.copy(alpha = .12f),
        radius = r * .994f,
        style = Stroke(r * .0025f),
    )
}

private fun DrawScope.drawVinJaxGrooves() {
    val r = size.minDimension * .5f
    val inner = VinJaxDimensions.INNER_GROOVE
    val outer = VinJaxDimensions.OUTER_GROOVE
    // Fine grooves stay subpixel at small sizes; major band dividers create the
    // recognisable pressed-vinyl texture without aliasing high contrast rings.
    repeat(80) { index ->
        val fraction = index / 79f
        val radius = r * (inner + (outer - inner) * fraction)
        val strong = index % 17 == 0
        drawCircle(
            color = Color.White.copy(alpha = if (strong) .12f else .043f),
            radius = radius,
            style = Stroke(width = r * if (strong) .0026f else .0017f),
        )
        if (strong) {
            drawCircle(
                color = Color.Black.copy(alpha = .8f),
                radius = radius - r * .004f,
                style = Stroke(r * .003f),
            )
        }
    }
    // Short asymmetric marks rotate with the actual record, making the
    // motion readable even when a cover is nearly monochromatic.
    for (index in 0..6) {
        val radius = r * (.52f + index * .062f)
        drawArc(
            color = Color.White.copy(alpha = .035f),
            startAngle = 29f + index * 13f,
            sweepAngle = 22f,
            useCenter = false,
            topLeft = center - Offset(radius, radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(r * .002f),
        )
    }
}

private fun DrawScope.drawVinJaxLeadIn() {
    val r = size.minDimension * .5f
    for (radius in listOf(.958f, .967f, .975f)) {
        drawCircle(
            color = Color.White.copy(alpha = .075f),
            radius = r * radius,
            style = Stroke(r * .0018f),
        )
    }
}

private fun DrawScope.drawVinJaxDeadWax(palette: VinJaxPalette) {
    val r = size.minDimension * .5f
    val labelFraction = VinJaxDimensions.LABEL_RADIUS / VinJaxDimensions.RECORD_RADIUS
    drawCircle(
        color = Color(0xFF090C12),
        radius = r * .425f,
    )
    for (fraction in listOf(.405f, .414f, .424f)) {
        drawCircle(
            color = palette.ice.copy(alpha = .065f),
            radius = r * fraction,
            style = Stroke(r * .002f),
        )
    }
    drawCircle(
        color = palette.blue.copy(alpha = .25f),
        radius = r * (labelFraction + .016f),
        style = Stroke(r * .004f),
    )
    drawCircle(
        color = palette.ice.copy(alpha = .22f),
        radius = r * (labelFraction + .004f),
        style = Stroke(r * .002f),
    )
}

private fun DrawScope.drawVinJaxLabelFallback(palette: VinJaxPalette) {
    val r = size.minDimension * .5f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette.blue.copy(alpha = .45f), Color(0xFF0C1424)),
            center = center - Offset(r * .3f, r * .3f),
            radius = r * 1.6f,
        ),
    )
    for (index in 1..3) {
        drawCircle(
            color = palette.ice.copy(alpha = .13f),
            radius = r * (.28f + index * .17f),
            style = Stroke(r * .008f),
        )
    }
    drawCircle(palette.ice.copy(alpha = .8f), r * .025f, center + Offset(r * .43f, -r * .3f))
}

/** Specular light is fixed in the room rather than painted onto the spinning LP. */
private fun DrawScope.drawVinJaxRecordLight(palette: VinJaxPalette) {
    val r = vinJaxRadius()
    val c = vinJaxCenter()
    val label = size.minDimension * VinJaxDimensions.LABEL_RADIUS
    val middle = (r + label) * .5f
    val band = r - label - size.minDimension * .004f
    drawCircle(
        brush = Brush.sweepGradient(
            0f to Color.Transparent,
            .12f to Color.Transparent,
            .18f to palette.ice.copy(alpha = .045f),
            .24f to Color.Transparent,
            .56f to Color.Transparent,
            .64f to palette.ice.copy(alpha = .085f),
            .70f to Color.Transparent,
            1f to Color.Transparent,
            center = c,
        ),
        radius = middle,
        center = c,
        style = Stroke(band),
    )
    drawCircle(
        color = Color.Black.copy(alpha = .28f),
        radius = label,
        center = c,
        style = Stroke(size.minDimension * .004f),
    )
}

private fun DrawScope.drawVinJaxSpindle(palette: VinJaxPalette) {
    val c = vinJaxCenter()
    val unit = size.minDimension
    drawCircle(Color.Black.copy(alpha = .65f), unit * .014f, c + Offset(0f, unit * .002f))
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(palette.ice, palette.mutedMetal, Color(0xFF101725)),
            start = c - Offset(unit * .008f, unit * .009f),
            end = c + Offset(unit * .009f, unit * .01f),
        ),
        radius = unit * .009f,
        center = c,
    )
    drawCircle(Color(0xFF05080F), unit * .003f, c)
    drawCircle(
        color = Color.White.copy(alpha = .75f),
        radius = unit * .0018f,
        center = c - Offset(unit * .003f, unit * .003f),
    )
}

/**
 * Intersect two circles: the arm's fixed reach around its pivot and the current
 * groove around the record spindle. This is the actual geometry of a pivoted
 * tonearm, not a linear interpolation of its tip or an elastic elbow.
 *
 * The selected intersection stays on the right/lower side of the LP. The other
 * intersection would make the arm cross the album label. The geometry guarantees
 * a constant pivot-to-stylus distance, including after arbitrary seeks.
 */
private data class VinJaxArmGeometry(val angleDegrees: Float) {
    companion object {
        fun at(progress: Float): VinJaxArmGeometry {
            val p = if (progress.isFinite()) progress.coerceIn(0f,1f) else 0f
            val pivot=Offset(VinJaxDimensions.PIVOT_X, VinJaxDimensions.PIVOT_Y)
            val spindle = Offset(VinJaxDimensions.CENTER_X, VinJaxDimensions.CENTER_Y)
            val delta = spindle - pivot
            val distance = sqrt(delta.x * delta.x + delta.y * delta.y)
            val length = VinJaxDimensions.ARM_LENGTH
            val radius = VinJaxDimensions.RECORD_RADIUS * (
                VinJaxDimensions.OUTER_GROOVE +
                    (VinJaxDimensions.INNER_GROOVE - VinJaxDimensions.OUTER_GROOVE) * p
                )
            val along = (length * length - radius * radius + distance * distance) /
                (2f * distance)
            val across = sqrt((length * length - along * along).coerceAtLeast(0f))
            val direction = delta / distance
            val base = pivot + direction * along
            val needle=Offset(
                x = base.x + direction.y * across,
                y = base.y - direction.x * across,
            )
            val arm = needle - pivot
            val angle = atan2(arm.y.toDouble(), arm.x.toDouble()) * 180.0 / PI - 90.0
            return VinJaxArmGeometry(angle.toFloat())
        }
    }
}

private fun DrawScope.drawVinJaxCueRest(palette: VinJaxPalette) {
    val unit = size.minDimension
    val position = vinJaxPoint(.88f, .338f)
    drawRoundRect(
        color = Color(0xFF141E2C),
        topLeft = position - Offset(unit * .023f, unit * .013f),
        size = Size(unit * .046f, unit * .026f),
        cornerRadius = CornerRadius(unit * .008f),
    )
    drawLine(
        color = palette.mutedMetal,
        start = position - Offset(unit * .014f, 0f),
        end = position + Offset(unit * .014f, 0f),
        strokeWidth = unit * .005f,
        cap = StrokeCap.Round,
    )
    drawCircle(palette.ice.copy(alpha = .4f), unit * .003f, position)
}

private fun DrawScope.drawVinJaxTonearm(
    geometry: VinJaxArmGeometry,
    lift: Float,
    palette: VinJaxPalette,
) {
    val unit = size.minDimension
    val pivot = vinJaxPoint(VinJaxDimensions.PIVOT_X, VinJaxDimensions.PIVOT_Y)
    val length = unit * VinJaxDimensions.ARM_LENGTH
    val height = lift.coerceIn(0f, 1f)
    // A lifted stylus retains its groove position. Moving a paused arm to its
    // rest would falsely imply that playback restarted when the user resumes.
    val shadowOffset = unit * (.007f + height * .012f)
    translate(left = shadowOffset, top = shadowOffset) {
        rotate(geometry.angleDegrees, pivot) {
            drawVinJaxArmShadow(pivot, length, unit, height)
        }
    }
    rotate(geometry.angleDegrees, pivot) {
        drawVinJaxCounterweight(pivot, unit, palette)
        drawVinJaxArmTube(pivot, length, unit, palette)
        drawVinJaxHeadshell(pivot, length, unit, height, palette)
    }
}

private fun DrawScope.drawVinJaxArmShadow(
    pivot: Offset,
    length: Float,
    unit: Float,
    lift: Float,
) {
    val path=Path().apply {
        moveTo(pivot.x, pivot.y - unit * .054f)
        lineTo(pivot.x, pivot.y + length * .73f)
        lineTo(pivot.x - unit * .012f, pivot.y + length * .87f)
        lineTo(pivot.x, pivot.y + length)
    }
    drawPath(path, Color.Black.copy(alpha = .36f - lift * .13f), style = Stroke(
        width = unit * (.019f + lift * .007f),
        cap = StrokeCap.Round,
    ))
    drawRoundRect(
        color = Color.Black.copy(alpha = .32f),
        topLeft = Offset(pivot.x - unit * .019f, pivot.y + length - unit * .061f),
        size = Size(unit * .038f, unit * .065f),
        cornerRadius = CornerRadius(unit * .007f),
    )
}

private fun DrawScope.drawVinJaxCounterweight(
    pivot: Offset,
    unit: Float,
    palette: VinJaxPalette,
) {
    val topLeft = pivot + Offset(-unit * .025f, -unit * .082f)
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFF0D1521), palette.mutedMetal, Color(0xFF172232)),
            startX = topLeft.x,
            endX = topLeft.x + unit * .05f,
        ),
        topLeft = topLeft,
        size = Size(unit * .05f, unit * .046f),
        cornerRadius = CornerRadius(unit * .009f),
    )
    repeat(5) { index ->
        val y = topLeft.y + unit * (.007f + index * .008f)
        drawLine(
            color = palette.ice.copy(alpha = .19f),
            start = Offset(topLeft.x + unit * .005f, y),
            end = Offset(topLeft.x + unit * .045f, y),
            strokeWidth = unit * .0015f,
        )
    }
}

private fun DrawScope.drawVinJaxArmTube(
    pivot: Offset,
    length: Float,
    unit: Float,
    palette: VinJaxPalette,
) {
    val path = Path().apply {
        moveTo(pivot.x, pivot.y - unit * .044f)
        lineTo(pivot.x, pivot.y + length * .73f)
        lineTo(pivot.x - unit * .012f, pivot.y + length * .87f)
        lineTo(pivot.x - unit * .004f, pivot.y + length - unit * .025f)
    }
    drawPath(
        path = path,
        color = Color(0xFF121B29),
        style = Stroke(unit * .019f, cap = StrokeCap.Round),
    )
    drawPath(
        path = path,
        brush = Brush.horizontalGradient(
            0f to Color(0xFF3B4B60),
            .35f to palette.metal,
            .58f to Color(0xFFF0F6FF),
            .76f to palette.mutedMetal,
            1f to Color(0xFF233145),
            startX = pivot.x - unit * .014f,
            endX = pivot.x + unit * .01f,
        ),
        style = Stroke(unit * .012f, cap = StrokeCap.Round),
    )
    drawLine(
        color = palette.ice.copy(alpha = .5f),
        start = pivot + Offset(-unit * .002f, unit * .047f),
        end = pivot + Offset(-unit * .002f, length * .69f),
        strokeWidth = unit * .0015f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawVinJaxHeadshell(
    pivot: Offset,
    length: Float,
    unit: Float,
    lift: Float,
    palette: VinJaxPalette,
) {
    val tip = pivot + Offset(0f, length)
    val left = tip.x - unit * .016f
    val top = tip.y - unit * .059f
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF4B5E76), Color(0xFF152030), Color(0xFF070D16)),
            start = Offset(left, top),
            end = Offset(left + unit * .032f, tip.y),
        ),
        topLeft = Offset(left, top),
        size = Size(unit * .032f, unit * .048f),
        cornerRadius = CornerRadius(unit * .006f),
    )
    drawLine(
        color = palette.metal.copy(alpha = .7f),
        start = Offset(left + unit * .002f, top + unit * .006f),
        end = Offset(left + unit * .002f, top + unit * .039f),
        strokeWidth = unit * .0015f,
    )
    repeat(3) { index ->
        drawRoundRect(
            color = Color(0xFF060B13),
            topLeft = Offset(left + unit * .009f, top + unit * (.008f + index * .01f)),
            size = Size(unit * .015f, unit * .004f),
            cornerRadius = CornerRadius(unit * .002f),
        )
    }
    // Finger lift belongs to the same rigid assembly, including its shadow.
    drawLine(
        color = palette.mutedMetal,
        start = Offset(left + unit * .03f, top + unit * .012f),
        end = Offset(left + unit * .048f, top + unit * .006f),
        strokeWidth = unit * .004f,
        cap = StrokeCap.Round,
    )
    drawRoundRect(
        color = palette.blue.copy(alpha = .8f),
        topLeft = Offset(tip.x - unit * .01f, tip.y - unit * .016f),
        size = Size(unit * .02f, unit * .009f),
        cornerRadius = CornerRadius(unit * .002f),
    )
    drawLine(
        color = palette.ice.copy(alpha = 1f - lift * .35f),
        start = tip - Offset(0f, unit * .008f),
        end = tip,
        strokeWidth = unit * .002f,
        cap = StrokeCap.Round,
    )
    // Contact light fades on pause; the exact contact point remains the solved
    // circle intersection, never an offset decorative dot beside the groove.
    drawCircle(
        color = palette.blue.copy(alpha = (1f - lift) * .12f),
        radius = unit * .012f,
        center = tip,
    )
    drawCircle(
        color = palette.ice.copy(alpha = 1f - lift * .65f),
        radius = unit * .0028f,
        center = tip,
    )
}

private fun DrawScope.drawVinJaxPivot(palette: VinJaxPalette) {
    val unit = size.minDimension
    val pivot = vinJaxPoint(VinJaxDimensions.PIVOT_X, VinJaxDimensions.PIVOT_Y)
    drawCircle(Color.Black.copy(alpha = .5f), unit * .053f, pivot + Offset(0f, unit * .006f))
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF53667E), Color(0xFF131D2B), Color(0xFF060B13)),
            start = pivot - Offset(unit * .05f, unit * .05f),
            end = pivot + Offset(unit * .05f, unit * .05f),
        ),
        radius = unit * .049f,
        center = pivot,
    )
    drawCircle(
        color = palette.ice.copy(alpha = .23f),
        radius = unit * .048f,
        center = pivot,
        style = Stroke(unit * .0015f),
    )
    drawCircle(Color(0xFF050A12), unit * .035f, pivot)
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(palette.metal, palette.mutedMetal, Color(0xFF172333)),
            start = pivot - Offset(unit * .03f, unit * .03f),
            end = pivot + Offset(unit * .03f, unit * .03f),
        ),
        radius = unit * .027f,
        center = pivot,
    )
    drawCircle(
        color = palette.blue.copy(alpha = .5f),
        radius = unit * .033f,
        center = pivot,
        style = Stroke(unit * .002f),
    )
    drawCircle(Color(0xFF111B29), unit * .009f, pivot)
    drawLine(
        color = palette.metal.copy(alpha = .6f),
        start = pivot - Offset(unit * .004f, 0f),
        end = pivot + Offset(unit * .004f, 0f),
        strokeWidth = unit * .0015f,
    )
}

private fun DrawScope.drawVinJaxPower(isPlaying: Boolean, palette: VinJaxPalette) {
    val unit = size.minDimension
    val indicator = vinJaxPoint(.85f, .88f)
    // A status light, intentionally not another play button or fake control.
    val alpha = if (isPlaying) .9f else .24f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette.blue.copy(alpha = alpha * .17f), Color.Transparent),
            center = indicator,
            radius = unit * .025f,
        ),
        radius = unit * .025f,
        center = indicator,
    )
    drawCircle(palette.blue.copy(alpha = alpha), unit * .004f, indicator)
    drawLine(
        color = palette.ice.copy(alpha = .1f),
        start = indicator + Offset(unit * .013f, 0f),
        end = indicator + Offset(unit * .052f, 0f),
        strokeWidth = unit * .0015f,
        cap = StrokeCap.Round,
    )
}
