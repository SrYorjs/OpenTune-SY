package com.arturo254.opentune.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arturo254.opentune.core.designsystem.glass.GlassOptics
import com.arturo254.opentune.core.designsystem.glass.openTuneGlassEffect

/**
 * Compatibility API for existing call sites. The canonical optical backend is
 * core/designsystem/glass/GlassEngine.kt. These locals reference the same
 * CompositionLocal instances used by the new engine.
 */
val LocalSky = com.arturo254.opentune.core.designsystem.glass.LocalSky
val LocalEnableLiquidGlass = com.arturo254.opentune.core.designsystem.glass.LocalEnableLiquidGlass
val LocalLiquidBackdrop = com.arturo254.opentune.core.designsystem.glass.LocalLiquidBackdrop

@Composable
fun Modifier.liquidGlassSurface(
    shape: RoundedCornerShape,
    pureBlack: Boolean,
    blurRadius: Int = 6,
    refraction: Float = 0.06f,
    curve: Float = 0.14f,
    dispersion: Float = 0.003f,
    edge: Float = 0.20f,
): Modifier =
    openTuneGlassEffect(
        shape = shape,
        pureBlack = pureBlack,
        optics =
            GlassOptics(
                blurRadius = blurRadius,
                refraction = refraction,
                curve = curve,
                dispersion = dispersion,
                edge = edge,
            ),
    )
