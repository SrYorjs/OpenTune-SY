package com.arturo254.opentune.ui.player.panels

import java.util.Locale

/**
 * Presentation of the *actual current* FormatEntity supplied by PlayerConnection.
 * This is not a source-quality selector or a claim about downloadable variants.
 * The Search/Sources version-quality selector keeps its own policy and state.
 */
fun playerCodecContainer(mimeType: String): String =
    mimeType.substringAfter("/").uppercase(Locale.ROOT)

/**
 * Preserve the previous V7/V8 badge labels in one place.
 * This reflects the current decoder format; it does not infer bitrate,
 * bit depth, offline quality or provider capabilities.
 */
fun playerFormatBadgeLabel(mimeType: String): String {
    val codec = playerCodecContainer(mimeType)
    return when {
        codec.contains("FLAC") || codec.contains("ALAC") -> "Lossless"
        codec.contains("OPUS") -> codec
        codec.contains("AAC") -> codec
        codec.contains("MP4A") -> "AAC"
        codec.contains("VORBIS") -> "Vorbis"
        else -> codec
    }
}
