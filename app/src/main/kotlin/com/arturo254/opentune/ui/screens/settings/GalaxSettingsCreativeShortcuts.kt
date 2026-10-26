package com.arturo254.opentune.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.screens.drawAsterGlyph

@Composable
fun GalaxSettingsCreativeShortcuts(
    onPlayerStudio: () -> Unit,
    onLiquidGlass: () -> Unit,
    onPalette: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        AsterSettingsHeading(stringResource(R.string.aster_settings_creative), trailing = "03")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item("player") {
                AsterCreativeTile(
                    title = stringResource(R.string.aster_settings_studio),
                    subtitle = stringResource(R.string.aster_settings_studio_detail),
                    kind = 1, tint = MaterialTheme.colorScheme.primary,
                    onClick = onPlayerStudio,
                )
            }
            item("glass") {
                AsterCreativeTile(
                    title = stringResource(R.string.aster_settings_glass),
                    subtitle = stringResource(R.string.aster_settings_glass_detail),
                    kind = 0, tint = MaterialTheme.colorScheme.secondary,
                    onClick = onLiquidGlass,
                )
            }
            item("palette") {
                AsterCreativeTile(
                    title = stringResource(R.string.aster_settings_palette),
                    subtitle = stringResource(R.string.aster_settings_palette_detail),
                    kind = 2, tint = MaterialTheme.colorScheme.tertiary,
                    onClick = onPalette,
                )
            }
        }
    }
}

@Composable
private fun AsterCreativeTile(title: String, subtitle: String, kind: Int, tint: Color, onClick: () -> Unit) {
    val phase = LocalAsterSettingsPhase.current
    val scheme = MaterialTheme.colorScheme
    Surface(onClick = onClick, modifier = Modifier.width(210.dp), shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerLow, border = BorderStroke(.75.dp, scheme.outlineVariant)) {
        Column {
            Box(Modifier.fillMaxWidth().height(104.dp)
                .background(Brush.verticalGradient(listOf(tint.copy(alpha = .1f), Color.Transparent)))) {
                Canvas(Modifier.matchParentSize()) { drawAsterGlyph(kind, tint, phase.value) }
            }
            Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}
