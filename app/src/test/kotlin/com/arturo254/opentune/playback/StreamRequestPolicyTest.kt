package com.arturo254.opentune.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamRequestPolicyTest {
    @Test
    fun directHttpAudioAndHlsSegmentsBypassYoutubeResolution() {
        assertTrue(StreamRequestPolicy.isDirectHttpStream("https://cdn.example.com/audio.m4a"))
        assertTrue(StreamRequestPolicy.isDirectHttpStream("https://cdn.example.com/playlist.m3u8"))
        assertTrue(StreamRequestPolicy.isDirectHttpStream("https://cdn.example.com/segment.ts"))
        assertTrue(StreamRequestPolicy.isDirectHttpStream("http://localhost:8080/audio"))
    }

    @Test
    fun nonHttpMediaIdentifiersRemainOnExistingResolver() {
        assertFalse(StreamRequestPolicy.isDirectHttpStream("dQw4w9WgXcQ"))
        assertFalse(StreamRequestPolicy.isDirectHttpStream("content://music/song"))
        assertFalse(StreamRequestPolicy.isDirectHttpStream("file:///sdcard/song.mp3"))
        assertFalse(StreamRequestPolicy.isDirectHttpStream("https:///missing-host"))
    }

    @Test
    fun unknownLength_usesOneChunk() {
        assertEquals(
            StreamRequestPolicy.CHUNK_LENGTH,
            StreamRequestPolicy.boundedLength(-1L),
        )
    }

    @Test
    fun smallRequest_keepsRequestedLength() {
        assertEquals(
            128L * 1024L,
            StreamRequestPolicy.boundedLength(128L * 1024L),
        )
    }

    @Test
    fun largeRequest_isCappedToOneChunk() {
        assertEquals(
            StreamRequestPolicy.CHUNK_LENGTH,
            StreamRequestPolicy.boundedLength(4L * 1024L * 1024L),
        )
    }
}
