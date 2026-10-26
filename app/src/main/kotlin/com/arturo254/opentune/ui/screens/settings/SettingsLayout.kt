package com.arturo254.opentune.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.arturo254.opentune.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowSizeClass
import com.arturo254.opentune.LocalPlayerAwareWindowInsets

enum class SettingsLayoutMode {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

@Composable
fun resolveLayoutMode(): SettingsLayoutMode {
    val windowInfo = currentWindowAdaptiveInfo().windowSizeClass
    return when {
        windowInfo.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) ->
            SettingsLayoutMode.EXPANDED
        windowInfo.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
            SettingsLayoutMode.MEDIUM
        else ->
            SettingsLayoutMode.COMPACT
    }
}

data class SettingsContentState(
    val quickActions: List<SettingsQuickAction>,
    val integrations: List<SettingsIntegrationAction>,
    val groups: List<SettingsGroup>,
    val internalGroup: SettingsGroup?,
    val showPermissionBanner: Boolean,
    val showUpdateBanner: Boolean,
    val latestVersion: String,
    val isSearchActive: Boolean,
    val hasSearchResults: Boolean,
    val onRequestPermission: () -> Unit,
    val onUpdateClick: () -> Unit,
    val onPlayerStudioClick: () -> Unit,
    val onLiquidGlassClick: () -> Unit,
    val onPaletteClick: () -> Unit,
    val onSearchClick: () -> Unit,
)

@Composable
fun AdaptiveSettingsLayout(
    state: SettingsContentState,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    topPadding: Dp = 0.dp,
) {
    val mode = resolveLayoutMode()
    val columns = if (mode == SettingsLayoutMode.COMPACT) 1 else 2
    val quickColumns = if (mode == SettingsLayoutMode.COMPACT) 2 else 4
    androidx.compose.foundation.layout.Box(modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier = Modifier.widthIn(max = 1040.dp).fillMaxSize().windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = topPadding + 22.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (!state.isSearchActive) {
                item(key = "profile", contentType = "profile") { SettingsProfileHeader() }
                item(key = "search", contentType = "search") {
                    AsterSettingsSearchLauncher(state.onSearchClick)
                }
                item(key = "galaxCreativeShortcuts", contentType = "actions") {
                    GalaxSettingsCreativeShortcuts(
                        onPlayerStudio = state.onPlayerStudioClick,
                        onLiquidGlass = state.onLiquidGlassClick,
                        onPalette = state.onPaletteClick,
                    )
                }
                if (state.showPermissionBanner) item(key = "permission", contentType = "banner") {
                    SettingsPermissionBanner(onRequestPermission = state.onRequestPermission)
                }
                if (state.showUpdateBanner) item(key = "update", contentType = "banner") {
                    SettingsUpdateBanner(latestVersion = state.latestVersion, onClick = state.onUpdateClick)
                }
            }
            if (state.quickActions.isNotEmpty()) item(key = "quickActions", contentType = "actions") {
                Column {
                    AsterSettingsHeading(stringResource(R.string.aster_settings_essentials))
                    SettingsQuickActionsSection(actions = state.quickActions, columns = quickColumns)
                }
            }
            if (state.integrations.isNotEmpty()) item(key = "integrations", contentType = "integrations") {
                Column {
                    AsterSettingsHeading(stringResource(R.string.aster_settings_connections))
                    SettingsIntegrationsSection(integrations = state.integrations)
                }
            }
            if (state.isSearchActive && !state.hasSearchResults) {
                item(key = "empty", contentType = "empty") { SettingsSearchEmpty() }
            } else {
                state.internalGroup?.takeIf { it.items.isNotEmpty() }?.let { group ->
                    item(key = "internalSearchResults", contentType = "group") { SettingsGroupCard(group = group) }
                }
                val rows = state.groups.chunked(columns)
                items(rows.size, key = { rows[it].joinToString { group -> group.title } }, contentType = { "groups" }) { index ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        rows[index].forEach { group -> SettingsGroupCard(group = group, modifier = Modifier.weight(1f)) }
                        if (rows[index].size < columns) Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (!state.isSearchActive) item(key = "footer", contentType = "footer") {
                Text(
                    stringResource(R.string.aster_settings_footer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }
}
