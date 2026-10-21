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
import androidx.compose.foundation.layout.fillMaxSize
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
    val shape = RoundedCornerShape(27.dp)
    Box(
        modifier = modifier.fillMaxWidth().height(176.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0D1D35),
                        MaterialTheme.colorScheme.primary.copy(alpha = .30f),
                        Color(0xFF0B1121),
                    ),
                ),
            ),
    ) {
        com.arturo254.opentune.core.designsystem.component.GalaxOrbitField(
            modifier = Modifier.fillMaxSize(),
            active = isLoading,
            intensity = .34f,
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(21.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                "RESULTADOS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.9.sp,
                color = Color.White.copy(alpha = .66f),
            )
            Text(
                query,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (isLoading && count == 0) "Buscando"
                else "$"+"count canciones",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = .72f),
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
