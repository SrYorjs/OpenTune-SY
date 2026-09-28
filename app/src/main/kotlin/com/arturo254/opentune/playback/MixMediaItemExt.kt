package com.arturo254.opentune.playback

import android.os.Bundle
import androidx.media3.common.MediaItem

const val ExtraMixDurationMs = "com.arturo254.opentune.extra.MIX_DURATION_MS"
const val ExtraMixCurve = "com.arturo254.opentune.extra.MIX_CURVE"
const val ExtraMixBpm = "com.arturo254.opentune.extra.MIX_BPM"
const val ExtraMixGainDb = "com.arturo254.opentune.extra.MIX_GAIN_DB"

enum class MixTransitionCurve {
    FADE,
    BLEND,
    RISE,
    MELT,
}

fun MediaItem.withMixTransition(
    durationMs: Int,
    curve: MixTransitionCurve,
    bpm: Float? = null,
    gainDb: Float? = null,
): MediaItem {
    val extras = Bundle(mediaMetadata.extras ?: Bundle()).apply {
        putInt(ExtraMixDurationMs, durationMs.coerceIn(0, 20_000))
        putString(ExtraMixCurve, curve.name)
        bpm?.let { putFloat(ExtraMixBpm, it) }
        gainDb?.let { putFloat(ExtraMixGainDb, it) }
    }

    return buildUpon()
        .setMediaMetadata(
            mediaMetadata
                .buildUpon()
                .setExtras(extras)
                .build()
        )
        .build()
}

fun MediaItem.mixDurationMs(defaultValue: Int): Int =
    mediaMetadata.extras
        ?.getInt(ExtraMixDurationMs, defaultValue)
        ?.coerceIn(0, 20_000)
        ?: defaultValue

fun MediaItem.mixCurve(defaultValue: MixTransitionCurve): MixTransitionCurve =
    mediaMetadata.extras
        ?.getString(ExtraMixCurve)
        ?.let { stored ->
            runCatching { MixTransitionCurve.valueOf(stored) }.getOrNull()
        }
        ?: defaultValue
