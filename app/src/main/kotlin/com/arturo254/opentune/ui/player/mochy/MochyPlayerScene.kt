package com.arturo254.opentune.ui.player.mochy

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.player.state.PlayerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

@Composable
internal fun MochyPlayerScene(
    style: MochyPlayerStyle,
    state: PlayerState,
    accent: Color,
    queuePeekHeight: Dp,
    onCollapse: () -> Unit,
    onStyles: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFavorite: () -> Unit,
    onSeek: (Long) -> Unit,
    onQueue: () -> Unit,
    onLyrics: () -> Unit,
) {
    val night = style == MochyPlayerStyle.BEDSIDE || style == MochyPlayerStyle.BEDSIDE_TILES ||
        style == MochyPlayerStyle.IPOD || style == MochyPlayerStyle.VIDEO
    val base = if (night) Color(0xFF090B13) else MaterialTheme.colorScheme.surface
    val ink = if (night || style == MochyPlayerStyle.POSTER) Color.White else MaterialTheme.colorScheme.onSurface
    val softInk = ink.copy(alpha = .65f)
    val color by animateColorAsState(accent, animationSpec = spring(dampingRatio = .9f), label = "Mochy color")
    val duration = state.durationMs.coerceAtLeast(0L)
    var dragging by remember(state.trackId) { mutableStateOf(false) }
    var seekFraction by remember(state.trackId) { mutableFloatStateOf(0f) }
    val played = if (dragging) seekFraction else state.progressFraction.coerceIn(0f, 1f)
    val bottom = queuePeekHeight.coerceAtLeast(0.dp)

    BoxWithConstraints(Modifier.fillMaxSize().background(base)) {
        val landscape = maxWidth > maxHeight
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.radialGradient(
                    listOf(color.copy(alpha = if (night) .19f else .31f), Color.Transparent),
                    center = Offset(size.width * .66f, size.height * .27f),
                    radius = size.maxDimension * .79f,
                )
            )
            if (style == MochyPlayerStyle.TIMELINE || style == MochyPlayerStyle.FLAGSHIP) {
                repeat(7) { index ->
                    drawCircle(
                        color.copy(alpha = .055f),
                        radius = size.width * (.34f + index * .095f),
                        center = Offset(size.width * .5f, size.height * .40f),
                        style = Stroke(1.dp.toPx()),
                    )
                }
            }
        }
        Column(
            Modifier.fillMaxSize()
                .padding(
                    top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 8.dp,
                    start = if (landscape) 30.dp else 20.dp,
                    end = if (landscape) 30.dp else 20.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(painterResource(R.drawable.arrow_back), "Cerrar reproductor", tint = ink)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MOCHY × GALAXOT", style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold, color = color, letterSpacing = 1.6.sp)
                    Text(style.title, style = MaterialTheme.typography.labelMedium, color = softInk)
                }
                TextButton(onClick = onStyles) {
                    Text("Estilos", color = ink, fontWeight = FontWeight.SemiBold)
                }
            }
            if (landscape) {
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        MochyStyleArtwork(style, state, color, ink, onPlayPause, onNext, onPrevious, onLyrics)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        MochyTransport(
                            state, played, duration, color, ink, softInk, dragging,
                            onDrag = { seekFraction = it; dragging = true },
                            onDragFinished = {
                                onSeek((seekFraction * duration).roundToLong())
                                dragging = false
                            },
                            onPlayPause, onNext, onPrevious, onFavorite, onQueue, onLyrics,
                        )
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    MochyStyleArtwork(style, state, color, ink, onPlayPause, onNext, onPrevious, onLyrics)
                }
                MochyTransport(
                    state, played, duration, color, ink, softInk, dragging,
                    onDrag = { seekFraction = it; dragging = true },
                    onDragFinished = {
                        onSeek((seekFraction * duration).roundToLong())
                        dragging = false
                    },
                    onPlayPause, onNext, onPrevious, onFavorite, onQueue, onLyrics,
                )
            }
            Spacer(Modifier.height(bottom + if (landscape) 4.dp else 10.dp))
        }
    }
}

