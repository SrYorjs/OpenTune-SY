package com.arturo254.opentune.core.designsystem.liquid

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/**
 * Galax Liquid Renderer V1: new, standalone Kyant optical pipeline.
 *
 * A Backdrop is always supplied by the parent that owns the DRAW SOURCE.
 * The recording source may not contain any surface which samples it, or
 * Android HWUI can recurse into its own RenderNode (SIGSEGV).
 */
@Composable
fun Modifier.galaxLiquidEffect(
    variant: GalaxLiquidVariant = LocalGalaxLiquidVariant.current,
    pureBlack: Boolean = false,
    tuning: GalaxLiquidTuning = LocalGalaxLiquidTuning.current ?: galvanicRecipe(variant),
    backdrop: Backdrop? = LocalGalaxLiquidBackdrop.current,
    shapeOverride: RoundedCornerShape? = null,
): Modifier {
    val style = tuning.bounded()
    val finish = galvanicFinish(variant)
    val shape = shapeOverride ?: RoundedCornerShape(style.cornerRadiusDp.dp)
    val colors = MaterialTheme.colorScheme
    val dark = pureBlack || colors.background.luminance() < .46f

    if (variant == GalaxLiquidVariant.DISABLED) {
        return this.clip(shape).background(if (pureBlack) Color.Black else colors.surfaceContainerHigh)
            .border(1.dp, colors.outlineVariant, shape)
    }

    val context = LocalContext.current
    val powerSaver = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)
        ?.isPowerSaveMode == true
    val lowRam = (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
        ?.isLowRamDevice == true
    val surfaceTint = when (finish.colorStyle) {
        1 -> if (dark) Color(0xFF072344) else Color(0xFFC8F1FF)
        2 -> if (dark) Color(0xFF2F124E) else Color(0xFFFFE6FA)
        3 -> if (dark) Color(0xFF211046) else Color(0xFFE8D8FF)
        else -> if (dark) Color(0xFF111626) else Color.White
    }
    val rimTop = Color.White.copy(alpha = if (dark) finish.rimAlpha else finish.rimAlpha * .90f)
    val rimBottom = colors.primary.copy(alpha = finish.rimAlpha * .24f)
    val rim = Brush.verticalGradient(listOf(rimTop, rimBottom))
    val tint = surfaceTint.copy(alpha = style.tintAlpha)
    val opticalHighlight = when (finish.lightStyle) {
        1 -> Highlight.Default.copy(alpha = finish.rimAlpha)
        2 -> Highlight.Plain.copy(alpha = finish.rimAlpha * .74f)
        else -> Highlight.Ambient.copy(alpha = finish.rimAlpha)
    }

    val canRender = backdrop != null && !powerSaver && !lowRam &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    if (!canRender) {
        // The backend is unavailable: never pretend a static tint is live
        // refraction. Text remains legible, and a high-contrast outline survives.
        return this.clip(shape)
            .background(colors.surfaceContainerHigh.copy(alpha = if (dark) .92f else .91f))
            .border((.7f + finish.rimAlpha * .8f).dp, rim, shape)
    }

    return this
        .drawBackdrop(
            backdrop = backdrop!!,
            shape = { shape },
            effects = {
                if (finish.vibrancy) vibrancy()
                if (style.blurRadiusDp > 0f) blur(style.blurRadiusDp.dp.toPx())
                val dimension = size.minDimension.coerceAtLeast(1f)
                if (style.refractionHeight > 0f && style.refractionAmount > 0f) {
                    lens(
                        refractionHeight = dimension * style.refractionHeight * .5f,
                        refractionAmount = dimension * style.refractionAmount,
                        depthEffect = finish.depthEffect,
                        chromaticAberration = style.chromaticAberration > .45f,
                    )
                }
            },
            highlight = { opticalHighlight },
            shadow = {
                Shadow(radius = (4f + finish.shadowAlpha * 60f).dp,
                    color = Color.Black.copy(alpha = finish.shadowAlpha))
            },
            onDrawSurface = { drawRect(tint) },
        )
        .clip(shape)
        .border(1.dp, rim, shape)
}

/** Shape and semantics owner for screens and preview surfaces. */
@Composable
fun GalaxLiquidSurface(
    modifier: Modifier = Modifier,
    variant: GalaxLiquidVariant = LocalGalaxLiquidVariant.current,
    pureBlack: Boolean = false,
    tuning: GalaxLiquidTuning = LocalGalaxLiquidTuning.current ?: galvanicRecipe(variant),
    backdrop: Backdrop? = LocalGalaxLiquidBackdrop.current,
    shapeOverride: RoundedCornerShape? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.galaxLiquidEffect(
            variant = variant, pureBlack = pureBlack,
            tuning = tuning, backdrop = backdrop, shapeOverride = shapeOverride,
        ),
        content = content,
    )
}
