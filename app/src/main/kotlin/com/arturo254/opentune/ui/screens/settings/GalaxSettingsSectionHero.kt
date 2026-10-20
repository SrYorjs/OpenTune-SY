package com.arturo254.opentune.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class GalaxSettingsArea {
    APPEARANCE,
    PLAYER,
    CONTENT,
    STORAGE,
    PRIVACY,
    INTEGRATIONS,
}

@Composable
fun GalaxSettingsSectionHero(
    title: String,
    description: String,
    area: GalaxSettingsArea,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val accent = when (area) {
        GalaxSettingsArea.APPEARANCE -> colors.primary
        GalaxSettingsArea.PLAYER -> colors.tertiary
        GalaxSettingsArea.CONTENT -> colors.secondary
        GalaxSettingsArea.STORAGE -> colors.primary
        GalaxSettingsArea.PRIVACY -> colors.secondary
        GalaxSettingsArea.INTEGRATIONS -> colors.tertiary
    }
    val ordinal = when (area) {
        GalaxSettingsArea.APPEARANCE -> "01"
        GalaxSettingsArea.PLAYER -> "02"
        GalaxSettingsArea.CONTENT -> "03"
        GalaxSettingsArea.STORAGE -> "04"
        GalaxSettingsArea.PRIVACY -> "05"
        GalaxSettingsArea.INTEGRATIONS -> "06"
    }
    Box(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 13.dp)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = .22f),
                        colors.surfaceContainerLow,
                        colors.surfaceContainerLow,
                    ),
                ),
                RoundedCornerShape(30.dp),
            ),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val pivot = Offset(size.width * .86f, size.height * .28f)
            when (area) {
                GalaxSettingsArea.APPEARANCE -> {
                    for (i in 0..7) {
                        drawArc(
                            color = if (i % 2 == 0) accent.copy(alpha = .24f)
                                else colors.tertiary.copy(alpha = .20f),
                            startAngle = -80f + i * 18f,
                            sweepAngle = 134f + i * 10f,
                            useCenter = false,
                            topLeft = Offset(
                                pivot.x - size.width * (.12f + i * .06f),
                                pivot.y - size.width * (.12f + i * .06f),
                            ),
                            size = Size(
                                size.width * (.24f + i * .12f),
                                size.width * (.24f + i * .12f),
                            ),
                            style = Stroke(width = (1f + i * .1f).dp.toPx()),
                        )
                    }
                }
                GalaxSettingsArea.PLAYER -> {
                    for (i in 0..18) {
                        val x = size.width * (.53f + i * .024f)
                        val amplitude = ((i * 17 % 11) / 11f) * size.height * .20f
                        drawLine(
                            accent.copy(alpha = .12f + (i % 4) * .05f),
                            Offset(x, pivot.y - amplitude),
                            Offset(x, pivot.y + amplitude),
                            (1f + i % 3).dp.toPx(),
                        )
                    }
                }
                GalaxSettingsArea.CONTENT -> {
                    for (i in 0..6) {
                        val radius = size.width * (.12f + i * .065f)
                        drawCircle(
                            accent.copy(alpha = .11f + (i % 2) * .08f),
                            radius = radius,
                            center = pivot,
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
                }
                GalaxSettingsArea.STORAGE -> {
                    for (i in 0..4) {
                        val y = size.height * (.11f + i * .12f)
                        drawRoundRect(
                            color = accent.copy(alpha = .09f + i * .024f),
                            topLeft = Offset(size.width * .56f + i * 3.dp.toPx(), y),
                            size = Size(size.width * .36f, size.height * .075f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),
                        )
                    }
                }
                GalaxSettingsArea.PRIVACY -> {
                    for (i in 0..6) {
                        val radius = size.width * (.11f + i * .069f)
                        drawCircle(
                            accent.copy(alpha = if (i == 0) .30f else .09f),
                            radius = radius,
                            center = pivot,
                            style = Stroke(width = (1f + i * .1f).dp.toPx()),
                        )
                    }
                }
                GalaxSettingsArea.INTEGRATIONS -> {
                    val nodes = listOf(
                        Offset(size.width * .66f, size.height * .18f),
                        Offset(size.width * .89f, size.height * .12f),
                        Offset(size.width * .83f, size.height * .67f),
                        Offset(size.width * .60f, size.height * .75f),
                    )
                    for (i in nodes.indices) {
                        val next = nodes[(i + 1) % nodes.size]
                        drawLine(accent.copy(alpha = .25f), nodes[i], next, 1.dp.toPx())
                        drawCircle(accent.copy(alpha = .65f), 2.5.dp.toPx(), nodes[i])
                    }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier.size(7.dp).background(accent, CircleShape),
                )
                Text(
                    text = "GALAXOT / $ordinal",
                    color = accent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
            }
            Text(
                text = title,
                color = colors.onSurface,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(.78f),
            )
        }
    }
}
