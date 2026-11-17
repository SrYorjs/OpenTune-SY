package com.arturo254.opentune.playback

import android.app.Application
import android.net.Uri
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE, application = Application::class)
class PlaybackStartupTest {
    @Test fun clockUsesMonotonicTimeAndBoundsRetention() {
        var now = 7_000_000L
        val clock = PlaybackStartupClock { now }
        clock.begin("first")
        now += 2_500_000L
        assertEquals(2L, clock.elapsed("first"))
        clock.transition("first")
        assertEquals(2L, clock.elapsed("first"))
        clock.transition("first")
        assertEquals(0L, clock.elapsed("first"))
        repeat(64) { clock.begin("track-$it") }
        assertNull(clock.elapsed("first"))
    }

    @Test fun identitiesNeverExposePathsQueriesOrCredentials() {
        assertEquals("youtube", safePlaybackIdentity("Mcj75l2gJcY").second)
        assertEquals("audius", safePlaybackIdentity("audius:unit123").second)
        assertEquals("peertube", safePlaybackIdentity("peertube:12345678-1234-1234-1234-123456789abc").second)
        for (id in listOf("https://private.test/song?token=secret", "audius:secret?key=private", "file:/private/secret")) {
            assertEquals("redacted", safePlaybackIdentity(id).first)
        }
    }

    @Test fun preloadAcceptsCanonicalYoutubeAndDirectProvidersButNotLocalFiles() {
        assertTrue(canPreloadAudio("Mcj75l2gJcY", null))
        assertTrue(canPreloadAudio("audius:track", "https"))
        assertTrue(canPreloadAudio("peertube:track", "https"))
        assertFalse(canPreloadAudio("local:track", "file"))
        assertFalse(canPreloadAudio("unsupported-provider", null))
    }

    @Test fun preloadNeverReadsBeyond256KiB() = runBlocking {
        val bytes = ByteArrayDataSource(ByteArray(1024 * 1024))
        var read = 0
        var opened: DataSpec? = null
        val source = object : DataSource by bytes {
            override fun open(spec: DataSpec): Long { opened = spec; return bytes.open(spec) }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = bytes.read(buffer, offset, length).also { if (it > 0) read += it }
        }
        try {
            preloadAudioPrefix(source, DataSpec.Builder().setUri("https://unit.test/audio").setKey("Mcj75l2gJcY").build())
            assertEquals(262144, read)
            assertEquals(262144L, opened!!.length)
            assertEquals("Mcj75l2gJcY", opened!!.key)
        } finally { source.close() }
    }

    @Test fun rapidTransitionInterruptsPreloadAndNextTrackCanOpen() = runBlocking {
        val entered = CountDownLatch(1)
        val interrupted = CountDownLatch(1)
        val delegate = ByteArrayDataSource(ByteArray(32))
        val source = object : DataSource by delegate {
            override fun open(spec: DataSpec): Long {
                entered.countDown()
                try { Thread.sleep(20_000) } catch (error: InterruptedException) { interrupted.countDown(); throw error }
                return 0
            }
        }
        val job = launch(Dispatchers.IO) { preloadAudioPrefix(source, DataSpec(Uri.EMPTY)) }
        assertTrue(entered.await(3, TimeUnit.SECONDS))
        job.cancelAndJoin()
        assertTrue(interrupted.await(1, TimeUnit.SECONDS))
        val next = ByteArrayDataSource(ByteArray(32))
        try { preloadAudioPrefix(next, DataSpec(Uri.EMPTY)) } finally { next.close() }
    }
}
