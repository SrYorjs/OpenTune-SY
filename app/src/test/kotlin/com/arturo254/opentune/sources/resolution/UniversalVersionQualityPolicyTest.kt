package com.arturo254.opentune.sources.resolution

import com.arturo254.opentune.core.model.ArtistIdentity
import com.arturo254.opentune.core.model.AudioQualityInfo
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.core.model.TrackVersion
import com.arturo254.opentune.core.model.TrackVersionTrait
import com.arturo254.opentune.sources.MusicSource
import com.arturo254.opentune.sources.SourceCapabilities
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.SourceRegistry
import com.arturo254.opentune.sources.matching.TrackMatcher
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalVersionQualityPolicyTest {
    private val youtube =
        source(ProviderId.YOUTUBE_MUSIC, SourceCapability.STREAM)
    private val losslessProvider =
        source(ProviderId("streaming_flac"), SourceCapability.STREAM, SourceCapability.AUDIO_QUALITY)
    private val tidal =
        source(ProviderId.TIDAL, SourceCapability.TRACK_METADATA, SourceCapability.AUDIO_QUALITY)

    @Test
    fun automaticPrefersYoutubeAccordingToDefaultProviderOrder() {
        val policy = policy(youtube, losslessProvider)
        val candidates =
            listOf(
                variant(ProviderId.YOUTUBE_MUSIC, "yt", bitrate = 192_000),
                variant(ProviderId("streaming_flac"), "flac", bitrate = 1_400_000, lossless = true),
            )

        assertEquals(
            "yt",
            policy.choose(identity(), candidates, AudioSelectionMode.AUTOMATIC)?.sourceId,
        )
        assertEquals(
            "flac",
            policy.choose(identity(), candidates, AudioSelectionMode.BEST_QUALITY)?.sourceId,
        )
        assertEquals(
            "yt",
            policy.choose(identity(), candidates, AudioSelectionMode.DATA_SAVER)?.sourceId,
        )
    }

    @Test
    fun tidalCatalogQualityCannotMasqueradeAsPlayableFlac() {
        val policy = policy(youtube, tidal)
        val selected =
            policy.choose(
                identity(),
                listOf(
                    variant(ProviderId.TIDAL, "tidal-catalog", bitrate = 1_500_000, lossless = true),
                    variant(ProviderId.YOUTUBE_MUSIC, "yt", bitrate = 128_000),
                ),
                AudioSelectionMode.BEST_QUALITY,
            )
        assertEquals(ProviderId.YOUTUBE_MUSIC, selected?.provider)
    }

    @Test
    fun userCanExplicitlyChooseCatalogReferenceWithoutAdvertisingItAsStream() {
        val policy = policy(youtube, tidal)
        val choice =
            policy.choose(
                identity(),
                listOf(
                    variant(ProviderId.TIDAL, "tidal", bitrate = 1_400_000, lossless = true),
                    variant(ProviderId.YOUTUBE_MUSIC, "yt", bitrate = 128_000),
                ),
                AudioSelectionMode.BEST_QUALITY,
                SourceVariantKey(ProviderId.TIDAL, "tidal"),
            )

        assertEquals(ProviderId.TIDAL, choice?.provider)
        assertEquals("tidal", choice?.sourceId)
    }

    @Test
    fun unavailableManualVariantFallsBackToAutomatic() {
        val policy = policy(youtube)
        val choice =
            policy.choose(
                identity(),
                listOf(variant(ProviderId.YOUTUBE_MUSIC, "yt", bitrate = 192_000)),
                AudioSelectionMode.AUTOMATIC,
                SourceVariantKey(ProviderId.SPOTIFY, "missing"),
            )

        assertEquals("yt", choice?.sourceId)
    }

    @Test
    fun offlineRemainsPreferredOverOnlineHighBitrate() {
        val policy = policy(youtube, losslessProvider)
        val choice =
            policy.choose(
                identity(),
                listOf(
                    variant(ProviderId.OPEN_TUNE_OFFLINE, "download", bitrate = 192_000),
                    variant(ProviderId("streaming_flac"), "flac", bitrate = 1_400_000, lossless = true),
                ),
                AudioSelectionMode.BEST_QUALITY,
            )

        assertEquals(ProviderId.OPEN_TUNE_OFFLINE, choice?.provider)
    }

    @Test
    fun manualLiveRemixAndRemasterAreOfferedAsSeparateRecordings() {
        val policy = policy(youtube)
        val original = track("Song", "original")
        val live = track("Song (Live)", "live", TrackVersion(setOf(TrackVersionTrait.LIVE)))
        val remix = track("Song - Remix", "remix", TrackVersion(setOf(TrackVersionTrait.REMIX)))
        val remaster = track("Song (2024 Remaster)", "remaster", TrackVersion(setOf(TrackVersionTrait.REMASTER)))
        val otherArtist = track("Song (Live)", "other", TrackVersion(setOf(TrackVersionTrait.LIVE)), artist = "Other")

        val options =
            policy.relatedVersions(original, listOf(original, live, remix, remaster, otherArtist))

        assertEquals(setOf("live", "remix", "remaster"), options.flatMap { it.variants }.map { it.sourceId }.toSet())
        assertTrue(options.none { it.variants.single().sourceId == "other" })
    }

    private fun policy(vararg sources: MusicSource): UniversalVersionQualityPolicy =
        UniversalVersionQualityPolicy(
            SourceResolver(
                SourceRegistry(sources.toSet()),
                TrackMatcher(),
            ),
        )

    private fun identity(title: String = "Song", artist: String = "Artist") =
        TrackIdentity(
            title = title,
            artists = listOf(ArtistIdentity(name = artist)),
            durationMs = 180_000L,
        )

    private fun variant(
        provider: ProviderId,
        id: String,
        bitrate: Int,
        lossless: Boolean = false,
    ) =
        SourceVariant(
            provider = provider,
            sourceId = id,
            title = "Song",
            artists = listOf("Artist"),
            durationMs = 180_000L,
            quality =
                AudioQualityInfo(
                    bitrateBitsPerSecond = bitrate,
                    lossless = lossless,
                ),
        )

    private fun track(
        title: String,
        id: String,
        version: TrackVersion = TrackVersion(),
        artist: String = "Artist",
    ) =
        UnifiedTrackSearchResult(
            identity = identity(title, artist).copy(version = version),
            variants =
                listOf(
                    SourceVariant(
                        provider = ProviderId.YOUTUBE_MUSIC,
                        sourceId = id,
                        title = title,
                        artists = listOf(artist),
                        version = version,
                        durationMs = 180_000L,
                    ),
                ),
            recommendedVariant = null,
        )

    private fun source(
        id: ProviderId,
        vararg capabilities: SourceCapability,
    ): MusicSource =
        object : MusicSource {
            override val id = id
            override val displayName = id.value
            override val capabilities = SourceCapabilities.of(*capabilities)
        }
}
