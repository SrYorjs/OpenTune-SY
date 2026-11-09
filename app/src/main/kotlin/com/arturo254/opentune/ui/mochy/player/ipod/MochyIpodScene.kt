package com.arturo254.opentune.ui.mochy.player.ipod

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.ui.player.state.PlayerState
import kotlin.math.roundToLong

@Composable
fun MochyIpodScene(
    state: PlayerState,
    queuePeekHeight: Dp,
    onClose: () -> Unit,
    onStyles: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val look = rememberIpodLook()
    val haptics = rememberIpodHaptics()
    val density = LocalDensity.current
    val inset = WindowInsets.systemBars.asPaddingValues()
    val wheelActions = remember(
        state.trackId, state.positionMs, state.durationMs, state.canSeek,
        onClose, onPlayPause, onPrevious, onNext, onSeek,
    ) {
        object : IpodWheelActions {
            override val accelerates = false
            override fun rotate(steps: Int): Boolean {
                if (!state.canSeek || state.durationMs <= 0L) return false
                onSeek((state.positionMs + steps * 3000L).coerceIn(0L, state.durationMs))
                return true
            }
            override fun menu() = onClose()
            override fun menuHeld() = onClose()
            override fun center() = onPlayPause()
            override fun next() = onNext()
            override fun previous() = onPrevious()
            override fun playPause() = onPlayPause()
            override fun scan(forward: Boolean) {
                if (state.canSeek && state.durationMs > 0L) {
                    val delta = if (forward) 4000L else -4000L
                    onSeek((state.positionMs + delta).coerceIn(0L, state.durationMs))
                }
            }
        }
    }
    Column(
        modifier = modifier.fillMaxSize().background(Color.Black)
            .padding(
                top = inset.calculateTopPadding(),
                bottom = queuePeekHeight.coerceAtLeast(0.dp),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onClose) { Text("Volver", color = Color.White) }
            Text("iPod · Mochy", color = Color.White.copy(alpha = .7f),
                style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = onStyles) { Text("Estilos", color = Color.White) }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val w = with(density) { maxWidth.toPx() }
            val h = with(density) { maxHeight.toPx() }
            val geometry = remember(w, h, look.tall) {
                deviceGeometry(w, h, 3f, 4f, look.tall)
            }
            val bodyPaint = remember(geometry, look) { IpodBodyPaint(geometry, look) }
            Canvas(Modifier.fillMaxSize()) {
                with(bodyPaint) { draw(onBlack = true) }
            }
            Box(
                Modifier.place(geometry.screen, density)
                    .clip(RoundedCornerShape(with(density) { geometry.screenCorner.toDp() }))
                    .background(look.screen.background),
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().background(look.screen.header)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Now Playing", color = look.screen.ink,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall)
                        Text(if (state.isPlaying) "▶" else "Ⅱ",
                            color = look.screen.ink, fontSize = 13.sp)
                    }
                    val art = rememberCoverPainter(state.artworkUrl, 380)
                    Canvas(Modifier.weight(1f).fillMaxWidth(.80f)) {
                        val side = size.minDimension * .85f
                        val x = (size.width - side) / 2f
                        val y = (size.height - side) / 2f
                        withTransform({
                            translate(x, y)
                        }) {
                            drawCoverSquare(art, side, look.screen.artBlank)
                        }
                    }
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            state.title.orEmpty(),
                            color = look.screen.ink,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            state.mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                            color = look.screen.softInk,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        LinearProgressIndicator(
                            progress = { state.progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(5.dp),
                            color = look.screen.ink,
                            trackColor = look.screen.quietInk.copy(alpha = .27f),
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                }
            }
            CompositionLocalProvider(LocalIpodLook provides look) {
                Box(Modifier.place(geometry.wheel, density)) {
                    IpodClickWheel(
                        actions = wheelActions,
                        haptics = haptics,
                        enabled = true,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
