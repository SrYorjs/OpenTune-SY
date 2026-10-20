package com.arturo254.opentune.ui.screens.search

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R

@Composable
fun GalaxQualitySelectionHeader(
    versionCount: Int,
    providerCount: Int,
    manualSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        scheme.primary.copy(alpha = .20f),
                        scheme.surfaceContainerLow,
                        scheme.tertiary.copy(alpha = .10f),
                    ),
                ),
                RoundedCornerShape(23.dp),
            ),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * .88f, size.height * .12f)
            for (index in 1..6) {
                drawCircle(
                    color = scheme.primary.copy(alpha = .04f + index * .012f),
                    radius = size.width * (.08f + index * .075f),
                    center = center,
                    style = Stroke(width = .85.dp.toPx()),
                )
            }
        }
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(41.dp)
                    .background(scheme.primary.copy(alpha = .15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (manualSelected) "✓" else "♪",
                    color = scheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.galax_quality_selection),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.galax_quality_versions,
                        versionCount,
                        providerCount,
                    ),
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
