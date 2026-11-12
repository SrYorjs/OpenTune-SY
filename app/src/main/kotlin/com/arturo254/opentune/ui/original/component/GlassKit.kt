package com.arturo254.opentune.ui.original.component

import android.graphics.RenderNode
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.arturo254.opentune.ui.original.constants.DisableBlurKey
import com.arturo254.opentune.utils.rememberPreference

@Stable
class GlassState {
    
    var renderNode by mutableStateOf<Any?>(null)
    var rootPosition by mutableStateOf(Offset.Zero)
}



@RequiresApi(Build.VERSION_CODES.Q)
internal fun glassNodeFor(existing: Any?, width: Int, height: Int): Any =
    (existing as? RenderNode ?: RenderNode("GlassRoot")).apply { setPosition(0, 0, width, height) }

@RequiresApi(Build.VERSION_CODES.S)
internal fun glassNodeHasContent(node: Any): Boolean = (node as RenderNode).hasDisplayList()

@RequiresApi(Build.VERSION_CODES.S)
internal fun glassNodeBeginRecording(node: Any): android.graphics.Canvas = (node as RenderNode).beginRecording()

@RequiresApi(Build.VERSION_CODES.S)
internal fun glassNodeEndRecording(node: Any) {
    (node as RenderNode).endRecording()
}

@RequiresApi(Build.VERSION_CODES.S)
internal fun glassNodeWidth(node: Any): Int = (node as RenderNode).width

@RequiresApi(Build.VERSION_CODES.S)
internal fun glassNodeHeight(node: Any): Int = (node as RenderNode).height

@RequiresApi(Build.VERSION_CODES.S)
internal fun android.graphics.Canvas.drawGlassNode(node: Any) {
    drawRenderNode(node as RenderNode)
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun damageNode(width: Int, height: Int): Any =
    RenderNode("DamageBoundary").apply {
        setPosition(0, 0, width, height)
        
        
        
        setClipToBounds(true)
    }

@RequiresApi(Build.VERSION_CODES.Q)
private fun damageNodeBeginRecording(node: Any): android.graphics.Canvas = (node as RenderNode).beginRecording()

@RequiresApi(Build.VERSION_CODES.Q)
private fun damageNodeEndRecording(node: Any) {
    (node as RenderNode).endRecording()
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun android.graphics.Canvas.drawDamageNode(node: Any) {
    drawRenderNode(node as RenderNode)
}


fun Modifier.damageBoundary(): Modifier =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        this
    } else {
        this.drawWithCache {
            val width = size.width.toInt()
            val height = size.height.toInt()
            val node = if (width > 0 && height > 0) damageNode(width, height) else null
            onDrawWithContent {
                val target = drawContext.canvas
                if (node == null || !target.nativeCanvas.isHardwareAccelerated) {
                    drawContent()
                    return@onDrawWithContent
                }
                drawContext.canvas = Canvas(damageNodeBeginRecording(node))
                try {
                    drawContent()
                } finally {
                    drawContext.canvas = target
                    damageNodeEndRecording(node)
                }
                target.nativeCanvas.drawDamageNode(node)
            }
        }
            
            .graphicsLayer()
    }

fun Modifier.glassRoot(state: GlassState, isActive: () -> Boolean = { true }): Modifier = this
    .onGloballyPositioned { state.rootPosition = it.positionInWindow() }
    .drawWithCache {
        val width = size.width.toInt()
        val height = size.height.toInt()

        val node: Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && width > 0 && height > 0) {
            glassNodeFor(state.renderNode, width, height)
        } else null

        state.renderNode = node

        onDrawWithContent {
            val drawContextCanvas = drawContext.canvas

            if (node != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isActive() && drawContextCanvas.nativeCanvas.isHardwareAccelerated) {
                val nativeCanvas = glassNodeBeginRecording(node)
                val composeCanvas = Canvas(nativeCanvas)

                drawContext.canvas = composeCanvas
                try {
                    drawContent()
                } finally {
                    drawContext.canvas = drawContextCanvas
                    glassNodeEndRecording(node)
                }

                if (glassNodeHasContent(node)) {
                    drawIntoCanvas { it.nativeCanvas.drawGlassNode(node) }
                }
            } else {
                drawContent()
            }
        }
    }


