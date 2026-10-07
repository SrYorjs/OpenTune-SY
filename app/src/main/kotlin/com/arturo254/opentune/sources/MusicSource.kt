package com.arturo254.opentune.sources

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant

/**
 * Capabilities are facts about an integration, not promises that every provider
 * exposes the same API or that OpenTune may treat its media the same way.
 */
enum class SourceCapability {
    TRACK_SEARCH,
    TRACK_METADATA,
    ALBUMS,
    ARTISTS,
    PLAYLISTS,
    RADIO,
    ACCOUNT_LIBRARY,
    STREAM,
    OPEN_TUNE_MANAGED_DOWNLOAD,
    OFFLINE_LIBRARY,
    PROVIDER_MANAGED_OFFLINE,
    AUDIO_QUALITY,
    STATIC_ARTWORK,
    ANIMATED_ARTWORK,
    LINE_SYNCED_LYRICS,
    WORD_SYNCED_LYRICS,
}

/**
 * Immutable set of capabilities declared by a [MusicSource].
 */
data class SourceCapabilities private constructor(
    val values: Set<SourceCapability>,
) {
    operator fun contains(capability: SourceCapability): Boolean = capability in values

    fun supports(capability: SourceCapability): Boolean = capability in values

    fun supportsAll(vararg capabilities: SourceCapability): Boolean =
        capabilities.all(values::contains)

    companion object {
        val NONE = SourceCapabilities(emptySet())

        fun of(vararg capabilities: SourceCapability): SourceCapabilities =
            SourceCapabilities(capabilities.toSet())
    }
}

/**
 * Minimal provider contract for Phase 2.
 *
 * Playback, downloads and matching intentionally stay outside this base interface
 * until SourceResolver/TrackMatcher are introduced. This keeps provider discovery
 * separate from playback policy and prevents a source from pretending to support
 * operations it cannot legally or technically perform.
 */
interface MusicSource {
    val id: ProviderId
    val displayName: String
    val capabilities: SourceCapabilities

    fun supports(capability: SourceCapability): Boolean = capabilities.supports(capability)
}

/**
 * Optional contract implemented only by sources that can search tracks.
 */
interface TrackSearchSource : MusicSource {
    suspend fun searchTracks(request: TrackSearchRequest): Result<TrackSearchPage>
}

/**
 * Provider-neutral search request. Continuation tokens are opaque to callers and
 * are meaningful only to the source that produced them.
 */
data class TrackSearchRequest(
    val query: String,
    val limit: Int = DEFAULT_LIMIT,
    val continuationToken: String? = null,
) {
    init {
        require(limit > 0) { "Track search limit must be positive" }
    }

    companion object {
        const val DEFAULT_LIMIT = 50
    }
}

/**
 * One page of provider-specific variants. No matching/deduplication happens here.
 */
data class TrackSearchPage(
    val items: List<SourceVariant>,
    val continuationToken: String? = null,
)
