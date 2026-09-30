package com.arturo254.opentune.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFocusPolicyTest {
    @Test
    fun transientLoss_pausesAndRemembersActivePlayback() {
        val decision =
            AudioFocusPolicy.reduce(
                state = AudioFocusState(hasAudioFocus = true),
                event = AudioFocusEvent.LOSS_TRANSIENT,
                isPlaying = true,
            )

        assertFalse(decision.state.hasAudioFocus)
        assertTrue(decision.state.wasPlayingBeforeAudioFocusLoss)
        assertTrue(decision.pausePlayback)
        assertFalse(decision.resumePlayback)
        assertEquals(1f, decision.volumeFactor)
    }

    @Test
    fun ducking_keepsPlaybackRunningAndReducesVolumeToCurrentPolicy() {
        val decision =
            AudioFocusPolicy.reduce(
                state = AudioFocusState(hasAudioFocus = true),
                event = AudioFocusEvent.LOSS_TRANSIENT_CAN_DUCK,
                isPlaying = true,
            )

        assertFalse(decision.state.hasAudioFocus)
        assertTrue(decision.state.wasPlayingBeforeAudioFocusLoss)
        assertFalse(decision.pausePlayback)
        assertFalse(decision.resumePlayback)
        assertEquals(0.2f, decision.volumeFactor)
    }

    @Test
    fun gain_resumesOnlyWhenPlaybackWasInterruptedByFocusLoss() {
        val resume =
            AudioFocusPolicy.reduce(
                state =
                    AudioFocusState(
                        hasAudioFocus = false,
                        wasPlayingBeforeAudioFocusLoss = true,
                    ),
                event = AudioFocusEvent.GAIN,
                isPlaying = false,
            )
        val doNotResume =
            AudioFocusPolicy.reduce(
                state =
                    AudioFocusState(
                        hasAudioFocus = false,
                        wasPlayingBeforeAudioFocusLoss = false,
                    ),
                event = AudioFocusEvent.GAIN,
                isPlaying = false,
            )

        assertTrue(resume.state.hasAudioFocus)
        assertTrue(resume.resumePlayback)
        assertFalse(resume.state.wasPlayingBeforeAudioFocusLoss)
        assertFalse(doNotResume.resumePlayback)
        assertEquals(1f, resume.volumeFactor)
    }

    @Test
    fun permanentLoss_clearsResumeIntentAndPausesCurrentPlayback() {
        val decision =
            AudioFocusPolicy.reduce(
                state =
                    AudioFocusState(
                        hasAudioFocus = true,
                        wasPlayingBeforeAudioFocusLoss = true,
                    ),
                event = AudioFocusEvent.LOSS,
                isPlaying = true,
            )

        assertFalse(decision.state.hasAudioFocus)
        assertFalse(decision.state.wasPlayingBeforeAudioFocusLoss)
        assertTrue(decision.pausePlayback)
        assertFalse(decision.resumePlayback)
        assertEquals(1f, decision.volumeFactor)
    }

    @Test
    fun transientGain_restoresPlaybackAfterTransientLoss() {
        val decision =
            AudioFocusPolicy.reduce(
                state =
                    AudioFocusState(
                        hasAudioFocus = false,
                        wasPlayingBeforeAudioFocusLoss = true,
                    ),
                event = AudioFocusEvent.GAIN_TRANSIENT,
                isPlaying = false,
            )

        assertTrue(decision.state.hasAudioFocus)
        assertFalse(decision.state.wasPlayingBeforeAudioFocusLoss)
        assertTrue(decision.resumePlayback)
        assertEquals(1f, decision.volumeFactor)
    }

    @Test
    fun mayDuckGain_preservesExistingResumeFlagLikeLegacyService() {
        val decision =
            AudioFocusPolicy.reduce(
                state =
                    AudioFocusState(
                        hasAudioFocus = false,
                        wasPlayingBeforeAudioFocusLoss = true,
                    ),
                event = AudioFocusEvent.GAIN_TRANSIENT_MAY_DUCK,
                isPlaying = false,
            )

        assertTrue(decision.state.hasAudioFocus)
        assertTrue(decision.state.wasPlayingBeforeAudioFocusLoss)
        assertFalse(decision.resumePlayback)
        assertEquals(1f, decision.volumeFactor)
    }
}
