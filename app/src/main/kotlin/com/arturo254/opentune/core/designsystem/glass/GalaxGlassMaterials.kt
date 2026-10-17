package com.arturo254.opentune.core.designsystem.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Phase 4.5.5–4.5.6: five mode material contract on one renderer.
 * Classic retains the original theme-driven Liquid Glass colors.
 */
@Immutable
data class GalaxGlassMaterial(
    val surfaceTint: Color?,
    val borderTint: Color?,
)

fun galaxGlassMaterial(mode: GlassMode, dark: Boolean): GalaxGlassMaterial = when (mode) {
    GlassMode.FROSTED -> GalaxGlassMaterial(
        surfaceTint = Color.White.copy(alpha = if (dark) 0.20f else 0.42f),
        borderTint = Color.White.copy(alpha = if (dark) 0.29f else 0.62f),
    )
    GlassMode.CLEAR -> GalaxGlassMaterial(
        surfaceTint = Color.White.copy(alpha = if (dark) 0.055f else 0.10f),
        borderTint = Color.White.copy(alpha = if (dark) 0.48f else 0.38f),
    )
    GlassMode.BLUR -> GalaxGlassMaterial(
        surfaceTint = Color.White.copy(alpha = if (dark) 0.16f else 0.30f),
        borderTint = Color.White.copy(alpha = if (dark) 0.17f else 0.35f),
    )
    GlassMode.CLASSIC, GlassMode.DISABLED -> GalaxGlassMaterial(null, null)
}
