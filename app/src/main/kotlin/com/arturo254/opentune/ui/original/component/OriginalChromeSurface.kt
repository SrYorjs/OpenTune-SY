package com.arturo254.opentune.ui.original.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidVariant
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidVariant
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidEffect

val LocalOriginalGlassState = compositionLocalOf<GlassState?> { null }
val LocalDetailAccentColor = compositionLocalOf<MutableState<Color?>> { mutableStateOf(null) }
private val LocalOriginalChromeShape = staticCompositionLocalOf { RoundedCornerShape(50) }

@Composable
internal fun OriginalLiquidChrome(pureBlack: Boolean, modifier: Modifier): Boolean {
    val variant = LocalGalaxLiquidVariant.current
    if (variant == GalaxLiquidVariant.DISABLED) return false
    Box(
        modifier.galaxLiquidEffect(
            pureBlack = pureBlack,
            shapeOverride = LocalOriginalChromeShape.current,
        ),
    )
    return true
}

@Composable
fun OriginalChromeSurface(
    shape: RoundedCornerShape,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = LocalOriginalGlassState.current ?: remember { GlassState() }
    val accentWash = rememberChromeAccentWash()
    val nativeFrost = LocalGalaxLiquidVariant.current == GalaxLiquidVariant.DISABLED
    CompositionLocalProvider(LocalOriginalChromeShape provides shape) {
        Box(
            modifier
                .then(if (nativeFrost) Modifier.dropShadow(shape, ChromeShadow) else Modifier)
                .clip(shape)
                .drawWithContent {
                    drawContent()
                    if (nativeFrost) {
                        val outline = shape.createOutline(size, layoutDirection, this) as? Outline.Rounded
                        if (outline != null) drawChromeOutline(Offset.Zero, size, outline.roundRect.topLeftCornerRadius.x)
                    }
                },
        ) {
            FrostedChromeGlass(
                state = state,
                pureBlack = pureBlack,
                accentWash = accentWash,
                modifier = Modifier.matchParentSize(),
            )
            content()
        }
    }
}
