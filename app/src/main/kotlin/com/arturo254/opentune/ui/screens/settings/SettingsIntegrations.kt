package com.arturo254.opentune.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsIntegrationsSection(integrations: List<SettingsIntegrationAction>, modifier: Modifier = Modifier) {
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(integrations.size, key = { integrations[it].label }) {
            IntegrationPill(integrations[it])
        }
    }
}

@Composable
fun IntegrationPill(action: SettingsIntegrationAction, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = action.onClick, modifier = modifier.widthIn(min = 118.dp, max = 180.dp),
        shape = RoundedCornerShape(20.dp), color = colors.surfaceContainerLow,
        border = BorderStroke(.75.dp, colors.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(color = colors.primary.copy(alpha = .1f), shape = CircleShape) {
                Icon(action.icon, null, Modifier.padding(10.dp).size(22.dp), tint = colors.primary)
            }
            Text(action.label, style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
        }
    }
}
