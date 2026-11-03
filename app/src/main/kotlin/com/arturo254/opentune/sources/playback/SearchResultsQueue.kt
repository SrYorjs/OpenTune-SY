package com.arturo254.opentune.sources.playback

import androidx.media3.common.MediaItem
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.queues.Queue
import com.arturo254.opentune.sources.search.UnifiedSearchRepository
import com.arturo254.opentune.sources.search.UnifiedSearchRequest
import com.arturo254.opentune.sources.search.UnifiedSearchResult
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import com.arturo254.opentune.sources.matching.TrackMatcher
import com.arturo254.opentune.sources.search.sameRecording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

class SearchResultsQueue(
    private val title: String,
    initialResult: UnifiedSearchResult,
    selected: UnifiedTrackSearchResult,
    private val selectedVariants: List<SourceVariant>,
    private val repository: UnifiedSearchRepository,
    private val coordinator: SourcePlaybackCoordinator,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    private val mutex = Mutex()
    private val knownTracks = initialResult.tracks.toMutableList()
    private val pending = mutableListOf<UnifiedTrackSearchResult>()
    private val knownKeys = mutableSetOf<String>()
    private val recordingMatcher = TrackMatcher()
    private val emittedIds = mutableSetOf<String>()
    private val relatedQueries = mutableSetOf<String>()
    private var relatedSearches = 0
    private var exhausted = false
    private val semaphore = Semaphore(MAX_PARALLEL_RESOLUTIONS)

    @Volatile
    private var cursor = 0

    @Volatile
    private var currentResult = initialResult

    @Volatile
    private var nextPageAvailable = true

    private var initialItem: MediaItem? = null
    private var initialAttempted = false
    private var loadedCount = 0

    init {
        val selectedIndex = knownTracks.indexOfFirst { it == selected }
        val start = if (selectedIndex >= 0) selectedIndex else 0
        val rotated = if (knownTracks.isEmpty()) {
            listOf(selected)
        } else {
            knownTracks.drop(start) + knownTracks.take(start)
        }
        rotated.forEach(::rememberTrack)
        nextPageAvailable = pending.isNotEmpty() || currentResult.hasMore
    }

    override suspend fun getInitialStatus(): Queue.Status = mutex.withLock {
        if (!initialAttempted) {
            initialAttempted = true
            val selectedTrack = pending.firstOrNull()
            if (selectedTrack != null) {
                cursor = 1
                val choices = selectedVariants.ifEmpty { playbackOptions(selectedTrack) }
                val item = coordinator.resolveTrack(choices)
                if (item != null && emittedIds.add(item.mediaId)) {
                    initialItem = item
                    loadedCount = 1
                }
            }
        }
        updateAvailability()
        Queue.Status(
            title = title,
            items = listOfNotNull(initialItem),
            mediaItemIndex = 0,
        )
    }

    override fun hasNextPage(): Boolean = nextPageAvailable

    override suspend fun nextPage(): List<MediaItem> = mutex.withLock {
        if (!initialAttempted || initialItem == null) return@withLock emptyList()
        val target = if (loadedCount == 1) PAGE_SIZE - 1 else PAGE_SIZE
        val output = mutableListOf<MediaItem>()
        var pagesWithoutNewTracks = 0

        while (output.size < target) {
            if (cursor >= pending.size) {
                if (pagesWithoutNewTracks >= MAX_EMPTY_PAGES) break
                val before = pending.size
                if (currentResult.hasMore) {
                    val previous = currentResult
                    currentResult = repository.loadMore(
                        request = UnifiedSearchRequest(query = previous.query),
                        previous = previous,
                    )
                } else {
                    val relatedQuery = nextRelatedQuery() ?: break
                    relatedSearches++
                    currentResult = repository.search(UnifiedSearchRequest(query = relatedQuery))
                }
                currentResult.tracks.forEach(::rememberTrack)
                pagesWithoutNewTracks =
                    if (pending.size == before) pagesWithoutNewTracks + 1 else 0
                continue
            }

            val remaining = target - output.size
            val batch = pending.subList(
                cursor,
                minOf(cursor + remaining, pending.size),
            ).toList()
            cursor += batch.size

            val resolved = coroutineScope {
                batch.map { track ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            coordinator.resolveTrack(playbackOptions(track))
                        }
                    }
                }.awaitAll()
            }

            resolved.filterNotNull().forEach { item ->
                if (emittedIds.add(item.mediaId)) output.add(item)
            }
        }

        if (pagesWithoutNewTracks >= MAX_EMPTY_PAGES) exhausted = true
        loadedCount += output.size
        updateAvailability()
        output
    }

    private fun updateAvailability() {
        nextPageAvailable = !exhausted &&
            (cursor < pending.size || currentResult.hasMore || hasRelatedQuery())
    }

    private fun rememberTrack(track: UnifiedTrackSearchResult) {
        val sources = track.variants.map { it.provider to it.sourceId }.toSet()
        if (pending.any { existing ->
            existing.variants.any { (it.provider to it.sourceId) in sources } ||
                sameRecording(existing, track, recordingMatcher)
        }) return
        if (knownKeys.add(trackKey(track))) pending.add(track)
    }

    private fun hasRelatedQuery(): Boolean =
        relatedSearches < MAX_RELATED_SEARCHES &&
            pending.any { track ->
                track.identity.artists.any { artist ->
                    artist.name.isNotBlank() && artist.name.lowercase() !in relatedQueries
                }
            }

    private fun nextRelatedQuery(): String? {
        if (relatedSearches >= MAX_RELATED_SEARCHES) return null
        val artist = pending.asReversed()
            .flatMap { it.identity.artists }
            .firstOrNull { it.name.isNotBlank() && it.name.lowercase() !in relatedQueries }
            ?.name ?: return null
        relatedQueries.add(artist.lowercase())
        return artist
    }

    private fun playbackOptions(track: UnifiedTrackSearchResult): List<SourceVariant> =
        (listOfNotNull(track.recommendedVariant) + track.variants)
            .distinctBy { it.provider to it.sourceId }
            .sortedBy { variant ->
                when (variant.provider) {
                    ProviderId.LOCAL, ProviderId.OPEN_TUNE_OFFLINE -> 0
                    ProviderId.AUDIUS -> 1
                    ProviderId.YOUTUBE_MUSIC -> 2
                    else -> 3
                }
            }

    private fun trackKey(track: UnifiedTrackSearchResult): String =
        track.variants.map { it.provider.value + ":" + it.sourceId }.sorted().joinToString("|")
            .ifBlank {
                track.identity.title + "|" + track.identity.artists.joinToString(",") { it.name }
            }

    private companion object {
        const val PAGE_SIZE = 50
        const val MAX_PARALLEL_RESOLUTIONS = 2
        const val MAX_EMPTY_PAGES = 2
        const val MAX_RELATED_SEARCHES = 3
    }
}
