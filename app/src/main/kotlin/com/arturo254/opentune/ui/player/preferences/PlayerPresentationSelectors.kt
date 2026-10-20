package com.arturo254.opentune.ui.player.preferences

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.PlayerBackgroundStyle
import com.arturo254.opentune.constants.PlayerButtonsStyle
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.ui.component.EnumListPreference

/**
 * 4.17: the existing AppearanceSettings DataStore owns writes. These three
 * selectors display the historic persisted enum values without translation,
 * renaming or automatic migration (including V1 and V3).
 */
@Composable
fun PlayerLayoutSelector(
    selected: PlayerDesignStyle,
    onSelected: (PlayerDesignStyle) -> Unit,
) {
    EnumListPreference(
        title = { Text(stringResource(R.string.player_design_style)) },
        icon = { Icon(painterResource(R.drawable.palette), null) },
        selectedValue = selected,
        onValueSelected = onSelected,
        valueText = { playerLayoutLabel(it) },
    )
}

@Composable
fun PlayerBackgroundSkinSelector(
    selected: PlayerBackgroundStyle,
    onSelected: (PlayerBackgroundStyle) -> Unit,
) {
    EnumListPreference(
        title = { Text(stringResource(R.string.player_background_style)) },
        icon = { Icon(painterResource(R.drawable.gradient), null) },
        selectedValue = selected,
        onValueSelected = onSelected,
        valueText = { playerBackgroundLabel(it) },
    )
}

@Composable
fun PlayerButtonsSkinSelector(
    selected: PlayerButtonsStyle,
    onSelected: (PlayerButtonsStyle) -> Unit,
) {
    EnumListPreference(
        title = { Text(stringResource(R.string.player_buttons_style)) },
        icon = { Icon(painterResource(R.drawable.palette), null) },
        selectedValue = selected,
        onValueSelected = onSelected,
        valueText = { playerButtonsLabel(it) },
    )
}

@Composable
fun playerLayoutLabel(style: PlayerDesignStyle): String = when (style) {
    PlayerDesignStyle.V1 -> stringResource(R.string.player_design_v1)
    PlayerDesignStyle.V2 -> stringResource(R.string.player_design_v2)
    PlayerDesignStyle.V3 -> stringResource(R.string.player_design_v3)
    PlayerDesignStyle.V4 -> stringResource(R.string.player_design_v4)
    PlayerDesignStyle.V5 -> stringResource(R.string.player_design_v5)
    PlayerDesignStyle.V6 -> stringResource(R.string.player_design_v6)
    PlayerDesignStyle.V7 -> stringResource(R.string.player_design_v7)
    PlayerDesignStyle.V8 -> stringResource(R.string.Apple_Music)
    PlayerDesignStyle.V9 -> "Galax Nebula Drift"
    PlayerDesignStyle.V10 -> "Galax Stellar Rings"
    PlayerDesignStyle.V11 -> "Galax Cybergrid"
    PlayerDesignStyle.V12 -> "Galax Vinyl Orbit"
    PlayerDesignStyle.V13 -> "Galax Aurora Flow"
    PlayerDesignStyle.V14 -> "Galax Monolith"
    PlayerDesignStyle.V15 -> "Galax Quantum Pulse"
    PlayerDesignStyle.V16 -> "GalaxTheme"
    PlayerDesignStyle.V17 -> "VinJax"
}

@Composable
fun playerBackgroundLabel(style: PlayerBackgroundStyle): String = when (style) {
    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
    PlayerBackgroundStyle.CUSTOM -> stringResource(R.string.custom)
    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
    PlayerBackgroundStyle.COLORING -> stringResource(R.string.coloring)
    PlayerBackgroundStyle.BLUR_GRADIENT -> stringResource(R.string.blur_gradient)
    PlayerBackgroundStyle.GLOW -> stringResource(R.string.glow)
    PlayerBackgroundStyle.GLOW_ANIMATED -> "Glow Animated"
}

@Composable
fun playerButtonsLabel(style: PlayerButtonsStyle): String = when (style) {
    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
    PlayerButtonsStyle.SECONDARY -> stringResource(R.string.secondary_color_style)
}
