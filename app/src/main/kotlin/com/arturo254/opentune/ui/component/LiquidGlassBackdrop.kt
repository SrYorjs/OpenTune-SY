package com.arturo254.opentune.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidEffect

/**
 * Compatibility API for existing call sites. The canonical optical backend is
 * core/designsystem/glass/GlassEngine.kt. These locals reference the same
 * CompositionLocal instances used by the new engine.
 */
val LocalSky = com.arturo254.opentune.core.designsystem.glass.LocalSky
val LocalEnableLiquidGlass = com.arturo254.opentune.core.designsystem.glass.LocalEnableLiquidGlass
val LocalLiquidBackdrop = com.arturo254.opentune.core.designsystem.glass.LocalLiquidBackdrop

/**
 * Source-compatible wrapper for older call sites. All liquid buttons, pill
 * chips and control clusters now use the current selected Galax renderer.
 * Legacy optical numbers are intentionally ignored: the user owns all values
 * in Glass Studio, which are shared via LocalGalaxLiquidTuning.
 */
@Composable
fun Modifier.liquidGlassSurface(
    shape: RoundedCornerShape,
    pureBlack: Boolean,
    @Suppress("UNUSED_PARAMETER") blurRadius: Int = 6,
    @Suppress("UNUSED_PARAMETER") refraction: Float = 0.06f,
    @Suppress("UNUSED_PARAMETER") curve: Float = 0.14f,
    @Suppress("UNUSED_PARAMETER") dispersion: Float = 0.003f,
    @Suppress("UNUSED_PARAMETER") edge: Float = 0.20f,
): Modifier = galaxLiquidEffect(shapeOverride = shape, pureBlack = pureBlack)