@Composable
private fun MochyTransport(
    state: PlayerState,
    played: Float,
    duration: Long,
    accent: Color,
    ink: Color,
    softInk: Color,
    dragging: Boolean,
    onDrag: (Float) -> Unit,
    onDragFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFavorite: () -> Unit,
    onQueue: () -> Unit,
    onLyrics: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AnimatedContent(
            targetState = state.trackId to state.title,
            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(130)) },
            label = "Mochy song",
        ) { item ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(
                    item.second.orEmpty(),
                    color = ink,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                    color = softInk,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Slider(
            value = played.coerceIn(0f, 1f),
            onValueChange = onDrag,
            onValueChangeFinished = onDragFinished,
            enabled = duration > 0L && state.canSeek,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(timeLabel((played * duration).toLong()), color = softInk, style = MaterialTheme.typography.labelSmall)
            Text(timeLabel(duration), color = softInk, style = MaterialTheme.typography.labelSmall)
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconButton(onClick = onFavorite) {
                Icon(painterResource(R.drawable.favorite),
                    "Favorito", tint = if (state.isLiked) accent else softInk, modifier = Modifier.size(21.dp))
            }
            IconButton(onClick = onPrevious, enabled = state.canSkipPrevious) {
                Icon(painterResource(R.drawable.skip_previous), "Anterior", tint = ink, modifier = Modifier.size(28.dp))
            }
            Box(
                Modifier.size(66.dp).clip(CircleShape).background(accent).clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (state.isPlaying) R.drawable.pause else R.drawable.play),
                    if (state.isPlaying) "Pausar" else "Reproducir",
                    tint = Color.Black,
                    modifier = Modifier.size(31.dp),
                )
            }
            IconButton(onClick = onNext, enabled = state.canSkipNext) {
                Icon(painterResource(R.drawable.skip_next), "Siguiente", tint = ink, modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = onQueue) {
                Icon(painterResource(R.drawable.queue_music), "Cola", tint = softInk, modifier = Modifier.size(21.dp))
            }
        }
        TextButton(onClick = onLyrics) { Text("Letras", color = ink.copy(alpha = .74f)) }
    }
}

