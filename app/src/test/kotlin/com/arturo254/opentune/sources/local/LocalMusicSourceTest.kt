package com.arturo254.opentune.sources.local

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.db.entities.AlbumEntity
import com.arturo254.opentune.db.entities.ArtistEntity
import com.arturo254.opentune.db.entities.FormatEntity
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.db.entities.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalMusicSourceTest {
    @Test
    fun localSongMapsToLocalVariantAndPreservesKnownFormat() {
        val song =
            Song(
                song =
                    SongEntity(
                        id = "content://media/external/audio/1",
                        title = "Local Song",
                        duration = 245,
                        thumbnailUrl = "file:///cover.jpg",
                        albumId = "album-1",
                        albumName = "Album",
                        explicit = false,
                        isLocal = true,
                    ),
                artists =
                    listOf(
                        ArtistEntity(
                            id = "artist-1",
                            name = "Local Artist",
                            isLocal = true,
                        ),
                    ),
                album =
                    AlbumEntity(
                        id = "album-1",
                        title = "Album",
                        isLocal = true,
                    ),
                format =
                    FormatEntity(
                        id = "content://media/external/audio/1",
                        itag = 0,
                        mimeType = "audio/flac",
                        codecs = "flac",
                        bitrate = 1_000_000,
                        sampleRate = 48_000,
                        contentLength = 12_345_678,
                        loudnessDb = null,
                        playbackUrl = null,
                    ),
            )

        val variant = song.toLocalSourceVariant()

        assertEquals(ProviderId.LOCAL, variant.provider)
        assertEquals("content://media/external/audio/1", variant.sourceId)
        assertEquals("Local Song", variant.title)
        assertEquals(listOf("Local Artist"), variant.artists)
        assertEquals("Album", variant.albumName)
        assertEquals(245_000L, variant.durationMs)
        assertEquals("audio/flac", variant.quality?.mimeType)
        assertEquals("flac", variant.quality?.codec)
        assertEquals(1_000_000, variant.quality?.bitrateBitsPerSecond)
        assertEquals(48_000, variant.quality?.sampleRateHz)
        assertNull(variant.quality?.lossless)
    }

    @Test
    fun localSongDoesNotInventAudioQualityWhenFormatIsUnknown() {
        val song =
            Song(
                song =
                    SongEntity(
                        id = "local-2",
                        title = "Unknown Format",
                        duration = -1,
                        isLocal = true,
                    ),
                artists = emptyList(),
                album = null,
                format = null,
            )

        val variant = song.toLocalSourceVariant()

        assertNull(variant.durationMs)
        assertNull(variant.quality)
    }
}
