package com.arturo254.opentune.playback

import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadataMutations
import androidx.media3.datasource.cache.DefaultContentMetadata
import com.arturo254.opentune.innertube.models.response.PlayerResponse
import com.arturo254.opentune.utils.YTPlayerUtils
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

class PlaybackFormatRegressionTest {
    private fun playback(mime: String, length: Long?): YTPlayerUtils.PlaybackData {
        val format = PlayerResponse.StreamingData.Format(140, null, mime, 128000,
            null, null, length, "high", null, null, null, null, null, 44100,
            null, null, null, null, null)
        return YTPlayerUtils.PlaybackData(null, null, null, format, "https://resolver.example/audio/id", 240)
    }

    @Test fun missingV2CodecOrLengthDoesNotFailPlaybackOrInventRoomFields() {
        assertNull(persistablePlaybackFormat("abcdefghijk", playback("audio/mp4", 10000)))
        assertNull(persistablePlaybackFormat("abcdefghijk", playback("audio/mp4; codecs=\"mp4a.40.2\"", null)))
        assertNull(persistablePlaybackFormat("abcdefghijk", playback("audio/mp4; codecs=\"mp4a.40.2\"", -1)))
        val entity = persistablePlaybackFormat("abcdefghijk", playback("audio/mp4; codecs=\"mp4a.40.2\"", 10000))!!
        assertEquals("mp4a.40.2", entity.codecs)
        assertEquals("audio/mp4", entity.mimeType)
        assertEquals(10000L, entity.contentLength)
    }

    @Test fun variantLookupSurvivesResolverRecreationAndKeepsLegacyDownloadsSeparate() {
        val metadata = mutableMapOf<String, DefaultContentMetadata>()
        fun reader(): Cache = Proxy.newProxyInstance(Cache::class.java.classLoader, arrayOf(Cache::class.java)) { _, method, args ->
            when (method.name) {
                "getKeys" -> metadata.keys
                "getCachedSpans" -> java.util.TreeSet<androidx.media3.datasource.cache.CacheSpan>()
                "getContentMetadata" -> metadata[args!![0] as String] ?: DefaultContentMetadata.EMPTY
                "applyContentMetadataMutations" -> {
                    val id = args!![0] as String
                    metadata[id] = (metadata[id] ?: DefaultContentMetadata.EMPTY)
                        .copyWithMutationsApplied(args[1] as ContentMetadataMutations)
                    null
                }
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Cache
        val id = "abcdefghijk"
        assertEquals(id, audioPlayerCacheKey(reader(), id))
        rememberAudioPlayerCacheKey(reader(), id, playback("audio/mp4; codecs=\"mp4a.40.2\"", 10000))
        val key = audioPlayerCacheKey(reader(), id)
        assertNotEquals(id, key)
        assertEquals(key, audioPlayerCacheKey(reader(), id))
        metadata.remove(id)
        assertEquals(key, audioPlayerCacheKey(reader(), id))
        rememberAudioPlayerCacheKey(reader(), id, playback("audio/webm; codecs=\"opus\"", 9000))
        assertNotEquals(key, audioPlayerCacheKey(reader(), id))
        assertEquals("another-id", audioPlayerCacheKey(reader(), "another-id"))
    }
}
