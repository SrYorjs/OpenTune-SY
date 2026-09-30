package com.arturo254.opentune.playback

internal object StreamRequestPolicy {
    const val CHUNK_LENGTH = 512 * 1024L

    fun boundedLength(requestedLength: Long): Long =
        if (requestedLength >= 0L) {
            minOf(requestedLength, CHUNK_LENGTH)
        } else {
            CHUNK_LENGTH
        }
}
