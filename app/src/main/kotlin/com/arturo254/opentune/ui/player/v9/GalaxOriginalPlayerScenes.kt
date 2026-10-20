package com.arturo254.opentune.ui.player.v9

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.core.designsystem.component.GalaxOrbitField
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Immutable
data class GalaxOriginalStyleConfig(
    val name: String,
    val subtitle: String,
    val description: String,
    val cornerRadius: Float,
    val roundPlay: Boolean,
    val artScale: Float,
    val ornament: String,
)

fun originalPlayerStyleConfig(style: PlayerDesignStyle): GalaxOriginalStyleConfig = when (style) {
    PlayerDesignStyle.V9 -> GalaxOriginalStyleConfig(
        "NEBULA DRIFT", "DEEP SPACE", "Nebulosas en capas y portada flotante",
        32f, true, .72f, "NOVA  /  09",
    )
    PlayerDesignStyle.V10 -> GalaxOriginalStyleConfig(
        "STELLAR RINGS", "CELESTIAL", "Órbitas concéntricas y movimiento orbital",
        90f, true, .68f, "ORBIT  /  10",
    )
    PlayerDesignStyle.V11 -> GalaxOriginalStyleConfig(
        "CYBERGRID", "SYNTH CIRCUIT", "Rejilla digital, esquinas rectas y luces de neón",
        9f, false, .77f, "GRID  /  11",
    )
    PlayerDesignStyle.V12 -> GalaxOriginalStyleConfig(
        "VINYL ORBIT", "ANALOG SOUL", "Disco circular y surcos de vinilo",
        55f, true, .70f, "VINYL  /  12",
    )
    PlayerDesignStyle.V13 -> GalaxOriginalStyleConfig(
        "AURORA FLOW", "NORTHERN LIGHTS", "Auroras suaves y composición atmosférica",
        37f, false, .82f, "AURORA  /  13",
    )
    PlayerDesignStyle.V14 -> GalaxOriginalStyleConfig(
        "MONOLITH", "MONO EDITION", "Tipografía monumental y geometría esencial",
        4f, false, .56f, "MONO  /  14",
    )
    PlayerDesignStyle.V15 -> GalaxOriginalStyleConfig(
        "QUANTUM PULSE", "ENERGY CORE", "Ondas de energía sincronizadas con la reproducción",
        23f, true, .75f, "PULSE  /  15",
    )
    PlayerDesignStyle.V17 -> GalaxOriginalStyleConfig(
        "VINJAX", "33⅓ RPM", "Tocadiscos analógico con aguja móvil",
        40f, true, .73f, "VINJAX  /  17",
    )
    else -> GalaxOriginalStyleConfig(
        "GALAXOT", "MUSIC UNIVERSE", "Tu música sin límites", 24f, true, .72f, "GALAXOT",
    )
}

@Composable
fun GalaxOriginalPlayerScene(
    style: PlayerDesignStyle,
    isLandscape: Boolean,
    isPlaying: Boolean,
    colors: List<Color>,
    queuePeekHeight: Dp,
    artwork: @Composable BoxScope.() -> Unit,
    controls: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = remember(style) { originalPlayerStyleConfig(style) }
    val accent = colors.firstOrNull() ?: MaterialTheme.colorScheme.primary
    val alternate = colors.getOrNull(1) ?: MaterialTheme.colorScheme.tertiary
    Box(
        modifier = modifier.fillMaxSize()
            .background(Color(0xFF060813))
            .windowInsetsPadding(
                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            ),
    ) {
        GalaxOriginalBackdrop(
            style = style,
            accent = accent,
            secondary = alternate,
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxSize(),
        )
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = queuePeekHeight + 14.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(17.dp),
            ) {
                BoxWithConstraints(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    val art = min(maxWidth.value * config.artScale, maxHeight.value * .9f).dp
                    GalaxOriginalArtwork(
                        style = style,
                        accent = accent,
                        isPlaying = isPlaying,
                        modifier = Modifier.size(art),
                        artwork = artwork,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    GalaxOriginalMasthead(style, config, accent, isPlaying, horizontal = true)
                    Spacer(Modifier.height(13.dp))
                    controls()
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(bottom = queuePeekHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (style != PlayerDesignStyle.V11 && style != PlayerDesignStyle.V13) {
                    GalaxOriginalMasthead(
                        style = style,
                        config = config,
                        accent = accent,
                        isPlaying = isPlaying,
                        horizontal = false,
                    )
                }
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    val width = maxWidth.value * config.artScale
                    val height = maxHeight.value * .89f
                    val artSize = min(width, height).coerceAtLeast(60f).dp
                    GalaxOriginalArtwork(
                        style = style,
                        accent = accent,
                        isPlaying = isPlaying,
                        modifier = Modifier.size(artSize),
                        artwork = artwork,
                    )
                }
                if (style == PlayerDesignStyle.V11 || style == PlayerDesignStyle.V13) {
                    GalaxOriginalMasthead(
                        style = style,
                        config = config,
                        accent = accent,
                        isPlaying = isPlaying,
                        horizontal = false,
                    )
                }
                GalaxOriginalControlSurface(
                    style = style,
                    accent = accent,
                ) {
                    controls()
                }
                Spacer(Modifier.height(13.dp))
            }
        }
    }
}

