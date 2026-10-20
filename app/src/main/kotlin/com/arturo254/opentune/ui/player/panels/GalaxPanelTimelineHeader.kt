package com.arturo254.opentune.ui.player.panels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GalaxPanelTimelineHeader(
    title: String,
    detail: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        colors.primary.copy(alpha = .14f),
                        colors.surfaceContainerLow.copy(alpha = .83f),
                    ),
                ),
                RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(32.dp)
                .background(colors.primary.copy(alpha = .15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(25.dp)) {
                val c = center
                for (i in 1..3) {
                    drawCircle(
                        colors.primary.copy(alpha = .17f + i * .13f),
                        radius = size.minDimension * (.12f + i * .12f),
                        center = c,
                        style = Stroke(width = .85.dp.toPx()),
                    )
                }
                drawCircle(colors.primary, radius = 2.3.dp.toPx(), center = c)
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        if (!detail.isNullOrBlank()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary,
                fontWeight = FontWeight.Medium,
                letterSpacing = .4.sp,
            )
        }
    }
}
