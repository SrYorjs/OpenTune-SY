package com.arturo254.opentune.foundation.theme

import android.content.Context
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


object CoverThemeColor {
    private val cache = LruCache<String, Int>(96)

    fun cached(url: String?): Color? = url?.let { cache.get(it) }?.let { Color(it) }

    suspend fun extract(context: Context, url: String): Color? {
        cached(url)?.let { return it }
        return try {
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(112, 112)
                    .allowHardware(false)
                    .build()
                val bitmap = context.imageLoader.execute(request).image?.toBitmap() ?: return@withContext null
                val argb = withContext(Dispatchers.Default) {
                    val palette = Palette.from(bitmap)
                        .setRegion(0, (bitmap.height * 0.62f).toInt(), bitmap.width, bitmap.height)
                        .maximumColorCount(12)
                        .generate()
                    val dominant = palette.swatches.maxByOrNull { it.population }?.rgb
                        ?: return@withContext null
                    darken(dominant)
                } ?: return@withContext null
                cache.put(url, argb)
                Color(argb)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    
    internal fun darken(argb: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(argb, hsl)
        hsl[1] = (hsl[1] * 0.9f).coerceAtMost(0.62f)
        hsl[2] = (hsl[2] * 0.5f).coerceIn(0.09f, 0.3f)
        return ColorUtils.HSLToColor(hsl)
    }
}


@Composable
fun rememberCoverThemeColor(url: String?): State<Color?> {
    val context = LocalContext.current
    val state = remember(url) { mutableStateOf(CoverThemeColor.cached(url)) }
    LaunchedEffect(url) {
        if (url != null && state.value == null) {
            state.value = CoverThemeColor.extract(context, url)
        }
    }
    return state
}
