package com.arturo254.opentune.ui.original

import com.arturo254.opentune.innertube.models.response.PlayerResponse
import com.arturo254.opentune.ui.original.adapters.*
import com.arturo254.opentune.ui.original.player.video.VideoDash
import com.arturo254.opentune.ui.original.utils.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class OriginalVideoResolutionTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private fun format(itag: Int, codec: String, height: Int, extra: String = "") =
        json.decodeFromString<PlayerResponse.StreamingData.Format>("""{
            "itag":$itag,"mimeType":"video/mp4; codecs=\"$codec\"","bitrate":1000000,
            "width":1280,"height":$height,"quality":"hd720","fps":30,
            "url":"https://example.invalid/video?x=1&y=2","contentLength":"9000000",
            "initRange":{"start":"0","end":"739"},"indexRange":{"start":"740","end":"999"}$extra
        }""")

    @Test fun serializedRangesReachDashInsteadOfBeingDiscarded() {
        val f = format(136, "avc1.4d401f", 720)
        val track = VideoTrack(f.itag, f.url!!, f.containerType(), f.codecs(), f.width!!, f.height!!,
            f.fps!!, f.bitrate, f.initRange!!.videoRange(), f.indexRange!!.videoRange(), f.contentLength)
        assertTrue(track.chunked)
        val mpd = VideoDash.manifest(listOf(track), 200000L)!!
        assertTrue(mpd.contains("indexRange=\"740-999\""))
        assertTrue(mpd.contains("Initialization range=\"0-739\""))
        assertTrue(mpd.contains("x=1&amp;y=2"))
        assertNull(VideoDash.manifest(listOf(track.copy(initRange = null)), 200000L))
        assertNull(PlayerResponse.StreamingData.Format.ByteRange("10", "2").videoRange())
        assertNull(PlayerResponse.StreamingData.Format.ByteRange("-1", "2").videoRange())
    }

    @Test fun h264FamilyExcludesMuxedAudioAndOtherCodecs() {
        val f = format(136, "avc1.4d401f", 720)
        val muxed = format(22, "avc1.4d401f, mp4a.40.2", 720, """, "audioChannels":2""")
        val vp9 = format(247, "vp09.00.31.08", 720)
        val av1 = format(398, "av01.0.05M.08", 720)
        val small = format(135, "avc1.4d401e", 480)
        assertEquals(listOf(135,136), pickVideoFamily(listOf(vp9,muxed,av1,f,small)).map { it.itag })
        assertEquals(listOf(247), pickVideoFamily(listOf(muxed,av1,vp9)).map { it.itag })
    }

    @Test fun triesFollowingClientsAfterExceptionOrUnplayableResponse() = runBlocking {
        val tried = mutableListOf<String>()
        val result = firstPlayableVideoClient(videoClients) { client ->
            tried += client.clientName
            when (tried.size) { 1 -> throw java.io.IOException("test failure"); 2 -> null; else -> 42 }
        }
        assertEquals(42, result)
        assertEquals(videoClients.take(3).map { it.clientName }, tried)
        assertEquals(listOf("VISIONOS", "WEB_REMIX", "TVHTML5_SIMPLY_EMBEDDED_PLAYER", "ANDROID_VR", "WEB"), videoClients.map { it.clientName })
    }

    @Test fun cancellationStopsFallbackAndExhaustionIsAnExplicitFailure() = runBlocking {
        var calls = 0
        try {
            firstPlayableVideoClient<Int>(videoClients) { calls++; throw CancellationException() }
            fail("cancellation was swallowed")
        } catch (_: CancellationException) { assertEquals(1,calls) }
        calls = 0
        try {
            firstPlayableVideoClient<Int>(videoClients) { calls++; null }
            fail("missing streams were reported as success")
        } catch (_: IllegalStateException) { assertEquals(videoClients.size,calls) }
    }

    @Test fun tailProbeRejectsEarlyPartialDataAndHttpErrors() {
        assertTrue(videoTailServed(206,"bytes 8999999-8999999/9000000",8999999,9000000))
        assertFalse(videoTailServed(206,"bytes 0-0/9000000",8999999,9000000))
        assertFalse(videoTailServed(206,"bytes 8999999-8999999/10000000",8999999,9000000))
        assertFalse(videoTailServed(403,null,8999999,9000000))
        assertFalse(videoTailServed(200,null,8999999,9000000))
        assertFalse(videoTailServed(416,null,8999999,9000000))
        assertTrue(videoTailServed(416,null,1048576,null))
    }
}