val GlassChromeColor = Color(0xFF131313)


const val GlassChromeBlurRadius = 48f


private const val ChromeAccentWash = 0.55f


@Composable
fun rememberChromeAccentWash(): () -> Color {
    val accent = com.arturo254.opentune.ui.original.component.LocalDetailAccentColor.current
    val wash = remember { androidx.compose.animation.Animatable(Color.Transparent) }
    LaunchedEffect(accent) {
        
        var hue = Color.Black
        androidx.compose.runtime.snapshotFlow { accent.value }.collect { color ->
            if (color != null) hue = color
            if (wash.value.alpha == 0f) wash.snapTo(hue.copy(alpha = 0f))
            wash.animateTo(
                hue.copy(alpha = if (color != null) ChromeAccentWash else 0f),
                androidx.compose.animation.core.tween(500),
            )
        }
    }
    return remember(wash) { { wash.value } }
}


internal fun glassDownscale(effectiveRadius: Float, low: Boolean = false): Int = when {
    effectiveRadius >= 64f -> 4
    
    low && effectiveRadius >= 32f -> 4
    effectiveRadius >= 16f -> 2
    low && effectiveRadius >= 8f -> 2
    else -> 1
}


val LocalBlurQuality = androidx.compose.runtime.staticCompositionLocalOf { DefaultBlurQuality }


private val StrongGlassChips = listOf(
    "sm8350", "sm8450", "sm8475", "sm8550", "sm8635", "sm8650", "sm8750", 
    "mt6983", "mt6985", "mt6989", "mt6991", 
)


val DefaultBlurQuality: com.arturo254.opentune.ui.original.constants.BlurQuality by lazy {
    val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL.lowercase(java.util.Locale.ROOT) else ""
    
    val strong = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        (Build.VERSION.MEDIA_PERFORMANCE_CLASS >= Build.VERSION_CODES.S || StrongGlassChips.any { soc.startsWith(it) })
    if (strong) com.arturo254.opentune.ui.original.constants.BlurQuality.HIGH else com.arturo254.opentune.ui.original.constants.BlurQuality.LOW
}


internal fun Modifier.bleed(margin: () -> Int): Modifier = layout { measurable, constraints ->
    val m = margin().coerceAtLeast(0)
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
    val height = if (constraints.hasBoundedHeight) constraints.maxHeight else constraints.minHeight
    val placeable = measurable.measure(Constraints.fixed(width + m * 2, height + m * 2))
    layout(width, height) { placeable.place(-m, -m) }
}


private class FullSize {
    var width = 0
    var height = 0
}


private fun Modifier.downscaledLayout(downscale: () -> Int, full: FullSize): Modifier = layout { measurable, constraints ->
    val k = downscale().coerceAtLeast(1)
    val fullWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
    val fullHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
    full.width = fullWidth
    full.height = fullHeight
    val placeable = measurable.measure(
        Constraints.fixed((fullWidth + k - 1) / k, (fullHeight + k - 1) / k)
    )
    layout(fullWidth, fullHeight) { placeable.place(0, 0) }
}


private class MaskedEffectCache {
    var radius = -1f
    var width = -1f
    var height = -1f
    var tileOrdinal = -1
    var mask: Brush? = null
    var effect: RenderEffect? = null
}


@RequiresApi(Build.VERSION_CODES.S)
private fun maskedBlurEffect(radius: Float, tileMode: android.graphics.Shader.TileMode, mask: android.graphics.Shader): RenderEffect {
    val content = if (radius > 0.5f) {
        android.graphics.RenderEffect.createBlurEffect(radius, radius, tileMode)
    } else {
        android.graphics.RenderEffect.createOffsetEffect(0f, 0f)
    }
    return android.graphics.RenderEffect.createBlendModeEffect(
        content,
        android.graphics.RenderEffect.createShaderEffect(mask),
        android.graphics.BlendMode.DST_IN,
    ).asComposeRenderEffect()
}


