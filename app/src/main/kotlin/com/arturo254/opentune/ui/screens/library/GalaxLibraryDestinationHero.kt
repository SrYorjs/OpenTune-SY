package com.arturo254.opentune.ui.screens.library

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class GalaxLibraryDestination {
    ALBUMS,
    ARTISTS,
    PLAYLISTS,
}

@Composable
fun GalaxLibraryDestinationHero(
    destination: GalaxLibraryDestination,
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val (chapter, caption, description) = when (destination) {
        GalaxLibraryDestination.ALBUMS ->
            Triple("01", "DISCOGRAFÍA", "Cada portada es una puerta a otro universo.")
        GalaxLibraryDestination.ARTISTS ->
            Triple("02", "CONSTELACIONES", "Las voces y creadores que forman tu galaxia.")
        GalaxLibraryDestination.PLAYLISTS ->
            Triple("03", "COLECCIONES", "Historias y recuerdos reunidos en listas.")
    }
    Box(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(33.dp))
            .background(
                Brush.linearGradient(
                    when (destination) {
                        GalaxLibraryDestination.ALBUMS -> listOf(
                            scheme.primary.copy(alpha = .22f),
                            scheme.surfaceContainerLow,
                            scheme.tertiary.copy(alpha = .10f),
                        )
                        GalaxLibraryDestination.ARTISTS -> listOf(
                            scheme.tertiary.copy(alpha = .21f),
                            scheme.surfaceContainerLow,
                            scheme.primary.copy(alpha = .13f),
                        )
                        GalaxLibraryDestination.PLAYLISTS -> listOf(
                            scheme.secondary.copy(alpha = .21f),
                            scheme.surfaceContainerLow,
                            scheme.primary.copy(alpha = .17f),
                        )
                    },
                ),
            ),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * .90f, size.height * .19f)
            val r = size.width * .35f
            when (destination) {
                GalaxLibraryDestination.ALBUMS -> {
                    for (i in 0..7) {
                        drawCircle(
                            scheme.primary.copy(alpha = .085f),
                            r * (.41f + i * .15f),
                            center,
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
                    drawCircle(scheme.tertiary.copy(alpha = .29f), r*.46f, center)
                }
                GalaxLibraryDestination.ARTISTS -> {
                    for (i in 0..7) {
                        val x = size.width * (.54f + i*.06f)
                        val y = size.height * (.10f + (i%3)*.21f)
                        drawCircle(
                            scheme.primary.copy(alpha = .47f),
                            radius = (2+i%3).dp.toPx(),
                            center = Offset(x,y),
                        )
                        if (i>0) {
                            drawLine(
                                scheme.tertiary.copy(alpha = .16f),
                                Offset(size.width*(.54f+(i-1)*.06f), size.height*(.10f+((i-1)%3)*.21f)),
                                Offset(x,y), 1.dp.toPx(),
                            )
                        }
                    }
                }
                GalaxLibraryDestination.PLAYLISTS -> {
                    for (i in 0..4) {
                        drawRoundRect(
                            scheme.primary.copy(alpha = .08f + i * .018f),
                            topLeft = Offset(
                                size.width*(.63f+i*.035f), size.height*(.10f+i*.09f),
                            ),
                            size = Size(size.width*.29f,size.height*.56f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(13.dp.toPx()),
                            style = Stroke(width = 1.2.dp.toPx()),
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 23.dp, vertical = 23.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Box(Modifier.size(6.dp).background(scheme.primary, CircleShape))
                Text(
                    text = "GALAXOT  /  BIBLIOTECA  $chapter",
                    color = scheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp,
                )
            }
            Text(
                text = title,
                color = scheme.onSurface,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                letterSpacing = (-.8).sp,
            )
            Text(
                text = description,
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    Modifier.clip(RoundedCornerShape(18.dp))
                        .background(scheme.primary.copy(alpha = .16f))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = "$count ELEMENTOS",
                        color = scheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    caption,
                    color = scheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
