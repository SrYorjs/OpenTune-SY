package com.arturo254.opentune.ui.visual.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val GlowPeakAlpha = 0.9f

private const val GlowReachFraction = 0.95f
private const val GlowStopCount = 12
private val GlowStopPositions = FloatArray(GlowStopCount + 1) { it / GlowStopCount.toFloat() }

@Composable
fun HomeTopGlow(
    coverUrls: List<String>,
    depth: Dp,
    visibility: () -> Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    
    val colors = remember { mutableStateOf(cachedGlowPair(coverUrls)) }
    LaunchedEffect(coverUrls) { colors.value = loadGlowPair(context, coverUrls) }

    
    val left = animateColorAsState(colors.value?.first ?: Color.Transparent, tween(700), label = "homeGlowLeft")
    val right = animateColorAsState(colors.value?.second ?: Color.Transparent, tween(700), label = "homeGlowRight")
    val leftPaint = remember { Paint().apply { isDither = true } }
    val rightPaint = remember { Paint().apply { isDither = true } }

    Spacer(
        modifier = modifier
            
            .graphicsLayer {
                alpha = visibility().coerceIn(0f, 1f)
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .drawWithCache {
                val leftColor = left.value
                val rightColor = right.value
                val width = size.width
                val height = depth.toPx()
                if (width <= 0f || height <= 0f || (leftColor.alpha == 0f && rightColor.alpha == 0f)) {
                    return@drawWithCache onDrawBehind {}
                }
                leftPaint.shader = cornerGlow(leftColor, width * 0.08f, width, height)
                rightPaint.shader = cornerGlow(rightColor, width * 0.92f, width, height)
                
                onDrawBehind {
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawRect(0f, 0f, width, height, leftPaint)
                        canvas.nativeCanvas.drawRect(0f, 0f, width, height, rightPaint)
                    }
                }
            }
    )
}

private fun cornerGlow(color: Color, centerX: Float, width: Float, height: Float): Shader {
    val rgb = color.copy(alpha = 1f).toArgb()
    val stops = IntArray(GlowStopCount + 1) { i ->
        val remaining = 1f - GlowStopPositions[i]
        val falloff = remaining * remaining * (3f - 2f * remaining)
        ColorUtils.setAlphaComponent(rgb, (color.alpha * GlowPeakAlpha * falloff * 255f).roundToInt())
    }
    return RadialGradient(centerX, 0f, height, stops, GlowStopPositions, Shader.TileMode.CLAMP).apply {
        setLocalMatrix(Matrix().apply { setScale(width * GlowReachFraction / height, 1f, centerX, 0f) })
    }
}

private fun glowPair(left: Color?, right: Color?): Pair<Color, Color>? = when {
    left != null -> left to (right ?: left)
    right != null -> right to right
    else -> null
}

private fun cachedGlowPair(urls: List<String>): Pair<Color, Color>? =
    glowPair(urls.getOrNull(0)?.let(HomeGlowColor::cached), urls.getOrNull(1)?.let(HomeGlowColor::cached))

private suspend fun loadGlowPair(context: Context, urls: List<String>): Pair<Color, Color>? =
    glowPair(urls.getOrNull(0)?.let { HomeGlowColor.load(context, it) }, urls.getOrNull(1)?.let { HomeGlowColor.load(context, it) })

private object HomeGlowColor {
    private val cache = LruCache<String, Int>(48)

    fun cached(url: String): Color? = cache.get(url)?.let { Color(it) }

    suspend fun load(context: Context, url: String): Color? {
        cached(url)?.let { return it }
        return try {
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(64)
                    .allowHardware(false)
                    .build()
                val bitmap = context.imageLoader.execute(request).image?.toBitmap() ?: return@withContext null
                val argb = withContext(Dispatchers.Default) { glowTone(bitmap) } ?: return@withContext null
                cache.put(url, argb)
                Color(argb)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    
    private fun glowTone(bitmap: Bitmap): Int? {
        val swatches = Palette.from(bitmap).maximumColorCount(12).generate().swatches
        val dominant = swatches.maxByOrNull { it.population } ?: return null
        val pick = swatches
            .filter { it.hsl[1] >= 0.2f && it.hsl[2] in 0.08f..0.9f && it.population >= dominant.population / 6 }
            .maxByOrNull { it.population }
            ?: dominant
        val hsl = pick.hsl.copyOf()
        hsl[1] = hsl[1].coerceAtMost(0.6f)
        hsl[2] = hsl[2].coerceIn(0.2f, 0.32f)
        return ColorUtils.HSLToColor(hsl)
    }
}
