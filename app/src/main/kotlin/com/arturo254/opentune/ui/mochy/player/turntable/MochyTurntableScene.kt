package com.arturo254.opentune.ui.mochy.player.turntable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.player.state.PlayerState
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val CenterX = .162f
private const val CenterY = .703f
private const val DiscRadius = .703f
private const val LabelFraction = .526f
private const val PivotX = .895f
private const val PivotY = .125f

private fun armRadians(degrees: Float) = Math.toRadians(degrees.toDouble()).toFloat()
private fun armDegrees(radians: Float) = Math.toDegrees(radians.toDouble()).toFloat()
private val stylusReach = hypot(ArmLength + ArmStylusAlong * cos(armRadians(ArmHeadBend)),
    ArmStylusAlong * sin(armRadians(ArmHeadBend)))
private val stylusDegrees = armDegrees(atan2(ArmStylusAlong * sin(armRadians(ArmHeadBend)),
    ArmLength + ArmStylusAlong * cos(armRadians(ArmHeadBend))))
private fun armAngleFor(fraction: Float): Float {
    val vx = CenterX - PivotX
    val vy = CenterY - PivotY
    val distance = hypot(vx, vy)
    val radius = fraction * DiscRadius
    val turn = acos(((stylusReach * stylusReach + distance * distance - radius * radius) /
        (2f * stylusReach * distance)).coerceIn(-1f, 1f))
    return armDegrees(atan2(vy, vx)) - armDegrees(turn) - stylusDegrees
}
private val startAngle = armAngleFor(.94f)
private val endAngle = armAngleFor(LabelFraction + .075f)

@Composable
fun MochyTurntableScene(
    state: PlayerState,
    queuePeekHeight: Dp,
    onClose: () -> Unit,
    onStyles: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onQueue: () -> Unit,
    onLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(state.isPlaying) {
        if (state.isPlaying) {
            while (true) {
                rotation.animateTo(rotation.value + 360f,
                    tween(durationMillis = 4600, easing = LinearEasing))
                rotation.snapTo(0f)
            }
        }
    }
    Column(
        modifier.fillMaxSize().background(Color(0xFF0C0D0F)).padding(
            top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 8.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.arrow_back), "Cerrar", tint = Color.White)
            }
            Text("MOCHY · TURNTABLE", color = Color.White.copy(alpha = .74f),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = onStyles) { Text("Estilos", color = Color.White) }
        }
        BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val side = minOf(maxWidth, maxHeight)
            val labelSide = side * DiscRadius * LabelFraction * 2f
            Box(Modifier.size(side)) {
                Canvas(Modifier.fillMaxSize()) {
                    val u = size.width
                    drawArmBase(u)
                    drawVinyl(Offset(u * CenterX, u * CenterY), u * DiscRadius, LabelFraction)
                }
                AsyncImage(
                    model = state.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .offset(
                            x = side * CenterX - labelSide / 2f,
                            y = side * CenterY - labelSide / 2f,
                        )
                        .size(labelSide).clip(CircleShape)
                        .graphicsLayer { rotationZ = rotation.value },
                )
                Canvas(Modifier.fillMaxSize()) {
                    val u = size.width
                    val progress = state.progressFraction.coerceIn(0f, 1f)
                    val angle = startAngle + (endAngle - startAngle) * progress
                    drawTonearm(Offset(u * PivotX, u * PivotY), angle, u, silhouette = false)
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 23.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(state.title.orEmpty(), color = Color.White,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(state.mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                color = Color.White.copy(alpha = .57f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevious, enabled = state.canSkipPrevious) {
                    Icon(painterResource(R.drawable.skip_previous), "Anterior", tint = Color.White)
                }
                IconButton(onClick = onPlayPause) {
                    Icon(painterResource(if (state.isPlaying) R.drawable.pause else R.drawable.play),
                        if (state.isPlaying) "Pausar" else "Reproducir",
                        tint = Color.White, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = onNext, enabled = state.canSkipNext) {
                    Icon(painterResource(R.drawable.skip_next), "Siguiente", tint = Color.White)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                TextButton(onClick = onLyrics) { Text("Letras", color = Color.White.copy(alpha = .65f)) }
                TextButton(onClick = onQueue) { Text("Cola", color = Color.White.copy(alpha = .65f)) }
            }
        }
        Spacer(Modifier.height(queuePeekHeight.coerceAtLeast(0.dp) + 6.dp))
    }
}
