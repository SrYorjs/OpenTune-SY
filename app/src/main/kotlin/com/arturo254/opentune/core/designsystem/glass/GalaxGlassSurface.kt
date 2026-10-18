package com.arturo254.opentune.core.designsystem.glass

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.theme.shapes
import com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidSurface
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidEffect

/**
 * Compatibility facade only. None of the previous 4.5.4–4.5.7 optics or
 * fallback algorithms are executed here. The actual implementation is the
 * independently written GalaxLiquidRenderer in core/designsystem/liquid.
 *
 * Source capture and effect consumers belong to disjoint composition scopes.
 */
@Composable
fun Modifier.galaxGlassEffect(
    shape: RoundedCornerShape,
    pureBlack: Boolean,
    @Suppress("UNUSED_PARAMETER") spec: GlassSurfaceSpec = GlassSurfaceSpec(),
): Modifier = galaxLiquidEffect(pureBlack = pureBlack, shapeOverride = shape)

@Composable
fun GalaxGlassSurface(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") shape: RoundedCornerShape = OpenTuneDesignSystem.shapes.glassCard,
    pureBlack: Boolean = false,
    @Suppress("UNUSED_PARAMETER") spec: GlassSurfaceSpec = GlassSurfaceSpec(),
    @Suppress("UNUSED_PARAMETER") opaqueColor: Color = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable BoxScope.() -> Unit,
) {
    GalaxLiquidSurface(modifier = modifier, pureBlack = pureBlack, content = content)
}
