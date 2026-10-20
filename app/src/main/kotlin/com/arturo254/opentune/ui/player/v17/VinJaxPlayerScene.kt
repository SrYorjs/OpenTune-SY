package com.arturo254.opentune.ui.player.v17

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import kotlin.math.min

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
    val scheme = MaterialTheme.colorScheme
    val reducedMotion = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val discRotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying, reducedMotion) {
        while (isPlaying && !reducedMotion) {
            discRotation.animateTo(
                targetValue = discRotation.value + 360f,
                animationSpec = tween(4800, easing = LinearEasing),
            )
            discRotation.snapTo(discRotation.value % 360f)
        }
    }
    Box(
        modifier = modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090F19),
                        androidx.compose.ui.graphics.lerp(scheme.primary, Color(0xFF0E0C0C), .76f),
                        Color(0xFF150E0C),
                        Color(0xFF070709),
                    ),
                ),
            )
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val space = 26.dp.toPx()
            var y = 0f
            while (y < size.height) {
                drawLine(Color.White.copy(alpha = .018f), Offset(0f, y), Offset(size.width, y), .65.dp.toPx())
                y += space
            }
        }
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize().padding(
                    start = 13.dp, end = 13.dp, bottom = queuePeekHeight + 10.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BoxWithConstraints(Modifier.weight(1.07f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    val diameter = min(maxWidth.value * .96f, maxHeight.value * .87f).dp
                    VinJaxTurntable(
                        accent = accent,
                        progress = progress,
                        rotation = discRotation.value,
                        artwork = artwork,
                        modifier = Modifier.size(diameter),
                    )
                }
                Column(
                    modifier = Modifier.weight(.93f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    VinJaxDeckTitle(accent = accent, isPlaying = isPlaying)
                    controls()
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(bottom = queuePeekHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                VinJaxDeckTitle(accent = accent, isPlaying = isPlaying)
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    val diameter = min(maxWidth.value * .93f, maxHeight.value * .94f).dp
                    VinJaxTurntable(
                        accent = accent,
                        progress = progress,
                        rotation = discRotation.value,
                        artwork = artwork,
                        modifier = Modifier.size(diameter),
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(Color.Black.copy(alpha = .58f))
                        .padding(vertical = 13.dp),
                ) {
                    controls()
                }
            }
        }
    }
}

@Composable
private fun VinJaxDeckTitle(accent: Color, isPlaying: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 23.dp, end = 23.dp, top = 18.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "VINJAX  /  ANALOG STUDIO",
                color = accent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                "El universo gira contigo",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
            )
        }
        Text(
            if (isPlaying) "●  33⅓ RPM" else "○  STANDBY",
            color = Color.White.copy(alpha = .75f),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun VinJaxTurntable(
    accent: Color,
    progress: Float,
    rotation: Float,
    artwork: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(25.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF191E23), Color(0xFF090A0C), Color(0xFF1D1714)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width * .46f, size.height * .52f)
            val r = size.minDimension * .385f
            drawCircle(
                color = Color.Black.copy(alpha = .85f),
                radius = r * 1.07f,
                center = c,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF33353A), Color(0xFF030405), Color(0xFF1E2124), Color.Black),
                    center = c,
                    radius = r,
                ),
                radius = r,
                center = c,
            )
            drawCircle(
                color = accent.copy(alpha = .56f),
                radius = r * .99f,
                center = c,
                style = Stroke(width = 1.1.dp.toPx()),
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width * .46f, size.height * .52f)
                val r = size.minDimension * .385f
                for (index in 1..38) {
                    val radius = r * (.35f + .0168f * index)
                    drawCircle(
                        color = if (index % 3 == 0)
                            Color.White.copy(alpha = .125f)
                        else Color.White.copy(alpha = .036f),
                        radius = radius,
                        center = c,
                        style = Stroke(width = .7.dp.toPx()),
                    )
                }
                drawArc(
                    color = accent.copy(alpha = .57f),
                    startAngle = -64f,
                    sweepAngle = 92f,
                    useCenter = false,
                    topLeft = Offset(c.x - r * .88f, c.y - r * .88f),
                    size = Size(r * 1.76f, r * 1.76f),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(.40f)
                        .graphicsLayer {
                            translationX = -size.width * .10f
                            translationY = size.height * .04f
                        }
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    artwork()
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width * .46f, size.height * .52f)
            val r = size.minDimension * .385f
            val pivot = Offset(size.width * .88f, size.height * .13f)
            val needle = Offset(
                c.x + r * (.88f - fraction * .70f),
                c.y - r * (.14f + fraction * .22f),
            )
            val elbow = Offset(
                pivot.x - r * (.07f + fraction * .03f),
                pivot.y + r * .41f,
            )
            val arm = Path().apply {
                moveTo(pivot.x, pivot.y)
                lineTo(elbow.x, elbow.y)
                lineTo(needle.x, needle.y)
            }
            drawPath(arm, Color.Black.copy(alpha = .79f), style = Stroke(width = 9.dp.toPx()))
            drawPath(arm, Color(0xFFD1D6DD), style = Stroke(width = 5.dp.toPx()))
            drawCircle(Color(0xFF282C30), radius = r * .135f, center = pivot)
            drawCircle(Color(0xFFE1E4E9), radius = r * .084f, center = pivot, style = Stroke(2.dp.toPx()))
            drawLine(Color(0xFF2B2C2D), elbow, needle, strokeWidth = 3.dp.toPx())
            drawCircle(accent.copy(alpha = .89f), radius = 3.dp.toPx(), center = needle)
            val stylus = Offset(needle.x - 3.dp.toPx(), needle.y + 5.dp.toPx())
            drawLine(Color.White.copy(alpha = .96f), needle, stylus, strokeWidth = 1.dp.toPx())
            drawCircle(Color(0xFFCF8E50), radius = 6.dp.toPx(),
                center = Offset(size.width * .12f, size.height * .14f))
            drawCircle(Color(0xFFD6D7DA), radius = 3.dp.toPx(),
                center = Offset(size.width * .12f, size.height * .14f))
        }
    }
}
