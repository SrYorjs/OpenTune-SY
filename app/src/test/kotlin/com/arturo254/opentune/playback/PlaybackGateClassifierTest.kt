package com.arturo254.opentune.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackGateClassifierTest {
    @Test
    fun ageVerificationIsRecognizedInEnglishAndSpanish() {
        assertTrue(PlaybackGateClassifier.requiresAgeVerification("Sign in to confirm your age"))
        assertTrue(PlaybackGateClassifier.requiresAgeVerification("This video is age-restricted"))
        assertTrue(PlaybackGateClassifier.requiresAgeVerification("Accede para confirmar tu edad"))
    }

    @Test
    fun unrelatedLoginAndNetworkErrorsAreNotAgeRestrictions() {
        assertFalse(PlaybackGateClassifier.requiresAgeVerification(null))
        assertFalse(PlaybackGateClassifier.requiresAgeVerification("Sign in to confirm you're not a bot"))
        assertFalse(PlaybackGateClassifier.requiresAgeVerification("HTTP 403"))
        assertFalse(PlaybackGateClassifier.requiresAgeVerification("LOGIN_REQUIRED"))
    }
}
