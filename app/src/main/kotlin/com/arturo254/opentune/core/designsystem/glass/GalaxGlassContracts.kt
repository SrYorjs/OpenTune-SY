package com.arturo254.opentune.core.designsystem.glass

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.motion.galaxMotion

/**
 * GalaxOT Glass Engine 4.5.4.
 *
 * The concepts are routed now; their final optical treatments and the global
 * Appearance switch belong to 4.5.5–4.5.7. CLASSIC maps to the proven Kyant
 * renderer and the existing saved Liquid Glass toggle is left untouched.
 */
enum class GlassMode { FROSTED, CLEAR, BLUR, CLASSIC, DISABLED }

enum class GlassSurfaceType {
    NAVIGATION, MINI_PLAYER, PANEL, DIALOG, CONTROL, CARD, SCROLLING_ITEM
}

enum class GlassPerformanceTier { HIGH, BALANCED, LOW }

enum class GlassFallbackStrategy { NONE, STATIC_SHADER, TINT_AND_OUTLINE, OPAQUE }

@Immutable
data class GlassCapabilities(
    val sdkInt: Int,
    val hasLiveBackdrop: Boolean,
    val isPowerSaveMode: Boolean = false,
    val isLowRamDevice: Boolean = false,
) {
    val hasRuntimeShader: Boolean get() = sdkInt >= 33
    val hasRenderEffect: Boolean get() = sdkInt >= 31
}

/** Surface policy; optics are explicit and clipped by the renderer to safe bounds. */
@Immutable
data class GlassSurfaceSpec(
    val mode: GlassMode = GlassMode.CLASSIC,
    val surface: GlassSurfaceType = GlassSurfaceType.CARD,
    val optics: GlassOptics? = null,
    val preferHighQuality: Boolean = true,
)

@Immutable
data class GlassRenderPlan(
    val mode: GlassMode,
    val renderer: GlassRenderMode,
    val performanceTier: GlassPerformanceTier,
    val fallback: GlassFallbackStrategy,
    val optics: GlassOptics,
) {
    val hasExpensiveBackdrop: Boolean get() = renderer == GlassRenderMode.LIVE_BACKDROP
}

/**
 * Compatibility with existing GlassPreset: no new shaders are invented.
 * The three concept defaults establish constraints for later art-directed
 * treatments without pretending that the optical materials are complete.
 */
fun galaxGlassOptics(mode: GlassMode): GlassOptics = when (mode) {
    GlassMode.FROSTED -> GlassOptics(
        blurRadius = 10, refraction = 0.035f, curve = 0.085f,
        dispersion = 0.001f, edge = 0.13f,
    )
    GlassMode.CLEAR -> GlassOptics(
        blurRadius = 2, refraction = 0.085f, curve = 0.19f,
        dispersion = 0.003f, edge = 0.43f,
    )
    GlassMode.BLUR -> GlassOptics(
        blurRadius = 12, refraction = 0.025f, curve = 0.075f,
        dispersion = 0f, edge = 0.10f,
    )
    GlassMode.CLASSIC -> GlassPreset.STANDARD.optics
    GlassMode.DISABLED -> GlassOptics(blurRadius = 0, refraction = 0f, curve = 0f, dispersion = 0f, edge = 0f)
}

/** Pure, deterministic quality policy. Never infer GPU performance from SDK alone. */
fun chooseGlassPerformanceTier(
    capabilities: GlassCapabilities,
    surface: GlassSurfaceType,
    reducedMotion: Boolean,
    preferHighQuality: Boolean = true,
): GlassPerformanceTier = when {
    capabilities.isPowerSaveMode || capabilities.isLowRamDevice ||
        surface == GlassSurfaceType.SCROLLING_ITEM -> GlassPerformanceTier.LOW
    reducedMotion || !preferHighQuality || capabilities.sdkInt < 31 ->
        GlassPerformanceTier.BALANCED
    else -> GlassPerformanceTier.HIGH
}

/** Only a supplied Kyant Backdrop is evidence that a live backdrop exists. */
fun resolveGalaxGlassPlan(
    enabled: Boolean,
    spec: GlassSurfaceSpec,
    capabilities: GlassCapabilities,
    reducedMotion: Boolean = false,
): GlassRenderPlan {
    val optics = spec.optics ?: galaxGlassOptics(spec.mode)
    if (!enabled || spec.mode == GlassMode.DISABLED) {
        return GlassRenderPlan(spec.mode, GlassRenderMode.OPAQUE,
            GlassPerformanceTier.LOW, GlassFallbackStrategy.OPAQUE, optics)
    }
    val tier = chooseGlassPerformanceTier(
        capabilities, spec.surface, reducedMotion, spec.preferHighQuality,
    )
    if (tier == GlassPerformanceTier.LOW) {
        return GlassRenderPlan(spec.mode, GlassRenderMode.STATIC_FALLBACK,
            tier, if (capabilities.hasRuntimeShader) GlassFallbackStrategy.STATIC_SHADER
            else GlassFallbackStrategy.TINT_AND_OUTLINE, optics)
    }
    if (capabilities.hasLiveBackdrop) {
        val safeOptics = if (tier == GlassPerformanceTier.BALANCED) {
            optics.copy(blurRadius = optics.blurRadius.coerceAtMost(6), dispersion = 0f)
        } else optics
        return GlassRenderPlan(spec.mode, GlassRenderMode.LIVE_BACKDROP,
            tier, GlassFallbackStrategy.NONE, safeOptics)
    }
    return GlassRenderPlan(spec.mode, GlassRenderMode.STATIC_FALLBACK,
        tier, if (capabilities.hasRuntimeShader) GlassFallbackStrategy.STATIC_SHADER
        else GlassFallbackStrategy.TINT_AND_OUTLINE, optics)
}

/** Null means legacy on/off selection: the 4.5.7 five-way selector is not active yet. */
val LocalGalaxGlassMode = staticCompositionLocalOf<GlassMode?> { null }

@Composable
fun rememberGalaxGlassCapabilities(hasLiveBackdrop: Boolean): GlassCapabilities {
    val context = LocalContext.current
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    return GlassCapabilities(
        sdkInt = Build.VERSION.SDK_INT,
        hasLiveBackdrop = hasLiveBackdrop,
        isPowerSaveMode = powerManager?.isPowerSaveMode == true,
        isLowRamDevice = activityManager?.isLowRamDevice == true,
    )
}
