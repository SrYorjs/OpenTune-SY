package com.arturo254.opentune.ui.visual.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp

private const val FrostTintAlpha = 0.78f

private const val ChromeOutlineWidthPx = 2f
private val ChromeOutlineColor = Color.White.copy(alpha = 0.05f)
private val ChromeOutlineStroke = Stroke(ChromeOutlineWidthPx)

val ChromeShadow = Shadow(radius = 6.3.dp, color = Color.Black.copy(alpha = 0.12f), spread = 2.dp)

val ChromeHighlightColor = Color.White.copy(alpha = 0.1f)

private const val FrostGrainAlpha = 0.035f
private const val FrostGrainTileSize = 64

private val FrostGrainBrush: ShaderBrush by lazy {
    val random = java.util.Random(0x6A55)
    val pixels = IntArray(FrostGrainTileSize * FrostGrainTileSize) {
        val alpha = random.nextInt(256) shl 24
        if (random.nextBoolean()) alpha or 0xFFFFFF else alpha
    }
    val tile = android.graphics.Bitmap.createBitmap(
        pixels, FrostGrainTileSize, FrostGrainTileSize, android.graphics.Bitmap.Config.ARGB_8888,
    )
    ShaderBrush(ImageShader(tile.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
}

@Composable
fun FrostedChromeGlass(
    state: GlassState,
    pureBlack: Boolean,
    accentWash: (() -> Color)?,
    modifier: Modifier = Modifier,
) {
    
    val color = if (pureBlack) Color.Black else GlassChromeColor
    
    val grain = LocalBlurQuality.current == com.arturo254.opentune.ui.visual.BlurQuality.HIGH
    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            if (grain) drawRect(brush = FrostGrainBrush, alpha = FrostGrainAlpha)
        }
    ) {
        GlassPillBackground(
            state = state,
            blurRadius = { GlassChromeBlurRadius },
            tint = color.copy(alpha = FrostTintAlpha),
            foundationColor = color,
            accentWash = accentWash,
            
            tileMode = android.graphics.Shader.TileMode.CLAMP,
            bleed = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

fun DrawScope.drawChromeOutline(
    topLeft: Offset,
    size: Size,
    cornerRadius: Float,
    alpha: Float = 1f,
) {
    val half = ChromeOutlineWidthPx / 2f
    drawRoundRect(
        color = ChromeOutlineColor,
        topLeft = Offset(topLeft.x + half, topLeft.y + half),
        size = Size(size.width - ChromeOutlineWidthPx, size.height - ChromeOutlineWidthPx),
        cornerRadius = CornerRadius((cornerRadius - half).coerceAtLeast(0f)),
        style = ChromeOutlineStroke,
        alpha = alpha,
    )
}
