package com.arturo254.opentune.ui.visual.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.arturo254.opentune.ui.component.BottomSheetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val HandleActiveAlpha = 0.6f
private const val HandleIdleAlpha = 0.15f

private const val HandleIdleDelayMs = 4_000L

private val HandleLength = 100.dp
private val HandleThickness = 5.dp
private val HandleCentreBelowStatusBar = 10.dp

private val ChevronLength = 44.dp
private val ChevronDepth = 10.dp

private val HandleTargetWidth = 150.dp
private val HandleTargetHeight = 28.dp

private const val HandleTapMinProgress = 0.9f

private const val HandleSampleIntervalMs = 1_500L

private val SampleBelowCentre = 6.dp
private val SampleHeight = 10.dp
private const val SampleBitmapWidth = 24
private const val SampleBitmapHeight = 4

private val HandleWakeSpec = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)
private val HandleFadeSpec = tween<Float>(durationMillis = 900, easing = FastOutSlowInEasing)
private val HandleBendSpring = spring<Float>(dampingRatio = 0.82f, stiffness = 420f)

@Stable
class PlayerHandleState internal constructor(private val scope: CoroutineScope) {
    internal val alpha = Animatable(HandleActiveAlpha)
    private var idleJob: Job? = null

    
    fun onTouchActivity(touching: Boolean) {
        if (touching) {
            idleJob?.cancel()
            idleJob = null
            scope.launch { alpha.animateTo(HandleActiveAlpha, HandleWakeSpec) }
        } else {
            restartIdleWait()
        }
    }

    
    internal fun wake() {
        scope.launch { alpha.animateTo(HandleActiveAlpha, HandleWakeSpec) }
        restartIdleWait()
    }

    private fun restartIdleWait() {
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(HandleIdleDelayMs)
            alpha.animateTo(HandleIdleAlpha, HandleFadeSpec)
        }
    }
}

@Composable
fun rememberPlayerHandleState(): PlayerHandleState {
    val scope = rememberCoroutineScope()
    return remember(scope) { PlayerHandleState(scope) }
}

@Composable
fun PlayerHandle(
    state: PlayerHandleState,
    sheet: BottomSheetState,
    statusBarTop: Dp,
    
    enabled: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val currentEnabled by rememberUpdatedState(enabled)
    val bend = remember { Animatable(0f) }
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(sheet) {
        snapshotFlow { pressed || (!sheet.progress > 0.05f && sheet.progress > 0f) }
            .collectLatest { chevron -> bend.animateTo(if (chevron) 1f else 0f, HandleBendSpring) }
    }
    LaunchedEffect(sheet, state) {
        snapshotFlow { sheet.progress > 0.05f }.collectLatest { open -> if (open) state.wake() }
    }

    val targetBounds = remember { FloatArray(4) }
    val onLight by rememberOnLightBehind(sheet, targetBounds)
    val color by animateColorAsState(
        targetValue = if (onLight) Color.Black else Color.White,
        animationSpec = tween(durationMillis = 300),
        label = "playerHandleColor",
    )

    Box(
        modifier = modifier
            .offset(y = statusBarTop + HandleCentreBelowStatusBar - HandleTargetHeight / 2)
            .size(HandleTargetWidth, HandleTargetHeight)
            .onGloballyPositioned {
                val bounds = it.boundsInWindow()
                targetBounds[0] = bounds.left
                targetBounds[1] = bounds.top
                targetBounds[2] = bounds.right
                targetBounds[3] = bounds.bottom
            }
            .pointerInput(sheet) {
                awaitEachGesture {
                    awaitFirstDown()
                    if (!currentEnabled() || sheet.progress < HandleTapMinProgress) {
                        return@awaitEachGesture
                    }
                    pressed = true
                    val up = waitForUpOrCancellation()
                    pressed = false
                    if (up != null) {
                        up.consume()
                        sheet.collapseSoft()
                    }
                }
            }
            .graphicsLayer {
                alpha = if (currentEnabled()) state.alpha.value else 0f
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .drawWithCache {
                val path = Path()
                val stroke = Stroke(
                    width = HandleThickness.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )
                val barHalf = (HandleLength - HandleThickness).toPx() / 2f
                val chevronHalf = (ChevronLength - HandleThickness).toPx() / 2f
                val depthPx = ChevronDepth.toPx()
                onDrawBehind {
                    val b = bend.value
                    val half = lerp(barHalf, chevronHalf, b)
                    val depth = depthPx * b
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    path.reset()
                    path.moveTo(cx - half, cy - depth / 2f)
                    path.lineTo(cx, cy + depth / 2f)
                    path.lineTo(cx + half, cy - depth / 2f)
                    drawPath(path = path, color = color, style = stroke)
                }
            },
    )
}

@Composable
private fun rememberOnLightBehind(sheet: BottomSheetState, targetBounds: FloatArray): State<Boolean> {
    val onLight = remember { mutableStateOf(false) }
    val view = LocalView.current
    val density = LocalDensity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(view, sheet, density) {
        val window = view.context.findActivity()?.window ?: return@LaunchedEffect
        val bitmap = Bitmap.createBitmap(SampleBitmapWidth, SampleBitmapHeight, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(SampleBitmapWidth * SampleBitmapHeight)
        val handler = Handler(Looper.getMainLooper())
        val halfBar = with(density) { HandleLength.toPx() } / 2f
        val below = with(density) { SampleBelowCentre.toPx() }
        val height = with(density) { SampleHeight.toPx() }
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { sheet.isExpanded }.collectLatest { open ->
                if (!open) return@collectLatest
                while (true) {
                    val cx = (targetBounds[0] + targetBounds[2]) / 2f
                    val cy = (targetBounds[1] + targetBounds[3]) / 2f
                    val rect = android.graphics.Rect(
                        (cx - halfBar).toInt(),
                        (cy + below).toInt(),
                        (cx + halfBar).toInt(),
                        (cy + below + height).toInt(),
                    )
                    if (rect.width() > 0 && rect.height() > 0 && copyWindow(window, rect, bitmap, handler)) {
                        bitmap.getPixels(pixels, 0, SampleBitmapWidth, 0, 0, SampleBitmapWidth, SampleBitmapHeight)
                        var sum = 0f
                        for (c in pixels) {
                            sum += 0.299f * ((c shr 16) and 0xFF) + 0.587f * ((c shr 8) and 0xFF) + 0.114f * (c and 0xFF)
                        }
                        val luminance = sum / (pixels.size * 255f)
                        onLight.value = if (onLight.value) luminance > 0.52f else luminance > 0.66f
                    }
                    delay(HandleSampleIntervalMs)
                }
            }
        }
    }
    return onLight
}

private suspend fun copyWindow(window: Window, rect: android.graphics.Rect, bitmap: Bitmap, handler: Handler): Boolean =
    suspendCancellableCoroutine { continuation ->
        try {
            PixelCopy.request(
                window,
                rect,
                bitmap,
                { result -> if (continuation.isActive) continuation.resume(result == PixelCopy.SUCCESS) },
                handler,
            )
        } catch (_: IllegalArgumentException) {
            if (continuation.isActive) continuation.resume(false)
        }
    }

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
