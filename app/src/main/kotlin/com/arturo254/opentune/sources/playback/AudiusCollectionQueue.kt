package com.arturo254.opentune.sources.playback

import androidx.media3.common.MediaItem
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.queues.Queue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AudiusCollectionQueue(
    private val title: String,
    tracks: List<SourceVariant>,
    startIndex: Int,
    private val resolver: suspend (SourceVariant) -> MediaItem?,
) : Queue {
    override val preloadItem: MediaMetadata? = null
    private val mutex = Mutex()
    private val orderedTracks = rotateAudiusTracks(tracks, startIndex)
    private var initialAttempted = false
    private var initialItem: MediaItem? = null
    private var cursor = 1

    override suspend fun getInitialStatus(): Queue.Status = mutex.withLock {
        if (!initialAttempted) {
            initialAttempted = true
            initialItem = orderedTracks.firstOrNull()?.let { resolver(it) }
        }
        Queue.Status(title, listOfNotNull(initialItem), 0)
    }

    override fun hasNextPage(): Boolean =
        initialAttempted && initialItem != null && cursor < orderedTracks.size

    override suspend fun nextPage(): List<MediaItem> = mutex.withLock {
        if (!hasNextPage()) return@withLock emptyList()
        val end = audiusQueuePageEnd(cursor, orderedTracks.size)
        val page = orderedTracks.subList(cursor, end).toList()
        cursor = end
        page.mapNotNull { resolver(it) }
    }

}

internal fun <T> rotateAudiusTracks(tracks: List<T>, startIndex: Int): List<T> =
    if (startIndex in tracks.indices) tracks.drop(startIndex) + tracks.take(startIndex) else emptyList()

internal fun audiusQueuePageEnd(cursor: Int, trackCount: Int): Int =
    minOf(cursor + 49, trackCount)
