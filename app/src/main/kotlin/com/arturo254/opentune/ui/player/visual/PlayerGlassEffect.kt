package com.arturo254.opentune.ui.player.visual

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arturo254.opentune.core.designsystem.glass.GlassPreset
import com.arturo254.opentune.core.designsystem.glass.GlassMode
import com.arturo254.opentune.core.designsystem.glass.GlassSurfaceSpec
import com.arturo254.opentune.core.designsystem.glass.GlassSurfaceType
import com.arturo254.opentune.core.designsystem.glass.galaxGlassEffect

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
): Modifier = galaxGlassEffect(
    shape = shape,
    pureBlack = pureBlack,
    spec = GlassSurfaceSpec(
        mode = GlassMode.CLASSIC,
        surface = GlassSurfaceType.MINI_PLAYER,
        optics = GlassPreset.SOFT.optics,
    ),
)
