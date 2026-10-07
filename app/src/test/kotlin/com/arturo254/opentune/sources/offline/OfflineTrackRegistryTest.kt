package com.arturo254.opentune.sources.offline

import com.arturo254.opentune.core.model.ProviderId
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineTrackRegistryTest {
    @Test
    fun registryPersistsProviderNeutralAssetAndState() =
        runBlocking {
            val file = Files.createTempDirectory("offline-registry").resolve("offline.json").toFile()
            val registry = OfflineTrackRegistry(file)

            registry.registerPending(
                OfflineTrackAsset(
                    mediaId = "yt-123",
                    discoverySources =
                        listOf(
                            OfflineSourceRef(
                                provider = ProviderId.TIDAL,
                                sourceId = "tidal-55",
                            ),
                        ),
                    backingProvider = ProviderId.YOUTUBE_MUSIC,
                    backingSourceId = "yt-123",
                    title = "Song",
                    artists = listOf("Artist"),
                    albumName = "Album",
                    durationMs = 180_000L,
                    isrc = "USAAA2600100",
                ),
            )
            registry.markAvailable("yt-123")

            val reloaded = OfflineTrackRegistry(file)
            val asset = reloaded.findByMediaId("yt-123")

            assertEquals(OfflineTrackState.AVAILABLE, asset?.state)
            assertEquals(ProviderId.YOUTUBE_MUSIC, asset?.backingProvider)
            assertEquals("USAAA2600100", asset?.isrc)
            assertEquals(
                OfflineSourceRef(ProviderId.TIDAL, "tidal-55"),
                asset?.discoverySources?.single(),
            )
        }

    @Test
    fun registeringSameBackingTrackMergesDiscoveryProviders() =
        runBlocking {
            val file = Files.createTempDirectory("offline-registry-merge").resolve("offline.json").toFile()
            val registry = OfflineTrackRegistry(file)

            registry.registerPending(asset(ProviderId.TIDAL, "tidal-1"))
            registry.registerPending(asset(ProviderId.SPOTIFY, "spotify-1"))
            registry.markAvailable("yt-1")

            val asset = registry.findByMediaId("yt-1")!!
            assertEquals(2, asset.discoverySources.size)
            assertTrue(
                asset.discoverySources.contains(
                    OfflineSourceRef(ProviderId.TIDAL, "tidal-1"),
                ),
            )
            assertTrue(
                asset.discoverySources.contains(
                    OfflineSourceRef(ProviderId.SPOTIFY, "spotify-1"),
                ),
            )
        }

    @Test
    fun registryPersistsOfflineAssetSidecarUris() =
        runBlocking {
            val file = Files.createTempDirectory("offline-registry-assets").resolve("offline.json").toFile()
            val registry = OfflineTrackRegistry(file)

            registry.registerPending(
                asset(ProviderId.SPOTIFY, "spotify-assets").copy(
                    animatedArtworkUrl = "https://example.test/canvas.mp4",
                ),
            )
            registry.updateOfflineAssets(
                mediaId = "yt-1",
                artworkUri = "file:///offline/cover.jpg",
                lyricsUri = "file:///offline/lyrics.lrc",
                animationUri = "file:///offline/canvas.mp4",
                manifestUri = "file:///offline/metadata.json",
            )
            registry.markAvailable("yt-1")

            val reloaded = OfflineTrackRegistry(file)
            val restored = reloaded.findByMediaId("yt-1")

            assertEquals("file:///offline/cover.jpg", restored?.offlineArtworkUri)
            assertEquals("file:///offline/lyrics.lrc", restored?.offlineLyricsUri)
            assertEquals("https://example.test/canvas.mp4", restored?.animatedArtworkUrl)
            assertEquals("file:///offline/canvas.mp4", restored?.offlineAnimationUri)
            assertEquals("file:///offline/metadata.json", restored?.assetManifestUri)
            assertEquals(
                "file:///offline/cover.jpg",
                restored?.toSourceVariant()?.artworkUrl,
            )
            assertEquals(
                "file:///offline/canvas.mp4",
                restored?.toSourceVariant()?.animatedArtworkUrl,
            )
        }

    @Test
    fun removingDownloadRemovesOfflineAsset() =
        runBlocking {
            val file = Files.createTempDirectory("offline-registry-remove").resolve("offline.json").toFile()
            val registry = OfflineTrackRegistry(file)

            registry.registerPending(asset(ProviderId.DEEZER, "deezer-1"))
            registry.markAvailable("yt-1")
            registry.remove("yt-1")

            assertNull(registry.findByMediaId("yt-1"))
            assertTrue(registry.availableSnapshot().isEmpty())
        }

    private fun asset(
        provider: ProviderId,
        sourceId: String,
    ): OfflineTrackAsset =
        OfflineTrackAsset(
            mediaId = "yt-1",
            discoverySources =
                listOf(
                    OfflineSourceRef(
                        provider = provider,
                        sourceId = sourceId,
                    ),
                ),
            backingProvider = ProviderId.YOUTUBE_MUSIC,
            backingSourceId = "yt-1",
            title = "Song",
            artists = listOf("Artist"),
            durationMs = 180_000L,
        )
}
