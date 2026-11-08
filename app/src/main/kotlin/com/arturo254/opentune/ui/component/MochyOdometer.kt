package com.arturo254.opentune.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun MochyOdometer(
    value: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 50.sp,
    color: Color = Color.White,
    fontWeight: FontWeight = FontWeight.Light,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        value.forEach { character ->
            if (character.isDigit()) {
                MochyOdometerDigit(
                    target = character.digitToInt(),
                    fontSize = fontSize,
                    color = color,
                    fontWeight = fontWeight,
                )
            } else {
                Text(character.toString(), fontSize = fontSize, color = color, fontWeight = fontWeight)
            }
        }
    }
}

@Composable
private fun MochyOdometerDigit(
    target: Int,
    fontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight,
) {
    val density = LocalDensity.current
    val height = with(density) { fontSize.toDp() * 1.18f }
    val width = with(density) { fontSize.toDp() * .65f }
    val anim = remember { Animatable(target.toFloat()) }

    LaunchedEffect(target) {
        val from = anim.value
        val forward = (target - (from.toInt() % 10) + 10) % 10
        anim.animateTo(
            from + forward,
            animationSpec = tween(650, easing = FastOutSlowInEasing),
        )
        anim.snapTo(target.toFloat())
    }
    Box(
        Modifier.width(width).height(height)
            .clipToBounds()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        .20f to Color.Black,
                        .80f to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val rolling = anim.value
        for (offset in -1..1) {
            val digit = (((rolling.toInt() + offset) % 10) + 10) % 10
            val position = (rolling.toInt() + offset - rolling)
            val displacement = with(density) { (position * height.toPx()).toDp() }
            val alpha = (1f - abs(position)).coerceIn(0f, 1f)
            Text(
                digit.toString(),
                modifier = Modifier.offset(y = displacement),
                fontSize = fontSize,
                fontFamily = FontFamily.Default,
                fontWeight = fontWeight,
                color = color.copy(alpha = alpha),
            )
        }
    }
}
