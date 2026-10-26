package com.arturo254.opentune.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.BuildConfig
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.component.OpenTuneEmptyPresentation
import com.arturo254.opentune.core.designsystem.component.OpenTuneEmptyState

@Composable
fun SettingsProfileHeader(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("ASTER", color = colors.primary, letterSpacing = 3.sp,
                fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f))
            Surface(color = colors.surfaceContainerHigh, shape = CircleShape,
                border = BorderStroke(.75.dp, colors.outlineVariant)) {
                Text("v" + BuildConfig.VERSION_NAME, style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.aster_settings_title), fontSize = 44.sp, lineHeight = 48.sp,
                    letterSpacing = (-2).sp, fontWeight = FontWeight.Light, color = colors.onSurface)
                Text(stringResource(R.string.aster_settings_hub_description),
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            Box(Modifier.size(62.dp).background(
                Brush.linearGradient(listOf(colors.primary.copy(alpha = .16f), colors.secondary.copy(alpha = .07f))),
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 8.dp)),
                contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.settings), null, Modifier.size(28.dp), tint = colors.primary)
            }
        }
    }
}

@Composable
fun AsterSettingsSearchLauncher(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        color = colors.surfaceContainerLow, border = BorderStroke(.75.dp, colors.outlineVariant)) {
        Row(Modifier.padding(17.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.search), null, Modifier.size(22.dp), tint = colors.primary)
            Text(stringResource(R.string.aster_settings_search_hint), Modifier.weight(1f),
                color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Icon(painterResource(R.drawable.arrow_forward), null, Modifier.size(18.dp), tint = colors.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsPermissionBanner(onRequestPermission: () -> Unit, modifier: Modifier = Modifier) {
    AsterSettingsPanel(modifier) {
        AsterPreferenceRow(
            title = { Text(stringResource(R.string.permissions_title)) },
            supporting = { Text(stringResource(R.string.permissions_subtitle)) },
            icon = { Icon(painterResource(R.drawable.security), null) },
            grouped = true,
        )
        FilledTonalButton(onClick = onRequestPermission,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
            Text(stringResource(R.string.allow))
        }
    }
}

@Composable
fun SettingsUpdateBanner(latestVersion: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(painterResource(R.drawable.update), null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.new_version_available), style = MaterialTheme.typography.titleSmall)
                Text("v" + latestVersion, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(painterResource(R.drawable.arrow_forward), null, Modifier.size(20.dp))
        }
    }
}

@Composable
fun SettingsSearchEmpty(modifier: Modifier = Modifier) {
    OpenTuneEmptyState(
        icon = R.drawable.search,
        text = stringResource(R.string.no_results_found),
        supportingText = stringResource(R.string.search_try_different),
        presentation = OpenTuneEmptyPresentation.SETTINGS,
        settingsCardShape = RoundedCornerShape(SettingsDimensions.GroupCardCornerRadius),
        modifier = modifier,
    )
}

@Composable
fun SettingsGroupCard(group: SettingsGroup, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        AsterSettingsHeading(group.title, trailing = group.items.size.toString().padStart(2, '0'))
        AsterSettingsPanel {
            group.items.forEachIndexed { index, item ->
                SettingsRow(item, showDivider = index < group.items.lastIndex)
            }
        }
    }
}

@Composable
fun SettingsRow(item: SettingsItem, showDivider: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val accent = if (item.accentColor.isSpecified) item.accentColor else colors.primary
    Column(modifier) {
        AsterPreferenceRow(
            title = { Text(item.title) },
            supporting = item.subtitle?.let { { Text(it) } },
            icon = { Icon(item.icon, null, tint = accent) },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.showUpdateIndicator) Box(Modifier.size(6.dp).background(colors.error, CircleShape))
                    item.badge?.let {
                        Surface(color = colors.surfaceContainerHighest, shape = CircleShape) {
                            Text(it, style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    Icon(painterResource(R.drawable.navigate_next), null, Modifier.size(18.dp),
                        tint = colors.onSurfaceVariant.copy(alpha = .6f))
                }
            },
            grouped = true,
            onClick = item.onClick,
        )
        if (showDivider) HorizontalDivider(modifier = Modifier.padding(start = 66.dp, end = 16.dp),
            color = colors.outlineVariant.copy(alpha = .6f), thickness = .5.dp)
    }
}
