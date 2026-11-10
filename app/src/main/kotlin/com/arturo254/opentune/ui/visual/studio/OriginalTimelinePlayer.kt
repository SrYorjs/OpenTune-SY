package com.arturo254.opentune.ui.visual.studio

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.media3.common.Timeline
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.ui.player.actions.PlayerActions
import com.arturo254.opentune.ui.player.state.PlayerState
import com.arturo254.opentune.ui.visual.player.CarouselPictures
import com.arturo254.opentune.ui.visual.player.rememberOriginalSongCarousel
import com.arturo254.opentune.ui.visual.theme.InterFontFamily
import com.arturo254.opentune.utils.makeTimeString

private class TimelineInsets(val start: Dp, val top: Dp, val end: Dp, val bottom: Dp)

@Composable
private fun timelineInsets(landscape: Boolean): TimelineInsets {
    val direction = LocalLayoutDirection.current
    val sides = WindowInsets.navigationBars.union(WindowInsets.displayCutout).asPaddingValues()
    val status = if (landscape) 0.dp else WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return TimelineInsets(
        start = sides.calculateStartPadding(direction),
        top = status,
        end = sides.calculateEndPadding(direction),
        bottom = sides.calculateBottomPadding(),
    )
}

private fun timelineType(size: TextUnit, weight: FontWeight = FontWeight.Bold) = TextStyle(
    fontFamily = InterFontFamily,
    fontSize = size,
    lineHeight = size * 0.96f,
    fontWeight = weight,
    letterSpacing = (-0.035).em,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    textMotion = TextMotion.Animated,
)

@Composable
fun OriginalTimelinePlayer(
    state: PlayerState,
    actions: PlayerActions,
    artworkColor: Color,
    queueWindows: List<Timeline.Window>,
    currentIndex: Int,
    metadata: MediaMetadata?,
    onStyles: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    preview: Boolean = false,
) {
    val tones = rememberOriginalStyleTones(artworkColor)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val shortSide = LocalConfiguration.current.screenWidthDp.coerceAtMost(LocalConfiguration.current.screenHeightDp).toFloat()
    val icon = (shortSide * 0.15f).dp.coerceIn(50.dp, 72.dp)
    val circle = (shortSide * 0.078f).dp.coerceIn(28.dp, 40.dp)
    val elapsedSize = (shortSide * 0.185f).coerceIn(56f, 96f).sp
    val titleSize = (shortSide * 0.04f).coerceIn(14f, 18f).sp
    val artistSize = (shortSide * 0.034f).coerceIn(12f, 16f).sp
    val insets = timelineInsets(isLandscape)
    val carousel = rememberOriginalSongCarousel(queueWindows, currentIndex, metadata)
    val title = state.title.orEmpty()
    val artist = metadata?.artists?.joinToString { it.name }.orEmpty()

    OriginalTimelineRidingTimes(
        positionMs = state.displayPositionMs,
        durationMs = state.durationMs,
        tones = tones,
        landscape = isLandscape,
        insets = insets,
        circle = circle,
        timeSize = elapsedSize,
        topReserve = if (isLandscape) 0.dp else 20.dp + icon + 14.dp,
        modifier = modifier.pointerInput(onStyles, actions, preview) {
            if (!preview) detectTapGestures(
                onDoubleTap = { actions.playPause() },
                onLongPress = { onStyles() },
            )
        },
    ) {
        Row(
            modifier = Modifier
                .padding(
                    start = 22.dp + insets.start,
                    top = insets.top + 20.dp,
                    end = 70.dp + insets.end,
                )
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CarouselPictures(
                carousel = carousel,
                fallbackUrl = state.artworkUrl,
                modifier = Modifier
                    .size(if (isLandscape) icon * 0.86f else icon)
                    .clip(RoundedCornerShape(icon * 0.26f))
                    .background(Color.White.copy(alpha = 0.08f)),
            ) { url, childModifier ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = childModifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = tones.ink,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    color = tones.inkSoft,
                    fontSize = artistSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(if (isLandscape) Alignment.BottomEnd else Alignment.BottomCenter)
                .padding(
                    end = if (isLandscape) 20.dp + insets.end else 0.dp,
                    bottom = if (isLandscape) 20.dp else insets.bottom + 36.dp,
                ),
        ) {
            OriginalStyleTransportCircles(
                state = state,
                actions = actions,
                tones = tones,
                vertical = isLandscape,
                size = circle,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = insets.top + 20.dp + (icon - circle) / 2f,
                    end = 22.dp + insets.end,
                )
                .size(circle)
                .clip(CircleShape)
                .background(tones.chip)
                .clickable(onClick = onMenu),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.more_horiz),
                contentDescription = "Menú",
                tint = tones.ink,
                modifier = Modifier.size(circle * 0.58f),
            )
        }
    }
}

