package com.arturo254.opentune.ui.visual.component

import android.content.Context
import android.graphics.Shader
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.arturo254.opentune.R
import com.arturo254.opentune.models.MediaMetadata
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal val LocalHeroPeek = staticCompositionLocalOf { false }

private fun smoothstep(from: Float, to: Float, x: Float): Float {
    val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private const val GachaDesignHeight = 527f
private const val GachaItemSize = 187f
private const val GachaFocusSize = 221f
private const val GachaPitch = 207f
private const val GachaFocusGap = 17f
private const val GachaCenterY = 230.5f
private const val GachaOverhang = 28f
private const val GachaCornerRadius = 25f
private const val GachaPointerX = 120f
private const val GachaPointerY = 236f
private const val GachaPointerWidth = 68f
private const val GachaPointerHeight = 75f

private const val GachaCandidates = 10
private const val GachaMinPool = 4
private const val GachaMinRollMillis = 3500
private const val GachaMaxRollMillis = 9000

private val GachaRollEasing = CubicBezierEasing(0.15f, 0.45f, 0.2f, 1f)

private const val IdleStepSeconds = 1f / 30f
private const val IdleWakeMillis = 28L

private const val TopShadeEnd = 0.44f
private const val BottomShadeStart = 0.56f

private val WashStops = floatArrayOf(0f, 0.5f, 1f)
private val WanderStops = floatArrayOf(0f, 0.5f, 1f)
private val GlowStops = floatArrayOf(0f, 0.45f, 1f)

private class UnitShader(private val make: (IntArray) -> android.graphics.Shader) {
    private var cached: android.graphics.Shader? = null
    private var c0 = 0
    private var c1 = 0
    private var c2 = 0

    fun shader(a: Int, b: Int, c: Int, matrix: android.graphics.Matrix): android.graphics.Shader {
        val current = cached
        val shader = if (current != null && a == c0 && b == c1 && c == c2) {
            current
        } else {
            make(intArrayOf(a, b, c)).also {
                cached = it
                c0 = a
                c1 = b
                c2 = c
            }
        }
        shader.setLocalMatrix(matrix)
        return shader
    }
}

private fun settle(from: Color, to: Color, fraction: Float): Color {
    val next = lerp(from, to, fraction)
    return if (abs(next.red - to.red) < 0.002f && abs(next.green - to.green) < 0.002f &&
        abs(next.blue - to.blue) < 0.002f && abs(next.alpha - to.alpha) < 0.002f
    ) to else next
}

private val GachaPointerColor = Color(0xFFF0908D)
private val GachaPipColor = Color(0xFF3C3C3C)

@Immutable
private class GachaTint(val light: Color, val deep: Color)

private val GachaDefaultTint = GachaTint(light = Color(0xFFDEA0AC), deep = Color(0xFF7C82B4))

private suspend fun gachaTint(context: Context, url: String): GachaTint? = withContext(Dispatchers.IO) {
    val request = ImageRequest.Builder(context)
        .data(url)
        .size(64)
        .allowHardware(false)
        .build()
    val bitmap = context.imageLoader.execute(request).image?.toBitmap() ?: return@withContext null
    val palette = Palette.from(bitmap).maximumColorCount(12).generate()
    val lit = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.dominantSwatch ?: return@withContext null
    val deep = palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.mutedSwatch ?: lit
    GachaTint(
        light = shiftBrightness(lit.rgb, minValue = 0.78f, maxValue = 1f, maxSaturation = 0.8f),
        deep = shiftBrightness(deep.rgb, minValue = 0.28f, maxValue = 0.5f, maxSaturation = 0.85f),
    )
}

private fun shiftBrightness(rgb: Int, minValue: Float, maxValue: Float, maxSaturation: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(rgb, hsv)
    hsv[1] = hsv[1].coerceAtMost(maxSaturation)
    hsv[2] = hsv[2].coerceIn(minValue, maxValue)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun easedShade(height: Float, alphaAt: (Float) -> Float): android.graphics.LinearGradient {
    val steps = 24
    val positions = FloatArray(steps + 1) { it / steps.toFloat() }
    val colors = IntArray(steps + 1) { android.graphics.Color.argb((alphaAt(positions[it]) * 255f).roundToInt(), 0, 0, 0) }
    return android.graphics.LinearGradient(0f, 0f, 0f, height, colors, positions, Shader.TileMode.CLAMP)
}

private const val GachaMeshShader = """
uniform float2 size;
uniform float time;
uniform float2 washFrom;
uniform float2 washDir;
uniform float3 wander;
uniform float3 glow;
uniform float glowCore;
uniform float3 bloomA;
uniform float3 bloomB;
uniform float3 bloomC;
layout(color) uniform half4 washA;
layout(color) uniform half4 washB;
layout(color) uniform half4 washC;
layout(color) uniform half4 lit;
layout(color) uniform half4 accentA;
layout(color) uniform half4 accentB;
layout(color) uniform half4 accentC;

float3 light(float3 col, float3 tint, float d, float core, float mid, float knee) {
    float dd = clamp(d, 0.0, 1.0);
    float a;
    float3 rgb;
    if (dd < knee) {
        a = mix(core, mid, dd / knee);
        rgb = tint;
    } else {
        float u = (dd - knee) / (1.0 - knee);
        a = mid * (1.0 - u);
        rgb = tint * (1.0 - u);
    }
    return col * (1.0 - a) + rgb * a;
}

half4 main(float2 p) {
    float2 uv = p / size;
    float2 q = p + float2(
        sin(uv.y * 4.2 + time * 0.38) + 0.5 * sin(uv.x * 6.3 - time * 0.21),
        cos(uv.x * 3.6 - time * 0.31) + 0.5 * cos(uv.y * 5.1 + time * 0.27)
    ) * size.x * 0.03;

    float s = clamp(dot(q - washFrom, washDir) / dot(washDir, washDir), 0.0, 1.0);
    float3 col = s < 0.5
        ? mix(float3(washA.rgb), float3(washB.rgb), s * 2.0)
        : mix(float3(washB.rgb), float3(washC.rgb), s * 2.0 - 1.0);

    float bc = 1.0 - smoothstep(0.0, 1.0, length(q - bloomC.xy) / bloomC.z);
    col = mix(col, float3(accentC.rgb), bc * 0.4);
    float ba = 1.0 - smoothstep(0.0, 1.0, length(q - bloomA.xy) / bloomA.z);
    col = mix(col, float3(accentA.rgb), ba * 0.62);
    float bb = 1.0 - smoothstep(0.0, 1.0, length(q - bloomB.xy) / bloomB.z);
    col = mix(col, float3(accentB.rgb), bb * 0.55);

    col = light(col, float3(lit.rgb), length(q - wander.xy) / wander.z, 0.32, 0.1, 0.5);
    col = light(col, float3(lit.rgb), length(p - glow.xy) / glow.z, glowCore, 0.32, 0.45);

    float n = fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
    col += (n - 0.5) / 255.0;
    return half4(half3(col), 1.0);
}
"""

@androidx.annotation.RequiresApi(33)
private fun createGachaMesh(): Any = createAgslShader(GachaMeshShader)

private val GachaHsv = FloatArray(3)

private fun shiftHue(color: Color, degrees: Float, saturation: Float, value: Float): Int {
    android.graphics.Color.colorToHSV(color.toArgb(), GachaHsv)
    GachaHsv[0] = (GachaHsv[0] + degrees).mod(360f)
    GachaHsv[1] = (GachaHsv[1] * saturation).coerceIn(0f, 0.9f)
    GachaHsv[2] = (GachaHsv[2] * value).coerceIn(0f, 1f)
    return android.graphics.Color.HSVToColor(GachaHsv)
}

@androidx.annotation.RequiresApi(33)
private fun drawGachaMesh(
    canvas: android.graphics.Canvas,
    shader: Any,
    paint: android.graphics.Paint,
    width: Float,
    height: Float,
    lit: Color,
    deep: Color,
    above: Color,
    below: Color,
    washFromX: Float,
    washDirX: Float,
    washDirY: Float,
    wanderX: Float,
    wanderY: Float,
    wanderR: Float,
    glowX: Float,
    glowY: Float,
    glowR: Float,
    glowCore: Float,
    time: Float,
) {
    shader.setAgslUniform("size", width, height)
    shader.setAgslUniform("time", time)
    shader.setAgslUniform("washFrom", washFromX, 0f)
    shader.setAgslUniform("washDir", washDirX, washDirY)
    shader.setAgslUniform("wander", wanderX, wanderY, wanderR)
    shader.setAgslUniform("glow", glowX, glowY, glowR)
    shader.setAgslUniform("glowCore", glowCore)

    shader.setAgslUniform(
        "bloomA",
        width * (0.3f + 0.16f * sin(time * 0.17f + 0.7f)),
        height * (0.4f + 0.08f * kotlin.math.cos(time * 0.23f)),
        width * 0.68f,
    )
    shader.setAgslUniform(
        "bloomB",
        width * (0.72f + 0.12f * sin(time * 0.13f + 2.1f)),
        height * (0.64f + 0.07f * sin(time * 0.29f + 0.4f)),
        width * 0.72f,
    )
    shader.setAgslUniform(
        "bloomC",
        width * (0.55f + 0.2f * sin(time * 0.11f + 4.2f)),
        height * (0.24f + 0.06f * kotlin.math.cos(time * 0.19f + 1.1f)),
        width * 0.6f,
    )
    shader.setAgslColorUniform("washA", lerp(deep, lit, 0.55f).toArgb())
    shader.setAgslColorUniform("washB", lerp(deep, lit, 0.22f).toArgb())
    shader.setAgslColorUniform("washC", deep.toArgb())
    shader.setAgslColorUniform("lit", lit.toArgb())

    shader.setAgslColorUniform("accentA", shiftHue(above, 26f, 1.05f, 0.95f))
    shader.setAgslColorUniform("accentB", shiftHue(below, -22f, 1.1f, 1.45f))
    shader.setAgslColorUniform("accentC", shiftHue(lit, 48f, 1f, 0.85f))
    paint.shader = shader.asAgslPaintShader()
    canvas.drawRect(0f, 0f, width, height, paint)
}

private enum class GachaPhase { Idle, Rolling, Landed }

@Stable
private class GachaState {
    var candidates by mutableStateOf<List<MediaMetadata>>(emptyList())
    var phase by mutableStateOf(GachaPhase.Idle)
    var result by mutableStateOf<MediaMetadata?>(null)

    
    val reel = Animatable(0f)
    val pointerKick = Animatable(0f)
    val landPop = Animatable(1f)

    
    val burst = Animatable(0f)

    
    val tints = mutableStateMapOf<String, GachaTint>()
}

private fun pickGachaCandidates(pool: List<MediaMetadata>): List<MediaMetadata> =
    pool.distinctBy { it.id }.shuffled().take(GachaCandidates)

private fun refreshHiddenCandidates(state: GachaState, pool: List<MediaMetadata>): List<MediaMetadata> {
    val current = state.candidates
    if (current.isEmpty()) return pickGachaCandidates(pool)
    val spare = pool
        .distinctBy { it.id }
        .filter { song -> current.none { it.id == song.id } }
        .shuffled()
        .iterator()
    val reel = state.reel.value
    return current.mapIndexed { index, song ->
        if (abs(reelDistance(index, reel, current.size)) > 2.5f && spare.hasNext()) spare.next() else song
    }
}

private fun reelDistance(index: Int, reel: Float, count: Int): Float {
    var d = (reel - index) % count
    if (d < -count / 2f) d += count
    if (d >= count / 2f) d -= count
    return d
}

private class GachaSounds(private val context: Context) {
    private var pool: SoundPool? = null
    private var tickSound = 0
    private var winSound = 0
    private var lastTick = 0L

    fun prepare() {
        if (pool != null) return
        val created = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        tickSound = created.load(context, R.raw.gacha_tick, 1)
        winSound = created.load(context, R.raw.gacha_win, 1)
        pool = created
    }

    fun tick() {

        val now = SystemClock.uptimeMillis()
        if (now - lastTick < 28L) return
        lastTick = now
        pool?.play(tickSound, 0.5f, 0.5f, 1, 0, 0.94f + Random.nextFloat() * 0.12f)
    }

    fun win() {
        pool?.play(winSound, 0.75f, 0.75f, 2, 0, 1f)
    }

    fun release() {
        pool?.release()
        pool = null
    }
}

@Composable
private fun GachaPage(
    state: GachaState,
    pageOffset: () -> Float,
    isResultPlaying: Boolean,
    wipeCurve: () -> Float,
    transitionBlur: Boolean,
    
    held: () -> Boolean,
    onShown: () -> Unit,
    onRoll: () -> Unit,
) {
    val context = LocalContext.current
    val rolling = state.phase == GachaPhase.Rolling
    val result = state.result

    LaunchedEffect(Unit) { onShown() }

    val candidates = state.candidates
    LaunchedEffect(candidates) {
        candidates.forEach { song ->
            if (song.id in state.tints) return@forEach
            val url = song.thumbnailUrl ?: return@forEach
            gachaTint(context, url)?.let { state.tints[song.id] = it }
        }
    }

    val light = remember { mutableStateOf(GachaDefaultTint.light) }
    val deep = remember { mutableStateOf(GachaDefaultTint.deep) }

    val above = remember { mutableStateOf(GachaDefaultTint.light) }
    val below = remember { mutableStateOf(GachaDefaultTint.deep) }
    val clock = remember { mutableFloatStateOf(0f) }
    val speed = remember { mutableFloatStateOf(0f) }

    val currentHeld by androidx.compose.runtime.rememberUpdatedState(held)
    LaunchedEffect(state) {
        var lastNanos = 0L
        var lastReel = state.reel.value
        var idleTime = 0f
        var wasBusy = false
        while (isActive) {

            if (abs(pageOffset()) >= 0.999f || currentHeld()) {
                lastNanos = 0L
                snapshotFlow { abs(pageOffset()) < 0.999f && !currentHeld() }.first { it }
                continue
            }
            withFrameNanos { now ->
                val dt = if (lastNanos == 0L) 1f / 60f else ((now - lastNanos) / 1e9f).coerceIn(0f, 0.1f)
                lastNanos = now
                val reel = state.reel.value
                val count = state.candidates.size

                val instant = abs(reel - lastReel) / dt.coerceAtLeast(1e-3f)
                speed.floatValue += (instant - speed.floatValue) * (1f - exp(-dt / 0.12f))
                lastReel = reel

                var aboveTarget = GachaDefaultTint.light
                var belowTarget = GachaDefaultTint.deep
                val target = if (count == 0) {
                    GachaDefaultTint
                } else {
                    val base = floor(reel).toInt()
                    val between = reel - base
                    fun tintAt(offset: Int) = state.tints[state.candidates[(base + offset).mod(count)].id]
                    val from = tintAt(0) ?: GachaDefaultTint
                    val to = tintAt(1) ?: from
                    aboveTarget = lerp((tintAt(-1) ?: from).light, from.light, between)
                    belowTarget = lerp(to.deep, (tintAt(2) ?: to).deep, between)
                    GachaTint(lerp(from.light, to.light, between), lerp(from.deep, to.deep, between))
                }
                val busy = speed.floatValue > 0.05f || state.burst.value > 0f

                idleTime += dt
                if (busy || idleTime >= IdleStepSeconds) {
                    val step = if (busy) dt else idleTime
                    idleTime = 0f
                    val follow = 1f - exp(-step / 0.22f)
                    light.value = settle(light.value, target.light, follow)
                    deep.value = settle(deep.value, target.deep, follow)
                    above.value = settle(above.value, aboveTarget, follow)
                    below.value = settle(below.value, belowTarget, follow)
                    clock.floatValue += step
                }
                wasBusy = busy
            }

            if (!wasBusy) delay(IdleWakeMillis)
        }
    }

    val peek = LocalHeroPeek.current
    PageWipe(pageOffset = pageOffset, curve = wipeCurve, blur = transitionBlur, peek = peek) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().pageMediaDrift(pageOffset, peek)) {
            val unit = maxHeight / GachaDesignHeight
            val focus = unit * GachaFocusSize
            val coverShape = RoundedCornerShape(unit * GachaCornerRadius)
            val count = candidates.size

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer()
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        val focusPx = focus.toPx()
                        val overhangPx = (unit * GachaOverhang).toPx()
                        val glowX = w + overhangPx - focusPx / 2f
                        val glowY = (unit * GachaCenterY).toPx()
                        val mesh: Any? = if (Build.VERSION.SDK_INT >= 33) createGachaMesh() else null
                        val meshPaint = android.graphics.Paint()
                        val washPaint = android.graphics.Paint().apply { isDither = true }
                        val wanderPaint = android.graphics.Paint().apply { isDither = true }
                        val glowPaint = android.graphics.Paint().apply { isDither = true }
                        val ringPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            style = android.graphics.Paint.Style.STROKE
                        }
                        val wash = UnitShader { colors -> android.graphics.LinearGradient(0f, 0f, 1f, 0f, colors, WashStops, Shader.TileMode.CLAMP) }
                        val wander = UnitShader { colors -> android.graphics.RadialGradient(0f, 0f, 1f, colors, WanderStops, Shader.TileMode.CLAMP) }
                        val glow = UnitShader { colors -> android.graphics.RadialGradient(0f, 0f, 1f, colors, GlowStops, Shader.TileMode.CLAMP) }
                        val matrix = android.graphics.Matrix()
                        val values = FloatArray(9)
                        onDrawBehind {
                            val lit = light.value
                            val dark = deep.value
                            val t = clock.floatValue
                            val rush = (speed.floatValue / 30f).coerceIn(0f, 1f)
                            val win = state.burst.value
                            drawIntoCanvas { canvas ->
                                val native = canvas.nativeCanvas

                                val sway = sin(t * 0.35f) * w * 0.12f
                                val x0 = w * 0.85f + sway
                                val dx = (w * 0.15f - sway) - x0
                                val dy = h

                                val wanderX = w * (0.18f + 0.08f * sin(t * 0.23f))
                                val wanderY = h * (0.55f + 0.1f * sin(t * 0.31f + 1.3f))
                                val wanderR = w * 0.7f

                                val breathe = 0.05f * sin(t * 1.4f)
                                val flare = if (win > 0f) sin(win * PI.toFloat()) * 0.55f else 0f
                                val glowR = focusPx * (1.05f + breathe + rush * 0.35f + flare)
                                val glowY2 = glowY + sin(t * 0.8f) * focusPx * 0.04f
                                val glowCore = (0.75f + rush * 0.2f).coerceAtMost(1f)

                                if (mesh != null && Build.VERSION.SDK_INT >= 33) {
                                    drawGachaMesh(
                                        canvas = native,
                                        shader = mesh,
                                        paint = meshPaint,
                                        width = w,
                                        height = h,
                                        lit = lit,
                                        deep = dark,
                                        above = above.value,
                                        below = below.value,
                                        washFromX = x0,
                                        washDirX = dx,
                                        washDirY = dy,
                                        wanderX = wanderX,
                                        wanderY = wanderY,
                                        wanderR = wanderR,
                                        glowX = glowX,
                                        glowY = glowY2,
                                        glowR = glowR,
                                        glowCore = glowCore,
                                        time = t,
                                    )
                                } else {

                                    values[0] = dx; values[1] = -dy; values[2] = x0
                                    values[3] = dy; values[4] = dx; values[5] = 0f
                                    values[6] = 0f; values[7] = 0f; values[8] = 1f
                                    matrix.setValues(values)
                                    washPaint.shader = wash.shader(
                                        lerp(dark, lit, 0.55f).toArgb(),
                                        lerp(dark, lit, 0.22f).toArgb(),
                                        dark.toArgb(),
                                        matrix,
                                    )
                                    native.drawRect(0f, 0f, w, h, washPaint)

                                    matrix.setScale(wanderR, wanderR)
                                    matrix.postTranslate(wanderX, wanderY)
                                    wanderPaint.shader = wander.shader(
                                        lit.copy(alpha = 0.32f).toArgb(),
                                        lit.copy(alpha = 0.1f).toArgb(),
                                        Color.Transparent.toArgb(),
                                        matrix,
                                    )
                                    native.drawCircle(wanderX, wanderY, wanderR, wanderPaint)

                                    matrix.setScale(glowR, glowR)
                                    matrix.postTranslate(glowX, glowY2)
                                    glowPaint.shader = glow.shader(
                                        lit.copy(alpha = glowCore).toArgb(),
                                        lit.copy(alpha = 0.32f).toArgb(),
                                        Color.Transparent.toArgb(),
                                        matrix,
                                    )
                                    native.drawCircle(glowX, glowY2, glowR, glowPaint)
                                }

                                if (win > 0f && win < 1f) {
                                    ringPaint.color = lerp(lit, Color.White, 0.5f).copy(alpha = (1f - win) * 0.9f).toArgb()
                                    ringPaint.strokeWidth = (1f - win) * 10.dp.toPx() + 1.dp.toPx()
                                    native.drawCircle(glowX, glowY, focusPx * (0.55f + win * 1.1f), ringPaint)
                                }
                            }
                        }
                    }
            )

            val blankCover = remember { ColorPainter(Color.White) }
            candidates.forEachIndexed { index, song ->
                key(song.id) {
                    AsyncImage(
                        model = song.thumbnailUrl?.resize(544, 544),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        placeholder = blankCover,
                        error = blankCover,
                        fallback = blankCover,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = unit * GachaOverhang, y = unit * GachaCenterY - focus / 2)
                            .size(focus)
                            .graphicsLayer {
                                val d = reelDistance(index, state.reel.value, count)
                                val near = (1f - abs(d)).coerceIn(0f, 1f)
                                val landed = if (near > 0.5f) state.landPop.value else 1f
                                val scale = (GachaItemSize + (GachaFocusSize - GachaItemSize) * near) / GachaFocusSize * landed

                                val stretch = 1f + (speed.floatValue / 120f).coerceAtMost(0.1f)
                                scaleX = scale
                                scaleY = scale * stretch

                                transformOrigin = TransformOrigin(1f, 0.5f)
                                val slots = d * GachaPitch + GachaFocusGap * d.coerceIn(-1f, 1f)
                                translationY = slots * unit.toPx()

                                rotationX = (-d * 18f).coerceIn(-50f, 50f)
                                cameraDistance = 14f * density

                                alpha = (2.7f - abs(d)).coerceIn(0f, 1f)
                                compositingStrategy = CompositingStrategy.ModulateAlpha
                                shape = coverShape
                                clip = true
                            },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer()
                    .drawWithCache {
                        val h = size.height
                        val shadePaint = android.graphics.Paint().apply { isDither = true }
                        val top = easedShade(h) { y -> 0.85f * (1f - smoothstep(0f, TopShadeEnd, y)).let { it * it } }
                        val bottom = easedShade(h) { y -> smoothstep(BottomShadeStart, 1f, y) }
                        onDrawBehind {
                            drawIntoCanvas { canvas ->
                                shadePaint.shader = top
                                canvas.nativeCanvas.drawRect(0f, 0f, size.width, h * TopShadeEnd, shadePaint)
                                shadePaint.shader = bottom
                                canvas.nativeCanvas.drawRect(0f, h * BottomShadeStart, size.width, h, shadePaint)
                            }
                        }
                    }
            )

            Icon(
                painter = painterResource(R.drawable.ic_untitled_play),
                contentDescription = null,
                tint = GachaPointerColor,
                modifier = Modifier
                    .offset(
                        x = unit * (GachaPointerX - GachaPointerWidth / 2f),
                        y = unit * (GachaPointerY - GachaPointerHeight / 2f),
                    )
                    .size(width = unit * GachaPointerWidth, height = unit * GachaPointerHeight)
                    .graphicsLayer {
                        val kick = state.pointerKick.value
                        translationX = -kick * 7.dp.toPx()
                        rotationZ = -kick * 9f

                        val win = state.burst.value
                        val pop = if (win > 0f) 1f + 0.18f * sin(win * PI.toFloat()) else 1f
                        scaleX = pop
                        scaleY = pop
                    }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = unit * GachaOverhang, y = unit * GachaCenterY - focus / 2)
                    .size(focus)
                    .clickable(
                        interactionSource = null,
                        indication = RemoteFocus,
                        enabled = !rolling,
                        onClick = onRoll,
                    )
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .pageTextDrift(pageOffset, peek)
        ) {
            HeroText(
                label = when {
                    result == null || rolling -> "Gacha here!"
                    isResultPlaying -> "Now playing"
                    else -> "You got"
                },
                title = if (result == null || rolling) {
                    "Random plays"
                } else {
                    val artists = result.artists.joinToString(", ") { it.name }
                    if (artists.isNotEmpty()) "${result.title} - $artists" else result.title
                },
                pill = if (rolling) HeroPillKind.Rolling else HeroPillKind.Roll,
                pillEnabled = !rolling,
                onPillClick = onRoll,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
internal fun GachaDie(spinning: Boolean) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(spinning) {
        if (spinning) {
            while (true) {
                rotation.animateTo(rotation.value + 360f, tween(620, easing = LinearEasing))
            }
        } else {

            val rest = (rotation.value / 90f).roundToInt() * 90f
            rotation.animateTo(rest, spring(dampingRatio = 0.45f, stiffness = 300f))
        }
    }
    Canvas(
        modifier = Modifier
            .size(21.dp)
            .graphicsLayer { rotationZ = rotation.value }
    ) {
        drawRoundRect(color = Color.White, cornerRadius = CornerRadius(4.5.dp.toPx()))
        val pip = 2.1.dp.toPx()
        val near = size.width * 0.28f
        val far = size.width * 0.72f
        val mid = size.width / 2f
        listOf(
            Offset(near, near), Offset(far, near),
            Offset(mid, mid),
            Offset(near, far), Offset(far, far),
        ).forEach { drawCircle(color = GachaPipColor, radius = pip, center = it) }
    }
}
