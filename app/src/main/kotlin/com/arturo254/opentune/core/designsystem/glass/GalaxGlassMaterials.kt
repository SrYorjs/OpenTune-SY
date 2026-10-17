package com.arturo254.opentune.core.designsystem.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Material-only art direction for 4.5.5 and 4.5.6.
 *
 * The canonical GlassEngine owns clipping, shaders, backdrop and fallback.
 * CLASSIC and DISABLED intentionally return a null visual treatment, so
 * CLASSIC keeps its established Design System colors and optical effects.
 * Colors are sampled from the active Material 3 theme by the caller.
 */
@Immutable
data class GalaxGlassMaterial(
    val surfaceTint: Color?,
    val borderTint: Color?,
    val fallbackTint: Color? = null,
    val sheenTop: Color = Color.Transparent,
    val sheenBottom: Color = Color.Transparent,
    val rimTop: Color? = null,
    val rimBottom: Color? = null,
    val enableLens: Boolean = true,
    val enableVibrancy: Boolean = true,
    val enableStaticLens: Boolean = true,
    val highlightScale: Float = 1f,
)

/**
 * Independent optical material profiles. The fallback is deliberately
 * near-opaque for legibility when a real backdrop is unavailable.
 *
 * [accent] must be the current theme primary, never a baked-in brand color.
 */
fun galaxGlassMaterial(
    mode: GlassMode,
    dark: Boolean,
    accent: Color = Color.White,
    surfaceBase: Color = if (dark) Color.Black else Color.White,
): GalaxGlassMaterial = when (mode) {
    GlassMode.FROSTED -> GalaxGlassMaterial(
        surfaceTint = Color.White.copy(alpha = if (dark) 0.22f else 0.48f),
        borderTint = Color.White.copy(alpha = if (dark) 0.32f else 0.68f),
        fallbackTint = surfaceBase.copy(alpha = 0.94f),
        sheenTop = Color.White.copy(alpha = if (dark) 0.13f else 0.24f),
        sheenBottom = accent.copy(alpha = if (dark) 0.08f else 0.045f),
        rimTop = Color.White.copy(alpha = if (dark) 0.55f else 0.85f),
        rimBottom = accent.copy(alpha = if (dark) 0.20f else 0.13f),
        enableLens = false,
        enableVibrancy = false,
        enableStaticLens = false,
        highlightScale = 0.46f,
    )
    GlassMode.CLEAR -> GalaxGlassMaterial(
        surfaceTint = Color.White.copy(alpha = if (dark) 0.035f else 0.085f),
        borderTint = Color.White.copy(alpha = if (dark) 0.40f else 0.34f),
        fallbackTint = surfaceBase.copy(alpha = 0.93f),
        sheenTop = Color.White.copy(alpha = if (dark) 0.10f else 0.16f),
        sheenBottom = accent.copy(alpha = if (dark) 0.07f else 0.055f),
        rimTop = Color.White.copy(alpha = if (dark) 0.68f else 0.90f),
        rimBottom = accent.copy(alpha = if (dark) 0.29f else 0.24f),
        enableLens = true,
        enableVibrancy = true,
        enableStaticLens = true,
        highlightScale = 1.08f,
    )
    GlassMode.BLUR -> GalaxGlassMaterial(
        surfaceTint = Color.White.copy(alpha = if (dark) 0.14f else 0.30f),
        borderTint = Color.White.copy(alpha = if (dark) 0.16f else 0.33f),
        fallbackTint = surfaceBase.copy(alpha = 0.96f),
        sheenTop = Color.White.copy(alpha = if (dark) 0.045f else 0.07f),
        sheenBottom = accent.copy(alpha = 0.035f),
        rimTop = Color.White.copy(alpha = if (dark) 0.21f else 0.37f),
        rimBottom = accent.copy(alpha = 0.10f),
        enableLens = false,
        enableVibrancy = false,
        enableStaticLens = false,
        highlightScale = 0.22f,
    )
    GlassMode.CLASSIC, GlassMode.DISABLED -> GalaxGlassMaterial(
        surfaceTint = null,
        borderTint = null,
    )
}

/** Low tier is strictly a single tint and a solid border, without gradient layers. */
fun GalaxGlassMaterial.forPerformanceTier(tier: GlassPerformanceTier): GalaxGlassMaterial =
    if (tier != GlassPerformanceTier.LOW) this else copy(
        sheenTop = Color.Transparent,
        sheenBottom = Color.Transparent,
        rimTop = null,
        rimBottom = null,
    )
