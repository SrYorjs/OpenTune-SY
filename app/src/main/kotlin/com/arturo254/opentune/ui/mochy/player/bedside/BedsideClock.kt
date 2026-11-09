package com.arturo254.opentune.ui.mochy.player.bedside

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

internal enum class BedsideClockFace { BIG, STACKED, ANALOG, COVER }

internal class BedsideInk(private val color: Color) {
    fun ink() = color
    val main = ColorProducer { ink() }
    val soft = ColorProducer { ink().copy(alpha = .58f) }
    fun coverFilter(): ColorFilter? = null
}

internal fun DrawScope.drawCover(painter: Painter, filter: ColorFilter?) {
    val source = painter.intrinsicSize
    if (source.isUnspecified || source.width <= 0f || source.height <= 0f) {
        with(painter) { draw(size, colorFilter = filter) }
        return
    }
    val scale = maxOf(size.width / source.width, size.height / source.height)
    val drawn = Size(source.width * scale, source.height * scale)
    clipRect {
        translate((size.width - drawn.width) / 2f, (size.height - drawn.height) / 2f) {
            with(painter) { draw(drawn, colorFilter = filter) }
        }
    }
}

@Immutable
internal class BedsideTime(
    val hours: String,
    val minutes: String,

    val period: String?,
    val date: String,

    val shortDate: String,
    val hour: Int,
    val minute: Int,
)

private fun bedsideTime(context: Context, millis: Long): BedsideTime {
    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
    val is24 = DateFormat.is24HourFormat(context)
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val shownHour = if (is24) hour else (hour % 12).takeIf { it != 0 } ?: 12
    val datePattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEMMMMd")
    return BedsideTime(
        hours = if (is24) shownHour.toString().padStart(2, '0') else shownHour.toString(),
        minutes = minute.toString().padStart(2, '0'),
        period = if (is24) null else DateFormat.format("a", calendar).toString(),
        date = DateFormat.format(datePattern, calendar).toString(),
        shortDate = DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEd"), calendar).toString(),
        hour = hour,
        minute = minute,
    )
}

@Composable
internal fun rememberBedsideTime(live: Boolean): BedsideTime {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    if (live) {
        DisposableEffect(context) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    now = System.currentTimeMillis()
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            now = System.currentTimeMillis()
            onDispose { context.unregisterReceiver(receiver) }
        }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { now = System.currentTimeMillis() }
    }

    return remember(now) { bedsideTime(context, now) }
}

@Composable
internal fun BedsideClockFaceView(
    face: BedsideClockFace,
    time: BedsideTime,
    ink: BedsideInk,
    cover: Painter,
    modifier: Modifier = Modifier,
) {
    when (face) {
        BedsideClockFace.BIG -> BigFace(time, ink, modifier)
        BedsideClockFace.STACKED -> StackedFace(time, ink, modifier)
        BedsideClockFace.ANALOG -> AnalogFace(time, ink, modifier)
        BedsideClockFace.COVER -> CoverFace(time, ink, cover, modifier)
    }
}

@Composable
private fun BigFace(time: BedsideTime, ink: BedsideInk, modifier: Modifier) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val digits = minOf(maxHeight * 0.5f, maxWidth / 2.9f)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(time.date, style = labelStyle(digits * 0.13f), color = ink.soft)
            Spacer(Modifier.height(digits * 0.04f))
            Row(verticalAlignment = Alignment.Top) {
                FlipDigits("${time.hours}:${time.minutes}", clockStyle(digits, FontWeight.SemiBold), ink.main)
                time.period?.let { BasicText(it, style = labelStyle(digits * 0.14f), color = ink.soft) }
            }
        }
    }
}

@Composable
private fun StackedFace(time: BedsideTime, ink: BedsideInk, modifier: Modifier) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val landscape = maxWidth > maxHeight
        val line = if (landscape) maxHeight * 0.44f else minOf(maxWidth * 0.5f, maxHeight * 0.3f)
        val style = clockStyle(line, FontWeight.Bold, lineHeight = 0.86f)
        val numbers: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FlipDigits(time.hours.padStart(2, '0'), style, ink.main)
                FlipDigits(time.minutes, style, ink.soft)
            }
        }
        val date: @Composable () -> Unit = {
            Column(horizontalAlignment = if (landscape) Alignment.Start else Alignment.CenterHorizontally) {
                BasicText(time.date, style = labelStyle(line * 0.15f), color = ink.soft)
                time.period?.let { BasicText(it, style = labelStyle(line * 0.15f), color = ink.soft) }
            }
        }
        if (landscape) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(line * 0.3f)) {
                numbers()
                date()
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(line * 0.12f)) {
                numbers()
                date()
            }
        }
    }
}

@Composable
private fun AnalogFace(time: BedsideTime, ink: BedsideInk, modifier: Modifier) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val landscape = maxWidth > maxHeight
        val dial = if (landscape) maxHeight * 0.78f else minOf(maxWidth * 0.78f, maxHeight * 0.5f)
        val date: @Composable () -> Unit = {
            BasicText(time.date, style = labelStyle(dial * 0.07f), color = ink.soft)
        }
        if (landscape) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dial * 0.16f)) {
                AnalogDial(time, ink, Modifier.size(dial))
                date()
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(dial * 0.1f)) {
                AnalogDial(time, ink, Modifier.size(dial))
                date()
            }
        }
    }
}

