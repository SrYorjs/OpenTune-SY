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
    private var currentPagingSeedMediaId: String? =
        fallbackSeedMediaId
    private var consecutiveEmptyPageLoads = 0

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
                first.items.toMutableList()

            if (recommendationMode) {
                var attempts = 0

                while (
                    uniqueUnseenCount(
                        songs,
                        emptySet(),
                    ) < TARGET_BATCH_SIZE &&
                    latest.continuation != null &&
                    attempts <
                        MAX_PAGE_FETCHES_PER_BATCH
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
                    uniqueUnseenCount(
                        songs,
                        emptySet(),
                    ) < TARGET_BATCH_SIZE
                ) {
                    songs +=
                        first.relatedEndpoint
                            ?.let { relatedEndpoint ->
                                YouTube
                                    .related(
                                        relatedEndpoint,
                                    ).getOrNull()
                                    ?.songs
                            }.orEmpty()
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

            currentPagingSeedMediaId =
                preloadItem?.id
                    ?: mediaItems
                        .lastOrNull()
                        ?.mediaId
                    ?: currentPagingSeedMediaId

            consecutiveEmptyPageLoads = 0

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

    override fun updatePagingSeed(
        mediaId: String?,
    ) {
        val normalized =
            mediaId
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: return

        if (
            normalized !=
            currentPagingSeedMediaId
        ) {
            currentPagingSeedMediaId =
                normalized
            consecutiveEmptyPageLoads = 0
        }
    }

    override fun hasNextPage(): Boolean =
        continuation != null ||
            (
                recommendationMode &&
                    (
                        currentPagingSeedMediaId != null ||
                            fallbackSeedMediaId != null
                    ) &&
                    consecutiveEmptyPageLoads <
                        MAX_CONSECUTIVE_EMPTY_LOADS
            )

    override suspend fun nextPage(): List<MediaItem> =
        withContext(IO) {
            val collected =
                LinkedHashMap<String, MediaItem>()

            if (continuation != null) {
                loadContinuationBatch()
                    .forEach { item ->
                        if (
                            item.mediaId !in
                            seenMediaIds
                        ) {
                            collected
                                .putIfAbsent(
                                    item.mediaId,
                                    item,
                                )
                        }
                    }
            }

            if (
                recommendationMode &&
                collected.size <
                    TARGET_BATCH_SIZE
            ) {
                val excluded =
                    buildSet {
                        addAll(seenMediaIds)
                        addAll(collected.keys)
                    }

                loadSyntheticRecommendationBatch(
                    excludedMediaIds =
                        excluded,
                    limit =
                        TARGET_BATCH_SIZE -
                            collected.size,
                ).forEach { item ->
                    if (
                        item.mediaId !in
                        seenMediaIds
                    ) {
                        collected
                            .putIfAbsent(
                                item.mediaId,
                                item,
                            )
                    }
                }
            }

            val nextItems =
                collected
                    .values
                    .take(TARGET_BATCH_SIZE)

            if (nextItems.isEmpty()) {
                consecutiveEmptyPageLoads += 1
                return@withContext emptyList()
            }

            consecutiveEmptyPageLoads = 0

            nextItems.forEach {
                seenMediaIds += it.mediaId
            }

            fallbackSeedMediaId =
                nextItems
                    .lastOrNull()
                    ?.mediaId
                    ?: fallbackSeedMediaId

            nextItems
        }

    private suspend fun loadContinuationBatch():
        List<MediaItem> {
        val songs =
            mutableListOf<SongItem>()

        var latestEndpoint = endpoint
        var latestContinuation =
            continuation
        var attempts = 0

        while (
            latestContinuation != null &&
            uniqueUnseenCount(
                songs,
                seenMediaIds,
            ) < TARGET_BATCH_SIZE &&
            attempts <
                MAX_PAGE_FETCHES_PER_BATCH
        ) {
            val result =
                runCatching {
                    loadNextResilient(
                        endpoint =
                            latestEndpoint,
                        continuation =
                            latestContinuation,
                    )
                }.getOrNull()
                    ?: break

            songs += result.items
            latestEndpoint =
                result.endpoint
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

    private suspend fun loadSyntheticRecommendationBatch(
        excludedMediaIds: Set<String>,
        limit: Int,
    ): List<MediaItem> {
        if (limit <= 0) return emptyList()

        val candidateSeeds =
            buildList {
                currentPagingSeedMediaId
                    ?.let(::add)
                fallbackSeedMediaId
                    ?.let(::add)

                seenMediaIds
                    .toList()
                    .asReversed()
                    .take(
                        RECENT_SEED_CANDIDATES,
                    ).forEach(::add)

                preloadItem
                    ?.id
                    ?.let(::add)
            }.asSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .take(MAX_FALLBACK_SEEDS)
                .toList()

        if (candidateSeeds.isEmpty()) {
            return emptyList()
        }

        val collected =
            LinkedHashMap<String, MediaItem>()

        for (seed in candidateSeeds) {
            if (collected.size >= limit) {
                break
            }

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
                    ?: continue

            var latest = first
            val songs =
                first.items.toMutableList()
            var attempts = 0

            while (
                uniqueUnseenCount(
                    songs,
                    excludedMediaIds +
                        collected.keys,
                ) <
                    limit -
                        collected.size &&
                latest.continuation != null &&
                attempts <
                    MAX_PAGE_FETCHES_PER_SEED
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
                uniqueUnseenCount(
                    songs,
                    excludedMediaIds +
                        collected.keys,
                ) <
                    limit -
                        collected.size
            ) {
                songs +=
                    first.relatedEndpoint
                        ?.let { relatedEndpoint ->
                            YouTube
                                .related(
                                    relatedEndpoint,
                                ).getOrNull()
                                ?.songs
                        }.orEmpty()
            }

            songs
                .asSequence()
                .distinctBy { it.id }
                .filter {
                    it.id != seed &&
                        it.id !in
                            excludedMediaIds &&
                        it.id !in
                            collected
                }.take(
                    limit -
                        collected.size,
                ).forEach { song ->
                    collected[
                        song.id
                    ] = song.toMediaItem()
                }

            if (collected.isNotEmpty()) {
                endpoint = latest.endpoint
                continuation =
                    latest.continuation
            }
        }

        return collected.values.toList()
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

    private fun uniqueUnseenCount(
        songs: List<SongItem>,
        excludedMediaIds: Set<String>,
    ): Int =
        songs
            .asSequence()
            .map { it.id }
            .distinct()
            .count {
                it !in excludedMediaIds
            }

    companion object {
        private const val TARGET_BATCH_SIZE = 50
        private const val MAX_PAGE_FETCHES_PER_BATCH = 4
        private const val MAX_PAGE_FETCHES_PER_SEED = 2
        private const val MAX_FALLBACK_SEEDS = 6
        private const val RECENT_SEED_CANDIDATES = 12
        private const val MAX_CONSECUTIVE_EMPTY_LOADS = 3

        fun radio(
            song: MediaMetadata,
        ) = YouTubeQueue(
            WatchEndpoint(song.id),
            song,
        )
    }
}
