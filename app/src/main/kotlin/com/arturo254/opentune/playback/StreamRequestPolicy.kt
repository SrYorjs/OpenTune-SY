package com.arturo254.opentune.playback

internal object StreamRequestPolicy {

    fun isDirectHttpStream(uri: String): Boolean =
        runCatching { java.net.URI(uri) }.getOrNull()?.let { address ->
            (address.scheme.equals("http", true) || address.scheme.equals("https", true)) &&
                !address.host.isNullOrBlank()
        } ?: false

    fun resolveDataSpec(spec: androidx.media3.datasource.DataSpec, uri: android.net.Uri): androidx.media3.datasource.DataSpec =
        spec.withUri(uri)
}
