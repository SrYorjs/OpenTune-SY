package com.arturo254.opentune.sources.playback

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import org.junit.Assert.assertEquals
import org.junit.Test

class SourcePlaybackRouteTest {
    @Test
    fun youtubeKeepsNativeQueuePath() {
        assertEquals(
            SourcePlaybackRoute.YOUTUBE_QUEUE,
            variant(ProviderId.YOUTUBE_MUSIC).playbackRoute(),
        )
    }

    @Test
    fun localAndOfflineStayOnLocalMediaPath() {
        assertEquals(
            SourcePlaybackRoute.LOCAL_OR_OFFLINE,
            variant(ProviderId.LOCAL).playbackRoute(),
        )
        assertEquals(
            SourcePlaybackRoute.LOCAL_OR_OFFLINE,
            variant(ProviderId.OPEN_TUNE_OFFLINE).playbackRoute(),
        )
    }

    @Test
    fun soundCloudUsesItsDirectResolver() {
        assertEquals(
            SourcePlaybackRoute.SOUNDCLOUD_DIRECT,
            variant(ProviderId.SOUNDCLOUD).playbackRoute(),
        )
    }

    @Test
    fun metadataOnlyCatalogProvidersNeverPretendToBeDirectStreams() {
        listOf(
            ProviderId.DEEZER,
            ProviderId.SPOTIFY,
            ProviderId.TIDAL,
            ProviderId.APPLE_MUSIC,
        ).forEach { provider ->
            assertEquals(
                provider.value,
                SourcePlaybackRoute.CATALOG_BRIDGE,
                variant(provider).playbackRoute(),
            )
        }
    }

    @Test
    fun unknownProviderFailsClosed() {
        assertEquals(
            SourcePlaybackRoute.UNSUPPORTED,
            variant(ProviderId("future_provider")).playbackRoute(),
        )
    }

    private fun variant(provider: ProviderId): SourceVariant =
        SourceVariant(
            provider = provider,
            sourceId = provider.value + "-id",
            title = "Song",
            artists = listOf("Artist"),
        )
}
