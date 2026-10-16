package com.arturo254.opentune.ui.player.panels

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/** Mapping of the *known current audio format* only, not inferred source quality. */
class PlayerQualityFormatTest {
    @Test fun v7AndV8KeepExistingCodecBadgeMappings() {
        assertEquals("Lossless", playerFormatBadgeLabel("audio/flac"))
        assertEquals("Lossless", playerFormatBadgeLabel("audio/alac"))
        assertEquals("OPUS", playerFormatBadgeLabel("audio/opus"))
        assertEquals("AAC", playerFormatBadgeLabel("audio/aac"))
        assertEquals("AAC", playerFormatBadgeLabel("audio/mp4a"))
        assertEquals("Vorbis", playerFormatBadgeLabel("audio/vorbis"))
        assertEquals("MPEG", playerFormatBadgeLabel("audio/mpeg"))
    }

    @Test fun mimeSubtypePresentationRemainsIndependentFromPreferredProvider() {
        assertEquals("FLAC", playerCodecContainer("audio/flac"))
        assertEquals("M4A", playerCodecContainer("audio/m4a"))
        assertEquals("AUDIO", playerCodecContainer("audio"))
        assertEquals("", playerCodecContainer(""))
    }

    @Test fun qualityLabelDoesNotCrashInTurkishLocale() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("Lossless", playerFormatBadgeLabel("audio/flac"))
            assertEquals("MIDI", playerCodecContainer("audio/midi"))
        } finally {
            Locale.setDefault(old)
        }
    }
}
