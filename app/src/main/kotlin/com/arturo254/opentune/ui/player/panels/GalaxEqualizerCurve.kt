package com.arturo254.opentune.ui.player.panels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

@Composable
fun GalaxEqualizerCurve(levels: List<Int>, minMb: Int, maxMb: Int, description: String) {
    val accent = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f)
    Canvas(Modifier.fillMaxWidth().height(144.dp).padding(16.dp)
        .semantics { contentDescription = description }) {
        val range = (maxMb - minMb).coerceAtLeast(1).toFloat()
        val zeroY = size.height * (1f - (-minMb / range).coerceIn(0f, 1f))
        drawLine(grid, Offset(0f, zeroY), Offset(size.width, zeroY), 1.dp.toPx())
        if (levels.isNotEmpty()) {
            val points = levels.mapIndexed { index, value ->
                Offset(size.width * index / (levels.size - 1).coerceAtLeast(1),
                    size.height * (1f - ((value - minMb) / range).coerceIn(0f, 1f)))
            }
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.zipWithNext().forEach { (start, end) ->
                    val middle = (start.x + end.x) / 2f
                    cubicTo(middle, start.y, middle, end.y, end.x, end.y)
                }
            }
            val area = Path().apply {
                addPath(path)
                lineTo(points.last().x, size.height)
                lineTo(points.first().x, size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(accent.copy(alpha = .22f), Color.Transparent)))
            drawPath(path, accent, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            points.forEach { drawCircle(accent, 3.dp.toPx(), it) }
        }
    }
}
