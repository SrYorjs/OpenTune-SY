package com.arturo254.opentune.ui.screens.search

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.core.designsystem.component.GalaxFilterChip
import com.arturo254.opentune.sources.resolution.AudioSelectionMode
import com.arturo254.opentune.ui.screens.GalaxEditorialBackdrop
import com.arturo254.opentune.ui.screens.GalaxEditorialOrb

@Composable
fun GalaxSearchEditorialHero(
    query: String,
    count: Int,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier.fillMaxWidth().height(198.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF111B34),
                        MaterialTheme.colorScheme.primary.copy(alpha = .23f),
                        Color(0xFF070E1E),
                    ),
                ),
            ),
    ) {
        GalaxEditorialBackdrop(
            modifier = Modifier.fillMaxSize(),
            active = isLoading,
            density = 24,
            accent = Color(0xFFAACBFF),
            intensity = .65f,
        )
        GalaxEditorialOrb(
            modifier = Modifier.fillMaxSize(),
            active = isLoading,
            rings = 3,
            accent = Color(0xFFA0C6FF),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart)
                .fillMaxWidth().padding(21.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "BÚSQUEDA",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = .75f),
                letterSpacing = 2.1.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = query.ifBlank { "Explorar" },
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier.size(6.dp).clip(CircleShape)
                        .background(Color(0xFF9CCAFF)),
                )
                Text(
                    text = if (isLoading && count == 0) "Buscando"
                        else if (count == 1) "1 canción"
                        else count.toString() + " canciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = .76f),
                )
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
    val modes = listOf(
        AudioSelectionMode.AUTOMATIC to "Automática",
        AudioSelectionMode.BEST_QUALITY to "Máxima",
        AudioSelectionMode.DATA_SAVER to "Ahorro",
    )
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Calidad",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = when (mode) {
                    AudioSelectionMode.AUTOMATIC -> "Auto"
                    AudioSelectionMode.BEST_QUALITY -> "Alta"
                    AudioSelectionMode.DATA_SAVER -> "Ahorro"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
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
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
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
