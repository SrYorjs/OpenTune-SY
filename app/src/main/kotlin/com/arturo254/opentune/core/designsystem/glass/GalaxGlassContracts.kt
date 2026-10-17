package com.arturo254.opentune.core.designsystem.glass

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * GalaxOT Glass Engine 4.5.4.
 *
 * Modes have independent optical profiles (4.5.5/4.5.6) while the global
 * persisted Appearance picker remains the responsibility of 4.5.7.
 * CLASSIC remains wired to the existing Kyant optical preset.
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
 * Distinct optical defaults for three new materials; CLASSIC retains STANDARD.
 * Actual native backdrop rendering/fallback remains the sole GlassEngine.
 */
fun galaxGlassOptics(mode: GlassMode): GlassOptics = when (mode) {
    GlassMode.FROSTED -> GlassOptics(
        blurRadius = 14, refraction = 0.030f, curve = 0.080f,
        dispersion = 0f, edge = 0.14f,
    )
    GlassMode.CLEAR -> GlassOptics(
        blurRadius = 2, refraction = 0.115f, curve = 0.21f,
        dispersion = 0.0045f, edge = 0.52f,
    )
    GlassMode.BLUR -> GlassOptics(
        blurRadius = 18, refraction = 0f, curve = 0f,
        dispersion = 0f, edge = 0.08f,
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
        // Low-power and scrolling surfaces must NOT allocate shader layers.
        return GlassRenderPlan(spec.mode, GlassRenderMode.STATIC_FALLBACK,
            tier, GlassFallbackStrategy.TINT_AND_OUTLINE, optics)
    }
    if (capabilities.hasLiveBackdrop) {
        val safeOptics = if (tier == GlassPerformanceTier.BALANCED) {
            optics.copy(blurRadius = optics.blurRadius.coerceAtMost(6), dispersion = 0f)
        } else optics
        return GlassRenderPlan(spec.mode, GlassRenderMode.LIVE_BACKDROP,
            tier, GlassFallbackStrategy.NONE, safeOptics)
    }
    return GlassRenderPlan(spec.mode, GlassRenderMode.STATIC_FALLBACK,
        tier, if (capabilities.hasRuntimeShader &&
            (spec.mode == GlassMode.CLASSIC || spec.mode == GlassMode.CLEAR))
            GlassFallbackStrategy.STATIC_SHADER
        else GlassFallbackStrategy.TINT_AND_OUTLINE, optics)
}


/**
 * Local preview mode has priority over the legacy boolean. Until Appearance
 * persists its own mode in 4.5.7, a null selection preserves the on/off switch.
 */
fun isGalaxGlassEnabled(legacyEnabled: Boolean, selectedMode: GlassMode?): Boolean =
    selectedMode?.let { it != GlassMode.DISABLED } ?: legacyEnabled

/**
 * Host presets (NAVIGATION/MINI_PLAYER) are CLASSIC-only compatibility optics.
 * When another mode is selected, do not let SOFT optics override its identity.
 * Explicit per-surface optics are preserved when no global override is present.
 */
fun resolveGalaxGlassSpec(
    spec: GlassSurfaceSpec,
    globalMode: GlassMode?,
): GlassSurfaceSpec = when {
    globalMode == null || globalMode == spec.mode -> spec
    else -> spec.copy(mode = globalMode, optics = null)
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
