package com.arturo254.opentune.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidChrome
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object AetherInk {
    val midnight = Color(0xFF090E1C)
    val indigo = Color(0xFF182644)
    val ultramarine = Color(0xFF5575DD)
    val cyan = Color(0xFF95DEF1)
    val silver = Color(0xFFDEE9FF)
    val lilac = Color(0xFFB9A4F8)
    val rose = Color(0xFFE4B0DF)
    val pearl = Color(0xFFF3F6FF)
}

internal data class AetherSpeck(
    val x: Float,
    val y: Float,
    val depth: Float,
    val tint: Float,
)

private fun makeAetherSpecks(count: Int): List<AetherSpeck> =
    List(count.coerceIn(6, 48)) { index ->
        val x = (((index + 1) * 37 + index * index * 11) % 997) / 997f
        val y = (((index + 1) * 83 + index * index * 7) % 991) / 991f
        AetherSpeck(
            x = x,
            y = y,
            depth = .30f + (index % 6) * .12f,
            tint = .31f + (index % 5) * .09f,
        )
    }

@Composable
internal fun AetherMotionClock(
    active: Boolean,
    durationMillis: Int = 34000,
    content: @Composable (Float) -> Unit,
) {
    val reduced = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val phase = if (active && !reduced) {
        val timeline = rememberInfiniteTransition(label = "aether-motion")
        val value by timeline.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = LinearEasing),
            ),
            label = "aether-phase",
        )
        value
    } else 0f
    content(phase)
}

@Composable
internal fun AetherAmbient(
    modifier: Modifier = Modifier,
    active: Boolean,
    variant: Int,
    intensity: Float = 1f,
) {
    val palette = MaterialTheme.colorScheme
    val dark = palette.background
    val stars = remember(variant) { makeAetherSpecks(if (variant == 0) 29 else 40) }
    AetherMotionClock(active = active, durationMillis = if (variant == 0) 46000 else 54000) { time ->
        Canvas(modifier.fillMaxSize()) {
            val wide = size.width.coerceAtLeast(1f)
            val tall = size.height.coerceAtLeast(1f)
            val foreground = if (variant == 0) AetherInk.ultramarine else AetherInk.lilac
            val secondary = if (variant == 0) AetherInk.cyan else AetherInk.rose
            drawRect(
                Brush.radialGradient(
                    colors = listOf(
                        foreground.copy(alpha = .095f * intensity),
                        Color.Transparent,
                    ),
                    center = Offset(wide * if (variant == 0) .82f else .12f, tall * .18f),
                    radius = wide * 1.10f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(
                        secondary.copy(alpha = .075f * intensity),
                        Color.Transparent,
                    ),
                    center = Offset(wide * .26f, tall * .78f),
                    radius = wide * 1.19f,
                ),
            )
            stars.forEachIndexed { index, particle ->
                val cycle = (time * particle.depth + index * .117f) * PI * 2
                val alpha = (.19f + .09f * cos(cycle).toFloat()) * intensity
                drawCircle(
                    color = if (particle.tint > .53f) palette.onBackground.copy(alpha = alpha)
                        else foreground.copy(alpha = alpha),
                    radius = (if (index % 9 == 0) 1.25f else .65f).dp.toPx(),
                    center = Offset(
                        wide * particle.x,
                        tall * particle.y,
                    ),
                )
            }
        }
    }
}

private fun DrawScope.drawAetherRibbon(
    phase: Float,
    anchor: Float,
    heightScale: Float,
    tint: Color,
    alpha: Float,
) {
    val wave = Path()
    val step = size.width / 32f
    val base = size.height * anchor
    val twopi = (PI * 2).toFloat()
    for (n in 0..32) {
        val dx = n * step
        val progress = n / 32f
        val envelope = sin(progress * PI).toFloat()
        val delta = sin(progress * twopi * 1.4f + phase * twopi).toFloat()
        val dy = base + delta * size.height * heightScale * envelope
        if (n == 0) wave.moveTo(dx, dy) else wave.lineTo(dx, dy)
    }
    drawPath(wave, color = tint.copy(alpha = alpha), style = Stroke(width = 1.4.dp.toPx()))
}

@Composable
internal fun AetherSoundRibbon(
    active: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    AetherMotionClock(active = active, durationMillis = 18000) { phase ->
        Canvas(modifier) {
            drawAetherRibbon(phase, .38f, .18f, color, .51f)
            drawAetherRibbon(phase + .3f, .56f, .11f, AetherInk.lilac, .35f)
            drawAetherRibbon(phase + .61f, .68f, .07f, AetherInk.cyan, .31f)
        }
    }
}

