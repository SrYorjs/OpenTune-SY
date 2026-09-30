package com.arturo254.opentune.playback

/**
 * Android-independent audio-focus policy.
 *
 * Keeping the decisions pure lets us lock the current playback behavior down
 * with JVM tests while MusicService is split into smaller controllers.
 */
internal enum class AudioFocusEvent {
    GAIN,
    LOSS,
    LOSS_TRANSIENT,
    LOSS_TRANSIENT_CAN_DUCK,
    GAIN_TRANSIENT,
    GAIN_TRANSIENT_MAY_DUCK,
}

internal data class AudioFocusState(
    val hasAudioFocus: Boolean = false,
    val wasPlayingBeforeAudioFocusLoss: Boolean = false,
    val lastEvent: AudioFocusEvent? = null,
)

internal data class AudioFocusDecision(
    val state: AudioFocusState,
    val volumeFactor: Float,
    val pausePlayback: Boolean = false,
    val resumePlayback: Boolean = false,
)

internal object AudioFocusPolicy {
    fun reduce(
        state: AudioFocusState,
        event: AudioFocusEvent,
        isPlaying: Boolean,
    ): AudioFocusDecision =
        when (event) {
            AudioFocusEvent.GAIN ->
                AudioFocusDecision(
                    state =
                        state.copy(
                            hasAudioFocus = true,
                            wasPlayingBeforeAudioFocusLoss = false,
                            lastEvent = event,
                        ),
                    volumeFactor = 1f,
                    resumePlayback = state.wasPlayingBeforeAudioFocusLoss,
                )

            AudioFocusEvent.LOSS ->
                AudioFocusDecision(
                    state =
                        state.copy(
                            hasAudioFocus = false,
                            wasPlayingBeforeAudioFocusLoss = false,
                            lastEvent = event,
                        ),
                    volumeFactor = 1f,
                    pausePlayback = isPlaying,
                )

            AudioFocusEvent.LOSS_TRANSIENT ->
                AudioFocusDecision(
                    state =
                        state.copy(
                            hasAudioFocus = false,
                            wasPlayingBeforeAudioFocusLoss = isPlaying,
                            lastEvent = event,
                        ),
                    volumeFactor = 1f,
                    pausePlayback = isPlaying,
                )

            AudioFocusEvent.LOSS_TRANSIENT_CAN_DUCK ->
                AudioFocusDecision(
                    state =
                        state.copy(
                            hasAudioFocus = false,
                            wasPlayingBeforeAudioFocusLoss = isPlaying,
                            lastEvent = event,
                        ),
                    volumeFactor = 0.2f,
                )

            AudioFocusEvent.GAIN_TRANSIENT ->
                AudioFocusDecision(
                    state =
                        state.copy(
                            hasAudioFocus = true,
                            wasPlayingBeforeAudioFocusLoss = false,
                            lastEvent = event,
                        ),
                    volumeFactor = 1f,
                    resumePlayback = state.wasPlayingBeforeAudioFocusLoss,
                )

            AudioFocusEvent.GAIN_TRANSIENT_MAY_DUCK ->
                AudioFocusDecision(
                    state =
                        state.copy(
                            hasAudioFocus = true,
                            lastEvent = event,
                        ),
                    volumeFactor = 1f,
                )
        }
}
