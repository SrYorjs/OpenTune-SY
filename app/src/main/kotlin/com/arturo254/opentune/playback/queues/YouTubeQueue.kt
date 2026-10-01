/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.playback.queues

import androidx.media3.common.MediaItem
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.innertube.models.WatchEndpoint
import com.arturo254.opentune.innertube.pages.NextResult
import com.arturo254.opentune.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class YouTubeQueue(
    private var endpoint: WatchEndpoint,
    override val preloadItem: MediaMetadata? = null,
) : Queue {
    private var continuation: String? = null
    private val seenMediaIds = LinkedHashSet<String>()
    private var fallbackSeedMediaId: String? =
        preloadItem?.id ?: endpoint.videoId
    private var fallbackPagingExhausted = false

    private val recommendationMode: Boolean
        get() = preloadItem != null

    override suspend fun getInitialStatus(): Queue.Status =
        withContext(IO) {
            val first =
                loadNextResilient(
                    endpoint = endpoint,
                    continuation = null,
                )

            var latest = first
            val songs =
                first.items
                    .toMutableList()

            if (recommendationMode) {
                var attempts = 0
                while (
                    songs
                        .asSequence()
                        .distinctBy { it.id }
                        .count() < TARGET_BATCH_SIZE &&
                    latest.continuation != null &&
                    attempts < MAX_PAGE_FETCHES_PER_BATCH
                ) {
                    val more =
                        runCatching {
                            loadNextResilient(
                                endpoint =
                                    latest.endpoint,
                                continuation =
                                    latest.continuation,
                            )
                        }.getOrNull()
                            ?: break

                    songs += more.items
                    latest = more
                    attempts += 1
                }

                if (
                    songs
                        .asSequence()
                        .distinctBy { it.id }
                        .count() < TARGET_BATCH_SIZE
                ) {
                    val related =
                        first.relatedEndpoint
                            ?.let { relatedEndpoint ->
                                YouTube
                                    .related(
                                        relatedEndpoint,
                                    ).getOrNull()
                                    ?.songs
                            }.orEmpty()

                    songs += related
                }
            }

            endpoint = latest.endpoint
            continuation = latest.continuation

            val mediaItems =
                buildInitialMediaItems(
                    songs = songs,
                    preferredIndex =
                        first.currentIndex,
                )

            mediaItems.forEach {
                seenMediaIds += it.mediaId
            }

            fallbackSeedMediaId =
                mediaItems
                    .lastOrNull()
                    ?.mediaId
                    ?: fallbackSeedMediaId

            Queue.Status(
                title = first.title,
                items = mediaItems,
                mediaItemIndex =
                    mediaItems.indexOfFirst {
                        it.mediaId ==
                            preloadItem?.id
                    }.takeIf { it >= 0 }
                        ?: first.currentIndex
                        ?: 0,
            )
        }

    override fun hasNextPage(): Boolean =
        continuation != null ||
            (
                recommendationMode &&
                    !fallbackPagingExhausted
            )

    override suspend fun nextPage(): List<MediaItem> =
        withContext(IO) {
            val nextItems =
                if (continuation != null) {
                    loadContinuationBatch()
                } else if (
                    recommendationMode &&
                    !fallbackPagingExhausted
                ) {
                    loadSyntheticRecommendationBatch()
                } else {
                    emptyList()
                }

            if (nextItems.isEmpty()) {
                if (continuation == null) {
                    fallbackPagingExhausted = true
                }
                return@withContext emptyList()
            }

            nextItems.forEach {
                seenMediaIds += it.mediaId
            }

            fallbackSeedMediaId =
                nextItems.lastOrNull()?.mediaId
                    ?: fallbackSeedMediaId

            nextItems
        }

    private suspend fun loadContinuationBatch():
        List<MediaItem> {
        val songs = mutableListOf<SongItem>()
        var latestEndpoint = endpoint
        var latestContinuation = continuation
        var attempts = 0

        while (
            latestContinuation != null &&
            songs
                .asSequence()
                .distinctBy { it.id }
                .filter {
                    it.id !in seenMediaIds
                }.count() < TARGET_BATCH_SIZE &&
            attempts < MAX_PAGE_FETCHES_PER_BATCH
        ) {
            val result =
                loadNextResilient(
                    endpoint = latestEndpoint,
                    continuation =
                        latestContinuation,
                )

            songs += result.items
            latestEndpoint = result.endpoint
            latestContinuation =
                result.continuation
            attempts += 1
        }

        endpoint = latestEndpoint
        continuation = latestContinuation

        return songs
            .asSequence()
            .distinctBy { it.id }
            .filter {
                it.id !in seenMediaIds
            }.take(TARGET_BATCH_SIZE)
            .map { it.toMediaItem() }
            .toList()
    }

    private suspend fun loadSyntheticRecommendationBatch():
        List<MediaItem> {
        val seed =
            fallbackSeedMediaId
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: preloadItem
                    ?.id
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                ?: return emptyList()

        val first =
            runCatching {
                loadNextResilient(
                    endpoint =
                        WatchEndpoint(
                            videoId = seed,
                        ),
                    continuation = null,
                )
            }.getOrNull()
                ?: run {
                    fallbackPagingExhausted = true
                    return emptyList()
                }

        val songs =
            first.items
                .toMutableList()

        var latest = first
        var attempts = 0

        while (
            songs
                .asSequence()
                .distinctBy { it.id }
                .filter {
                    it.id !in seenMediaIds
                }.count() < TARGET_BATCH_SIZE &&
            latest.continuation != null &&
            attempts < MAX_PAGE_FETCHES_PER_BATCH
        ) {
            val more =
                runCatching {
                    loadNextResilient(
                        endpoint =
                            latest.endpoint,
                        continuation =
                            latest.continuation,
                    )
                }.getOrNull()
                    ?: break

            songs += more.items
            latest = more
            attempts += 1
        }

        if (
            songs
                .asSequence()
                .distinctBy { it.id }
                .filter {
                    it.id !in seenMediaIds
                }.count() < TARGET_BATCH_SIZE
        ) {
            val related =
                first.relatedEndpoint
                    ?.let { relatedEndpoint ->
                        YouTube
                            .related(
                                relatedEndpoint,
                            ).getOrNull()
                            ?.songs
                    }.orEmpty()

            songs += related
        }

        endpoint = latest.endpoint
        continuation = latest.continuation

        val items =
            songs
                .asSequence()
                .distinctBy { it.id }
                .filter {
                    it.id !in seenMediaIds &&
                        it.id != seed
                }.take(TARGET_BATCH_SIZE)
                .map { it.toMediaItem() }
                .toList()

        if (
            items.isEmpty() &&
            continuation == null
        ) {
            fallbackPagingExhausted = true
        }

        return items
    }

    private suspend fun loadNextResilient(
        endpoint: WatchEndpoint,
        continuation: String?,
    ): NextResult {
        val primary =
            YouTube.next(
                endpoint = endpoint,
                continuation = continuation,
                followAutomixPreview = true,
            )

        primary.getOrNull()?.let {
            return it
        }

        return YouTube
            .next(
                endpoint = endpoint,
                continuation = continuation,
                followAutomixPreview = false,
            ).getOrThrow()
    }

    private fun buildInitialMediaItems(
        songs: List<SongItem>,
        preferredIndex: Int?,
    ): List<MediaItem> {
        val distinctSongs =
            songs
                .asSequence()
                .distinctBy { it.id }
                .take(TARGET_BATCH_SIZE)
                .map { it.toMediaItem() }
                .toMutableList()

        val preload =
            preloadItem ?: return distinctSongs

        val currentIndex =
            distinctSongs.indexOfFirst {
                it.mediaId == preload.id
            }

        if (currentIndex >= 0) {
            return distinctSongs
        }

        val insertAt =
            preferredIndex
                ?.coerceIn(
                    0,
                    distinctSongs.size,
                )
                ?: 0

        distinctSongs.add(
            insertAt,
            preload.toMediaItem(),
        )

        if (
            distinctSongs.size >
            TARGET_BATCH_SIZE
        ) {
            distinctSongs.removeLast()
        }

        return distinctSongs
    }

    companion object {
        private const val TARGET_BATCH_SIZE = 50
        private const val MAX_PAGE_FETCHES_PER_BATCH = 3

        fun radio(
            song: MediaMetadata,
        ) = YouTubeQueue(
            WatchEndpoint(song.id),
            song,
        )
    }
}
