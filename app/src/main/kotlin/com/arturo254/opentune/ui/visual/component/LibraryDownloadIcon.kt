package com.arturo254.opentune.ui.visual.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arturo254.opentune.LocalDownloadUtil
import androidx.media3.exoplayer.offline.Download
import com.arturo254.opentune.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BadgeSize = 26.dp
private val CoverSize = 19.dp
private val RingWidth = 2.dp

private const val PileDepth = 3

private val PileStep = 4.dp

private val DropHeight = 24.dp

private val PopIn = spring<Float>(dampingRatio = 0.45f, stiffness = 420f)
private val DropIn = spring<Float>(dampingRatio = 0.5f, stiffness = 300f)
private val Settle = spring<Float>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)

private class DroppedCover(val key: String, val url: String?) {
    
    val drop = Animatable(0f)
    
    val depth = Animatable(0f)
}

@Composable
fun LibraryDownloadIcon(icon: @Composable () -> Unit) {
    val downloadUtil = LocalDownloadUtil.current
    val batchState = downloadUtil.downloads.collectAsState()
    val downloading by remember {
        derivedStateOf {
            batchState.value.values.any {
                it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED
            }
        }
    }
    
    val progress by animateFloatAsState(
        targetValue = if (downloading) {
            val active = batchState.value.values.filter {
                it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED
            }
            if (active.isEmpty()) 1f else active.map {
                (it.percentDownloaded.takeIf { p -> p >= 0f } ?: 0f) / 100f
            }.average().toFloat()
        } else 1f,
        animationSpec = tween(450),
        label = "libraryDownloadProgress",
    )

    val iconScale = remember { Animatable(1f) }
    val badgeScale = remember { Animatable(0f) }
    val covers = remember { mutableStateListOf<DroppedCover>() }

    LaunchedEffect(downloading) {
        if (downloading) {
            
            iconScale.animateTo(0f, tween(150, easing = FastOutLinearInEasing))
            badgeScale.animateTo(1f, PopIn)
        } else if (badgeScale.value > 0f) {
            
            delay(500)
            badgeScale.animateTo(1.12f, tween(110))
            badgeScale.animateTo(0f, tween(140, easing = FastOutLinearInEasing))
            covers.clear()
            iconScale.animateTo(1f, PopIn)
        }
    }

    
    LaunchedEffect(Unit) {
        val seen = HashSet<String>()
        snapshotFlow { batchState.value }.collect { batch ->
            val active = batch.values.filter {
                it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED
            }
            if (active.isEmpty()) {
                seen.clear()
                return@collect
            }
            val fresh = active.filter { seen.add(it.request.id) }.takeLast(PileDepth)
            fresh.forEachIndexed { i, item ->
                val key = item.request.id
                val cover = DroppedCover(key, downloadUtil.getOfflineCoverUri(key))
                covers += cover
                if (covers.size > PileDepth + 1) covers.removeAt(0)
                
                covers.forEachIndexed { index, c -> launch { c.depth.animateTo((covers.lastIndex - index).toFloat(), Settle) } }
                launch {
                    
                    delay(if (badgeScale.value < 0.5f) 220L + 90L * i else 90L * i)
                    cover.drop.animateTo(1f, DropIn)
                }
            }
        }
    }

    Box(contentAlignment = Alignment.Center) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = iconScale.value
                scaleY = iconScale.value
            },
        ) { icon() }

        val badgeShown by remember { derivedStateOf { badgeScale.value > 0.001f } }
        if (badgeShown) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(BadgeSize)
                    .graphicsLayer {
                        scaleX = badgeScale.value
                        scaleY = badgeScale.value
                    },
            ) {
                
                covers.forEach { cover ->
                    key(cover.key) { PileCover(cover) }
                }
                
                Box(
                    modifier = Modifier
                        .size(CoverSize)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f)),
                )
                Icon(
                    painter = painterResource(R.drawable.arrow_downward),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = RingWidth.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.32f),
                        radius = size.minDimension / 2f - inset,
                        style = Stroke(stroke),
                    )
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = 360f * progress.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
    }
}

@Composable
private fun PileCover(cover: DroppedCover) {
    val modifier = Modifier
        .size(CoverSize)
        .graphicsLayer {
            val p = cover.drop.value
            val d = cover.depth.value
            
            val pile = 1f - 0.1f * d
            val dropScale = 1.25f + (1f - 1.25f) * p
            scaleX = pile * dropScale
            scaleY = pile * dropScale
            translationX = d * PileStep.toPx()
            translationY = -d * PileStep.toPx() - (1f - p) * DropHeight.toPx()
            rotationZ = (1f - p) * -12f
            alpha = (p * 3f).coerceIn(0f, 1f) * (1f - 0.3f * d).coerceIn(0f, 1f)
        }
        .clip(CircleShape)
    if (cover.url == null) {
        Box(modifier.background(Color.White.copy(alpha = 0.18f)))
    } else {
        AsyncImage(
            model = cover.url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}
