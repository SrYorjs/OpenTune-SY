package com.arturo254.opentune.ui.screens.search

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.arturo254.opentune.core.designsystem.component.GalaxFilterChip
import com.arturo254.opentune.sources.resolution.AudioSelectionMode
import com.arturo254.opentune.ui.screens.AetherInk
import com.arturo254.opentune.ui.screens.AetherMotionClock
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun GalaxSearchEditorialHero(
    query: String,
    count: Int,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = Color(0xFF111D35)
    val shape = RoundedCornerShape(27.dp)
    Box(
        modifier = modifier.fillMaxWidth()
            .height(190.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        background,
                        Color(0xFF1F3156),
                        Color(0xFF0A1328),
                    )
                )
            ),
    ) {
        AetherMotionClock(active = isLoading, durationMillis = 14000) { phase ->
            SearchFrequencySpectrum(
                modifier = Modifier.fillMaxSize(),
                phase = phase,
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0A1429).copy(alpha = .86f),
                        Color(0xFF0A1429).copy(alpha = .36f),
                        Color.Transparent,
                    )
                )
            )
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart)
                .fillMaxWidth().padding(23.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "RESULTADOS   /   TODOS LOS PROVEEDORES",
                style = MaterialTheme.typography.labelSmall,
                color = AetherInk.cyan.copy(alpha = .90f),
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = query.ifBlank { "Buscar música" },
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-.76).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(6.dp).clip(CircleShape)
                        .background(AetherInk.cyan),
                )
                Text(
                    text = when {
                        isLoading && count == 0 -> "Buscando…"
                        count == 1 -> "1 canción"
                        else -> count.toString() + " canciones"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = .78f),
                )
            }
        }
    }
}

@Composable
private fun SearchFrequencySpectrum(
    modifier: Modifier = Modifier,
    phase: Float,
) {
    Canvas(modifier) {
        val gap = size.width / 45f
        val right = size.width * .63f
        val mid = size.height * .47f
        for (i in 0..28) {
            val p = i / 28f
            val x = right + gap * i
            val envelope = sin(p * PI).toFloat().coerceAtLeast(0f)
            val modulation = sin(i * 1.38 + phase * PI * 2).toFloat()
            val amplitude = size.height * (.08f + envelope * (.28f + .13f * modulation))
            drawLine(
                color = if (i % 3 == 0) AetherInk.cyan.copy(alpha = .49f)
                    else AetherInk.lilac.copy(alpha = .37f),
                start = Offset(x, mid - amplitude * .55f),
                end = Offset(x, mid + amplitude * .55f),
                strokeWidth = (if (i % 4 == 0) 1.7f else .8f).dp.toPx(),
            )
        }
        for (ring in 0..4) {
            drawCircle(
                color = AetherInk.silver.copy(alpha = .095f),
                radius = size.width * (.11f + .04f * ring),
                center = Offset(size.width * .89f, size.height * .32f),
                style = Stroke(width = .6.dp.toPx()),
            )
        }
    }
}

@Composable
fun GalaxSearchQualityBar(
    mode: AudioSelectionMode,
    onModeChange: (AudioSelectionMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = listOf(
        AudioSelectionMode.AUTOMATIC to "Automática",
        AudioSelectionMode.BEST_QUALITY to "Máxima",
        AudioSelectionMode.DATA_SAVER to "Ahorro",
    )
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 6.dp, bottom = 5.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Calidad de audio",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = when (mode) {
                    AudioSelectionMode.AUTOMATIC -> "AUTO"
                    AudioSelectionMode.BEST_QUALITY -> "HI-RES"
                    AudioSelectionMode.DATA_SAVER -> "AHORRO"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = .9.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            modes.forEach { (value, label) ->
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
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-.40).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (detail.isNotBlank()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
