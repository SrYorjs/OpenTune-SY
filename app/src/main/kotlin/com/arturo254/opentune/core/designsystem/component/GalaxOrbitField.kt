package com.arturo254.opentune.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun GalaxOrbitField(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    intensity: Float = 1f,
    accent: Color = MaterialTheme.colorScheme.primary,
    secondary: Color = MaterialTheme.colorScheme.tertiary,
) {
    val reducedMotion = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val orbitPhase = if (active && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "galaxOrbitCycle")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 34000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "orbitRevolution",
        )
        value
    } else {
        0f
    }
    val strength = intensity.coerceIn(0f, 1f)
    Canvas(modifier.fillMaxSize()) {
        val radius = min(size.width, size.height) * .37f
        val center = Offset(size.width * .52f, size.height * .50f)
        val orbitStroke = (.72.dp.toPx()).coerceAtLeast(.7f)
        for (orbit in 0..2) {
            val ring = radius * (.55f + orbit * .34f)
            drawCircle(
                color = if (orbit == 1) secondary.copy(alpha = .24f * strength)
                    else accent.copy(alpha = .16f * strength),
                radius = ring,
                center = center,
                style = Stroke(width = orbitStroke),
            )
            drawArc(
                color = if (orbit == 1) secondary.copy(alpha = .46f * strength)
                    else accent.copy(alpha = .38f * strength),
                startAngle = -75f + orbitPhase * (if (orbit % 2 == 0) .8f else -.55f) + orbit * 86f,
                sweepAngle = 46f + orbit * 13f,
                useCenter = false,
                topLeft = Offset(center.x - ring, center.y - ring),
                size = Size(ring * 2f, ring * 2f),
                style = Stroke(width = 1.55.dp.toPx()),
            )
            val angle = ((orbitPhase * (if (orbit % 2 == 0) 1f else -.65f)
                + orbit * 121f - 44f) / 180f * PI).toFloat()
            val satellite = Offset(
                center.x + cos(angle) * ring,
                center.y + sin(angle) * ring,
            )
            drawCircle(
                color = Color.White.copy(alpha = .55f * strength),
                radius = if (orbit == 1) 2.25.dp.toPx() else 1.6.dp.toPx(),
                center = satellite,
            )
            drawCircle(
                color = if (orbit == 1) secondary.copy(alpha = .18f * strength)
                    else accent.copy(alpha = .15f * strength),
                radius = 6.dp.toPx(),
                center = satellite,
            )
        }
        for (star in 0 until 23) {
            val x = ((star * 67 + 17) % 101) / 101f
            val y = ((star * 43 + 9) % 97) / 97f
            val twinkle = .52f + .48f * cos(
                (orbitPhase * .013f + star * 17f) / 180f * PI.toFloat(),
            )
            drawCircle(
                color = lerp(accent, Color.White, if (star % 4 == 0) .9f else .55f).copy(
                    alpha = (.12f + twinkle * .29f) * strength,
                ),
                radius = if (star % 6 == 0) 1.25.dp.toPx() else .65.dp.toPx(),
                center = Offset(size.width * x, size.height * y),
            )
        }
    }
}
