package com.arturo254.opentune.ui.original.adapters
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.glass.LocalLiquidBackdrop
import com.arturo254.opentune.core.designsystem.glass.LocalSky
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidBackdrop
import com.skydoves.cloudy.rememberSky
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidEffect
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
/** Call inside each surface/window. Only background is recorded; consumers are sibling content. */
@Composable fun OriginalStudioGlassScope(
 background: @Composable BoxScope.() -> Unit,
 modifier: Modifier = Modifier,
 content: @Composable BoxScope.() -> Unit,
) {
 val backdrop = rememberLayerBackdrop()
 val sky = rememberSky()
 Box(modifier) {
  Box(Modifier.matchParentSize().layerBackdrop(backdrop), content = background)
  CompositionLocalProvider(LocalGalaxLiquidBackdrop provides backdrop, LocalLiquidBackdrop provides null, LocalSky provides sky) { content() }
 }
}
/** Inherits the selected Galax liquid variant/tuning; never records controls. */
@Composable fun StudioGlassBackground(
 state: com.arturo254.opentune.ui.original.component.GlassState,
 maxBlurRadius: () -> Float = {24f}, tint: Color = Color.Transparent,
 foundationColor: Color? = null,
 direction: com.arturo254.opentune.ui.original.component.BlurDirection = com.arturo254.opentune.ui.original.component.BlurDirection.TopToBottom,
 modifier: Modifier = Modifier, steps: Int = 5,
) { Box(modifier.galaxLiquidEffect(shapeOverride = RoundedCornerShape(0.dp))) }
