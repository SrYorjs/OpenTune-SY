package com.arturo254.opentune.ui.screens.search

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.core.designsystem.component.GalaxFilterChip
import com.arturo254.opentune.core.designsystem.theme.GalaxVisualKit
import com.arturo254.opentune.sources.resolution.AudioSelectionMode

@Composable
fun GalaxSearchEditorialHero(
    query: String,
    count: Int,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(33.dp)
    Box(
        modifier = modifier.fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        scheme.primary.copy(alpha = .16f),
                        scheme.surfaceContainerLow,
                        scheme.tertiary.copy(alpha = .08f),
                    ),
                ),
            ),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val r = size.width * .44f
            val cx = size.width * .85f
            val cy = size.height * .2f
            for (i in 0..3) {
                val radius = r * (.28f + i * .34f)
                drawCircle(
                    scheme.primary.copy(alpha = .09f + i * .022f),
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            drawArc(
                scheme.tertiary.copy(alpha = .65f),
                startAngle = -75f,
                sweepAngle = 94f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 23.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).background(scheme.primary, CircleShape))
                Text(
                    text = "GALAXOT  /  BÚSQUEDA UNIVERSAL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                    letterSpacing = 1.6.sp,
                )
            }
            Text(
                text = query,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = (-.8).sp,
            )
            Text(
                text = "MÚSICA DE TODOS TUS PROVEEDORES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = .7.sp,
                color = scheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isLoading) "Explorando fuentes..." else "$count resultados encontrados",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.clip(GalaxVisualKit.pillShape)
                        .background(scheme.primary.copy(alpha = .16f))
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "TODOS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
fun GalaxSearchQualityBar(
    mode: AudioSelectionMode,
    onModeChange: (AudioSelectionMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(4.dp).background(scheme.primary, CircleShape))
            Text(
                text = "CALIDAD DE REPRODUCCIÓN",
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurfaceVariant,
                letterSpacing = 1.25.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf(
                AudioSelectionMode.AUTOMATIC to "Automática",
                AudioSelectionMode.BEST_QUALITY to "Máxima calidad",
                AudioSelectionMode.DATA_SAVER to "Ahorro de datos",
            ).forEach { (value, label) ->
                GalaxFilterChip(
                    label = label,
                    selected = mode == value,
                    onClick = { onModeChange(value) },
                )
            }
        }
    }
}

@Composable
fun GalaxSearchSectionHeading(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "GALAXOT  /  RESULTADOS",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}
