package com.arturo254.opentune.playback

import android.app.Application
import android.net.Uri
import androidx.media3.datasource.DataSpec
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE, application = Application::class)
class StreamRequestPolicyTest {
    @Test fun directHttpAudioAndHlsSegmentsBypassYoutubeResolution() {
        assertTrue(StreamRequestPolicy.isDirectHttpStream("https://cdn.example.com/audio.m4a"))
        assertTrue(StreamRequestPolicy.isDirectHttpStream("https://cdn.example.com/playlist.m3u8"))
        assertTrue(StreamRequestPolicy.isDirectHttpStream("http://localhost:8080/audio"))
    }

    @Test fun nonHttpMediaIdentifiersRemainOnExistingResolver() {
        assertFalse(StreamRequestPolicy.isDirectHttpStream("Mcj75l2gJcY"))
        assertFalse(StreamRequestPolicy.isDirectHttpStream("content://music/song"))
        assertFalse(StreamRequestPolicy.isDirectHttpStream("file:///sdcard/song.mp3"))
        assertFalse(StreamRequestPolicy.isDirectHttpStream("https:///missing-host"))
    }

    @Test fun resolutionDoesNotTurnUnknownFileLengthIntoAnArtificialEof() {
        val original = DataSpec.Builder().setUri("Mcj75l2gJcY").setKey("Mcj75l2gJcY").build()
        val resolved = StreamRequestPolicy.resolveDataSpec(original, Uri.parse("https://resolver.example/v2/stream/youtube/Mcj75l2gJcY"))
        assertEquals(-1L, resolved.length)
        assertEquals(0L, resolved.position)
        assertEquals(original.key, resolved.key)
    }

    @Test fun resolutionPreservesExplicitRangesOffsetsHeadersAndFlags() {
        val original = DataSpec.Builder().setUri("-9l5t3MWss8").setPosition(1024).setUriPositionOffset(128)
            .setLength(4L * 1024 * 1024).setKey("-9l5t3MWss8").setFlags(DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION)
            .setHttpRequestHeaders(mapOf("Accept" to "audio/mp4")).build()
        val resolved = StreamRequestPolicy.resolveDataSpec(original, Uri.parse("https://resolver.example/v2/stream/youtube/-9l5t3MWss8"))
        assertEquals(original.length, resolved.length)
        assertEquals(original.position, resolved.position)
        assertEquals(original.uriPositionOffset, resolved.uriPositionOffset)
        assertEquals(original.flags, resolved.flags)
        assertEquals(original.httpRequestHeaders, resolved.httpRequestHeaders)
        assertEquals(original.key, resolved.key)
    }
}
