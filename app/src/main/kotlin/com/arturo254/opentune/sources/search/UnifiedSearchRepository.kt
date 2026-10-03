package com.arturo254.opentune.sources.search

import com.arturo254.opentune.core.model.AlbumIdentity
import com.arturo254.opentune.core.model.ArtistIdentity
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.sources.SourceRegistry
import com.arturo254.opentune.sources.TrackSearchPage
import com.arturo254.opentune.sources.TrackSearchRequest
import com.arturo254.opentune.sources.TrackSearchSource
import com.arturo254.opentune.sources.matching.TrackMatchIssue
import com.arturo254.opentune.sources.matching.TrackMatcher
import com.arturo254.opentune.sources.resolution.PlaybackAvailability
import com.arturo254.opentune.sources.resolution.SourcePlaybackCandidate
import com.arturo254.opentune.sources.resolution.SourceResolutionPreferences
import com.arturo254.opentune.sources.resolution.SourceResolver
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

/**
 * Queries every installed [TrackSearchSource] concurrently and folds provider
 * results into conceptual tracks.
 *
 * A provider failure is isolated: successful sources still produce results.
 * Deduplication is conservative and delegated to TrackMatcher so different
 * recordings (live/remix/remaster/etc.) stay separate.
 */
@Singleton
class UnifiedSearchRepository
@Inject
constructor(
    private val registry: SourceRegistry,
    private val trackMatcher: TrackMatcher,
    private val sourceResolver: SourceResolver,
) {
    suspend fun search(
        request: UnifiedSearchRequest,
    ): UnifiedSearchResult {
        val query = request.query.trim()
        if (query.isEmpty()) {
            return UnifiedSearchResult(query = query)
        }

        val sources = eligibleSources(request)
        if (sources.isEmpty()) {
            return UnifiedSearchResult(query = query)
        }

        val sourceResults =
            searchSources(
                sources = sources,
                query = query,
                limit = request.limitPerSource,
                continuationTokens = emptyMap(),
            )

        val sourceStates =
            sourceResults.associate { sourceResult ->
                sourceResult.provider to
                    UnifiedSearchSourceResult(
                        provider = sourceResult.provider,
                        itemCount = sourceResult.page?.items?.size ?: 0,
                        continuationToken = sourceResult.page?.continuationToken,
                        error = sourceResult.error,
                    )
            }

        return withContext(Dispatchers.Default) {
            buildResult(
                query = query,
                variants =
                    sourceResults
                        .flatMap { result -> result.page?.items.orEmpty() }
                        .distinctBy { variant -> variant.provider to variant.sourceId },
                sources = sourceStates,
                preferredProviderOrder = request.preferredProviderOrder,
            )
        }
    }

    suspend fun loadMore(
        request: UnifiedSearchRequest,
        previous: UnifiedSearchResult,
    ): UnifiedSearchResult {
        val query = request.query.trim()
        if (query.isEmpty()) return UnifiedSearchResult(query = query)
        if (previous.query != query) return search(request)

        val continuationTokens =
            previous.sources
                .mapNotNull { (provider, state) ->
                    state.continuationToken
                        ?.takeIf { it.isNotBlank() }
                        ?.let { provider to it }
                }.toMap()

        if (continuationTokens.isEmpty()) return previous

        val sources =
            eligibleSources(request)
                .filter { source -> source.id in continuationTokens }

        if (sources.isEmpty()) return previous

        val sourceResults =
            searchSources(
                sources = sources,
                query = query,
                limit = request.limitPerSource,
                continuationTokens = continuationTokens,
            )

        val mergedSourceStates = previous.sources.toMutableMap()
        sourceResults.forEach { sourceResult ->
            val oldState =
                mergedSourceStates[sourceResult.provider]
                    ?: UnifiedSearchSourceResult(
                        provider = sourceResult.provider,
                        itemCount = 0,
                    )

            mergedSourceStates[sourceResult.provider] =
                if (sourceResult.page != null) {
                    oldState.copy(
                        itemCount = oldState.itemCount + sourceResult.page.items.size,
                        continuationToken = sourceResult.page.continuationToken,
                        error = null,
                    )
                } else {
                    oldState.copy(error = sourceResult.error)
                }
        }

        val variants =
            (
                previous.tracks.flatMap { track -> track.variants } +
                    sourceResults.flatMap { result -> result.page?.items.orEmpty() }
            ).distinctBy { variant -> variant.provider to variant.sourceId }

        return withContext(Dispatchers.Default) {
            buildResult(
                query = query,
                variants = variants,
                sources = mergedSourceStates,
                preferredProviderOrder = request.preferredProviderOrder,
            )
        }
    }

    private fun eligibleSources(
        request: UnifiedSearchRequest,
    ): List<TrackSearchSource> =
        registry
            .trackSearchSources()
            .filter { source ->
                request.providers.isNullOrEmpty() || source.id in request.providers
            }.sortedWith(sourceComparator(request.preferredProviderOrder))

    private suspend fun searchSources(
        sources: List<TrackSearchSource>,
        query: String,
        limit: Int,
        continuationTokens: Map<ProviderId, String>,
    ): List<RawSourceSearchResult> =
        supervisorScope {
            sources
                .map { source ->
                    async {
                        searchSource(
                            source = source,
                            query = query,
                            limit = limit,
                            continuationToken = continuationTokens[source.id],
                        )
                    }
                }.awaitAll()
        }

    private suspend fun searchSource(
        source: TrackSearchSource,
        query: String,
        limit: Int,
        continuationToken: String?,
    ): RawSourceSearchResult {
        val result =
            runCatching {
                source.searchTracks(
                    TrackSearchRequest(
                        query = query,
                        limit = limit,
                        continuationToken = continuationToken,
                    ),
                )
            }.fold(
                onSuccess = { it },
                onFailure = { throwable -> Result.failure(throwable) },
            )

        return result.fold(
            onSuccess = { page ->
                RawSourceSearchResult(
                    provider = source.id,
                    page = page,
                )
            },
            onFailure = { throwable ->
                RawSourceSearchResult(
                    provider = source.id,
                    error =
                        UnifiedSearchError(
                            type = throwable::class.java.simpleName.ifBlank { "SearchError" },
                            message = throwable.message,
                        ),
                )
            },
        )
    }

    private fun buildResult(
        query: String,
        variants: List<SourceVariant>,
        sources: Map<ProviderId, UnifiedSearchSourceResult>,
        preferredProviderOrder: List<ProviderId>,
    ): UnifiedSearchResult {
        val groups = groupVariants(variants)

        return UnifiedSearchResult(
            query = query,
            tracks =
                groups.map { group ->
                    val resolution =
                        sourceResolver.resolve(
                            reference = group.identity,
                            candidates = group.variants.map(::toPlaybackCandidate),
                            preferences =
                                SourceResolutionPreferences(
                                    preferOffline = true,
                                    preferLocal = true,
                                    preferredProviders = preferredProviderOrder,
                                ),
                        )

                    UnifiedTrackSearchResult(
                        identity = group.identity,
                        variants = group.variants,
                        recommendedVariant = resolution.selected?.candidate?.variant,
                    )
                },
            sources = sources,
        )
    }

    private fun groupVariants(
        variants: List<SourceVariant>,
    ): List<MutableTrackGroup> {
        val groups = mutableListOf<MutableTrackGroup>()

        variants.forEach { variant ->
            val group =
                groups.firstOrNull { candidateGroup ->
                    canJoinGroup(
                        identity = candidateGroup.identity,
                        variants = candidateGroup.variants,
                        candidate = variant,
                    )
                }

            if (group == null) {
                groups +=
                    MutableTrackGroup(
                        identity = variant.toTrackIdentity(),
                        variants = mutableListOf(variant),
                    )
            } else {
                group.variants += variant
                group.identity = mergeIdentity(group.identity, variant)
            }
        }

        return groups
    }

    private fun canJoinGroup(
        identity: TrackIdentity,
        variants: List<SourceVariant>,
        candidate: SourceVariant,
    ): Boolean {
        val identityMatch = trackMatcher.match(identity, candidate)
        if (!identityMatch.isAccepted) return false

        return variants.none { existing ->
            val pairMatch = trackMatcher.match(existing, candidate)
            !pairMatch.isAccepted &&
                (
                    TrackMatchIssue.ISRC_CONFLICT in pairMatch.issues ||
                        TrackMatchIssue.RECORDING_VERSION_CONFLICT in pairMatch.issues
                )
        }
    }

    private fun mergeIdentity(
        current: TrackIdentity,
        variant: SourceVariant,
    ): TrackIdentity =
        current.copy(
            artists =
                if (current.artists.isEmpty() && variant.artists.isNotEmpty()) {
                    variant.artists.map { ArtistIdentity(name = it) }
                } else {
                    current.artists
                },
            album =
                current.album
                    ?: variant.albumName?.let { albumName ->
                        AlbumIdentity(
                            title = albumName,
                            canonicalId = variant.albumId,
                        )
                    },
            durationMs = current.durationMs ?: variant.durationMs,
            isrc = current.isrc ?: variant.isrc,
            explicit = current.explicit ?: variant.explicit,
            version =
                if (current.version.traits.isEmpty() && current.version.label == null) {
                    variant.version
                } else {
                    current.version
                },
        )

    private fun SourceVariant.toTrackIdentity(): TrackIdentity =
        TrackIdentity(
            title = title,
            artists = artists.map { ArtistIdentity(name = it) },
            album =
                albumName?.let { name ->
                    AlbumIdentity(
                        title = name,
                        canonicalId = albumId,
                    )
                },
            durationMs = durationMs,
            isrc = isrc,
            explicit = explicit,
            version = version,
        )

    private fun toPlaybackCandidate(variant: SourceVariant): SourcePlaybackCandidate =
        SourcePlaybackCandidate(
            variant = variant,
            availability =
                if (variant.provider == ProviderId.LOCAL) {
                    PlaybackAvailability.LOCAL_FILE
                } else {
                    PlaybackAvailability.ONLINE
                },
        )

    private fun sourceComparator(
        preferredProviders: List<ProviderId>,
    ): Comparator<TrackSearchSource> =
        compareBy<TrackSearchSource> { source ->
            val preferredIndex = preferredProviders.indexOf(source.id)
            if (preferredIndex >= 0) preferredIndex else Int.MAX_VALUE
        }.thenBy { source -> source.id.value }

    private data class RawSourceSearchResult(
        val provider: ProviderId,
        val page: TrackSearchPage? = null,
        val error: UnifiedSearchError? = null,
    )

    private data class MutableTrackGroup(
        var identity: TrackIdentity,
        val variants: MutableList<SourceVariant>,
    )
}

