package com.arturo254.opentune.ui.player.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun GalaxEqualizerCurve(levels: List<Int>, minMb: Int, maxMb: Int, description: String) {
    val main = MaterialTheme.colorScheme.primary
    val violet = MaterialTheme.colorScheme.tertiary
    val text = MaterialTheme.colorScheme.onSurface
    val base = MaterialTheme.colorScheme.surfaceContainerLow
    Box(
        Modifier.fillMaxWidth()
            .height(188.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(base)
            .drawWithCache {
                val left = 23.dp.toPx()
                val top = 19.dp.toPx()
                val right = size.width - left
                val bottom = size.height - top
                val width = (right - left).coerceAtLeast(1f)
                val height = (bottom - top).coerceAtLeast(1f)
                val levelRange = (maxMb - minMb).coerceAtLeast(1).toFloat()
                val centerY = bottom - ((0 - minMb) / levelRange).coerceIn(0f, 1f) * height
                val points = levels.mapIndexed { index, mb ->
                    Offset(
                        left + (index.toFloat() / (levels.size - 1).coerceAtLeast(1)) * width,
                        bottom - ((mb - minMb) / levelRange).coerceIn(0f, 1f) * height,
                    )
                }
                val line = Path()
                if (points.isNotEmpty()) {
                    line.moveTo(points.first().x, points.first().y)
                    points.zipWithNext().forEach { (a, b) ->
                        val mid = (a.x + b.x) / 2f
                        line.cubicTo(mid, a.y, mid, b.y, b.x, b.y)
                    }
                }
                val fill = Path()
                if (points.isNotEmpty()) {
                    fill.addPath(line)
                    fill.lineTo(points.last().x, bottom)
                    fill.lineTo(points.first().x, bottom)
                    fill.close()
                }
                val areaBrush = Brush.verticalGradient(
                    0f to main.copy(alpha = .30f),
                    1f to violet.copy(alpha = .015f),
                    startY = top,
                    endY = bottom,
                )
                val strokeBrush = Brush.horizontalGradient(
                    listOf(violet, main, Color(0xFFB9E9FF)),
                    startX = left,
                    endX = right,
                )
                val backgroundGlow = Brush.radialGradient(
                    listOf(main.copy(alpha = .12f), Color.Transparent),
                    center = Offset(size.width * .70f, 0f),
                    radius = size.width,
                )
                onDrawBehind {
                    drawRect(backgroundGlow)
                    for (i in 0..4) {
                        val lineY = top + i * height / 4f
                        drawLine(
                            text.copy(alpha = if (i == 2) .11f else .065f),
                            Offset(left, lineY),
                            Offset(right, lineY),
                            strokeWidth = .9.dp.toPx(),
                        )
                    }
                    for (i in 0..6) {
                        val lineX = left + i * width / 6f
                        drawLine(
                            text.copy(alpha = .045f),
                            Offset(lineX, top),
                            Offset(lineX, bottom),
                            strokeWidth = .8.dp.toPx(),
                        )
                    }
                    drawLine(
                        main.copy(alpha = .21f),
                        Offset(left, centerY),
                        Offset(right, centerY),
                        strokeWidth = 1.dp.toPx(),
                    )
                    if (points.isNotEmpty()) {
                        drawPath(fill, areaBrush)
                        drawPath(
                            line,
                            main.copy(alpha = .16f),
                            style = Stroke(8.dp.toPx(), cap = StrokeCap.Round),
                        )
                        drawPath(
                            line,
                            strokeBrush,
                            style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round),
                        )
                        points.forEach { point ->
                            drawCircle(main.copy(alpha = .18f), 8.dp.toPx(), point)
                            drawCircle(Color.White, 3.dp.toPx(), point)
                            drawCircle(main, 1.8.dp.toPx(), point)
                        }
                    }
                }
            }
            .semantics { contentDescription = description },
    )
}
