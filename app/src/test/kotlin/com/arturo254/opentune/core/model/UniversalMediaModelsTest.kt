package com.arturo254.opentune.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalMediaModelsTest {
    @Test
    fun providerIds_keepProvidersIndependent() {
        assertNotEquals(ProviderId.YOUTUBE_MUSIC, ProviderId.SPOTIFY)
        assertNotEquals(ProviderId.YOUTUBE_MUSIC, ProviderId.DEEZER)
        assertEquals("youtube_music", ProviderId.YOUTUBE_MUSIC.value)
    }

    @Test
    fun providerId_acceptsFutureProvidersWithoutChangingTheModel() {
        val future = ProviderId("future_provider")

        assertEquals("future_provider", future.value)
    }

    @Test
    fun trackVersion_preservesMultipleVersionTraits() {
        val version =
            TrackVersion(
                traits =
                    setOf(
                        TrackVersionTrait.LIVE,
                        TrackVersionTrait.ACOUSTIC,
                    ),
                label = "Live acoustic session",
            )

        assertTrue(TrackVersionTrait.LIVE in version.traits)
        assertTrue(TrackVersionTrait.ACOUSTIC in version.traits)
        assertEquals("Live acoustic session", version.label)
    }

    @Test
    fun sourceVariants_keepProviderSpecificIdentityAndQuality() {
        val quality =
            AudioQualityInfo(
                mimeType = "audio/mp4",
                codec = "mp4a.40.2",
                bitrateBitsPerSecond = 256_000,
                sampleRateHz = 44_100,
                channelCount = 2,
                lossless = false,
            )
        val variant =
            SourceVariant(
                provider = ProviderId.YOUTUBE_MUSIC,
                sourceId = "video-id",
                title = "Song",
                artists = listOf("Artist"),
                durationMs = 180_000,
                quality = quality,
            )

        assertEquals(ProviderId.YOUTUBE_MUSIC, variant.provider)
        assertEquals("video-id", variant.sourceId)
        assertEquals(256_000, variant.quality?.bitrateBitsPerSecond)
        assertEquals(44_100, variant.quality?.sampleRateHz)
    }

    @Test
    fun conceptualTrackDoesNotRequireProviderId() {
        val track =
            TrackIdentity(
                title = "Song",
                artists = listOf(ArtistIdentity(name = "Artist")),
                durationMs = 180_000,
                isrc = "TEST12345678",
            )

        assertEquals("Song", track.title)
        assertEquals("Artist", track.artists.single().name)
        assertEquals("TEST12345678", track.isrc)
    }
}
