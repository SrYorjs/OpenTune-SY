package com.arturo254.opentune.ui.player.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.arturo254.opentune.constants.PlayerBackgroundStyle
import com.arturo254.opentune.constants.PlayerBackgroundStyleKey
import com.arturo254.opentune.constants.PlayerButtonsStyle
import com.arturo254.opentune.constants.PlayerButtonsStyleKey
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.constants.PlayerDesignStyleKey
import com.arturo254.opentune.constants.SliderStyle
import com.arturo254.opentune.constants.SliderStyleKey
import com.arturo254.opentune.utils.rememberEnumPreference

/**
 * Player 2.0 presentation snapshot. Its enum values and DataStore keys are
 * identical to the legacy settings; there is no second persistence store.
 *
 * Do not confuse the selected *design* with a playback source or audio quality.
 */
@Immutable
data class PlayerPresentationPreferences(
    val designStyle: PlayerDesignStyle,
    val backgroundStyle: PlayerBackgroundStyle,
    val buttonStyle: PlayerButtonsStyle,
    val sliderStyle: SliderStyle,
)

/** Original defaults are intentionally preserved for upgrades and reinstalls. */
object PlayerPresentationDefaults {
    val designStyle: PlayerDesignStyle = PlayerDesignStyle.V8
    val backgroundStyle: PlayerBackgroundStyle = PlayerBackgroundStyle.DEFAULT
    val buttonStyle: PlayerButtonsStyle = PlayerButtonsStyle.DEFAULT
    val sliderStyle: SliderStyle = SliderStyle.Standard
}

/**
 * The same DataStore-backed preferences used by AppearanceSettings, read once
 * for the full Player and its MiniPlayer child. V1/V3 and all other previously
 * saved enum values remain valid and are never silently remapped to V8.
 */
@Composable
fun rememberPlayerPresentationPreferences(): PlayerPresentationPreferences {
    val designStyle by rememberEnumPreference(
        key = PlayerDesignStyleKey,
        defaultValue = PlayerPresentationDefaults.designStyle,
    )
    val backgroundStyle by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerPresentationDefaults.backgroundStyle,
    )
    val buttonStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerPresentationDefaults.buttonStyle,
    )
    val sliderStyle by rememberEnumPreference(
        key = SliderStyleKey,
        defaultValue = PlayerPresentationDefaults.sliderStyle,
    )
    return remember(designStyle, backgroundStyle, buttonStyle, sliderStyle) {
        PlayerPresentationPreferences(designStyle, backgroundStyle, buttonStyle, sliderStyle)
    }
}