@Composable
internal fun AetherGlassPlate(
    modifier: Modifier = Modifier,
    radius: Dp = 23.dp,
    emphasis: Float = .60f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val base = MaterialTheme.colorScheme.surfaceContainerLow
    val coat = modifier
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(
                    base.copy(alpha = .91f),
                    lerp(base, MaterialTheme.colorScheme.primary, .095f).copy(alpha = .95f),
                    base.copy(alpha = .92f),
                ),
            ),
        )
        .galaxLiquidChrome(shape, emphasis = emphasis.coerceIn(0f, 1f))
    Box(
        modifier = if (onClick == null) coat else coat.clickable(onClick = onClick),
        content = content,
    )
}

@Composable
internal fun AetherSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 23.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (eyebrow != null) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.9.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!action.isNullOrBlank() && onAction != null) {
                Text(
                    text = action,
                    modifier = Modifier.clickable(onClick = onAction).padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
internal fun AetherAction(
    title: String,
    icon: Int,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    onClick: () -> Unit,
) {
    AetherGlassPlate(
        modifier = modifier.height(56.dp),
        radius = 19.dp,
        emphasis = .72f,
        onClick = if (busy) null else onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun AetherReveal(
    key: Any,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reducedMotion = OpenTuneDesignSystem.galaxMotion.reducedMotion
    var arrived by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) { arrived = true }
    AnimatedVisibility(
        modifier = modifier,
        visible = reducedMotion || arrived,
        enter = fadeIn(animationSpec = tween(560, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(560, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 12 },
            ),
        exit = fadeOut(tween(160)),
    ) {
        content()
    }
}

@Composable
internal fun AetherIdentityDot(
    modifier: Modifier = Modifier,
    active: Boolean,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val size by animateFloatAsState(
        targetValue = if (active) 7f else 4f,
        animationSpec = tween(360, easing = EaseInOutCubic),
        label = "identity",
    )
    Box(
        modifier.size(size.dp).clip(CircleShape)
            .background(tint.copy(alpha = if (active) .97f else .48f)),
    )
}

@Composable
internal fun AetherNumberPlate(
    value: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

internal data class AetherPoint(
    val x: Float,
    val y: Float,
    val cluster: Int,
)

internal fun aetherConstellation(seed: Int, count: Int): List<AetherPoint> {
    return List(count.coerceIn(5, 36)) { i ->
        val ordinal = i + 1
        AetherPoint(
            x = ((ordinal * 47 + seed * 23 + ordinal * ordinal * 5) % 997) / 997f,
            y = ((ordinal * 67 + seed * 29 + ordinal * ordinal * 13) % 991) / 991f,
            cluster = ordinal % 4,
        )
    }
}

@Composable
internal fun AetherConstellation(
    modifier: Modifier = Modifier,
    seed: Int = 1,
    active: Boolean = true,
) {
    val points = remember(seed) { aetherConstellation(seed, 23) }
    AetherMotionClock(active = active, durationMillis = 40000) { phase ->
        Canvas(modifier) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas
            val centers = points.map { p -> Offset(w * p.x, h * p.y) }
            for (i in 0 until centers.size - 1) {
                val u = centers[i]
                val v = centers[i + 1]
                val dist = (u - v).getDistance()
                if (dist < w * .43f) {
                    drawLine(
                        AetherInk.silver.copy(alpha = .10f),
                        start = u,
                        end = v,
                        strokeWidth = .65.dp.toPx(),
                    )
                }
            }
            centers.forEachIndexed { index, center ->
                val flicker = .6f + .4f * cos((phase + index * .17f) * PI * 2).toFloat()
                drawCircle(
                    color = if (index % 4 == 0) AetherInk.lilac else AetherInk.silver,
                    radius = (if (index % 7 == 0) 1.9f else 1.1f).dp.toPx(),
                    center = center,
                    alpha = .44f * flicker,
                )
            }
        }
    }
}

@Composable
internal fun AetherProgressShimmer(
    modifier: Modifier = Modifier,
    active: Boolean = true,
) {
    AetherMotionClock(active, durationMillis = 2200) { phase ->
        Canvas(modifier.height(2.dp)) {
            drawRect(
                color = AetherInk.cyan.copy(alpha = .14f),
                size = size,
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, AetherInk.cyan.copy(alpha = .94f), Color.Transparent),
                    startX = (phase - .3f) * size.width,
                    endX = (phase + .3f) * size.width,
                ),
                size = size,
            )
        }
    }
}
