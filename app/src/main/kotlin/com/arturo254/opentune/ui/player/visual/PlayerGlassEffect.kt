package com.arturo254.opentune.ui.player.visual

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arturo254.opentune.core.designsystem.glass.GlassPreset
import com.arturo254.opentune.core.designsystem.glass.openTuneGlassEffect

/**
 * Single Player-specific optical preset. Leaves clipping, opaque background,
 * borders and theme to each host; uses the canonical Design System engine.
 *
 * The SOFT preset matches the mini-player's existing optical constants.
 */
@Composable
fun Modifier.playerVisualGlassEffect(
    shape: RoundedCornerShape,
    pureBlack: Boolean,
): Modifier = openTuneGlassEffect(
    shape = shape,
    pureBlack = pureBlack,
    optics = GlassPreset.SOFT.optics,
)
