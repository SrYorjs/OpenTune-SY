package com.arturo254.opentune.playback

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Owns Android audio-focus integration for playback.
 *
 * MusicService delegates focus acquisition/release here while AudioFocusPolicy
 * owns the testable behavioral decisions.
 */
internal class AudioFocusController(
    context: Context,
    private val player: Player,
    private val audioFocusVolumeFactor: MutableStateFlow<Float>,
) {
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var state = AudioFocusState()

    private val audioFocusRequest =
        AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes
                    .Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            ).setOnAudioFocusChangeListener(::handleSystemFocusChange)
            .setAcceptsDelayedFocusGain(true)
            .build()

    fun requestFocus(): Boolean {
        if (state.hasAudioFocus) return true

        val result = audioManager.requestAudioFocus(audioFocusRequest)
        val granted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        state = state.copy(hasAudioFocus = granted)
        return granted
    }

    fun abandonFocus() {
        if (!state.hasAudioFocus) return

        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        state = state.copy(hasAudioFocus = false)
    }

    fun hasFocus(): Boolean = state.hasAudioFocus

    private fun handleSystemFocusChange(focusChange: Int) {
        val event =
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> AudioFocusEvent.GAIN
                AudioManager.AUDIOFOCUS_LOSS -> AudioFocusEvent.LOSS
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> AudioFocusEvent.LOSS_TRANSIENT
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                    AudioFocusEvent.LOSS_TRANSIENT_CAN_DUCK
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> AudioFocusEvent.GAIN_TRANSIENT
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK ->
                    AudioFocusEvent.GAIN_TRANSIENT_MAY_DUCK
                else -> return
            }

        val decision =
            AudioFocusPolicy.reduce(
                state = state,
                event = event,
                isPlaying = player.isPlaying,
            )

        state = decision.state
        audioFocusVolumeFactor.value = decision.volumeFactor

        if (decision.pausePlayback && player.isPlaying) {
            player.pause()
        }
        if (decision.resumePlayback) {
            player.play()
        }
    }
}
