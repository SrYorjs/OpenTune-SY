package com.arturo254.opentune.ui.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.skydoves.cloudy.Sky

val LocalSky = androidx.compose.runtime.staticCompositionLocalOf<Sky?> { null }

val LocalEnableLiquidGlass = androidx.compose.runtime.staticCompositionLocalOf { false }

/**
 * Kyant Backdrop source shared by the floating navigation bar and mini-player.
 *
 * It is intentionally separated from Cloudy's Sky: the navigation/player layer
 * uses a single low-cost backdrop pass, while other existing glass surfaces can
 * keep using Cloudy without nesting two blur pipelines on top of each other.
 */
val LocalLiquidBackdrop = androidx.compose.runtime.staticCompositionLocalOf<Backdrop?> { null }

/**
 * Shared transparent glass material for the two floating bars.
 *
 * Android 12+ uses Kyant Backdrop's real backdrop sampling with a very small
 * blur and lens. Older devices keep the same capsule/border with a cheap
 * translucent fallback so Liquid Glass does not become a performance penalty.
 */
@Composable
fun Modifier.liquidGlassSurface(
    shape: RoundedCornerShape,
    pureBlack: Boolean,
    blurRadius: Int = 4,
    refraction: Float = 0.045f,
    curve: Float = 0.10f,
    dispersion: Float = 0.0015f,
    edge: Float = 0f,
): Modifier {
    val enabled = LocalEnableLiquidGlass.current
    val backdrop = LocalLiquidBackdrop.current

    if (!enabled) return this

    val fallbackTint = Color.Black.copy(
        alpha = if (pureBlack) 0.075f else 0.025f
    )

    // Keep the effect intentionally lighter than the catalog sample. The user
    // asked for a transparent material, and a 1–2dp blur avoids the frosted
    // white look while preserving the refraction/lens response.
    val blurDp = (blurRadius / 2f).coerceIn(1f, 2f).dp
    val lensRadius = (8f + curve.coerceIn(0f, 0.30f) * 20f).dp
    val lensDepth = (12f + refraction.coerceIn(0f, 0.15f) * 80f).dp
    val borderAlpha =
        (if (pureBlack) 0.08f else 0.10f) +
            dispersion.coerceIn(0f, 0.01f) * 2f +
            edge.coerceIn(0f, 0.02f)

    val useBackdrop =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            backdrop != null

    return this
        .clip(shape)
        .then(
            if (useBackdrop) {
                Modifier.drawBackdrop(
                    backdrop = backdrop!!,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(blurDp.toPx())
                        lens(
                            lensRadius.toPx(),
                            lensDepth.toPx(),
                        )
                    },
                    // The default highlight/shadow is beautiful in the catalog,
                    // but on these moving bars it can create a persistent bright
                    // point. We keep only the optical material itself.
                    highlight = null,
                    shadow = null,
                    innerShadow = null,
                    onDrawSurface = {
                        drawRect(fallbackTint)
                    },
                )
            } else {
                Modifier.background(fallbackTint)
            }
        )
        .border(
            width = 1.dp,
            color = Color.White.copy(alpha = borderAlpha.coerceAtMost(0.13f)),
            shape = shape,
        )
}
