package com.arturo254.opentune.ui.player.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.arturo254.opentune.constants.PlayerDesignStyle

/**
 * Player 2.0 central layout choice: style is the persisted V1–V8 preference;
 * family groups styles with the SAME existing outer composition.
 *
 * No PlayerConnection, ViewModel, playback action or state subscriptions here.
 */
enum class PlayerLayoutFamily {
    CLASSIC,    // V1, V2, V3, V4, V6
    COMPACT,    // V5
    CINEMATIC,  // V7
    IMMERSIVE,  // V8
}

enum class PlayerLayoutOrientation { PORTRAIT, LANDSCAPE }

@Immutable
data class PlayerLayoutRoute(
    val style: PlayerDesignStyle,
    val family: PlayerLayoutFamily,
    val orientation: PlayerLayoutOrientation,
) {
    /** Exactly the original shared background guard (not V5, not V7). */
    val hasSharedBackdrop: Boolean
        get() = family != PlayerLayoutFamily.COMPACT && family != PlayerLayoutFamily.CINEMATIC
}

fun resolvePlayerLayout(style: PlayerDesignStyle, isLandscape: Boolean): PlayerLayoutRoute {
    val family = when (style) {
        PlayerDesignStyle.V1,
        PlayerDesignStyle.V2,
        PlayerDesignStyle.V3,
        PlayerDesignStyle.V4,
        PlayerDesignStyle.V6 -> PlayerLayoutFamily.CLASSIC
        PlayerDesignStyle.V5 -> PlayerLayoutFamily.COMPACT
        PlayerDesignStyle.V7 -> PlayerLayoutFamily.CINEMATIC
        PlayerDesignStyle.V8 -> PlayerLayoutFamily.IMMERSIVE
    }
    return PlayerLayoutRoute(
        style = style,
        family = family,
        orientation =
            if (isLandscape) PlayerLayoutOrientation.LANDSCAPE
            else PlayerLayoutOrientation.PORTRAIT,
    )
}

/**
 * Single selectable visual structure for every version/orientation.
 * Content slots preserve all existing behavior, pointer input and animations.
 * This composable deliberately adds no Box, padding, window insets or surfaces.
 *
 * Migration to dedicated layout implementations is planned for 4.13–4.16.
 */
@Composable
fun PlayerLayout(
    style: PlayerDesignStyle,
    isLandscape: Boolean,
    classicPortrait: @Composable () -> Unit,
    classicLandscape: @Composable () -> Unit,
    compactPortrait: @Composable () -> Unit,
    compactLandscape: @Composable () -> Unit,
    cinematicPortrait: @Composable () -> Unit,
    cinematicLandscape: @Composable () -> Unit,
    immersivePortrait: @Composable () -> Unit,
    immersiveLandscape: @Composable () -> Unit,
) {
    val route = remember(style, isLandscape) { resolvePlayerLayout(style, isLandscape) }
    when (route.orientation) {
        PlayerLayoutOrientation.PORTRAIT ->
            when (route.family) {
                PlayerLayoutFamily.CLASSIC -> classicPortrait()
                PlayerLayoutFamily.COMPACT -> compactPortrait()
                PlayerLayoutFamily.CINEMATIC -> cinematicPortrait()
                PlayerLayoutFamily.IMMERSIVE -> immersivePortrait()
            }
        PlayerLayoutOrientation.LANDSCAPE ->
            when (route.family) {
                PlayerLayoutFamily.CLASSIC -> classicLandscape()
                PlayerLayoutFamily.COMPACT -> compactLandscape()
                PlayerLayoutFamily.CINEMATIC -> cinematicLandscape()
                PlayerLayoutFamily.IMMERSIVE -> immersiveLandscape()
            }
    }
}
