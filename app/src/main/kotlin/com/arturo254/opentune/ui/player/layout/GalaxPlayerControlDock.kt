package com.arturo254.opentune.ui.player.layout

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun GalaxPlayerControlDock(
    accent: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 9.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = .44f))
            .drawWithCache {
                val top = Brush.verticalGradient(
                    0f to accent.copy(alpha = .13f),
                    .27f to Color.Transparent,
                    1f to Color.Black.copy(alpha = .14f),
                )
                val glow = Brush.radialGradient(
                    0f to accent.copy(alpha = .18f),
                    1f to Color.Transparent,
                    center = Offset(size.width * .90f, 0f),
                    radius = size.width.coerceAtLeast(1f) * .82f,
                )
                onDrawBehind {
                    drawRect(top)
                    drawRect(glow)
                }
            },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 21.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GalaxPlayerSignalIndicator(
                    isPlaying = isPlaying,
                    accent = accent,
                    modifier = Modifier.size(width = 21.dp, height = 14.dp),
                )
                Text(
                    text = if (isPlaying) "GALAXOT  /  AHORA SUENA"
                    else "GALAXOT  /  EN PAUSA",
                    color = Color.White.copy(alpha = .74f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                )
            }
            content()
        }
    }
}

@Composable
fun GalaxPlayerSignalIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val reduceMotion = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val phase = if (isPlaying && !reduceMotion) {
        val motion = rememberInfiniteTransition(label = "galaxAudioSignal")
        val value by motion.animateFloat(
            initialValue = 0f,
            targetValue = 6.2831855f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "galaxWavePhase",
        )
        value
    } else 0f
    Canvas(modifier) {
        val width = size.width / 7f
        for (index in 0 until 4) {
            val activity = if (isPlaying && !reduceMotion)
                abs(sin(phase + index * .95f))
            else if (isPlaying) .70f else .23f
            val barHeight = size.height * (.28f + activity * .66f)
            val x = size.width * (.15f + index * .225f)
            drawRoundRect(
                color = accent.copy(alpha = if (isPlaying) .94f else .50f),
                topLeft = Offset(x, (size.height - barHeight) * .5f),
                size = androidx.compose.ui.geometry.Size(width, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(width * .48f),
            )
        }
    }
}
