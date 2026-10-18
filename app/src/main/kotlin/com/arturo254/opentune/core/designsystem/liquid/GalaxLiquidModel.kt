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
/**
 * The first ten recipes now occupy visibly different optical ranges.
 * In particular, the old single-family .1-.3 refraction range made several
 * styles indistinguishable on real navigation imagery.
 */
fun galvanicRecipe(variant: GalaxLiquidVariant): GalaxLiquidTuning = when (variant) {
    GalaxLiquidVariant.SILK -> GalaxLiquidTuning(40f, 3f, .22f, .20f, 0f, .07f)
    GalaxLiquidVariant.CRYSTAL -> GalaxLiquidTuning(48f, 0.5f, .48f, .64f, 1f, .025f)
    GalaxLiquidVariant.AURORA -> GalaxLiquidTuning(34f, 9f, .40f, .48f, 1f, .20f)
    GalaxLiquidVariant.DEEP_SEA -> GalaxLiquidTuning(44f, 19f, .55f, .42f, 0f, .38f)
    GalaxLiquidVariant.PEARL -> GalaxLiquidTuning(32f, 24f, .10f, .12f, 0f, .34f)
    GalaxLiquidVariant.PRISM -> GalaxLiquidTuning(36f, 1f, .75f, .90f, 1f, .025f)
    GalaxLiquidVariant.ZERO_GRAVITY -> GalaxLiquidTuning(64f, 0f, .65f, .42f, 0f, .02f)
    GalaxLiquidVariant.GLACIER -> GalaxLiquidTuning(26f, 30f, .16f, .10f, 0f, .36f)
    GalaxLiquidVariant.NEBULA -> GalaxLiquidTuning(52f, 13f, .48f, .52f, 1f, .25f)
    GalaxLiquidVariant.MIRROR -> GalaxLiquidTuning(20f, 0f, .85f, .96f, 1f, .005f)
    // Official Kyant demo Button: blur 2dp, refraction 12dp/24dp.
    GalaxLiquidVariant.KYANT_BUTTON -> GalaxLiquidTuning(48f, 2f, .32f, .36f, 0f, .05f)
    // Toggle/Slider: interactive thumb lens with dynamic optical intensity.
    GalaxLiquidVariant.KYANT_TOGGLE -> GalaxLiquidTuning(28f, 8f, .22f, .30f, 1f, .10f)
    GalaxLiquidVariant.KYANT_SLIDER -> GalaxLiquidTuning(28f, 8f, .28f, .32f, 1f, .08f)
    // Official Kyant BottomTabs: blur 8dp, refraction 24dp/24dp capsule.
    GalaxLiquidVariant.KYANT_TABS -> GalaxLiquidTuning(52f, 8f, .46f, .40f, 0f, .06f)
    GalaxLiquidVariant.DISABLED -> GalaxLiquidTuning(28f, 0f, 0f, 0f, 0f, 1f)
}

/** Distinct highlights, tint palettes and depth treatments, not just slider presets. */
@Immutable
data class GalaxLiquidFinish(
    val lightStyle: Int = 0, // 0 ambient, 1 crisp, 2 plain
    val rimAlpha: Float = .60f,
    val shadowAlpha: Float = .10f,
    val colorStyle: Int = 0, // 0 transparent white, 1 ocean blue, 2 aurora, 3 violet
    val vibrancy: Boolean = true,
    val depthEffect: Boolean = true,
)

fun galvanicFinish(variant: GalaxLiquidVariant): GalaxLiquidFinish = when (variant) {
    GalaxLiquidVariant.SILK -> GalaxLiquidFinish(0, .46f, .06f, 0)
    GalaxLiquidVariant.CRYSTAL -> GalaxLiquidFinish(1, .96f, .18f, 0)
    GalaxLiquidVariant.AURORA -> GalaxLiquidFinish(0, .78f, .19f, 2)
    GalaxLiquidVariant.DEEP_SEA -> GalaxLiquidFinish(0, .30f, .22f, 1, false)
    GalaxLiquidVariant.PEARL -> GalaxLiquidFinish(2, .35f, .10f, 0, false, false)
    GalaxLiquidVariant.PRISM -> GalaxLiquidFinish(1, 1f, .21f, 3)
    GalaxLiquidVariant.ZERO_GRAVITY -> GalaxLiquidFinish(1, .64f, .05f, 0, true, false)
    GalaxLiquidVariant.GLACIER -> GalaxLiquidFinish(2, .51f, .18f, 1, false, false)
    GalaxLiquidVariant.NEBULA -> GalaxLiquidFinish(0, .90f, .24f, 3)
    GalaxLiquidVariant.MIRROR -> GalaxLiquidFinish(1, 1f, .27f, 0)
    GalaxLiquidVariant.KYANT_BUTTON -> GalaxLiquidFinish(0, .82f, .08f, 0)
    GalaxLiquidVariant.KYANT_TOGGLE -> GalaxLiquidFinish(0, .75f, .08f, 0)
    GalaxLiquidVariant.KYANT_SLIDER -> GalaxLiquidFinish(0, .73f, .07f, 0)
    GalaxLiquidVariant.KYANT_TABS -> GalaxLiquidFinish(1, .82f, .12f, 0)
    GalaxLiquidVariant.DISABLED -> GalaxLiquidFinish(2, .08f, 0f, 0, false, false)
}

fun parseGalaxLiquidVariant(raw: String?): GalaxLiquidVariant? =
    GalaxLiquidVariant.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }

val LocalGalaxLiquidVariant = staticCompositionLocalOf { GalaxLiquidVariant.DISABLED }
val LocalGalaxLiquidBackdrop = staticCompositionLocalOf<Backdrop?> { null }
val LocalGalaxLiquidTuning = staticCompositionLocalOf<GalaxLiquidTuning?> { null }

/** Canvas video is composited via TextureView OUTSIDE Kyant's captured layer.
 * Its controls must remain transparent to show the moving frame below.
 * Sampling the static cover would freeze the glass image while Canvas moves. */
val LocalGalaxCanvasLive = staticCompositionLocalOf { false }