@Composable
private fun GalaxOriginalMasthead(
    style: PlayerDesignStyle,
    config: GalaxOriginalStyleConfig,
    accent: Color,
    isPlaying: Boolean,
    horizontal: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(
            start = 25.dp, end = 20.dp, top = if (horizontal) 3.dp else 19.dp, bottom = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = config.ornament,
                color = accent,
                letterSpacing = 2.1.sp,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (isPlaying) "●  LIVE" else "○  PAUSED",
                color = Color.White.copy(alpha = .72f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Text(
            text = config.name,
            color = Color.White,
            style = if (style == PlayerDesignStyle.V14) MaterialTheme.typography.headlineMedium
                else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = if (style == PlayerDesignStyle.V11) 2.8.sp else 1.2.sp,
            maxLines = 1,
        )
        Text(
            text = config.description,
            color = Color.White.copy(alpha = .62f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun GalaxOriginalArtwork(
    style: PlayerDesignStyle,
    accent: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    artwork: @Composable BoxScope.() -> Unit,
) {
    val shape: Shape = when (style) {
        PlayerDesignStyle.V10, PlayerDesignStyle.V12 -> CircleShape
        PlayerDesignStyle.V11 -> RoundedCornerShape(3.dp)
        PlayerDesignStyle.V13 -> RoundedCornerShape(
            topStart = 72.dp, topEnd = 15.dp,
            bottomEnd = 72.dp, bottomStart = 15.dp,
        )
        PlayerDesignStyle.V14 -> RoundedCornerShape(0.dp)
        PlayerDesignStyle.V15 -> RoundedCornerShape(19.dp)
        else -> RoundedCornerShape(32.dp)
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(accent.copy(alpha = .15f)),
        contentAlignment = Alignment.Center,
    ) {
        artwork()
        if (style == PlayerDesignStyle.V12) {
            Canvas(Modifier.fillMaxSize()) {
                val center = center
                val radius = size.minDimension * .48f
                for (index in 0..10) {
                    drawCircle(
                        Color.Black.copy(alpha = .10f),
                        radius = radius * (1f - index * .073f),
                        center = center,
                        style = Stroke(width = .85.dp.toPx()),
                    )
                }
                drawCircle(
                    accent.copy(alpha = .30f),
                    radius = radius * .11f,
                    center = center,
                )
                drawCircle(
                    Color.White.copy(alpha = .70f),
                    radius = radius * .016f,
                    center = center,
                )
            }
        }
        if (style == PlayerDesignStyle.V11) {
            Box(
                modifier = Modifier.align(Alignment.TopStart)
                    .size(34.dp)
                    .background(Color.Black.copy(alpha = .76f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("11", color = accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GalaxOriginalControlSurface(
    style: PlayerDesignStyle,
    accent: Color,
    content: @Composable () -> Unit,
) {
    val shape = when (style) {
        PlayerDesignStyle.V11 -> RoundedCornerShape(3.dp)
        PlayerDesignStyle.V14 -> RoundedCornerShape(1.dp)
        PlayerDesignStyle.V12 -> RoundedCornerShape(42.dp)
        PlayerDesignStyle.V13 -> RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)
        else -> RoundedCornerShape(27.dp)
    }
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    when (style) {
                        PlayerDesignStyle.V9 -> listOf(
                            accent.copy(alpha = .25f),
                            Color(0xFF121020).copy(alpha = .82f),
                        )
                        PlayerDesignStyle.V10 -> listOf(
                            Color.White.copy(alpha = .13f),
                            accent.copy(alpha = .17f),
                            Color.Black.copy(alpha = .57f),
                        )
                        PlayerDesignStyle.V11 -> listOf(
                            accent.copy(alpha = .11f),
                            Color(0xFF020F14).copy(alpha = .94f),
                        )
                        PlayerDesignStyle.V12 -> listOf(
                            Color(0xFF34261D).copy(alpha = .91f),
                            Color.Black.copy(alpha = .69f),
                        )
                        PlayerDesignStyle.V13 -> listOf(
                            accent.copy(alpha = .30f),
                            Color(0xFF031A21).copy(alpha = .75f),
                        )
                        PlayerDesignStyle.V14 -> listOf(
                            Color(0xFF242427),
                            Color(0xFF080809),
                        )
                        PlayerDesignStyle.V15 -> listOf(
                            accent.copy(alpha = .30f),
                            Color(0xFF080B20).copy(alpha = .80f),
                        )
                        else -> listOf(accent.copy(alpha = .11f), Color.Black.copy(alpha = .66f))
                    },
                ),
            )
            .drawWithCache {
                onDrawBehind {
                    when (style) {
                        PlayerDesignStyle.V11 -> {
                            drawLine(
                                accent.copy(alpha = .81f),
                                Offset(0f, 0f), Offset(size.width, 0f),
                                2.dp.toPx(),
                            )
                            drawLine(
                                accent.copy(alpha = .43f),
                                Offset(0f, 0f), Offset(0f, size.height),
                                1.dp.toPx(),
                            )
                        }
                        PlayerDesignStyle.V14 -> {
                            drawRect(
                                Color.White.copy(alpha = .22f),
                                Offset(0f, 0f),
                                Size(size.width, 1.dp.toPx()),
                            )
                        }
                        PlayerDesignStyle.V15 -> {
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(accent.copy(alpha = .14f), Color.Transparent),
                                    center = Offset(size.width*.5f, 0f),
                                    radius = size.width*.80f,
                                ),
                                radius = size.width*.80f,
                                center = Offset(size.width*.5f,0f),
                            )
                        }
                        else -> Unit
                    }
                }
            }
            .padding(top = 12.dp, bottom = 14.dp),
    ) {
        content()
    }
}

@Composable
private fun GalaxOriginalBackdrop(
    style: PlayerDesignStyle,
    accent: Color,
    secondary: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val first = lerp(accent, Color(0xFF080914), .80f)
    val second = lerp(secondary, Color(0xFF03060F), .83f)
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                when (style) {
                    PlayerDesignStyle.V13 -> listOf(first, second, Color(0xFF06151D))
                    PlayerDesignStyle.V11 -> listOf(Color(0xFF071317), first, Color(0xFF030707))
                    PlayerDesignStyle.V12 -> listOf(Color(0xFF1B1010), first, Color(0xFF090507))
                    PlayerDesignStyle.V14 -> listOf(Color(0xFF101014), Color(0xFF07070A))
                    else -> listOf(first, second, Color(0xFF02050B))
                },
            ),
        ),
    ) {
        if (style == PlayerDesignStyle.V10 || style == PlayerDesignStyle.V9) {
            GalaxOrbitField(
                active = isPlaying,
                modifier = Modifier.fillMaxSize(),
                accent = accent,
                secondary = secondary,
                intensity = if (style == PlayerDesignStyle.V10) .88f else .52f,
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            val center = center
            when (style) {
                PlayerDesignStyle.V9 -> {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(secondary.copy(alpha = .32f), Color.Transparent),
                            center = Offset(size.width * .26f, size.height * .2f),
                            radius = size.width * .66f,
                        ),
                        radius = size.width * .65f,
                        center = Offset(size.width * .26f, size.height * .2f),
                    )
                    drawCircle(
                        Brush.radialGradient(
                            listOf(accent.copy(alpha = .22f), Color.Transparent),
                            center = Offset(size.width * .80f, size.height * .64f),
                            radius = size.width * .6f,
                        ),
                        radius = size.width * .6f,
                        center = Offset(size.width * .8f, size.height * .64f),
                    )
                }
                PlayerDesignStyle.V10 -> {
                    for (ring in 0..4) {
                        val radius = size.width * (.29f + ring * .10f)
                        drawArc(
                            color = if (ring % 2 == 0) accent.copy(alpha = .43f)
                                else secondary.copy(alpha = .33f),
                            startAngle = -73f + ring * 37f,
                            sweepAngle = 65f + ring * 9f,
                            useCenter = false,
                            topLeft = Offset(
                                size.width * .51f - radius,
                                size.height * .43f - radius,
                            ),
                            size = Size(radius * 2f, radius * 2f),
                            style = Stroke(width = (1f + ring * .21f).dp.toPx()),
                        )
                    }
                }
                PlayerDesignStyle.V11 -> {
                    val gap = 24.dp.toPx()
                    var x = 0f
                    while (x < size.width) {
                        drawLine(accent.copy(alpha = .12f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                        x += gap
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(secondary.copy(alpha = .15f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                        y += gap
                    }
                    drawRect(accent.copy(alpha = .5f), Offset(10.dp.toPx(), 28.dp.toPx()), Size(3.dp.toPx(), size.height * .28f))
                }
                PlayerDesignStyle.V12 -> {
                    val radius = min(size.width, size.height) * .53f
                    for (index in 1..13) {
                        drawCircle(
                            Color.White.copy(alpha = .025f),
                            radius = radius * index / 13f,
                            center = Offset(size.width * .5f, size.height * .40f),
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
                }
                PlayerDesignStyle.V13 -> {
                    for (index in 0 until 6) {
                        val y = size.height * (.11f + index * .105f)
                        val path = Path().apply {
                            moveTo(-size.width * .1f, y)
                            cubicTo(
                                size.width * .22f, y - size.height * .14f,
                                size.width * .55f, y + size.height * .23f,
                                size.width * 1.10f, y - size.height * .13f,
                            )
                        }
                        drawPath(
                            path,
                            color = if (index % 2 == 0) accent.copy(alpha = .12f)
                                else secondary.copy(alpha = .13f),
                            style = Stroke(width = (9 + index * 6).dp.toPx()),
                        )
                    }
                }
                PlayerDesignStyle.V14 -> {
                    drawRect(
                        accent.copy(alpha = .62f),
                        topLeft = Offset(size.width * .075f, size.height * .17f),
                        size = Size(3.dp.toPx(), size.height * .35f),
                    )
                    drawRect(
                        Color.White.copy(alpha = .045f),
                        topLeft = Offset(size.width * .91f, size.height * .10f),
                        size = Size(size.width * .01f, size.height * .74f),
                    )
                }
                PlayerDesignStyle.V15 -> {
                    for (index in 1..10) {
                        val radius = size.width * (.07f + index * .105f)
                        drawCircle(
                            if (index % 2 == 0) accent.copy(alpha = .16f) else secondary.copy(alpha = .11f),
                            radius = radius,
                            center = Offset(size.width * .51f, size.height * .40f),
                            style = Stroke(width = if (index % 3 == 0) 2.dp.toPx() else 1.dp.toPx()),
                        )
                    }
                    for (index in 0 until 15) {
                        val x = size.width * index / 14f
                        val h = size.height * (.02f + .04f * kotlin.math.abs(sin(index * 1.8f)))
                        drawLine(
                            accent.copy(alpha = .25f),
                            Offset(x, size.height * .82f - h),
                            Offset(x, size.height * .82f + h),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                }
                else -> Unit
            }
        }
    }
}
