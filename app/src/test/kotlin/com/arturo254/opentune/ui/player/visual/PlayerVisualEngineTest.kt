package com.arturo254.opentune.ui.player.visual

import com.arturo254.opentune.models.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerVisualEngineTest {
    private fun track(
        id: String = "a",
        title: String = "A song",
        artist: String = "A singer",
    ) = MediaMetadata(
        id = id,
        title = title,
        artists = listOf(MediaMetadata.Artist("singer", artist)),
        thumbnailUrl = "https://example.org/image",
    )

    @Test fun offlineArtworkIsPrioritizedAndBlankLocalUrlsIgnored() {
        assertEquals("content://local/artwork",
            selectPlayerArtwork("content://local/artwork", "https://remote"))
        assertEquals("https://remote", selectPlayerArtwork("", "https://remote"))
        assertEquals("https://remote", selectPlayerArtwork("  ", "https://remote"))
        assertNull(selectPlayerArtwork(null, ""))
        assertNull(selectPlayerArtwork(null, null))
    }

    @Test fun offlineCanvasRetainsMetadataAndDoesNotRequireNetwork() {
        val model = localPlayerCanvas(track(), "content://local/animation")
        assertEquals("content://local/animation", model?.preferredAnimationUrl)
        assertEquals("A song", model?.name)
        assertEquals("A singer", model?.artist)
        assertNull(localPlayerCanvas(track(), " "))
        assertNull(localPlayerCanvas(track(), null))
    }

    @Test fun canvasLookupRequiresTitleAndArtist() {
        assertTrue(canFetchPlayerCanvas(track()))
        assertFalse(canFetchPlayerCanvas(null))
        assertFalse(canFetchPlayerCanvas(track(title = "")))
        assertFalse(canFetchPlayerCanvas(track(artist = " ")))
    }

    @Test fun paletteCacheSeparatesTrackArtworkAndThemeFallback() {
        val first = PlayerPaletteCacheKey("song1", "content://a", 1, 2)
        assertEquals(first, PlayerPaletteCacheKey("song1", "content://a", 1, 2))
        assertTrue(first != PlayerPaletteCacheKey("song2", "content://a", 1, 2))
        assertTrue(first != PlayerPaletteCacheKey("song1", "content://b", 1, 2))
        assertTrue(first != PlayerPaletteCacheKey("song1", "content://a", 2, 2))
        assertTrue(first != PlayerPaletteCacheKey("song1", "content://a", 1, 3))
    }
}