@Composable
private fun OriginalTimelineRidingTimes(
    positionMs: Long,
    durationMs: Long,
    tones: OriginalStyleTones,
    landscape: Boolean,
    insets: TimelineInsets,
    circle: Dp,
    timeSize: TextUnit,
    topReserve: Dp,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val total = makeTimeString(durationMs.coerceAtLeast(0L))
    val timeStyle = timelineType(timeSize)
    val timeWidth = remember(total, timeStyle) { measurer.measure(total, timeStyle).size.width.toFloat() }
    val timeHeight = remember(timeStyle) { measurer.measure("0:00", timeStyle).size.height.toFloat() }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val (lo, hi) = with(density) {
            if (!landscape) {
                val top = (insets.top + topReserve).toPx() + timeHeight
                val bottom = hPx - (insets.bottom + 36.dp + circle + 18.dp).toPx() - timeHeight
                top to bottom.coerceAtLeast(top)
            } else {
                val left = (insets.start + 22.dp).toPx() + timeWidth + 10.dp.toPx()
                val right = wPx - (insets.end + 20.dp + circle + 22.dp).toPx() - timeWidth - 10.dp.toPx()
                left to right.coerceAtLeast(left)
            }
        }
        val fraction = if (durationMs <= 0L) 0f else
            (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        val linePx = lo + (hi - lo) * fraction
        val normalised = linePx / (if (landscape) wPx else hPx).coerceAtLeast(1f)
        Box(
            modifier = Modifier.fillMaxSize().originalProgressSplit(tones, vertical = !landscape) { normalised },
        ) {
            val gapPx = with(density) { 6.dp.toPx() }
            val startPx = with(density) { (insets.start + 24.dp).toPx() }
            val bottomPx = with(density) { 16.dp.toPx() }
            Text(
                text = makeTimeString(positionMs.coerceAtLeast(0L)),
                color = tones.ink,
                style = timeStyle,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.layout { measurable, limits ->
                    val item = measurable.measure(limits.copy(minWidth = 0, minHeight = 0))
                    layout(limits.maxWidth, limits.maxHeight) {
                        if (!landscape) {
                            item.place(startPx.toInt(), (linePx - item.height - gapPx * 0.3f).toInt())
                        } else {
                            item.place(
                                (linePx - item.width - gapPx * 1.6f).toInt(),
                                (hPx - bottomPx - item.height).toInt(),
                            )
                        }
                    }
                },
            )
            Text(
                text = total,
                color = tones.ink.copy(alpha = 0.78f),
                style = timeStyle,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.layout { measurable, limits ->
                    val item = measurable.measure(limits.copy(minWidth = 0, minHeight = 0))
                    layout(limits.maxWidth, limits.maxHeight) {
                        if (!landscape) {
                            item.place(startPx.toInt(), (linePx + gapPx * 0.6f).toInt())
                        } else {
                            item.place(
                                (linePx + gapPx * 1.6f).toInt(),
                                (hPx - bottomPx - item.height).toInt(),
                            )
                        }
                    }
                },
            )
            content()
        }
    }
}
