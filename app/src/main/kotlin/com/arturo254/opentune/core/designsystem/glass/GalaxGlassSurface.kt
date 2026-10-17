package com.arturo254.opentune.core.designsystem.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import com.arturo254.opentune.core.designsystem.theme.shapes

/**
 * 4.5.6 bridge: the existing Kyant / Sky optical renderer remains the single
 * implementation. This façade supplies bounded capabilities and mode-specific
 * parameters, without capturing Android views or adding another blur pass.
 */
@Composable
fun Modifier.galaxGlassEffect(
    shape: RoundedCornerShape,
    pureBlack: Boolean,
    spec: GlassSurfaceSpec = GlassSurfaceSpec(),
): Modifier {
    val globalMode = LocalGalaxGlassMode.current
    val enabled = isGalaxGlassEnabled(LocalEnableLiquidGlass.current, globalMode)
    val backdrop = LocalLiquidBackdrop.current
    val selectedMode = globalMode ?: spec.mode
    val motion = OpenTuneDesignSystem.galaxMotion
    val caps = rememberGalaxGlassCapabilities(hasLiveBackdrop = backdrop != null)
    val resolvedSpec = remember(spec, globalMode) { resolveGalaxGlassSpec(spec, globalMode) }
    val plan = remember(enabled, caps, resolvedSpec, motion.reducedMotion) {
        resolveGalaxGlassPlan(enabled, resolvedSpec, caps, motion.reducedMotion)
    }
    if (plan.renderer == GlassRenderMode.OPAQUE) {
        // Preserve the legacy disabled-toggle no-op; an explicit DISABLED
        // selection renders a fully opaque, clipped surface instead.
        if (selectedMode != GlassMode.DISABLED) return this
        return this.clip(shape)
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    }
    val dark = pureBlack || MaterialTheme.colorScheme.background.luminance() < 0.45f
    val material = if (plan.mode == GlassMode.CLASSIC) null else galaxGlassMaterial(
        plan.mode, dark, MaterialTheme.colorScheme.primary,
        if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh,
    )
    return this.openTuneGlassEffect(
        shape = shape,
        pureBlack = pureBlack,
        optics = plan.optics,
        renderModeOverride = plan.renderer,
        allowStaticShader = plan.fallback == GlassFallbackStrategy.STATIC_SHADER,
        material = material,
    )
}

/**
 * First-class Surface for new GalaxOT cards, dialogs and control islands.
 * Defaults to CLASSIC until the five-way Appearance selector is introduced
 * in 4.5.7; a disabled or unavailable renderer remains opaque and clipped.
 */
@Composable
fun GalaxGlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = OpenTuneDesignSystem.shapes.glassCard,
    pureBlack: Boolean = false,
    spec: GlassSurfaceSpec = GlassSurfaceSpec(),
    opaqueColor: Color = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable BoxScope.() -> Unit,
) {
    val globalMode = LocalGalaxGlassMode.current
    val enabled = isGalaxGlassEnabled(LocalEnableLiquidGlass.current, globalMode)
    val caps = rememberGalaxGlassCapabilities(LocalLiquidBackdrop.current != null)
    val motion = OpenTuneDesignSystem.galaxMotion
    val resolvedSpec = remember(spec, globalMode) { resolveGalaxGlassSpec(spec, globalMode) }
    val plan = remember(enabled, caps, resolvedSpec, motion.reducedMotion) {
        resolveGalaxGlassPlan(enabled, resolvedSpec, caps, motion.reducedMotion)
    }
    val dark = pureBlack || MaterialTheme.colorScheme.background.luminance() < 0.45f
    val material = if (plan.mode == GlassMode.CLASSIC || plan.mode == GlassMode.DISABLED) null else galaxGlassMaterial(
        plan.mode, dark, MaterialTheme.colorScheme.primary,
        if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh,
    )
    GlassSurface(
        modifier = modifier,
        shape = shape,
        pureBlack = pureBlack,
        optics = plan.optics,
        renderModeOverride = plan.renderer,
        allowStaticShader = plan.fallback == GlassFallbackStrategy.STATIC_SHADER,
        material = material,
        opaqueColor = opaqueColor,
        content = content,
    )
}
