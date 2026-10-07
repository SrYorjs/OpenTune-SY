package com.arturo254.opentune.sources.offline

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.TrackSearchRequest
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineMusicSourceTest {
    @Test
    fun sourceExposesOnlyAvailableOfflineTracks() =
        runBlocking {
            val registry = registry()
            registry.registerPending(
                asset(
                    mediaId = "pending",
                    title = "Pending Song",
                    state = OfflineTrackState.PENDING,
                ),
            )
            registry.registerPending(
                asset(
                    mediaId = "available",
                    title = "Available Song",
                    state = OfflineTrackState.PENDING,
                ),
            )
            registry.markAvailable("available")

            val source = OfflineMusicSource(registry)
            val result =
                source
                    .searchTracks(
                        TrackSearchRequest(
                            query = "available",
                            limit = 20,
                        ),
                    ).getOrThrow()

            assertEquals(listOf("available"), result.items.map { it.sourceId })
            assertEquals(
                ProviderId.OPEN_TUNE_OFFLINE,
                result.items.single().provider,
            )
        }

    @Test
    fun offlineSearchMatchesArtistAndAlbumAndPaginates() =
        runBlocking {
            val registry = registry()
            repeat(3) { index ->
                val mediaId = "song-$index"
                registry.registerPending(
                    asset(
                        mediaId = mediaId,
                        title = "Track $index",
                        artist = "Offline Artist",
                        album = "Offline Album",
                    ),
                )
                registry.markAvailable(mediaId)
            }

            val source = OfflineMusicSource(registry)
            val first =
                source
                    .searchTracks(
                        TrackSearchRequest(
                            query = "offline artist",
                            limit = 2,
                        ),
                    ).getOrThrow()

            assertEquals(2, first.items.size)
            assertEquals("offset:2", first.continuationToken)

            val second =
                source
                    .searchTracks(
                        TrackSearchRequest(
                            query = "offline artist",
                            limit = 2,
                            continuationToken = first.continuationToken,
                        ),
                    ).getOrThrow()

            assertEquals(1, second.items.size)
            assertNull(second.continuationToken)
        }

    @Test
    fun offlineSourceDeclaresManagedDownloadWithoutOnlineStream() {
        val source = OfflineMusicSource(registry())

        assertTrue(source.supports(SourceCapability.TRACK_SEARCH))
        assertTrue(source.supports(SourceCapability.TRACK_METADATA))
        assertTrue(source.supports(SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD))
        assertFalse(source.supports(SourceCapability.STREAM))
        assertFalse(source.supports(SourceCapability.PROVIDER_MANAGED_OFFLINE))
    }

    private fun registry(): OfflineTrackRegistry =
        OfflineTrackRegistry(
            Files
                .createTempDirectory("offline-source")
                .resolve("offline.json")
                .toFile(),
        )

    private fun asset(
        mediaId: String,
        title: String,
        artist: String = "Artist",
        album: String? = null,
        state: OfflineTrackState = OfflineTrackState.PENDING,
    ): OfflineTrackAsset =
        OfflineTrackAsset(
            mediaId = mediaId,
            discoverySources =
                listOf(
                    OfflineSourceRef(
                        provider = ProviderId.TIDAL,
                        sourceId = "tidal-$mediaId",
                    ),
                ),
            backingProvider = ProviderId.YOUTUBE_MUSIC,
            backingSourceId = mediaId,
            title = title,
            artists = listOf(artist),
            albumName = album,
            durationMs = 180_000L,
            state = state,
        )
}
