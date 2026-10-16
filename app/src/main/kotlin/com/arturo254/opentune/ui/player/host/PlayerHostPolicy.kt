package com.arturo254.opentune.ui.player.host

import com.arturo254.opentune.ui.component.COLLAPSED_ANCHOR
import com.arturo254.opentune.ui.component.DISMISSED_ANCHOR
import com.arturo254.opentune.ui.component.EXPANDED_ANCHOR

/** Match the existing mini-player anchor priority (expanded > dismissed > collapsed). */
internal fun hostPlayerAnchor(isExpanded: Boolean, isDismissed: Boolean): Int =
    when {
        isExpanded -> EXPANDED_ANCHOR
        isDismissed -> DISMISSED_ANCHOR
        else -> COLLAPSED_ANCHOR
    }

/**
 * A sheet in motion is neither settled-expanded, settled-collapsed nor
 * settled-dismissed. Do not persist its transient midpoint as COLLAPSED:
 * that loses fullscreen/restored state during gestures and animations.
 */
internal fun settledHostPlayerAnchor(
    isExpanded: Boolean,
    isCollapsed: Boolean,
    isDismissed: Boolean,
): Int? = when {
    isExpanded -> EXPANDED_ANCHOR
    isDismissed -> DISMISSED_ANCHOR
    isCollapsed -> COLLAPSED_ANCHOR
    else -> null
}

/** Never overwrite the saved anchor while special full-screen routes own it. */
internal fun shouldPersistHostAnchor(
    isYearInMusicScreen: Boolean,
    isAlwaysOnDisplayScreen: Boolean,
    persistenceEnabled: Boolean,
): Boolean = !isYearInMusicScreen && !isAlwaysOnDisplayScreen && persistenceEnabled

internal enum class PlayerHostBackDestination { LYRICS, QUEUE, PLAYER, NONE }

/** All player overlays obey a single predictable Back priority. */
internal fun playerHostBackDestination(
    lyricsOpen: Boolean,
    queueOpen: Boolean,
    playerOpen: Boolean,
): PlayerHostBackDestination = when {
    lyricsOpen -> PlayerHostBackDestination.LYRICS
    queueOpen -> PlayerHostBackDestination.QUEUE
    playerOpen -> PlayerHostBackDestination.PLAYER
    else -> PlayerHostBackDestination.NONE
}