@Composable
internal fun AnalogDial(time: BedsideTime, ink: BedsideInk, modifier: Modifier, ring: Boolean = false) {
    val minuteTurn = remember { Animatable(minuteAngle(time)) }
    val hourTurn = remember { Animatable(hourAngle(time)) }
    LaunchedEffect(time.hour, time.minute) {
        launch { minuteTurn.animateTo(minuteTurn.value + turnBetween(minuteTurn.value, minuteAngle(time)), HandSpring) }
        launch { hourTurn.animateTo(hourTurn.value + turnBetween(hourTurn.value, hourAngle(time)), HandSpring) }
    }
    Spacer(modifier.drawWithCache {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)

        val ticks = FloatArray(60 * 4)
        for (i in 0 until 60) {
            val a = Math.toRadians(i * 6.0 - 90.0)
            val inner = when {
                ring -> r * 0.86f
                i % 5 == 0 -> r * 0.84f
                else -> r * 0.92f
            }
            ticks[i * 4] = c.x + cos(a).toFloat() * inner
            ticks[i * 4 + 1] = c.y + sin(a).toFloat() * inner
            ticks[i * 4 + 2] = c.x + cos(a).toFloat() * r * 0.98f
            ticks[i * 4 + 3] = c.y + sin(a).toFloat() * r * 0.98f
        }
        val major = r * if (ring) 0.03f else 0.022f
        val minor = r * if (ring) 0.03f else 0.01f
        onDrawBehind {
            val colour = ink.ink()
            val faint = colour.copy(alpha = colour.alpha * if (ring) 0.55f else 0.35f)
            for (i in 0 until 60) {
                val bold = !ring && i % 5 == 0
                drawLine(
                    color = if (bold) colour else faint,
                    start = Offset(ticks[i * 4], ticks[i * 4 + 1]),
                    end = Offset(ticks[i * 4 + 2], ticks[i * 4 + 3]),
                    strokeWidth = if (bold) major else minor,
                    cap = StrokeCap.Round,
                )
            }
            rotate(hourTurn.value, c) {
                drawLine(colour, c, Offset(c.x, c.y - r * 0.48f), strokeWidth = r * if (ring) 0.075f else 0.055f, cap = StrokeCap.Round)
            }
            rotate(minuteTurn.value, c) {
                drawLine(colour, c, Offset(c.x, c.y - r * 0.74f), strokeWidth = r * if (ring) 0.06f else 0.032f, cap = StrokeCap.Round)
            }
            if (!ring) {
                drawCircle(colour, r * 0.045f, c)
                drawCircle(Color.Black, r * 0.018f, c)
            }
        }
    })
}

@Composable
private fun CoverFace(time: BedsideTime, ink: BedsideInk, cover: Painter, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val digits = minOf(maxHeight * 0.58f, maxWidth / 2.75f)
        val style = clockStyle(digits, FontWeight.Black)
        val text = "${time.hours}:${time.minutes}"
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(time.date, style = labelStyle(digits * 0.12f), color = ink.soft)
            Spacer(Modifier.height(digits * 0.04f))

            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(digits)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithCache {
                        val layout = measurer.measure(text, style)
                        val topLeft = Offset((size.width - layout.size.width) / 2f, (size.height - layout.size.height) / 2f)
                        onDrawBehind {
                            drawRect(ink.ink())
                            drawCover(cover, ink.coverFilter())
                            drawText(layout, color = Color.Black, topLeft = topLeft, blendMode = BlendMode.DstIn)
                        }
                    },
            )
        }
    }
}

@Composable
internal fun FlipDigits(text: String, style: TextStyle, color: ColorProducer) {
    Row {
        text.forEachIndexed { index, char ->

            key(text.length - index) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        (slideInVertically(DigitSlide) { it / 3 } + fadeIn(tween(420))) togetherWith
                            (slideOutVertically(DigitSlide) { -it / 3 } + fadeOut(tween(260))) using
                            SizeTransform(clip = false)
                    },
                    contentAlignment = Alignment.Center,
                    label = "bedsideDigit",
                ) { shown ->
                    BasicText(shown.toString(), style = style, color = color)
                }
            }
        }
    }
}

private val DigitSlide = spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = 0.85f, stiffness = 260f)
private val HandSpring = spring<Float>(dampingRatio = 0.62f, stiffness = 140f)

private fun minuteAngle(time: BedsideTime) = time.minute * 6f
private fun hourAngle(time: BedsideTime) = (time.hour % 12) * 30f + time.minute * 0.5f

private fun turnBetween(from: Float, to: Float): Float = ((to - from) % 360f + 540f) % 360f - 180f

@Composable
internal fun clockStyle(size: Dp, weight: FontWeight, lineHeight: Float = 1f): TextStyle {
    val sp = dpToSp(size)
    return TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = weight,
        fontSize = sp,
        lineHeight = sp * lineHeight,
        letterSpacing = (-0.04).em,
        fontFeatureSettings = "tnum",
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
}

@Composable
internal fun labelStyle(size: Dp, weight: FontWeight = FontWeight.Medium): TextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = dpToSp(size),
    letterSpacing = (-0.02).em,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@Composable
private fun dpToSp(size: Dp): TextUnit = with(LocalDensity.current) { size.toSp() }