@Composable
private fun MochyStyleArtwork(
    style: MochyPlayerStyle,
    state: PlayerState,
    accent: Color,
    ink: Color,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onLyrics: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val dimension = minOf(maxWidth * .83f, maxHeight * .89f).coerceAtLeast(100.dp)
        val coverShape = when (style) {
            MochyPlayerStyle.FLAGSHIP -> RoundedCornerShape(32.dp)
            MochyPlayerStyle.CLASSIC -> RoundedCornerShape(12.dp)
            MochyPlayerStyle.VIDEO -> RoundedCornerShape(20.dp)
            MochyPlayerStyle.POSTER -> RoundedCornerShape(5.dp)
            MochyPlayerStyle.TIMELINE -> RoundedCornerShape(50.dp)
            else -> RoundedCornerShape(23.dp)
        }
        when (style) {
            MochyPlayerStyle.TURNTABLE -> {
                val rotation = remember { androidx.compose.animation.core.Animatable(0f) }
                LaunchedEffect(state.isPlaying) {
                    while (state.isPlaying) {
                        rotation.animateTo(rotation.value + 360f, tween(5000, easing = LinearEasing))
                    }
                }
                Box(Modifier.size(dimension).clip(CircleShape).background(Color(0xFF16191C)), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = state.artworkUrl,
                        contentDescription = "Carátula del disco",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation.value }.clip(CircleShape),
                    )
                    Canvas(Modifier.fillMaxSize()) {
                        repeat(12) { i ->
                            drawCircle(
                                Color.White.copy(alpha = .11f),
                                size.minDimension * (.21f + i * .023f),
                                style = Stroke(1.dp.toPx()),
                            )
                        }
                        drawCircle(Color.Black.copy(alpha = .73f), size.minDimension * .075f)
                        drawCircle(accent, size.minDimension * .025f)
                    }
                }
            }
            MochyPlayerStyle.IPOD -> {
                Column(
                    Modifier.size(dimension).clip(RoundedCornerShape(29.dp))
                        .background(Color(0xFFE2E2DE)).padding(13.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF202630)),
                    ) {
                        AsyncImage(
                            model = state.artworkUrl, contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.size(dimension * .39f).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Text("MENU", Modifier.align(Alignment.TopCenter).padding(top = 10.dp).clickable(onClick = onLyrics),
                            color = Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("◀", Modifier.align(Alignment.CenterStart).padding(start = 11.dp).clickable(onClick = onPrevious), color = Color.DarkGray)
                        Text("▶", Modifier.align(Alignment.CenterEnd).padding(end = 11.dp).clickable(onClick = onNext), color = Color.DarkGray)
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFD2D3D5))
                            .clickable(onClick = onPlayPause))
                    }
                }
            }
            MochyPlayerStyle.BEDSIDE, MochyPlayerStyle.BEDSIDE_TILES -> {
                var clock by remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    while (true) {
                        clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        delay(15_000L)
                    }
                }
                if (style == MochyPlayerStyle.BEDSIDE) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(clock, fontWeight = FontWeight.Light, fontSize = 69.sp, color = ink, letterSpacing = -4.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(state.title.orEmpty(), color = ink.copy(alpha = .67f),
                            style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f).height(dimension * .70f).clip(RoundedCornerShape(23.dp))
                            .background(Color.White.copy(alpha = .08f)), contentAlignment = Alignment.Center) {
                            AsyncImage(model = state.artworkUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(23.dp)))
                        }
                        Box(Modifier.weight(1f).height(dimension * .70f).clip(RoundedCornerShape(23.dp))
                            .background(Color.White.copy(alpha = .09f)), contentAlignment = Alignment.Center) {
                            Text(clock, fontWeight = FontWeight.Light, fontSize = 28.sp, color = ink)
                        }
                    }
                }
            }
            MochyPlayerStyle.LYRICS -> {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(29.dp))
                        .background(accent.copy(alpha = .14f)).clickable(onClick = onLyrics)
                        .padding(27.dp),
                    verticalArrangement = Arrangement.spacedBy(17.dp),
                ) {
                    Text("LETRAS", color = accent, fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp, style = MaterialTheme.typography.labelSmall)
                    Text(state.title.orEmpty(), color = ink, fontSize = 37.sp,
                        fontWeight = FontWeight.Bold, lineHeight = 43.sp, maxLines = 3)
                    Text("Toca para abrir las letras sincronizadas", color = ink.copy(alpha = .70f),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            MochyPlayerStyle.VIDEO -> {
                Box(
                    Modifier.fillMaxWidth().height(dimension * .62f)
                        .clip(RoundedCornerShape(18.dp)).background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(model = state.artworkUrl, contentDescription = "Carátula",
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.size(57.dp).clip(CircleShape).background(Color.Black.copy(alpha = .58f))
                        .clickable(onClick = onPlayPause), contentAlignment = Alignment.Center) {
                        Icon(painterResource(if (state.isPlaying) R.drawable.pause else R.drawable.play),
                            "Reproducir o pausar", tint = Color.White, modifier = Modifier.size(27.dp))
                    }
                }
            }
            MochyPlayerStyle.TIMELINE -> {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(timeLabel(state.displayPositionMs), fontSize = 53.sp, color = ink,
                        fontWeight = FontWeight.Light, letterSpacing = -2.sp)
                    Spacer(Modifier.height(17.dp))
                    AsyncImage(model = state.artworkUrl, contentDescription = "Portada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(dimension * .66f).clip(CircleShape))
                }
            }
            else -> {
                val imageHeight = if (style == MochyPlayerStyle.POSTER) dimension * 1.05f else dimension
                Box(
                    Modifier.size(dimension, imageHeight).clip(coverShape)
                        .background(accent.copy(alpha = .21f)),
                ) {
                    AsyncImage(model = state.artworkUrl, contentDescription = "Portada",
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    if (style == MochyPlayerStyle.FLAGSHIP || style == MochyPlayerStyle.POSTER) {
                        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(90.dp)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .40f)))))
                    }
                }
            }
        }
    }
}

private fun timeLabel(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0L) / 1000L)
    return "%d:%02d".format(Locale.US, seconds / 60, seconds % 60)
}
