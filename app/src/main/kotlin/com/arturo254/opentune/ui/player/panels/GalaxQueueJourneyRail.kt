package com.arturo254.opentune.ui.player.panels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GalaxQueueJourneyRail(
    currentIndex: Int,
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    if (itemCount <= 0) return

    val scheme = MaterialTheme.colorScheme
    val selected = currentIndex.coerceIn(0, itemCount - 1)
    val progress = (selected + 1f) / itemCount
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 27.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${selected + 1} / $itemCount",
                color = scheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = "${(progress * 100f).toInt()}%",
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Canvas(
            Modifier.fillMaxWidth().height(6.dp),
        ) {
            val y = size.height * .5f
            drawLine(
                color = scheme.onSurface.copy(alpha = .10f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(scheme.primary, scheme.tertiary),
                ),
                start = Offset(0f, y),
                end = Offset(size.width * progress, y),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = scheme.primary,
                radius = 4.dp.toPx(),
                center = Offset(size.width * progress, y),
            )
        }
    }
}
