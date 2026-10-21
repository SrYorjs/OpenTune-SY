package com.arturo254.opentune.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.min
import kotlin.math.sin

internal data class GalaxEditorialStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val brightness: Float,
    val depth: Float,
    val seed: Float,
)

private fun galaxyStarField(count: Int): List<GalaxEditorialStar> =
    List(count) { index ->
        val id = index + 1
        GalaxEditorialStar(
            x = ((id * 61 + id * id * 7) % 997) / 997f,
            y = ((id * 83 + id * id * 3) % 991) / 991f,
            radius = .48f + (id % 5) * .18f,
            brightness = .14f + (id % 7) * .047f,
            depth = .25f + (id % 4) * .19f,
            seed = ((id * 37) % 360).toFloat(),
        )
    }

private fun DrawScope.drawGalaxStars(
    stars: List<GalaxEditorialStar>,
    phase: Float,
    accent: Color,
    scale: Float,
) {
    val tau = (PI * 2.0).toFloat()
    stars.forEach { star ->
        val beat = .67f + .33f * cos((phase * star.depth + star.seed / 360f) * tau)
        val position = Offset(
            size.width * star.x,
            size.height * star.y,
        )
        drawCircle(
            color = lerp(accent, Color.White, .72f)
                .copy(alpha = (star.brightness * beat * scale).coerceIn(0f, .55f)),
            radius = star.radius.dp.toPx().coerceAtLeast(.6f),
            center = position,
        )
    }
}

@Composable
fun GalaxEditorialBackdrop(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    density: Int = 35,
    accent: Color = MaterialTheme.colorScheme.primary,
    secondary: Color = MaterialTheme.colorScheme.tertiary,
    intensity: Float = 1f,
) {
    val reducedMotion = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val transition = rememberInfiniteTransition(label = "editorialStarlight")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32000, easing = LinearEasing),
        ),
        label = "editorialStarPhase",
    )
    val stars = remember(density) { galaxyStarField(density.coerceIn(8, 52)) }
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val motionPhase = if (reducedMotion || !active) 0f else phase
    val visible = intensity.coerceIn(0f, 1f)
    Canvas(modifier.fillMaxSize()) {
        val centerOne = Offset(size.width * .91f, size.height * .15f)
        val centerTwo = Offset(size.width * .06f, size.height * .76f)
        drawRect(
            Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = if (dark) .13f * visible else .07f * visible),
                    Color.Transparent,
                ),
                center = centerOne,
                radius = size.width.coerceAtLeast(1f) * .79f,
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(
                    secondary.copy(alpha = if (dark) .09f * visible else .04f * visible),
                    Color.Transparent,
                ),
                center = centerTwo,
                radius = size.width.coerceAtLeast(1f) * .66f,
            )
        )
        drawGalaxStars(stars, motionPhase, accent, visible)
    }
}

@Composable
fun GalaxEditorialOrb(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    accent: Color = MaterialTheme.colorScheme.primary,
    secondary: Color = MaterialTheme.colorScheme.tertiary,
    rings: Int = 3,
) {
    val reducedMotion = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val transition = rememberInfiniteTransition(label = "editorialOrbit")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 44000, easing = LinearEasing),
        ),
        label = "editorialRotation",
    )
    val angle = if (reducedMotion || !active) 0f else rotation
    Canvas(modifier.fillMaxSize()) {
        val minSide = min(size.width, size.height)
        val origin = Offset(size.width * .72f, size.height * .43f)
        val baseRadius = minSide * .24f
        repeat(rings.coerceIn(1, 5)) { index ->
            val radius = baseRadius * (1f + index * .36f)
            val color = if (index % 2 == 0) accent else secondary
            drawCircle(
                color = color.copy(alpha = .13f),
                radius = radius,
                center = origin,
                style = Stroke(width = .85.dp.toPx()),
            )
            drawArc(
                color = color.copy(alpha = .50f),
                startAngle = angle * (if (index % 2 == 0) 1f else -.7f) + 38f * index,
                sweepAngle = 37f + index * 9f,
                useCenter = false,
                topLeft = Offset(origin.x - radius, origin.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = 1.55.dp.toPx(), cap = StrokeCap.Round),
            )
            val rad = ((angle * (if (index % 2 == 0) 1f else -.7f) +
                38f * index) / 180f * PI).toFloat()
            val satellite = Offset(
                origin.x + radius * cos(rad),
                origin.y + radius * sin(rad),
            )
            drawCircle(Color.White.copy(alpha = .74f), 1.7.dp.toPx(), satellite)
            drawCircle(color.copy(alpha = .20f), 6.dp.toPx(), satellite)
        }
    }
}

@Composable
fun GalaxEditorialSurface(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    accent: Color = MaterialTheme.colorScheme.primary,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val scheme = MaterialTheme.colorScheme
    val base = if (elevated) scheme.surfaceContainerHigh else scheme.surfaceContainerLow
    val surface = modifier
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(
                    base,
                    lerp(base, accent, if (elevated) .12f else .055f),
                    base,
                ),
            )
        )
        .galaxLiquidChrome(shape, emphasis = if (elevated) .77f else .57f)
    Box(
        modifier = if (onClick == null) surface else surface.clickable(onClick = onClick),
        content = content,
    )
}

@Composable
fun GalaxEditorialHeading(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 23.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-.45).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                modifier = Modifier.clickable(onClick = onAction)
                    .padding(vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun GalaxEditorialIcon(
    icon: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    GalaxEditorialSurface(
        modifier = modifier.size(46.dp),
        radius = 17.dp,
        onClick = onClick,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center).size(21.dp),
        )
    }
}

@Composable
fun GalaxEditorialAction(
    title: String,
    icon: Int,
    modifier: Modifier = Modifier,
    processing: Boolean = false,
    onClick: () -> Unit,
) {
    GalaxEditorialSurface(
        modifier = modifier.height(53.dp),
        radius = 18.dp,
        onClick = if (processing) null else onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (processing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(17.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun GalaxEditorialReveal(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val reducedMotion = OpenTuneDesignSystem.galaxMotion.reducedMotion
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    AnimatedVisibility(
        visible = revealed || reducedMotion || !enabled,
        modifier = modifier,
        enter = fadeIn(tween(520, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(520, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 9 },
            ),
        exit = fadeOut(tween(130)),
    ) {
        content()
    }
}

@Composable
fun GalaxEditorialDivider(modifier: Modifier = Modifier) {
    val foreground = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier.fillMaxWidth().height(1.dp)
            .padding(horizontal = 23.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, foreground.copy(alpha = .36f), Color.Transparent),
                ),
            )
    )
}

@Composable
fun GalaxEditorialArtworkFrame(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(25.dp)
    Box(
        modifier = modifier.clip(shape).background(
            Brush.linearGradient(
                listOf(
                    accent.copy(alpha = .23f),
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    accent.copy(alpha = .09f),
                ),
            )
        ),
    ) {
        content()
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(48.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = .18f)),
                    ),
                )
        )
    }
}
