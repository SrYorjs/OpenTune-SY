package com.arturo254.opentune.extensions

import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE

/**
 * Exact legacy transport behavior, extracted for pure JVM regression tests.
 * The real Media3 player remains in MusicService/PlayerConnection.
 */
internal object PlayerTransportPolicy {
    fun shouldPrepareBeforeToggle(playWhenReady: Boolean, playbackState: Int): Boolean =
        !playWhenReady && playbackState == Player.STATE_IDLE

    fun nextRepeatMode(currentMode: Int): Int =
        when (currentMode) {
            REPEAT_MODE_OFF -> REPEAT_MODE_ALL
            REPEAT_MODE_ALL -> REPEAT_MODE_ONE
            REPEAT_MODE_ONE -> REPEAT_MODE_OFF
            else -> throw IllegalStateException()
        }
}
