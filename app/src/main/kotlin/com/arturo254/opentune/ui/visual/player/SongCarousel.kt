package com.arturo254.opentune.ui.visual.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import android.net.Uri
import com.arturo254.opentune.extensions.metadata
import com.arturo254.opentune.models.MediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor

@Immutable
data class CarouselSong(
    val uid: Int,
    val mediaId: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String?,
    val step: Int,
)

val LocalSongCarousel = staticCompositionLocalOf<SongCarousel?> { null }

private const val SettleOmega = 18f

private const val MaxFrameSec = 1f / 40f

private const val RestDistance = 0.0005f
private const val RestSpeed = 0.01f

private const val SkipDistance = 0.004f
private const val SkipSpeed = 0.06f

private const val SkipConfirmMs = 1_200L

private const val GlideStillMs = 400L

private const val SwipeCommit = 0.5f

private const val EdgeGive = 0.3f

@Stable
class SongCarousel internal constructor(private val scope: CoroutineScope) {
    
    var position by mutableFloatStateOf(0f)
        private set

    
    var step by mutableIntStateOf(0)
        private set

    
    var slots by mutableStateOf<List<CarouselSong>>(emptyList())
        private set

    
    var dragging by mutableStateOf(false)
        private set

    
    val away: Float get() = abs(position - step)

    
    fun offsetOf(slot: CarouselSong): Float = slot.step - position

    
    val playingOffset: Float get() = step - position

    fun offsetOfSong(mediaId: String): Float? {
        var best: Float? = null
        for (slot in slots) {
            if (slot.mediaId != mediaId) continue
            val o = offsetOf(slot)
            if (best == null || abs(o) < abs(best)) best = o
        }
        return best
    }

    
    fun crossfadeAlpha(slot: CarouselSong): Float {
        val lower = floor(position).toInt()
        val fraction = position - lower
        return when (slot.step) {
            lower -> 1f
            lower + 1 -> if (slots.any { it.step == lower }) fraction else 1f
            else -> 0f
        }
    }

    private var playing: CarouselSong? = null
    private var previousUid: Int? = null
    private var nextUid: Int? = null
    private var queueIndex = -1

    
    private var outgoing: CarouselSong? = null

    private var lastQueue: QueueState? = null
    private var lastTrack: TrackInfo? = null

    
    internal fun onQueue(queue: QueueState, track: TrackInfo) {
        lastQueue = queue
        lastTrack = track
        val items = queue.items
        val index = queue.currentIndex
        val now = if (index in items.indices) {
            songOf(items[index], step, track, current = true)
        } else {
            if (track.mediaId.isEmpty()) return
            CarouselSong(track.mediaId.hashCode(), track.mediaId, track.title, track.artist, track.album, track.thumbnailUrl, step)
        }
        val last = playing
        if (last != null && last.uid != now.uid) {
            pendingSkip = null
            expectedStep = null
            confirmJob?.cancel()
            val delta = when (now.uid) {
                nextUid -> 1
                previousUid -> -1
                else -> if (index in 0 until queueIndex) -1 else 1
            }
            outgoing = if (now.uid != nextUid && now.uid != previousUid) last else null
            step += delta
        }
        val current = now.copy(step = step)
        playing = current
        queueIndex = index
        previousUid = items.getOrNull(index - 1)?.uid
        nextUid = items.getOrNull(index + 1)?.uid

        val gone = outgoing
        val row = buildList {
            items.getOrNull(index - 1)?.let { add(songOf(it, step - 1, track, current = false)) }
            add(current)
            items.getOrNull(index + 1)?.let { add(songOf(it, step + 1, track, current = false)) }
            if (gone != null && none { it.uid == gone.uid }) {
                removeAll { it.step == gone.step }
                add(gone)
            }
        }.sortedBy { it.step }
        if (row != slots) slots = row
    }

    private fun songOf(item: QueueItemData, step: Int, track: TrackInfo, current: Boolean): CarouselSong {
        val isTrack = item.mediaId == track.mediaId
        return CarouselSong(
            uid = item.uid,
            mediaId = item.mediaId,
            title = item.title.ifEmpty { if (isTrack) track.title else "" },
            artist = item.artist.ifEmpty { if (isTrack) track.artist else "" },
            album = if (isTrack) track.album else "",
            artworkUrl = item.artworkUri?.toString() ?: track.thumbnailUrl.takeIf { current && isTrack },
            step = step,
        )
    }

    private var job: Job? = null
    private var velocity = 0f
    private var headingTo = Float.NaN

