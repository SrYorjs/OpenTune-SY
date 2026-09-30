package com.arturo254.opentune.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamRequestPolicyTest {
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
