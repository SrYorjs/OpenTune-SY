package com.arturo254.opentune.ui.visual.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.abs

private val PlayerCardHeight = 54.dp
private val PlayerCardCorner = 28.dp
private val PeekOffset = 9.dp
private val PeekInset = 10.dp
private val FlipDistance = 70.dp
private const val BackCardScale = 0.94f

@Composable
fun OriginalPlayerBottomCards(
    nextTitle: String?,
    nextArtist: String?,
    nextArtwork: String?,
    lyricPreview: String?,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val flip = remember { Animatable(0f) }
    var front by remember { mutableIntStateOf(0) }
    val flipPx = with(androidx.compose.ui.platform.LocalDensity.current) { FlipDistance.toPx() }
    val frontness: (Int) -> Float = { index ->
        if (index == 1) flip.value.coerceIn(0f, 1f)
        else 1f - flip.value.coerceIn(0f, 1f)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PlayerCardHeight + PeekOffset)
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var claimed = false
                    var travelled = 0f
                    var moved = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val dy = change.positionChange().y
                        travelled += dy
                        if (!claimed) {
                            if (travelled < -slop) {
                                claimed = true
                            } else if (travelled > slop) {
                                break
                            }
                        }
                        if (claimed) {
                            change.consume()
                            moved += -dy
                            val amount = (abs(moved) / flipPx).coerceIn(0f, 1f)
                            val target = if (front == 0) amount else 1f - amount
                            scope.launch { flip.snapTo(target) }
                        }
                    }
                    if (claimed) {
                        if (abs(moved) >= flipPx * 0.4f) front = 1 - front
                        scope.launch {
                            flip.animateTo(
                                front.toFloat(),
                                spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                    }
                }
            }
            .pointerInput(onOpenLyrics, onOpenQueue, onLongPress) {
                detectTapGestures(
                    onTap = { if (flip.value >= 0.5f) onOpenQueue() else onOpenLyrics() },
                    onLongPress = { onLongPress() },
                )
            },
    ) {
        for (side in listOf(1, 0)) {
            val f = frontness(side)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PlayerCardHeight)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        val scale = BackCardScale + (1f - BackCardScale) * f
                        scaleX = scale
                        scaleY = scale
                        translationY = -PeekOffset.toPx() * (1f - f)
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .drawBehind {
                        val fill = if (side == 0) 0.11f else 0.06f
                        val border = if (side == 0) 0.20f else 0.10f
                        val inset = PeekInset.toPx() * (1f - f)
                        drawRoundRect(
                            color = Color.White.copy(alpha = fill + 0.04f * f),
                            topLeft = Offset(inset, 0f),
                            size = Size(size.width - 2f * inset, size.height),
                            cornerRadius = CornerRadius(PlayerCardCorner.toPx()),
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = border),
                            topLeft = Offset(inset, 0f),
                            size = Size(size.width - 2f * inset, size.height),
                            cornerRadius = CornerRadius(PlayerCardCorner.toPx()),
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
            ) {
                if (side == 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    ) {
                        if (!nextArtwork.isNullOrEmpty()) {
                            AsyncImage(
                                model = nextArtwork,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)),
                            )
                        } else {
                            Box(Modifier.size(28.dp).background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(9.dp)))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Siguiente canción",
                                color = Color.White.copy(alpha = 0.50f),
                                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 11.sp),
                            )
                            Text(
                                text = listOfNotNull(
                                    nextTitle ?: "Final de la cola",
                                    nextArtist?.takeIf(String::isNotBlank),
                                ).joinToString(" - "),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 15.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "♫",
                            fontSize = 21.sp,
                            color = Color.White.copy(alpha = 0.78f),
                        )
                        Text(
                            text = lyricPreview?.takeIf(String::isNotBlank) ?: "Letras",
                            color = Color.White.copy(alpha = 0.82f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
