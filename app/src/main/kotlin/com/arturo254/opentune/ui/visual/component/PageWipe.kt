package com.arturo254.opentune.ui.visual.component

import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sqrt
import com.arturo254.opentune.ui.visual.utils.AppMotion

private const val PageMediaTravel = 0.18f
private const val PageTextTravel = 0.42f

private const val PageWipeFeather = 0.55f
private const val PageWipeTrail = 0.25f
private val PageSwitchBlur = 18.dp

private const val PageWipeCurveReach = 0.6f
private const val PageWipeFlat = 0.04f

private const val PageWipeSettled = 0.001f
private const val PageWipeHidden = 0.999f

private const val PeekDim = 0.5f
private const val PeekMediaTravel = 0.12f

@Composable
internal fun PageWipe(
    pageOffset: () -> Float,
    curve: () -> Float,
    blur: Boolean,
    modifier: Modifier = Modifier,
    
    peek: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    if (peek) {
        PeekPage(pageOffset = pageOffset, modifier = modifier, content = content)
        return
    }
    val blurPx = if (blur) with(LocalDensity.current) { PageSwitchBlur.toPx() } else 0f
    val pageLayer = rememberGraphicsLayer()
    val sourceEdge = remember { PageWipeEdge() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val d = pageOffset()
                translationX = -d * size.width
                
                
                
                
                alpha = if (abs(d) >= PageWipeHidden) 0f else 1f
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .drawWithContent {
                    if (sourceEdge.update(pageOffset(), size, curve(), blurPx)) {
                        pageLayer.record { this@drawWithContent.drawContent() }
                    } else {
                        drawContent()
                    }
                },
            content = content,
        )
        
        PageWipeCopy(pageLayer = pageLayer, pageOffset = pageOffset, curve = curve, blurPx = blurPx)
    }
}

@Composable
private fun PeekPage(pageOffset: () -> Float, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .drawWithContent {
                drawContent()
                val d = abs(pageOffset()).coerceAtMost(1f)
                if (d > PageWipeSettled) drawRect(Color.Black.copy(alpha = PeekDim * d))
            },
        content = content,
    )
}

private class PageWipeEdge {
    var incoming = false
    var centerX = 0f
    var centerY = 0f
    
    var inner = 0f
    
    var outer = 0f
    var blur = 0f

    

    fun update(d: Float, size: Size, curve: Float, blurPx: Float): Boolean {
        val a = abs(d)
        if (a < PageWipeSettled || a >= PageWipeHidden) return false
        val w = size.width
        val feather = w * PageWipeFeather
        
        
        val progress = if (d > 0f) 1f - d else -d
        val off = w * PageWipeCurveReach / curve.coerceIn(PageWipeFlat, 1f)
        centerX = w + off
        centerY = size.height / 2f
        
        
        val from = off - feather
        val to = hypot(centerX, centerY)
        val r = from + (to - from) * progress
        incoming = d > 0f
        if (incoming) {
            
            inner = r
            outer = r + feather
        } else {
            
            inner = r - feather * PageWipeTrail
            outer = inner + feather
        }
        blur = a * blurPx
        
        val erases = if (incoming) inner < to else outer > off
        return erases || blur > 0.5f
    }
}

@Composable
private fun PageWipeCopy(
    pageLayer: GraphicsLayer,
    pageOffset: () -> Float,
    curve: () -> Float,
    blurPx: Float,
) {
    val edge = remember { PageWipeEdge() }
    DownscaledBlur(
        radius = {
            val a = abs(pageOffset())
            
            if (a < PageWipeSettled || a >= PageWipeHidden || !AppMotion.elementBlur) 0f else BlurEffectCache.effectiveRadius(a * blurPx)
        },
        tileMode = Shader.TileMode.CLAMP,
        mask = null,
        offscreen = { abs(pageOffset()) in PageWipeSettled..PageWipeHidden },
        modifier = Modifier.fillMaxSize(),
    ) { fullSize ->
        if (!edge.update(pageOffset(), fullSize, curve(), blurPx)) return@DownscaledBlur
        val w = fullSize.width
        val h = fullSize.height
        val halfH = h / 2f
        
        fun fullyInside(radius: Float): Float =
            if (radius > halfH) min(w, edge.centerX - sqrt(radius * radius - halfH * halfH)) else w
        val left: Float
        val right: Float
        val maskLeft: Float
        val maskRight: Float
        if (edge.incoming) {
            
            left = (edge.centerX - edge.outer).coerceAtLeast(0f)
            right = w
            maskLeft = left
            maskRight = fullyInside(edge.inner)
        } else {
            
            left = 0f
            right = fullyInside(edge.inner)
            maskLeft = (edge.centerX - edge.outer).coerceAtLeast(0f)
            maskRight = right
        }
        if (right <= left) return@DownscaledBlur
        clipRect(left, 0f, right, h) { drawLayer(pageLayer) }
        if (maskRight > maskLeft) {
            val center = Offset(edge.centerX, edge.centerY)
            val stops = if (edge.incoming) {
                featherStops(edge.inner / edge.outer, inside = Color.Transparent, outside = Color.Black)
            } else {
                featherStops(edge.inner / edge.outer, inside = Color.Black, outside = Color.Transparent)
            }
            drawRect(
                brush = Brush.radialGradient(*stops, center = center, radius = edge.outer),
                topLeft = Offset(maskLeft, 0f),
                size = Size(maskRight - maskLeft, h),
                blendMode = BlendMode.DstOut,
            )
        }
    }
}

private fun featherStops(start: Float, inside: Color, outside: Color): Array<Pair<Float, Color>> {
    val s = start.coerceIn(0f, 0.999f)
    return Array(FeatherEase.size) { i ->
        val t = i / (FeatherEase.size - 1f)
        (s + (1f - s) * t) to lerp(inside, outside, FeatherEase[i])
    }
}

private val FeatherEase = floatArrayOf(0f, 0.104f, 0.352f, 0.648f, 0.896f, 1f)

internal fun Modifier.pageMediaDrift(pageOffset: () -> Float, peek: Boolean = false): Modifier = if (peek) peekMediaDrift(pageOffset) else graphicsLayer {
    val d = pageOffset().coerceIn(-1f, 1f)
    val scale = 1f + 2f * PageMediaTravel * abs(d)
    scaleX = scale
    scaleY = scale
    translationX = d * size.width * PageMediaTravel
}

internal fun Modifier.pageTextDrift(pageOffset: () -> Float, peek: Boolean = false): Modifier = graphicsLayer {
    val d = pageOffset()
    if (peek) {
        
        alpha = (1f - abs(d) * 0.75f).coerceIn(0f, 1f)
        return@graphicsLayer
    }
    translationX = d * size.width * (PageTextTravel + PageMediaTravel)
    alpha = (1f - abs(d) * 1.9f).coerceIn(0f, 1f)
}

private fun Modifier.peekMediaDrift(pageOffset: () -> Float): Modifier = graphicsLayer {
    val d = pageOffset().coerceIn(-1f, 1f)
    val scale = 1f + 2f * PeekMediaTravel * abs(d)
    scaleX = scale
    scaleY = scale
    translationX = -d * size.width * PeekMediaTravel
}
