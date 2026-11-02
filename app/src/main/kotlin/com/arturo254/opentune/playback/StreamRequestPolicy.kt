package com.arturo254.opentune.playback

internal object StreamRequestPolicy {
    const val CHUNK_LENGTH = 512 * 1024L

    fun isDirectHttpStream(uri: String): Boolean =
        runCatching { java.net.URI(uri) }.getOrNull()?.let { address ->
            (address.scheme.equals("http", true) || address.scheme.equals("https", true)) &&
                !address.host.isNullOrBlank()
        } ?: false

    fun boundedLength(requestedLength: Long): Long =
        if (requestedLength >= 0L) {
            minOf(requestedLength, CHUNK_LENGTH)
        } else {
            CHUNK_LENGTH
        }
}