@Composable
internal fun DownscaledBlur(
    radius: () -> Float,
    tileMode: android.graphics.Shader.TileMode?,
    mask: Brush?,
    modifier: Modifier = Modifier,
    
    offscreen: (() -> Boolean)? = null,
    
    growDown: Boolean? = null,
    
    growAcross: Boolean = false,
    growRamp: Float = 1f,
    draw: DrawScope.(fullSize: Size) -> Unit,
) {
    if (growDown != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ProgressiveBlurBands(radius, mask, modifier, offscreen, growDown, growAcross, growRamp, draw)
        return
    }
    val currentRadius by rememberUpdatedState(radius)
    val currentOffscreen by rememberUpdatedState(offscreen)
    
    val low = LocalBlurQuality.current == com.arturo254.opentune.ui.original.constants.BlurQuality.LOW
    val downscale by remember(low) { derivedStateOf { glassDownscale(currentRadius(), low) } }
    val maskCache = remember { MaskedEffectCache() }
    val full = remember { FullSize() }

    Box(
        modifier = modifier
            .clipToBounds()
            .then(
                
                if (mask != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(brush = mask, blendMode = BlendMode.DstIn)
                        }
                } else Modifier
            )
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .downscaledLayout({ downscale }, full)
                .graphicsLayer {
                    val k = downscale.toFloat()
                    scaleX = k
                    scaleY = k
                    transformOrigin = TransformOrigin(0f, 0f)
                    val r = currentRadius() / k
                    renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val mode = tileMode ?: android.graphics.Shader.TileMode.DECAL
                        if (mask is ShaderBrush) {
                            val quantized = if (r > 0.5f) BlurEffectCache.quantize(r) else 0f
                            val cache = maskCache
                            if (cache.effect == null || cache.radius != quantized || cache.width != size.width ||
                                cache.height != size.height || cache.tileOrdinal != mode.ordinal || cache.mask != mask
                            ) {
                                cache.effect = maskedBlurEffect(quantized, mode, mask.createShader(size))
                                cache.radius = quantized
                                cache.width = size.width
                                cache.height = size.height
                                cache.tileOrdinal = mode.ordinal
                                cache.mask = mask
                            }
                            cache.effect
                        } else {
                            BlurEffectCache.get(r, mode)
                        }
                    } else {
                        null
                    }
                    compositingStrategy = if (currentOffscreen?.invoke() == true) {
                        CompositingStrategy.Offscreen
                    } else {
                        CompositingStrategy.Auto
                    }
                    clip = true
                }
        ) {
            val k = downscale.toFloat()
            val fullSize = Size(full.width.toFloat(), full.height.toFloat())
            scale(1f / k, 1f / k, pivot = Offset.Zero) { draw(fullSize) }
        }
    }
}

@Composable
fun GlassPillBackground(
    state: GlassState,
    blurRadius: () -> Float = { 24f },
    tint: Color = Color.Transparent,
    foundationColor: Color? = null,
    shape: Shape? = null,

    
    tileMode: android.graphics.Shader.TileMode? = null,
    
    mask: Brush? = null,
    
    accentWash: (() -> Color)? = null,
    
    bleed: Boolean = false,
    
    progressive: BlurDirection? = null,
    
    progressiveRamp: Float = 1f,
    modifier: Modifier = Modifier
) {
    var position by remember { mutableStateOf(Offset.Zero) }

    val (disableBlur) = rememberPreference(DisableBlurKey, defaultValue = false)
    val (blurStrengthPx) = rememberPreference(com.arturo254.opentune.ui.original.constants.BlurStrengthPxKey, defaultValue = 25f)

    val currentBlurRadius by rememberUpdatedState(blurRadius)

    
    val radius = {
        if (disableBlur) 0f
        else BlurEffectCache.effectiveRadius(currentBlurRadius() * (blurStrengthPx / 25f))
    }

    DownscaledBlur(
        radius = radius,
        tileMode = tileMode,
        mask = mask,
        growDown = progressive?.let { it == BlurDirection.TopToBottom },
        growRamp = progressiveRamp,
        modifier = modifier
            .then(if (shape != null) Modifier.clip(shape) else Modifier)
            .then(
                
                
                if (bleed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    LocalBlurQuality.current == com.arturo254.opentune.ui.original.constants.BlurQuality.HIGH
                ) {
                    Modifier.bleed { kotlin.math.ceil(radius()).toInt() }
                } else Modifier
            )
            .onGloballyPositioned { position = it.positionInWindow() },
    ) { fullSize ->
        if (foundationColor != null) {
            drawRect(color = foundationColor, size = fullSize)
        }
        if (!disableBlur) {
            val node = state.renderNode
            if (node != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && glassNodeHasContent(node)) {
                val relX = position.x - state.rootPosition.x
                val relY = position.y - state.rootPosition.y
                translate(left = -relX, top = -relY) {
                    drawIntoCanvas { it.nativeCanvas.drawGlassNode(node) }
                }
            }
        }
        if (tint != Color.Transparent) {
            drawRect(color = tint, size = fullSize)
        }
        val wash = accentWash?.invoke()
        if (wash != null && wash.alpha > 0f) {
            drawRect(color = wash, size = fullSize)
        }
    }
}