    private var pendingSkip: (() -> Unit)? = null
    private var pendingTarget = 0

    private var expectedStep: Int? = null
    private var confirmJob: Job? = null

    private fun restingStep(): Int = expectedStep ?: step

    
    internal fun settle() {
        if (dragging) return
        glideTo(restingStep())
    }

    private fun firePendingSkip() {
        val skip = pendingSkip ?: return
        pendingSkip = null
        expect(pendingTarget, SkipConfirmMs)
        skip()
    }

    private fun expect(target: Int, confirmMs: Long) {
        expectedStep = target
        confirmJob?.cancel()
        confirmJob = scope.launch {
            delay(confirmMs)
            expectedStep = null
            settle()
        }
    }

    
    fun skipToNext(player: Player?, songChangesAfterMs: Long, skip: () -> Unit) {
        if (dragging || player?.hasNextMediaItem() != true || pendingSkip != null || expectedStep != null) {
            firePendingSkip()
            skip()
            return
        }
        val target = step + 1
        if (songChangesAfterMs >= GlideStillMs) {
            expect(target, SkipConfirmMs + songChangesAfterMs)
            skip()
        } else {
            pendingTarget = target
            pendingSkip = skip
        }
        glideTo(target)
    }

    private fun glideTo(target: Int) {
        val to = target.toFloat()
        if (headingTo == to && job?.isActive == true) return
        job?.cancel()
        headingTo = to
        job = scope.launch {
            var lastNanos = 0L
            var resting = false
            while (!resting) {
                resting = withFrameNanos { now ->
                    val dt = if (lastNanos == 0L) 1f / 120f else ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, MaxFrameSec)
                    lastNanos = now
                    val y = position - to
                    val e = exp(-SettleOmega * dt)
                    val tmp = (velocity + SettleOmega * y) * dt
                    val next = (y + tmp) * e
                    velocity = (velocity - SettleOmega * tmp) * e
                    position = to + next
                    abs(next) < RestDistance && abs(velocity) < RestSpeed
                }
                if (pendingSkip != null && abs(position - to) < SkipDistance && abs(velocity) < SkipSpeed) firePendingSkip()
            }
            position = to
            velocity = 0f
            headingTo = Float.NaN
            firePendingSkip()
            if (restingStep() != target) {
                glideTo(restingStep())
                return@launch
            }
            if (outgoing != null) {
                outgoing = null
                val queue = lastQueue
                val track = lastTrack
                if (queue != null && track != null) onQueue(queue, track)
            }
        }
    }

    private var dragFrom = 0f

    fun hold() {
        if (dragging) return
        job?.cancel()
        headingTo = Float.NaN
        velocity = 0f
        firePendingSkip()
    }

    fun ensureSettled() {
        if (dragging || job?.isActive == true) return
        if (abs(position - restingStep()) > RestDistance) glideTo(restingStep())
    }

    fun letGo() {
        if (dragging) return
        settle()
    }

    fun startDrag() {
        hold()
        dragFrom = position
        dragging = true
    }

    
    fun dragTo(travelledPx: Float, spacingPx: Float, hasPrevious: Boolean, hasNext: Boolean) {
        if (!dragging || spacingPx <= 0f) return
        val base = restingStep()
        val raw = dragFrom - travelledPx / spacingPx
        val low = minOf(base - (if (hasPrevious) 1f else 0f), dragFrom)
        val high = maxOf(base + (if (hasNext) 1f else 0f), dragFrom)
        position = when {
            raw < low -> low - (low - raw) * EdgeGive
            raw > high -> high + (raw - high) * EdgeGive
            else -> raw
        }
    }

    fun release(
        travelledPx: Float,
        velocityPx: Float,
        spacingPx: Float,
        flingPx: Float,
        canSkipPrevious: Boolean,
        canSkipNext: Boolean,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
    ) {
        if (!dragging) return
        dragging = false
        val spacing = spacingPx.coerceAtLeast(1f)
        val base = restingStep()
        val moved = position - base
        val speed = -velocityPx / spacing
        val fling = flingPx / spacing
        val forward = (moved > SwipeCommit || speed > fling) && speed > -fling && canSkipNext
        val back = !forward && (moved < -SwipeCommit || speed < -fling) && speed < fling &&
            canSkipPrevious
        val target = base + when {
            forward -> 1
            back -> -1
            else -> 0
        }
        pendingTarget = target
        pendingSkip = when {
            forward -> onNext
            back -> onPrevious
            else -> null
        }
        velocity = speed
        glideTo(target)
    }

    fun cancelDrag() {
        if (!dragging) return
        dragging = false
        velocity = 0f
        glideTo(restingStep())
    }
}

