package com.arturo254.opentune.ui.player

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Surface
import android.view.TextureView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal suspend fun copyCanvasFrame(view: TextureView): Bitmap? {
    val texture = view.surfaceTexture ?: return null
    if (!view.isAvailable || view.width <= 0 || view.height <= 0) return null
    val width = minOf(192, view.width)
    val height = (width.toFloat() * view.height / view.width).toInt().coerceIn(1, 384)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val surface = Surface(texture)
    return suspendCancellableCoroutine { continuation ->
        try {
            PixelCopy.request(surface, bitmap, { result ->
                surface.release()
                if (result == PixelCopy.SUCCESS && continuation.isActive) {
                    continuation.resume(bitmap)
                } else {
                    bitmap.recycle()
                    if (continuation.isActive) continuation.resume(null)
                }
            }, Handler(Looper.getMainLooper()))
        } catch (_: IllegalArgumentException) {
            surface.release()
            bitmap.recycle()
            if (continuation.isActive) continuation.resume(null)
        }
    }
}
