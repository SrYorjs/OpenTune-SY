package com.arturo254.opentune.core.designsystem.liquid

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.kyant.backdrop.Backdrop

/**
 * New 4.5 Liquid Glass identities. All effect equations and parameters are new;
 * no legacy GlassPreset, GlassOptics or GlassEngine is used by this renderer.
 *
 * The last four profiles are inspired by Kyant's Button, Toggle, Slider and
 * BottomTabs demos. We depend on the upstream Backdrop library, not on its
 * application-only catalog classes.
 */
enum class GalaxLiquidVariant(val title: String) {
    SILK("01 · Silk Water"),
    CRYSTAL("02 · Crystal Flow"),
    AURORA("03 · Aurora Glass"),
    DEEP_SEA("04 · Deep Sea"),
    PEARL("05 · Pearl Frost"),
    PRISM("06 · Prism Edge"),
    ZERO_GRAVITY("07 · Zero Gravity"),
    GLACIER("08 · Glacier"),
    NEBULA("09 · Nebula Fluid"),
    MIRROR("10 · Mirror Liquid"),
    KYANT_BUTTON("11 · Kyant Button"),
    KYANT_TOGGLE("12 · Kyant Toggle"),
    KYANT_SLIDER("13 · Kyant Slider"),
    KYANT_TABS("14 · Kyant Bottom Tabs"),
    DISABLED("Desactivado"),
}

@Immutable
data class GalaxLiquidTuning(
    val cornerRadiusDp: Float = 28f,
    val blurRadiusDp: Float = 8f,
    val refractionHeight: Float = 0.22f,
    val refractionAmount: Float = 0.16f,
    val chromaticAberration: Float = 0.0f,
    val tintAlpha: Float = 0.17f,
) {
    fun bounded() = copy(
        cornerRadiusDp = cornerRadiusDp.coerceIn(0f, 64f),
        blurRadiusDp = blurRadiusDp.coerceIn(0f, 32f),
        refractionHeight = refractionHeight.coerceIn(0f, 1f),
        refractionAmount = refractionAmount.coerceIn(0f, 1f),
        chromaticAberration = chromaticAberration.coerceIn(0f, 1f),
        tintAlpha = tintAlpha.coerceIn(0f, 0.65f),
    )
}

/**
 * Original presets deliberately differ in opacity, blur, lens depth, shape
 * and spectral edge. Kyant-inspired presets use the reference demo optics.
 */
fun galvanicRecipe(variant: GalaxLiquidVariant): GalaxLiquidTuning = when (variant) {
    GalaxLiquidVariant.SILK -> GalaxLiquidTuning(32f, 5f, .16f, .12f, 0f, .13f)
    GalaxLiquidVariant.CRYSTAL -> GalaxLiquidTuning(36f, 2f, .24f, .28f, .15f, .07f)
    GalaxLiquidVariant.AURORA -> GalaxLiquidTuning(34f, 8f, .20f, .17f, .50f, .18f)
    GalaxLiquidVariant.DEEP_SEA -> GalaxLiquidTuning(38f, 16f, .28f, .13f, 0f, .23f)
    GalaxLiquidVariant.PEARL -> GalaxLiquidTuning(28f, 22f, .10f, .07f, 0f, .27f)
    GalaxLiquidVariant.PRISM -> GalaxLiquidTuning(30f, 4f, .34f, .32f, 1f, .09f)
    GalaxLiquidVariant.ZERO_GRAVITY -> GalaxLiquidTuning(42f, 1.5f, .40f, .24f, .20f, .05f)
    GalaxLiquidVariant.GLACIER -> GalaxLiquidTuning(26f, 26f, .15f, .10f, .05f, .31f)
    GalaxLiquidVariant.NEBULA -> GalaxLiquidTuning(40f, 11f, .29f, .19f, .65f, .16f)
    GalaxLiquidVariant.MIRROR -> GalaxLiquidTuning(24f, 0f, .48f, .40f, .22f, .04f)
    // Kyant official samples: LiquidButton: blur(2dp), lens(12dp,24dp).
    GalaxLiquidVariant.KYANT_BUTTON -> GalaxLiquidTuning(48f, 2f, .24f, .20f, 0f, .12f)
    // LiquidToggle/Slider use a changing 8dp blur and chromatic thumb lens.
    GalaxLiquidVariant.KYANT_TOGGLE -> GalaxLiquidTuning(32f, 8f, .15f, .22f, 1f, .20f)
    GalaxLiquidVariant.KYANT_SLIDER -> GalaxLiquidTuning(32f, 8f, .20f, .24f, 1f, .17f)
    // LiquidBottomTabs: blur(8dp), lens(24dp,24dp) on capsule.
    GalaxLiquidVariant.KYANT_TABS -> GalaxLiquidTuning(34f, 8f, .26f, .24f, .20f, .15f)
    GalaxLiquidVariant.DISABLED -> GalaxLiquidTuning(28f, 0f, 0f, 0f, 0f, 1f)
}

fun parseGalaxLiquidVariant(raw: String?): GalaxLiquidVariant? =
    GalaxLiquidVariant.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }

val LocalGalaxLiquidVariant = staticCompositionLocalOf { GalaxLiquidVariant.DISABLED }
val LocalGalaxLiquidBackdrop = staticCompositionLocalOf<Backdrop?> { null }
val LocalGalaxLiquidTuning = staticCompositionLocalOf<GalaxLiquidTuning?> { null }
