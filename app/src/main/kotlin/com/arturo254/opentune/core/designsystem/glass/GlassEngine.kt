package com.arturo254.opentune.core.designsystem.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.theme.colors
import com.arturo254.opentune.core.designsystem.theme.shapes
import com.arturo254.opentune.core.designsystem.theme.surfaces
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.skydoves.cloudy.LiquidGlassDefaults
import com.skydoves.cloudy.Sky
import com.skydoves.cloudy.liquidGlass

/** Canonical runtime locals. Old ui.component names delegate to these objects. */
val LocalSky = staticCompositionLocalOf<Sky?> { null }
val LocalEnableLiquidGlass = staticCompositionLocalOf { false }
val LocalLiquidBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** Optical arguments are kept independent of surface shape, theme and player state. */
data class GlassOptics(
    val blurRadius: Int = 6,
    val refraction: Float = 0.06f,
    val curve: Float = 0.14f,
    val dispersion: Float = 0.003f,
    val edge: Float = 0.20f,
)

enum class GlassPreset(val optics: GlassOptics) {
    STANDARD(GlassOptics()),
    SOFT(GlassOptics(blurRadius = 4, refraction = 0.045f, curve = 0.10f, dispersion = 0.0015f, edge = 0f)),
    INTERACTIVE(GlassOptics(blurRadius = 6, refraction = 0.078f, curve = 0.17f, dispersion = 0.004f, edge = 0.48f)),
    CONTROL_CLUSTER(GlassOptics(blurRadius = 7, refraction = 0.072f, curve = 0.16f, dispersion = 0.0035f, edge = 0.36f)),
}

/** Branch-selection rules used for unit-testing fallback behavior. */
enum class GlassRenderMode { OPAQUE, STATIC_FALLBACK, LIVE_BACKDROP }

fun chooseGlassRenderMode(enabled: Boolean, hasBackdrop: Boolean): GlassRenderMode =
    when {
        !enabled -> GlassRenderMode.OPAQUE
        hasBackdrop -> GlassRenderMode.LIVE_BACKDROP
        else -> GlassRenderMode.STATIC_FALLBACK
    }

/**
 * Canonical Kyant Backdrop / Sky-compat engine.
 * This modifier is intentionally a no-op if glass is disabled so legacy
 * consumers retain the same behavior; use [GlassSurface] for a safe opaque
 * fallback that always clips to its shape.
 */
@Composable
fun Modifier.openTuneGlassEffect(
    shape: RoundedCornerShape,
    pureBlack: Boolean,
    optics: GlassOptics = GlassPreset.STANDARD.optics,
    renderModeOverride: GlassRenderMode? = null,
    allowStaticShader: Boolean = true,
    surfaceTintOverride: Color? = null,
    borderTintOverride: Color? = null,
): Modifier {
    val enabled = LocalEnableLiquidGlass.current
    val backdrop = LocalLiquidBackdrop.current
    val mode = renderModeOverride ?: chooseGlassRenderMode(enabled, backdrop != null)
    if (mode == GlassRenderMode.OPAQUE) return this

    val darkSurface =
        pureBlack || MaterialTheme.colorScheme.background.luminance() < 0.45f
    val designColors = OpenTuneDesignSystem.colors
    val surfaceTokens = OpenTuneDesignSystem.surfaces
    val fallbackSurface = surfaceTintOverride ?: designColors.surfaceGlass
    val borderColor = borderTintOverride ?: designColors.outlineGlass

    if (mode == GlassRenderMode.STATIC_FALLBACK) {
        val density = LocalDensity.current
        var surfaceSize by remember { mutableStateOf(Size.Zero) }
        val center =
            remember(surfaceSize) {
                Offset(
                    x = surfaceSize.width * 0.5f,
                    y = surfaceSize.height * 0.5f,
                )
            }
        val useSafeShader =
            allowStaticShader && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                surfaceSize != Size.Zero

        return this
            .onSizeChanged { size ->
                val next = Size(size.width.toFloat(), size.height.toFloat())
                if (next != surfaceSize) surfaceSize = next
            }
            .clip(shape)
            .background(fallbackSurface)
            .then(
                if (useSafeShader) {
                    Modifier.liquidGlass(
                        lensCenter = center,
                        lensSize = surfaceSize,
                        cornerRadius = shape.topStart.toPx(surfaceSize, density),
                        refraction = optics.refraction.coerceIn(0.035f, 0.10f),
                        curve = optics.curve.coerceIn(0.075f, 0.22f),
                        dispersion = optics.dispersion.coerceIn(0f, 0.006f),
                        saturation = 1.032f,
                        contrast = 1.014f,
                        tint = Color.Transparent,
                        edge = optics.edge.coerceIn(0f, 0.009f),
                        glow = LiquidGlassDefaults.NoGlow,
                    )
                } else {
                    Modifier
                }
            )
            .border(surfaceTokens.borders.fallbackGlassWidth, borderColor, shape)
    }

    val blurDp = optics.blurRadius.coerceIn(2, 12).dp
    val lensRadius = (11f + optics.refraction.coerceIn(0f, 0.14f) * 110f).dp
    val lensDepth = (17f + optics.curve.coerceIn(0f, 0.30f) * 58f).dp
    val highlightAlpha =
        (0.34f + optics.edge.coerceIn(0f, 1f) * 0.34f).coerceIn(0.34f, 0.68f)

    val liveBackdrop = backdrop ?: return this
    return this
        .drawBackdrop(
            backdrop = liveBackdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(blurDp.toPx())
                lens(
                    lensRadius.toPx(),
                    lensDepth.toPx(),
                    chromaticAberration = optics.dispersion >= 0.0015f,
                )
            },
            highlight = {
                Highlight.Ambient.copy(alpha = highlightAlpha)
            },
            shadow = {
                Shadow(
                    radius = surfaceTokens.shadows.glassOuterRadius,
                    color = designColors.shadowGlass,
                )
            },
            innerShadow = {
                InnerShadow(
                    radius = surfaceTokens.shadows.glassInnerRadius,
                    alpha =
                        if (darkSurface) surfaceTokens.opacity.innerGlassShadowDark
                        else surfaceTokens.opacity.innerGlassShadowLight,
                )
            },
            onDrawSurface = {
                drawRect(fallbackSurface)
            },
        )
        .clip(shape)
        .border(
            width = surfaceTokens.borders.activeGlassWidth,
            color = borderColor,
            shape = shape,
        )
}

/**
 * Canonical composable surface. Always clips even with glass OFF.
 * Consumers provide content only; no playback, sources, or ViewModel state.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = OpenTuneDesignSystem.shapes.glassCard,
    pureBlack: Boolean = false,
    preset: GlassPreset = GlassPreset.STANDARD,
    optics: GlassOptics = preset.optics,
    renderModeOverride: GlassRenderMode? = null,
    allowStaticShader: Boolean = true,
    surfaceTintOverride: Color? = null,
    borderTintOverride: Color? = null,
    opaqueColor: Color = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable BoxScope.() -> Unit,
) {
    val enabled = LocalEnableLiquidGlass.current && renderModeOverride != GlassRenderMode.OPAQUE
    val borders = OpenTuneDesignSystem.surfaces.borders
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) Color.Transparent else opaqueColor)
            .then(
                if (enabled) {
                    Modifier.openTuneGlassEffect(shape, pureBlack, optics, renderModeOverride, allowStaticShader, surfaceTintOverride, borderTintOverride)
                } else {
                    Modifier.border(
                        width = borders.glassCardWidth,
                        color = borders.glassCardOpaqueColor,
                        shape = shape,
                    )
                }
            ),
        content = content,
    )
}
