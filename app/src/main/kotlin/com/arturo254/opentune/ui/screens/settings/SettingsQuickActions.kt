package com.arturo254.opentune.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.ui.screens.drawAsterGlyph

@Composable
fun SettingsQuickActionsSection(
    actions: List<SettingsQuickAction>,
    columns: Int = 2,
    modifier: Modifier = Modifier,
) {
    val count = columns.coerceAtLeast(1)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        actions.chunked(count).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { QuickActionCard(it, Modifier.weight(1f)) }
                repeat(count - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun QuickActionCard(action: SettingsQuickAction, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val phase = LocalAsterSettingsPhase.current
    val kind = action.label.hashCode().and(Int.MAX_VALUE) % 4
    Surface(onClick = action.onClick, modifier = modifier.heightIn(min = 126.dp),
        shape = RoundedCornerShape(22.dp), color = colors.surfaceContainerLow,
        border = BorderStroke(.75.dp, colors.outlineVariant)) {
        Box(Modifier.background(Brush.linearGradient(listOf(action.accentColor.copy(alpha = .09f), Color.Transparent)))) {
            Canvas(Modifier.matchParentSize()) { drawAsterGlyph(kind, action.accentColor.copy(alpha = .5f), phase.value) }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    Icon(action.icon, null, Modifier.size(25.dp), tint = action.accentColor)
                }
                Text(action.label, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            }
        }
    }
}
