package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.core.designsystem.theme.GalaxVisualKit
import kotlin.math.min

enum class GalaxHomeZone(
    val index: String,
    val eyebrow: String,
    val description: String,
) {
    RECENT("02", "TU FRECUENCIA", "Continúa donde te quedaste"),
    CURATED("03", "SEÑALES PARA TI", "Canciones escogidas para volver a escuchar"),
    COLLECTION("04", "TUS CONSTELACIONES", "Toda tu música, a tu manera"),
    DISCOVERY("05", "EL UNIVERSO SIGUE", "Artistas, lanzamientos y nuevos sonidos"),
    MEMORY("06", "DESDE OTRA GALAXIA", "Recuerdos musicales que vale la pena revivir"),
    TRENDS("07", "NUEVAS COORDENADAS", "Explora sonidos de todos tus proveedores"),
    MIX("08", "EN MOVIMIENTO", "Accesos rápidos para empezar a escuchar"),
}

@Composable
fun GalaxHomeZoneHeader(
    title: String,
    zone: GalaxHomeZone,
    modifier: Modifier = Modifier,
    subtitle: String = zone.description,
    markerColor: Color = MaterialTheme.colorScheme.primary,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 30.dp, bottom = 13.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier.width(38.dp).height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(37.dp)) {
                val radius = min(size.width, size.height) * .40f
                drawCircle(
                    markerColor.copy(alpha = .18f),
                    radius = radius,
                    style = Stroke(1.dp.toPx()),
                )
                drawArc(
                    markerColor.copy(alpha = .78f),
                    startAngle = -82f,
                    sweepAngle = 118f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = 1.8.dp.toPx()),
                )
                drawCircle(markerColor, radius = 2.dp.toPx(), center = Offset(center.x + radius * .75f, center.y - radius * .65f))
            }
            Text(
                text = zone.index,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = markerColor,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = zone.eyebrow,
                color = markerColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.45.sp,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-.48).sp,
                ),
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            modifier = Modifier.padding(bottom = 3.dp)
                .height(31.dp)
                .width(3.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            markerColor.copy(alpha = .82f),
                            colors.tertiary.copy(alpha = .09f),
                        ),
                    ),
                ),
        )
    }
}

@Composable
fun GalaxHomeChapterDivider(
    chapter: String,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 23.dp, vertical = 21.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            Modifier.width(19.dp).height(19.dp).clip(CircleShape)
                .background(color.copy(alpha = .12f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(5.dp).background(color, CircleShape),
            )
        }
        Text(
            text = chapter,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .8f),
        )
        Box(
            Modifier.weight(1f).height(1.dp).background(
                Brush.horizontalGradient(
                    listOf(color.copy(alpha = .36f), Color.Transparent),
                ),
            ),
        )
    }
}

@Composable
fun GalaxHomeOrbitHeading(
    title: String,
    subtitle: String,
    songCount: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(start = 23.dp, end = 23.dp, top = 30.dp, bottom = 17.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "01   /   TU UNIVERSO PERSONAL",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.primary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.35.sp,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.clip(GalaxVisualKit.pillShape)
                    .background(scheme.primary.copy(alpha = .1f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = "$songCount EN ÓRBITA",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.0).sp,
            ),
            color = scheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
    }
}
