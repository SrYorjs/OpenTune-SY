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
import com.arturo254.opentune.sources.matching.TrackMatchIssue
import com.arturo254.opentune.sources.matching.TrackMatchStatus
import com.arturo254.opentune.sources.matching.TrackMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceResolverTest {
    private val youtube =
        FakeSource(
            id = ProviderId.YOUTUBE_MUSIC,
            SourceCapability.STREAM,
            SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD,
            SourceCapability.AUDIO_QUALITY,
        )
    private val local =
        FakeSource(
            id = ProviderId.LOCAL,
            SourceCapability.STREAM,
            SourceCapability.AUDIO_QUALITY,
        )
    private val premium = ProviderId("premium")
    private val premiumSource =
        FakeSource(
            id = premium,
            SourceCapability.STREAM,
            SourceCapability.AUDIO_QUALITY,
        )

    @Test
    fun openTuneOfflineCopyWinsDefaultResolutionAndDoesNotNeedProviderRegistration() {
        val offlineProvider = ProviderId("offline_only")
        val resolver = resolver(youtube)
        val reference = identity()
        val offline =
            candidate(
                provider = offlineProvider,
                sourceId = "offline",
                availability = PlaybackAvailability.OPEN_TUNE_OFFLINE,
            )
        val online =
            candidate(
                provider = ProviderId.YOUTUBE_MUSIC,
                sourceId = "online",
                availability = PlaybackAvailability.ONLINE,
            )

        val result = resolver.resolve(reference, listOf(online, offline))

        assertEquals("offline", result.selected?.candidate?.variant?.sourceId)
        assertTrue(SourceSelectionReason.OPEN_TUNE_OFFLINE in result.selected!!.selectionReasons)
    }

    @Test
    fun confirmedRecordingBeatsStrongOfflineCandidateBeforeSourcePolicy() {
        val resolver = resolver(youtube)
        val reference = identity(isrc = "USAAA1111111")
        val offlineStrong =
            candidate(
                provider = ProviderId("offline_only"),
                sourceId = "offline",
                availability = PlaybackAvailability.OPEN_TUNE_OFFLINE,
            )
        val onlineConfirmed =
            candidate(
                provider = ProviderId.YOUTUBE_MUSIC,
                sourceId = "confirmed",
                availability = PlaybackAvailability.ONLINE,
                isrc = "USAAA1111111",
            )

        val result = resolver.resolve(reference, listOf(offlineStrong, onlineConfirmed))

        assertEquals("confirmed", result.selected?.candidate?.variant?.sourceId)
        assertEquals(TrackMatchStatus.CONFIRMED, result.selected?.match?.status)
    }

    @Test
    fun bestQualityPrefersExplicitLosslessVariantAfterMatchAndAvailability() {
        val resolver = resolver(youtube, premiumSource)
        val reference = identity()
        val youtubeCandidate =
            candidate(
                provider = ProviderId.YOUTUBE_MUSIC,
                sourceId = "yt",
                quality =
                    AudioQualityInfo(
                        codec = "opus",
                        bitrateBitsPerSecond = 320_000,
                        sampleRateHz = 48_000,
                        lossless = false,
                    ),
            )
        val premiumCandidate =
            candidate(
                provider = premium,
                sourceId = "lossless",
                quality =
                    AudioQualityInfo(
                        codec = "flac",
                        bitrateBitsPerSecond = 1_500_000,
                        sampleRateHz = 96_000,
                        bitDepth = 24,
                        lossless = true,
                    ),
            )

        val result =
            resolver.resolve(
                reference,
                listOf(youtubeCandidate, premiumCandidate),
                SourceResolutionPreferences(
                    preferOffline = false,
                    preferLocal = false,
                    qualityMode = AudioSelectionMode.BEST_QUALITY,
                ),
            )

        assertEquals("lossless", result.selected?.candidate?.variant?.sourceId)
        assertTrue(SourceSelectionReason.BEST_QUALITY in result.selected!!.selectionReasons)
    }

    @Test
    fun automaticModeRespectsPreferredProviderBeforeQuality() {
        val resolver = resolver(youtube, premiumSource)
        val reference = identity()
        val youtubeCandidate =
            candidate(
                provider = ProviderId.YOUTUBE_MUSIC,
                sourceId = "yt",
                quality = AudioQualityInfo(bitrateBitsPerSecond = 160_000, lossless = false),
            )
        val premiumCandidate =
            candidate(
                provider = premium,
                sourceId = "premium",
                quality =
                    AudioQualityInfo(
                        bitrateBitsPerSecond = 1_500_000,
                        bitDepth = 24,
                        sampleRateHz = 96_000,
                        lossless = true,
                    ),
            )

        val result =
            resolver.resolve(
                reference,
                listOf(premiumCandidate, youtubeCandidate),
                SourceResolutionPreferences(
                    preferOffline = false,
                    preferLocal = false,
                    preferredProviders = listOf(ProviderId.YOUTUBE_MUSIC, premium),
                ),
            )

        assertEquals("yt", result.selected?.candidate?.variant?.sourceId)
        assertTrue(SourceSelectionReason.PREFERRED_PROVIDER in result.selected!!.selectionReasons)
    }

    @Test
    fun dataSaverUsesLowestKnownBitrate() {
        val resolver = resolver(youtube)
        val reference = identity()
        val high =
            candidate(
                sourceId = "high",
                quality = AudioQualityInfo(bitrateBitsPerSecond = 320_000),
            )
        val low =
            candidate(
                sourceId = "low",
                quality = AudioQualityInfo(bitrateBitsPerSecond = 96_000),
            )

        val result =
            resolver.resolve(
                reference,
                listOf(high, low),
                SourceResolutionPreferences(
                    preferOffline = false,
                    preferLocal = false,
                    qualityMode = AudioSelectionMode.DATA_SAVER,
                ),
            )

        assertEquals("low", result.selected?.candidate?.variant?.sourceId)
        assertTrue(SourceSelectionReason.DATA_SAVER in result.selected!!.selectionReasons)
    }

    @Test
    fun forcedProviderRestrictsSelectionEvenWhenAnotherProviderHasBetterQuality() {
        val resolver = resolver(youtube, premiumSource)
        val reference = identity()
        val youtubeCandidate = candidate(sourceId = "yt")
        val premiumCandidate =
            candidate(
                provider = premium,
                sourceId = "premium",
                quality = AudioQualityInfo(lossless = true, bitDepth = 24, sampleRateHz = 96_000),
            )

        val result =
            resolver.resolve(
                reference,
                listOf(premiumCandidate, youtubeCandidate),
                SourceResolutionPreferences(
                    forcedProvider = ProviderId.YOUTUBE_MUSIC,
                    qualityMode = AudioSelectionMode.BEST_QUALITY,
                ),
            )

        assertEquals("yt", result.selected?.candidate?.variant?.sourceId)
        assertTrue(SourceSelectionReason.USER_FORCED_PROVIDER in result.selected!!.selectionReasons)
        assertTrue(
            result.rejectedCandidates.any {
                it.candidate.variant.sourceId == "premium" &&
                    it.rejectionReason == SourceRejectionReason.FORCED_PROVIDER_MISMATCH
            },
        )
    }

    @Test
    fun forcedVariantSelectsTheExactProviderVariant() {
        val resolver = resolver(youtube)
        val reference = identity()
        val first = candidate(sourceId = "first")
        val second = candidate(sourceId = "second")

        val result =
            resolver.resolve(
                reference,
                listOf(first, second),
                SourceResolutionPreferences(
                    forcedVariant =
                        SourceVariantKey(
                            provider = ProviderId.YOUTUBE_MUSIC,
                            sourceId = "second",
                        ),
                ),
            )

        assertEquals("second", result.selected?.candidate?.variant?.sourceId)
        assertTrue(SourceSelectionReason.USER_FORCED_VARIANT in result.selected!!.selectionReasons)
    }

    @Test
    fun wrongRecordingVersionIsRejectedBeforeQualityRanking() {
        val resolver = resolver(youtube, premiumSource)
        val reference = identity()
        val live =
            candidate(
                provider = premium,
                sourceId = "live-lossless",
                title = "Song - Live",
                quality = AudioQualityInfo(lossless = true, bitrateBitsPerSecond = 2_000_000),
            )
        val original =
            candidate(
                sourceId = "original",
                quality = AudioQualityInfo(bitrateBitsPerSecond = 128_000, lossless = false),
            )

        val result =
            resolver.resolve(
                reference,
                listOf(live, original),
                SourceResolutionPreferences(qualityMode = AudioSelectionMode.BEST_QUALITY),
            )

        assertEquals("original", result.selected?.candidate?.variant?.sourceId)
        val rejectedLive = result.rejectedCandidates.single { it.candidate.variant.sourceId == "live-lossless" }
        assertEquals(SourceRejectionReason.MATCH_NOT_ACCEPTED, rejectedLive.rejectionReason)
        assertTrue(TrackMatchIssue.RECORDING_VERSION_CONFLICT in rejectedLive.match.issues)
    }

    @Test
    fun onlineVariantRequiresRegisteredStreamingSource() {
        val unknown = ProviderId("unknown")
        val resolver = resolver(youtube)
        val result =
            resolver.resolve(
                identity(),
                listOf(candidate(provider = unknown, sourceId = "unknown")),
            )

        assertNull(result.selected)
        assertEquals(
            SourceRejectionReason.SOURCE_NOT_REGISTERED,
            result.rejectedCandidates.single().rejectionReason,
        )
    }

    @Test
    fun metadataOnlySourceCannotBeUsedAsOnlinePlayback() {
        val metadataProvider = ProviderId("metadata")
        val metadataSource =
            FakeSource(
                id = metadataProvider,
                SourceCapability.TRACK_METADATA,
            )
        val resolver = resolver(youtube, metadataSource)

        val result =
            resolver.resolve(
                identity(),
                listOf(candidate(provider = metadataProvider, sourceId = "metadata")),
            )

        assertNull(result.selected)
        assertEquals(
            SourceRejectionReason.STREAM_NOT_SUPPORTED,
            result.rejectedCandidates.single().rejectionReason,
        )
    }

    @Test
    fun providerOfflineRequiresExplicitProviderOfflineCapability() {
        val provider = ProviderId("provider_offline")
        val withoutOffline = FakeSource(provider, SourceCapability.STREAM)
        val withOffline =
            FakeSource(
                provider,
                SourceCapability.STREAM,
                SourceCapability.PROVIDER_MANAGED_OFFLINE,
            )

        val rejected =
            resolver(withoutOffline)
                .resolve(
                    identity(),
                    listOf(
                        candidate(
                            provider = provider,
                            availability = PlaybackAvailability.PROVIDER_OFFLINE,
                        ),
                    ),
                )

        assertNull(rejected.selected)
        assertEquals(
            SourceRejectionReason.PROVIDER_OFFLINE_NOT_SUPPORTED,
            rejected.rejectedCandidates.single().rejectionReason,
        )

        val accepted =
            resolver(withOffline)
                .resolve(
                    identity(),
                    listOf(
                        candidate(
                            provider = provider,
                            availability = PlaybackAvailability.PROVIDER_OFFLINE,
                        ),
                    ),
                )

        assertEquals(provider, accepted.selected?.candidate?.variant?.provider)
    }

    @Test
    fun audioFirstPrefersOfficialAudioOverMusicVideoForSameRecording() {
        val resolver = resolver(youtube)
        val reference = identity()
        val video =
            candidate(
                sourceId = "video",
                version = TrackVersion(setOf(TrackVersionTrait.MUSIC_VIDEO)),
            )
        val audio =
            candidate(
                sourceId = "audio",
                version = TrackVersion(setOf(TrackVersionTrait.OFFICIAL_AUDIO)),
            )

        val result =
            resolver.resolve(
                reference,
                listOf(video, audio),
                SourceResolutionPreferences(
                    preferOffline = false,
                    preferLocal = false,
                    presentationPreference = PresentationPreference.AUDIO_FIRST,
                ),
            )

        assertEquals("audio", result.selected?.candidate?.variant?.sourceId)
        assertTrue(SourceSelectionReason.AUDIO_PRESENTATION in result.selected!!.selectionReasons)
    }

    @Test
    fun localFileCanBeSelectedWithoutNetworkAndKeepsLocalProviderIdentity() {
        val resolver = resolver(youtube, local)
        val localCandidate =
            candidate(
                provider = ProviderId.LOCAL,
                sourceId = "content://media/1",
                availability = PlaybackAvailability.LOCAL_FILE,
            )
        val online = candidate(sourceId = "yt")

        val result = resolver.resolve(identity(), listOf(online, localCandidate))

        assertEquals(ProviderId.LOCAL, result.selected?.candidate?.variant?.provider)
        assertTrue(SourceSelectionReason.LOCAL_FILE in result.selected!!.selectionReasons)
        assertTrue(SourceSelectionReason.LOCAL_PREFERRED in result.selected!!.selectionReasons)
    }

    private fun resolver(vararg sources: MusicSource): SourceResolver =
        SourceResolver(
            registry = SourceRegistry(sources.toSet()),
            trackMatcher = TrackMatcher(),
        )

    private fun identity(
        title: String = "Song",
        artist: String = "Artist",
        durationMs: Long = 180_000,
        isrc: String? = null,
    ): TrackIdentity =
        TrackIdentity(
            title = title,
            artists = listOf(ArtistIdentity(name = artist)),
            durationMs = durationMs,
            isrc = isrc,
        )

    private fun candidate(
        provider: ProviderId = ProviderId.YOUTUBE_MUSIC,
        sourceId: String = "source-id",
        title: String = "Song",
        artist: String = "Artist",
        durationMs: Long = 180_000,
        isrc: String? = null,
        version: TrackVersion = TrackVersion(),
        quality: AudioQualityInfo? = null,
        availability: PlaybackAvailability = PlaybackAvailability.ONLINE,
    ): SourcePlaybackCandidate =
        SourcePlaybackCandidate(
            variant =
                SourceVariant(
                    provider = provider,
                    sourceId = sourceId,
                    title = title,
                    artists = listOf(artist),
                    durationMs = durationMs,
                    isrc = isrc,
                    version = version,
                    quality = quality,
                ),
            availability = availability,
        )

    private class FakeSource(
        override val id: ProviderId,
        vararg capabilities: SourceCapability,
    ) : MusicSource {
        override val displayName: String = id.value
        override val capabilities: SourceCapabilities = SourceCapabilities.of(*capabilities)
    }
}
