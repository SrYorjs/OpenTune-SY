package com.arturo254.opentune.playback

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineMediaAssetStoreTest {
    @Test
    fun manifestRoundTripsMetadataAndProviderContext() =
        runBlocking {
            val root = Files.createTempDirectory("offline-assets").toFile()
            val store = OfflineMediaAssetStore(root)
            val manifest =
                OfflineMediaAssetManifest(
                    mediaId = "yt-asset-1",
                    title = "Song",
                    artists = listOf("Artist A", "Artist B"),
                    albumId = "album-1",
                    albumName = "Album",
                    durationSeconds = 205,
                    explicit = true,
                    remoteArtworkUrl = "https://example.test/cover.jpg",
                    localArtworkUri = "file:///tmp/cover.jpg",
                    localLyricsUri = "file:///tmp/lyrics.lrc",
                    localAnimationUri = "file:///tmp/canvas.mp4",
                    backingProvider = "youtube_music",
                    discoveryProviders = listOf("spotify", "tidal", "apple_music"),
                )

            val uri = store.saveManifest(manifest)
            val restored = store.readManifest("yt-asset-1")

            assertTrue(uri.startsWith("file:"))
            assertNotNull(restored)
            assertEquals("Song", restored?.title)
            assertEquals(listOf("Artist A", "Artist B"), restored?.artists)
            assertEquals("Album", restored?.albumName)
            assertEquals(205, restored?.durationSeconds)
            assertEquals(true, restored?.explicit)
            assertEquals("youtube_music", restored?.backingProvider)
            assertEquals(
                listOf("spotify", "tidal", "apple_music"),
                restored?.discoveryProviders,
            )
            assertEquals("file:///tmp/cover.jpg", restored?.localArtworkUri)
            assertEquals("file:///tmp/lyrics.lrc", restored?.localLyricsUri)
            assertEquals("file:///tmp/canvas.mp4", restored?.localAnimationUri)
        }

    @Test
    fun lyricsSidecarCanBeReadOffline() =
        runBlocking {
            val root = Files.createTempDirectory("offline-assets-lyrics").toFile()
            val store = OfflineMediaAssetStore(root)

            store.saveLyrics(
                mediaId = "song-lyrics",
                lyrics = "[00:01.00]Hello",
            )

            assertEquals(
                "[00:01.00]Hello",
                store.readLyrics("song-lyrics"),
            )
            assertTrue(store.getLyricsFile("song-lyrics").isFile)
        }

    @Test
    fun removingBundleDeletesManifestLyricsAndCachedFiles() =
        runBlocking {
            val root = Files.createTempDirectory("offline-assets-remove").toFile()
            val store = OfflineMediaAssetStore(root)

            store.saveManifest(
                OfflineMediaAssetManifest(
                    mediaId = "song-remove",
                    title = "Song",
                    artists = listOf("Artist"),
                ),
            )
            store.saveLyrics(
                mediaId = "song-remove",
                lyrics = "[00:00.00]Test",
            )

            assertNotNull(store.readManifest("song-remove"))
            assertNotNull(store.readLyrics("song-remove"))

            store.removeBundle("song-remove")

            assertNull(store.readManifest("song-remove"))
            assertNull(store.readLyrics("song-remove"))
            assertFalse(store.getLyricsFile("song-remove").isFile)
        }
}
