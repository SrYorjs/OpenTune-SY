package com.arturo254.opentune.ui.player.layout

import com.arturo254.opentune.constants.PlayerDesignStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLayoutTest {
    @Test
    fun allEightPersistedVersionsHaveExactlyOneLayoutFamily() {
        val expected = mapOf(
            PlayerDesignStyle.V1 to PlayerLayoutFamily.CLASSIC,
            PlayerDesignStyle.V2 to PlayerLayoutFamily.CLASSIC,
            PlayerDesignStyle.V3 to PlayerLayoutFamily.CLASSIC,
            PlayerDesignStyle.V4 to PlayerLayoutFamily.CLASSIC,
            PlayerDesignStyle.V5 to PlayerLayoutFamily.COMPACT,
            PlayerDesignStyle.V6 to PlayerLayoutFamily.CLASSIC,
            PlayerDesignStyle.V7 to PlayerLayoutFamily.CINEMATIC,
            PlayerDesignStyle.V8 to PlayerLayoutFamily.IMMERSIVE,
        )
        assertEquals(PlayerDesignStyle.entries.toSet(), expected.keys)
        expected.forEach { (style, family) ->
            for (landscape in listOf(false, true)) {
                val route = resolvePlayerLayout(style, isLandscape = landscape)
                assertEquals(style, route.style)
                assertEquals(family, route.family)
            }
        }
    }

    @Test
    fun portraitLandscapeKeepStyleAndFamily() {
        PlayerDesignStyle.entries.forEach { style ->
            val portrait = resolvePlayerLayout(style, false)
            val landscape = resolvePlayerLayout(style, true)
            assertEquals(PlayerLayoutOrientation.PORTRAIT, portrait.orientation)
            assertEquals(PlayerLayoutOrientation.LANDSCAPE, landscape.orientation)
            assertEquals(portrait.family, landscape.family)
            assertEquals(portrait.style, landscape.style)
        }
    }

    @Test
    fun existingSharedBackgroundRulesAreNotAltered() {
        for (style in PlayerDesignStyle.entries) {
            for (landscape in listOf(true, false)) {
                val expected = style != PlayerDesignStyle.V5 && style != PlayerDesignStyle.V7
                val route = resolvePlayerLayout(style, landscape)
                assertEquals("shared backdrop for $style / landscape=$landscape",
                    expected, route.hasSharedBackdrop)
            }
        }
        assertFalse(resolvePlayerLayout(PlayerDesignStyle.V7, false).hasSharedBackdrop)
        assertFalse(resolvePlayerLayout(PlayerDesignStyle.V5, true).hasSharedBackdrop)
        assertTrue(resolvePlayerLayout(PlayerDesignStyle.V8, true).hasSharedBackdrop)
    }

    @Test
    fun defaultV8IsStillImmersive() {
        val route = resolvePlayerLayout(PlayerDesignStyle.V8, false)
        assertEquals(PlayerLayoutFamily.IMMERSIVE, route.family)
        assertEquals(PlayerLayoutOrientation.PORTRAIT, route.orientation)
    }
}
