package com.arturo254.opentune.ui.player.layout

import com.arturo254.opentune.constants.PlayerDesignStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLayoutTest {
    @Test
    fun allSeventeenPersistedVersionsHaveExactlyOneLayoutFamily() {
        val expected = mapOf(
            PlayerDesignStyle.V1 to PlayerLayoutFamily.CLASSIC,
            PlayerDesignStyle.V2 to PlayerLayoutFamily.V2,
            PlayerDesignStyle.V3 to PlayerLayoutFamily.CLASSIC,
            PlayerDesignStyle.V4 to PlayerLayoutFamily.CLASSIC,
            PlayerDesignStyle.V5 to PlayerLayoutFamily.COMPACT,
            PlayerDesignStyle.V6 to PlayerLayoutFamily.CLASSIC,
            PlayerDesignStyle.V7 to PlayerLayoutFamily.CINEMATIC,
            PlayerDesignStyle.V8 to PlayerLayoutFamily.APPLE_MUSIC,
            PlayerDesignStyle.V9 to PlayerLayoutFamily.GALAX,
            PlayerDesignStyle.V10 to PlayerLayoutFamily.GALAX,
            PlayerDesignStyle.V11 to PlayerLayoutFamily.GALAX,
            PlayerDesignStyle.V12 to PlayerLayoutFamily.GALAX,
            PlayerDesignStyle.V13 to PlayerLayoutFamily.GALAX,
            PlayerDesignStyle.V14 to PlayerLayoutFamily.GALAX,
            PlayerDesignStyle.V15 to PlayerLayoutFamily.GALAX,
            PlayerDesignStyle.V16 to PlayerLayoutFamily.GALAX_THEME,
            PlayerDesignStyle.V17 to PlayerLayoutFamily.VINJAX,
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
    fun v2GetsDedicatedFamilyWithoutChangingClassicBackgroundPolicy() {
        for (isLandscape in listOf(false, true)) {
            val route = resolvePlayerLayout(PlayerDesignStyle.V2, isLandscape)
            assertEquals(PlayerLayoutFamily.V2, route.family)
            assertTrue(route.hasSharedBackdrop)
        }
        assertEquals(PlayerLayoutFamily.CLASSIC, resolvePlayerLayout(PlayerDesignStyle.V1, false).family)
        assertEquals(PlayerLayoutFamily.CLASSIC, resolvePlayerLayout(PlayerDesignStyle.V3, true).family)
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
    fun backgroundOwnershipIsolatedForSpecialAndGalaxStyles() {
        for (style in PlayerDesignStyle.entries) {
            for (landscape in listOf(true, false)) {
                val expected = style in setOf(
                    PlayerDesignStyle.V1, PlayerDesignStyle.V2, PlayerDesignStyle.V3,
                    PlayerDesignStyle.V4, PlayerDesignStyle.V6,
                )
                val route = resolvePlayerLayout(style, landscape)
                assertEquals("shared backdrop for $style / landscape=$landscape",
                    expected, route.hasSharedBackdrop)
            }
        }
        assertFalse(resolvePlayerLayout(PlayerDesignStyle.V7, false).hasSharedBackdrop)
        assertFalse(resolvePlayerLayout(PlayerDesignStyle.V5, true).hasSharedBackdrop)
        assertFalse(resolvePlayerLayout(PlayerDesignStyle.V8, true).hasSharedBackdrop)
    }

    @Test
    fun appleMusicAndGalaxThemeHaveSeparateFamilies() {
        val route = resolvePlayerLayout(PlayerDesignStyle.V8, false)
        assertEquals(PlayerLayoutFamily.APPLE_MUSIC, route.family)
        assertEquals(PlayerLayoutFamily.GALAX_THEME, resolvePlayerLayout(PlayerDesignStyle.V16, false).family)
        assertEquals(PlayerLayoutFamily.VINJAX, resolvePlayerLayout(PlayerDesignStyle.V17, false).family)
        assertEquals(PlayerLayoutOrientation.PORTRAIT, route.orientation)
    }
}
