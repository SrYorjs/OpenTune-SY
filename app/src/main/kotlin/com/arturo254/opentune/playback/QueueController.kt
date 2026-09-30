package com.arturo254.opentune.playback

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import com.arturo254.opentune.extensions.SilentHandler
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.toPersistQueue
import com.arturo254.opentune.extensions.toQueue
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.models.PersistQueue
import com.arturo254.opentune.playback.queues.EmptyQueue
import com.arturo254.opentune.playback.queues.Queue
import com.arturo254.opentune.playback.queues.filterExplicit
import com.arturo254.opentune.playback.queues.filterVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns queue state and the mechanics of loading/manipulating the ExoPlayer
 * timeline. MusicService keeps service-specific policy (Together permissions,
 * foreground lifecycle, automix orchestration) and delegates queue mechanics.
 */
internal class QueueController(
    private val player: ExoPlayer,
    private val scopeProvider: () -> CoroutineScope,
    private val hideExplicitProvider: () -> Boolean,
    private val hideVideoProvider: () -> Boolean,
    private val permanentShuffleProvider: () -> Boolean,
    private val onCurrentMetadataChanged: () -> Unit,
    private val onQueueRestored: () -> Unit,
    private val onDeferredLoadError: (Throwable) -> Unit,
) {
    internal var currentQueue: Queue = EmptyQueue
    internal var queueTitle: String? = null

    @Volatile
    internal var suppressAutoPlayback: Boolean = false

    suspend fun restorePersistentQueue(persistedQueue: PersistQueue) {
        val restoredQueue = persistedQueue.toQueue()
        val initialStatus =
            withContext(Dispatchers.IO) {
                restoredQueue
                    .getInitialStatus()
                    .filterExplicit(hideExplicitProvider())
                    .filterVideo(hideVideoProvider())
            }

        withContext(Dispatchers.Main) {
            currentQueue = restoredQueue
            queueTitle = initialStatus.title

            val items = initialStatus.items
            val window = QueueWindowPlanner.plan(items.size, initialStatus.mediaItemIndex)
                ?: return@withContext

            val initialChunk = items.subList(window.start, window.endExclusive)
            player.setMediaItems(
                initialChunk,
                window.relativeIndex,
                initialStatus.position,
            )
            player.prepare()
            player.playWhenReady = false
            onCurrentMetadataChanged()
            onQueueRestored()

            scheduleDeferredQueueLoad(
                allItems = items,
                window = window,
            )
        }
    }

    fun playQueue(
        queue: Queue,
        playWhenReady: Boolean,
        clearAuxiliaryQueueState: () -> Unit,
    ) {
        suppressAutoPlayback = false
        currentQueue = queue
        queueTitle = null

        if (!permanentShuffleProvider()) {
            player.shuffleModeEnabled = false
        }

        clearAuxiliaryQueueState()

        queue.preloadItem?.let { preload ->
            player.setMediaItem(preload.toMediaItem())
            player.prepare()
            player.playWhenReady = playWhenReady
        }

        scopeProvider().launch(SilentHandler) {
            val initialStatus =
                withContext(Dispatchers.IO) {
                    queue
                        .getInitialStatus()
                        .filterExplicit(hideExplicitProvider())
                        .filterVideo(hideVideoProvider())
                }

            initialStatus.title?.let { queueTitle = it }
            if (initialStatus.items.isEmpty()) return@launch

            if (queue.preloadItem != null) {
                val safeIndex =
                    initialStatus.mediaItemIndex.coerceIn(0, initialStatus.items.lastIndex)

                if (safeIndex > 0) {
                    player.addMediaItems(
                        0,
                        initialStatus.items.subList(0, safeIndex),
                    )
                }
                if (safeIndex + 1 < initialStatus.items.size) {
                    player.addMediaItems(
                        initialStatus.items.subList(safeIndex + 1, initialStatus.items.size),
                    )
                }

                if (player.shuffleModeEnabled) {
                    applyCurrentFirstShuffleOrder()
                }
                return@launch
            }

            val items = initialStatus.items
            val window = QueueWindowPlanner.plan(items.size, initialStatus.mediaItemIndex)
                ?: return@launch
            val initialChunk = items.subList(window.start, window.endExclusive)

            player.setMediaItems(
                initialChunk,
                window.relativeIndex,
                initialStatus.position,
            )
            player.prepare()
            player.playWhenReady = playWhenReady

            if (player.shuffleModeEnabled) {
                applyCurrentFirstShuffleOrder()
            }

            scheduleDeferredQueueLoad(
                allItems = items,
                window = window,
            )
        }
    }

    fun playNext(items: List<MediaItem>) {
        if (items.isEmpty()) return

        suppressAutoPlayback = false
        player.addMediaItems(
            if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1,
            items,
        )
        player.prepare()
    }

    fun addToQueue(items: List<MediaItem>) {
        if (items.isEmpty()) return

        suppressAutoPlayback = false
        player.addMediaItems(items)
        player.prepare()
    }

    fun clearState(
        suppressAutoPlayback: Boolean,
    ) {
        this.suppressAutoPlayback = suppressAutoPlayback
        currentQueue = EmptyQueue
        queueTitle = null
    }

    fun applyCurrentFirstShuffleOrder() {
        val count = player.mediaItemCount
        if (count <= 1) return

        val currentIndex = player.currentMediaItemIndex.coerceIn(0, count - 1)
        val shuffledIndices = IntArray(count) { it }
        shuffledIndices.shuffle()

        val currentPos = shuffledIndices.indexOf(currentIndex)
        if (currentPos >= 0) {
            shuffledIndices[currentPos] = shuffledIndices[0]
        }
        shuffledIndices[0] = currentIndex

        player.setShuffleOrder(
            DefaultShuffleOrder(
                shuffledIndices,
                System.currentTimeMillis(),
            ),
        )
    }

    fun hasNextPage(): Boolean =
        currentQueue.hasNextPage()

    suspend fun nextPage(): List<MediaItem> =
        withContext(Dispatchers.IO) {
            currentQueue
                .nextPage()
                .filterExplicit(hideExplicitProvider())
                .filterVideo(hideVideoProvider())
        }

    fun toPersistQueue(
        items: List<MediaMetadata>,
        mediaItemIndex: Int,
        position: Long,
    ): PersistQueue =
        currentQueue.toPersistQueue(
            title = queueTitle,
            items = items,
            mediaItemIndex = mediaItemIndex,
            position = position,
        )

    private fun scheduleDeferredQueueLoad(
        allItems: List<MediaItem>,
        window: QueueWindow,
    ) {
        if (allItems.size <= window.endExclusive - window.start) return

        scopeProvider().launch(SilentHandler) {
            try {
                delay(2_000)
                if (!isActive || player.mediaItemCount == 0) return@launch

                if (window.start > 0) {
                    player.addMediaItems(
                        0,
                        allItems.subList(0, window.start),
                    )
                }
                if (window.endExclusive < allItems.size) {
                    player.addMediaItems(
                        allItems.subList(window.endExclusive, allItems.size),
                    )
                }

                if (player.shuffleModeEnabled) {
                    applyCurrentFirstShuffleOrder()
                }
            } catch (error: Exception) {
                onDeferredLoadError(error)
            }
        }
    }
}