data class UnifiedSearchRequest(
    val query: String,
    val limitPerSource: Int = DEFAULT_LIMIT_PER_SOURCE,
    val providers: Set<ProviderId>? = null,
    val preferredProviderOrder: List<ProviderId> =
        listOf(
            ProviderId.YOUTUBE_MUSIC,
            ProviderId.LOCAL,
        ),
) {
    init {
        require(limitPerSource > 0) { "limitPerSource must be positive" }
    }

    companion object {
        const val DEFAULT_LIMIT_PER_SOURCE = 50
    }
}

data class UnifiedSearchResult(
    val query: String,
    val tracks: List<UnifiedTrackSearchResult> = emptyList(),
    val sources: Map<ProviderId, UnifiedSearchSourceResult> = emptyMap(),
) {
    val successfulSourceCount: Int
        get() = sources.values.count { it.error == null }

    val failedSourceCount: Int
        get() = sources.values.count { it.error != null }

    val hasMore: Boolean
        get() = sources.values.any { !it.continuationToken.isNullOrBlank() }
}

data class UnifiedTrackSearchResult(
    val identity: TrackIdentity,
    val variants: List<SourceVariant>,
    val recommendedVariant: SourceVariant?,
)

data class UnifiedSearchSourceResult(
    val provider: ProviderId,
    val itemCount: Int,
    val continuationToken: String? = null,
    val error: UnifiedSearchError? = null,
)

data class UnifiedSearchError(
    val type: String,
    val message: String?,
)
