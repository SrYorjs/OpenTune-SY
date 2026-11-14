package com.arturo254.opentune.ui.original.adapters

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.arturo254.opentune.constants.*
import com.arturo254.opentune.core.designsystem.glass.*
import com.arturo254.opentune.core.designsystem.liquid.*
import com.arturo254.opentune.ui.component.LocalEnableLiquidGlass
import com.arturo254.opentune.ui.original.component.GlassState
import com.arturo254.opentune.ui.original.component.LocalOriginalGlassState
import com.arturo254.opentune.ui.original.component.glassRoot
import com.arturo254.opentune.utils.rememberPreference
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.arturo254.opentune.ui.component.LocalSky
import com.skydoves.cloudy.rememberSky

/** Each activity/dialog records only this background sibling, never its consuming content. */
@Composable
fun ActivityGlassHost(pureBlack: Boolean, content: @Composable () -> Unit) {
    val enabled by rememberPreference(LiquidGlassNavBarKey, false)
    val modeText by rememberPreference(GalaxGlassModeKey, "")
    val rawVariant by rememberPreference(GalaxLiquidVariantKey, "")
    val mode = parseGalaxGlassMode(modeText)
    val variant = parseGalaxLiquidVariant(rawVariant) ?: when (mode) {
        GlassMode.FROSTED -> GalaxLiquidVariant.PEARL
        GlassMode.CLEAR -> GalaxLiquidVariant.CRYSTAL
        GlassMode.BLUR -> GalaxLiquidVariant.GLACIER
        GlassMode.CLASSIC -> GalaxLiquidVariant.SILK
        GlassMode.DISABLED, null -> if (enabled) GalaxLiquidVariant.SILK else GalaxLiquidVariant.DISABLED
    }
    val recipe = galvanicRecipe(variant)
    val radius by rememberPreference(GalaxLiquidCornerRadiusKey, recipe.cornerRadiusDp)
    val blur by rememberPreference(GalaxLiquidBlurKey, recipe.blurRadiusDp)
    val height by rememberPreference(GalaxLiquidRefractionHeightKey, recipe.refractionHeight)
    val amount by rememberPreference(GalaxLiquidRefractionAmountKey, recipe.refractionAmount)
    val chromatic by rememberPreference(GalaxLiquidChromaticKey, recipe.chromaticAberration)
    val tint by rememberPreference(GalaxLiquidTintKey, recipe.tintAlpha)
    val tuning = if (rawVariant.isBlank()) recipe else GalaxLiquidTuning(radius, blur, height, amount, chromatic, tint).bounded()
    val context = LocalContext.current
    val saver = (context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager)?.isPowerSaveMode == true
    val lowRam = (context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager)?.isLowRamDevice == true
    val backdrop = if (variant != GalaxLiquidVariant.DISABLED && !saver && !lowRam && Build.VERSION.SDK_INT >= 33) rememberLayerBackdrop() else null
    val glass = remember { GlassState() }
    val sky = rememberSky()
    CompositionLocalProvider(
        LocalOriginalGlassState provides glass,
        LocalGalaxGlassMode provides mode,
        LocalGalaxLiquidVariant provides variant,
        LocalGalaxLiquidTuning provides tuning,
        LocalGalaxLiquidBackdrop provides backdrop,
        LocalLiquidBackdrop provides null,
        LocalSky provides sky,
        LocalEnableLiquidGlass provides (variant != GalaxLiquidVariant.DISABLED),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.matchParentSize().glassRoot(glass)
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface))
            content()
        }
    }
}