@Composable
fun rememberOriginalSongCarousel(
    queueWindows: List<Timeline.Window>,
    currentIndex: Int,
    mediaMetadata: MediaMetadata?,
): SongCarousel {
    val scope = rememberCoroutineScope()
    val carousel = remember(scope) { SongCarousel(scope) }
    val queue = remember(queueWindows, currentIndex) {
        QueueState(
            items = queueWindows.mapIndexed { index, window ->
                val data = window.mediaItem.metadata
                QueueItemData(
                    uid = window.uid?.hashCode() ?: (window.mediaItem.mediaId + ":" + index).hashCode(),
                    mediaId = data?.id ?: window.mediaItem.mediaId,
                    title = data?.title.orEmpty(),
                    artist = data?.artists?.joinToString { it.name }.orEmpty(),
                    artworkUri = data?.thumbnailUrl?.let(Uri::parse),
                    duration = 0L,
                )
            },
            currentIndex = currentIndex,
        )
    }
    val track = remember(mediaMetadata) {
        TrackInfo(
            mediaId = mediaMetadata?.id.orEmpty(),
            title = mediaMetadata?.title.orEmpty(),
            artist = mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
            album = mediaMetadata?.album?.title.orEmpty(),
            thumbnailUrl = mediaMetadata?.thumbnailUrl,
        )
    }
    carousel.onQueue(queue, track)
    LaunchedEffect(carousel, carousel.step) { carousel.settle() }
    return carousel
}

@Composable
fun rememberTextSlots(carousel: SongCarousel): List<CarouselSong> {
    val shown by remember(carousel) {
        derivedStateOf {
            carousel.slots.filter { it.step == carousel.step || abs(carousel.offsetOf(it)) < 0.999f }
        }
    }
    return shown
}

fun Modifier.carouselText(carousel: SongCarousel, slot: CarouselSong): Modifier = graphicsLayer {
    val o = carousel.offsetOf(slot)
    translationX = o * size.width * TextTravel
    alpha = (1f - abs(o) * TextFade).coerceIn(0f, 1f)
}

private const val TextTravel = 0.75f
private const val TextFade = 1.5f

@Composable
fun CarouselPictures(
    carousel: SongCarousel?,
    fallbackUrl: String?,
    modifier: Modifier = Modifier,
    parallax: Float = 0.3f,
    cardShape: Shape? = null,
    cardGap: Dp = 0.dp,
    picture: @Composable BoxScope.(url: String?, modifier: Modifier) -> Unit,
) {
    Box(modifier = if (cardShape == null) modifier.clipToBounds() else modifier) {
        val slots = carousel?.slots.orEmpty()
        if (carousel == null || slots.isEmpty()) {
            picture(fallbackUrl, Modifier.fillMaxSize())
            return@Box
        }
        for (slot in slots) {
            key(slot.uid) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val o = carousel.offsetOf(slot)
                            alpha = if (abs(o) >= 1f) 0f else 1f
                            translationX = o * (size.width + cardGap.toPx())
                            if (cardShape != null) shape = cardShape
                            clip = true
                        }
                ) {
                    picture(
                        slot.artworkUrl,
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationX = -carousel.offsetOf(slot) * size.width * parallax },
                    )
                }
            }
        }
    }
}

@Composable
fun CarouselCrossfade(
    carousel: SongCarousel?,
    fallbackUrl: String?,
    modifier: Modifier = Modifier,
    background: @Composable BoxScope.(url: String?) -> Unit,
) {
    Box(modifier = modifier) {
        val slots = carousel?.slots.orEmpty()
        if (carousel == null || slots.isEmpty()) {
            background(fallbackUrl)
            return@Box
        }
        for (slot in slots) {
            key(slot.uid) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = carousel.crossfadeAlpha(slot) },
                ) {
                    val shown = remember(carousel, slot) {
                        val visible = derivedStateOf { carousel.crossfadeAlpha(slot) > 0f }
                        val read: () -> Boolean = { visible.value }
                        read
                    }
                    CompositionLocalProvider(LocalCrossfadeShown provides shown) {
                        background(slot.artworkUrl)
                    }
                }
            }
        }
    }
}

val LocalCrossfadeShown = compositionLocalOf<() -> Boolean> { { true } }
