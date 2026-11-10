package com.arturo254.opentune.ui.visual.studio

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

internal fun smoother(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * t * (t * (t * 6f - 15f) + 10f)
}

internal fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t
internal fun lerpO(a: Offset, b: Offset, t: Float) = Offset(lerpF(a.x, b.x, t), lerpF(a.y, b.y, t))

internal class StudioCamera(val zoom: Float, val anchor: Offset, val target: Offset) {
    fun zoomAt(cz: Float) = 1f + (zoom - 1f) * cz

    
    fun map(p: Offset, cz: Float): Offset {
        val a = lerpO(anchor, target, cz)
        return a + (p - anchor) * zoomAt(cz)
    }

    
    fun stillOrigin(cz: Float, w: Float, h: Float): Offset {
        val z = zoomAt(cz)
        val o = map(Offset.Zero, cz)
        return Offset(o.x.coerceIn(w - w * z, 0f), o.y.coerceIn(h - h * z, 0f))
    }

    
    fun pieceCenter(rect: Rect, to: Offset, cz: Float, spread: Float): Offset {
        val now = map(rect.center, cz)
        val landed = map(rect.center, 1f)
        return now + (to - landed) * spread
    }
}

internal fun DrawScope.drawStill(image: ImageBitmap?, camera: StudioCamera, cz: Float, alpha: Float) {
    if (image == null || alpha <= 0.003f) return
    val zoom = camera.zoomAt(cz)
    val origin = camera.stillOrigin(cz, size.width, size.height)
    drawImage(
        image = image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset(origin.x.roundToInt(), origin.y.roundToInt()),
        dstSize = IntSize((size.width * zoom).roundToInt(), (size.height * zoom).roundToInt()),
        alpha = alpha.coerceIn(0f, 1f),
        filterQuality = FilterQuality.Low,
    )
}

internal fun DrawScope.drawPiece(layer: GraphicsLayer, rect: Rect, center: Offset, scale: Float, alpha: Float, rotation: Float = 0f) {
    if (alpha <= 0.003f || rect.width <= 0f) return
    layer.alpha = alpha.coerceIn(0f, 1f)
    withTransform({
        translate(center.x - rect.width / 2f, center.y - rect.height / 2f)
        scale(scale, scale, pivot = Offset(rect.width / 2f, rect.height / 2f))
        if (rotation != 0f) rotate(rotation, pivot = Offset(rect.width / 2f, rect.height / 2f))
    }) {
        drawLayer(layer)
    }
}
