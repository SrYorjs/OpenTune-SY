package com.arturo254.opentune.core.model

/**
 * Stable identifier for a provider integration.
 *
 * The value is intentionally open instead of an enum so new providers can be added
 * without changing the universal track model.
 */
@JvmInline
value class ProviderId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "ProviderId cannot be blank" }
    }

    companion object {
        val YOUTUBE_MUSIC = ProviderId("youtube_music")
        val SPOTIFY = ProviderId("spotify")
        val DEEZER = ProviderId("deezer")
        val TIDAL = ProviderId("tidal")
        val APPLE_MUSIC = ProviderId("apple_music")
        val LOCAL = ProviderId("local")
    }
}

/**
 * Provider-independent artist identity.
 *
 * [canonicalId] is optional until a future matcher/identity store assigns one.
 */
data class ArtistIdentity(
    val name: String,
    val canonicalId: String? = null,
)

/**
 * Provider-independent album identity.
 *
 * Provider album IDs belong to [SourceVariant], not here.
 */
data class AlbumIdentity(
    val title: String,
    val artists: List<ArtistIdentity> = emptyList(),
    val releaseYear: Int? = null,
    val canonicalId: String? = null,
)

/**
 * Describes edition/version traits that must not be silently merged while matching.
 *
 * Traits are a set because a recording can legitimately be both LIVE and ACOUSTIC,
 * or both REMASTER and OFFICIAL_AUDIO.
 */
data class TrackVersion(
    val traits: Set<TrackVersionTrait> = emptySet(),
    val label: String? = null,
)

enum class TrackVersionTrait {
    ORIGINAL,
    REMASTER,
    LIVE,
    ACOUSTIC,
    REMIX,
    COVER,
    INSTRUMENTAL,
    CLEAN,
    SPED_UP,
    SLOWED,
    MUSIC_VIDEO,
    OFFICIAL_AUDIO,
}

/**
 * Provider-independent description of one conceptual recording.
 *
 * This does not replace MediaMetadata yet. Phase 2.1 only establishes the domain
 * model that later TrackMatcher/SourceResolver work can consume.
 */
data class TrackIdentity(
    val title: String,
    val artists: List<ArtistIdentity>,
    val album: AlbumIdentity? = null,
    val durationMs: Long? = null,
    val isrc: String? = null,
    val explicit: Boolean? = null,
    val version: TrackVersion = TrackVersion(),
    val canonicalId: String? = null,
)

/**
 * Audio properties for a concrete source variant or a future offline edition.
 *
 * Null means that a provider did not expose the property. This deliberately avoids
 * inventing quality values that are not known.
 */
data class AudioQualityInfo(
    val mimeType: String? = null,
    val codec: String? = null,
    val bitrateBitsPerSecond: Int? = null,
    val sampleRateHz: Int? = null,
    val bitDepth: Int? = null,
    val channelCount: Int? = null,
    val lossless: Boolean? = null,
)

/**
 * One provider-specific representation of a conceptual track.
 *
 * Search results from different providers remain distinct here so a future
 * TrackMatcher can decide whether they represent the same recording. Playback and
 * download capabilities are intentionally not modeled in 2.1; those belong to the
 * later MusicSource/SourceCapabilities contract.
 */
data class SourceVariant(
    val provider: ProviderId,
    val sourceId: String,
    val title: String,
    val artists: List<String>,
    val albumId: String? = null,
    val albumName: String? = null,
    val durationMs: Long? = null,
    val isrc: String? = null,
    val explicit: Boolean? = null,
    val version: TrackVersion = TrackVersion(),
    val artworkUrl: String? = null,
    val animatedArtworkUrl: String? = null,
    val quality: AudioQualityInfo? = null,
)