enum class BlurDirection { TopToBottom, BottomToTop }


private val FlatBarEdgeColor = Color.White.copy(alpha = 0.16f)
private val FlatBarEdgeWidth = 1.dp


const val FlatBarScrimAlpha = 0.45f


@Composable
fun rememberProgressiveBlur(): Boolean {
    val (enabled) = rememberPreference(com.arturo254.opentune.ui.original.constants.EnableProgressiveBlurKey, defaultValue = true)
    return enabled
}

@Composable
fun ProgressiveGlassBackground(
    state: GlassState,
    maxBlurRadius: () -> Float = { 24f },
    tint: Color = Color.Transparent,
    foundationColor: Color? = null,
    direction: BlurDirection = BlurDirection.TopToBottom,
    modifier: Modifier = Modifier,
    steps: Int = 5
) {
    
    if (!rememberProgressiveBlur()) {
        Box(
            modifier = modifier
                .clipToBounds()
                .drawWithContent {
                    drawContent()
                    val line = FlatBarEdgeWidth.toPx()
                    val y = if (direction == BlurDirection.TopToBottom) 0f else size.height - line
                    drawRect(FlatBarEdgeColor, topLeft = Offset(0f, y), size = Size(size.width, line))
                },
        ) {
            GlassPillBackground(
                state = state,
                blurRadius = maxBlurRadius,
                tint = tint,
                foundationColor = foundationColor,
                tileMode = android.graphics.Shader.TileMode.CLAMP,
                
                bleed = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
        return
    }
    val smooth = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        LocalBlurQuality.current == com.arturo254.opentune.ui.original.constants.BlurQuality.HIGH
    if (smooth) {
        
        val fade = 1f / steps.coerceAtLeast(1)
        val mask = remember(direction, fade) {
            if (direction == BlurDirection.TopToBottom) {
                Brush.verticalGradient(0f to Color.Transparent, fade to Color.Black, 1f to Color.Black)
            } else {
                Brush.verticalGradient(0f to Color.Black, (1f - fade) to Color.Black, 1f to Color.Transparent)
            }
        }
        Box(modifier = modifier) {
            GlassPillBackground(
                state = state,
                blurRadius = maxBlurRadius,
                tint = tint,
                foundationColor = foundationColor,
                mask = mask,
                progressive = direction,
                modifier = Modifier.fillMaxSize(),
            )
        }
        return
    }
    Box(modifier = modifier) {
        for (i in 1..steps) {
            val fraction = i.toFloat() / steps

            val radiusProvider = { maxBlurRadius() * (fraction * fraction) }

            val fadeStart = (i - 1).toFloat() / steps
            val heightFraction = 1f - fadeStart

            if (heightFraction > 0f) {
                val internalFadeDistance = (1f / steps) / heightFraction
                
                
                
                val mask = remember(direction, internalFadeDistance) {
                    if (direction == BlurDirection.TopToBottom) {
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            internalFadeDistance to Color.Black,
                            1f to Color.Black
                        )
                    } else {
                        Brush.verticalGradient(
                            0f to Color.Black,
                            (1f - internalFadeDistance) to Color.Black,
                            1f to Color.Transparent
                        )
                    }
                }

                GlassPillBackground(
                    state = state,
                    blurRadius = radiusProvider,
                    tint = tint,
                    foundationColor = foundationColor,
                    mask = mask,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(heightFraction)
                        .align(if (direction == BlurDirection.TopToBottom) androidx.compose.ui.Alignment.BottomCenter else androidx.compose.ui.Alignment.TopCenter)
                )
            }
        }
    }
}
