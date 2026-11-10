package com.arturo254.opentune.ui.visual.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLyricPreviewTest {
    @Test
    fun previewFollowsTheActualPlaybackPosition() {
        val lines = listOf(
            PlayerLyricLine(1_000L, "Primera línea"),
            PlayerLyricLine(4_000L, "Segunda línea"),
            PlayerLyricLine(8_000L, "Última línea"),
        )
        assertEquals("Primera línea", playerLyricPreviewAt(lines, 0L))
        assertEquals("Primera línea", playerLyricPreviewAt(lines, 3_999L))
        assertEquals("Segunda línea", playerLyricPreviewAt(lines, 4_000L))
        assertEquals("Última línea", playerLyricPreviewAt(lines, 40_000L))
    }

    @Test
    fun previewHandlesMissingAndPlainLyrics() {
        assertTrue(parsePlayerLyricPreview(null).isEmpty())
        assertTrue(parsePlayerLyricPreview("LYRICS_NOT_FOUND").isEmpty())
        val plain = parsePlayerLyricPreview("Una línea real\nOtra línea")
        assertEquals("Una línea real", playerLyricPreviewAt(plain, 12_000L))
        assertNull(playerLyricPreviewAt(emptyList(), 0L))
    }
}
