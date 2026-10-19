package com.arturo254.opentune.ui.player.layout

import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.ui.player.v9.originalPlayerStyleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxFifteenStylesTest {
    private val originals = listOf(
        PlayerDesignStyle.V1,
        PlayerDesignStyle.V2,
        PlayerDesignStyle.V3,
        PlayerDesignStyle.V4,
        PlayerDesignStyle.V5,
        PlayerDesignStyle.V6,
        PlayerDesignStyle.V7,
        PlayerDesignStyle.V8,
    )

    private val newStyles = listOf(
        PlayerDesignStyle.V9,
        PlayerDesignStyle.V10,
        PlayerDesignStyle.V11,
        PlayerDesignStyle.V12,
        PlayerDesignStyle.V13,
        PlayerDesignStyle.V14,
        PlayerDesignStyle.V15,
    )

    @Test
    fun fifteenStylesRemainPersistableEnumValues() {
        assertEquals(15, PlayerDesignStyle.entries.size)
        assertEquals(8, originals.size)
        assertEquals(7, newStyles.size)
        assertEquals(PlayerDesignStyle.entries, originals + newStyles)
        assertEquals(PlayerDesignStyle.V8, PlayerDesignStyle.valueOf("V8"))
        assertEquals(PlayerDesignStyle.V15, PlayerDesignStyle.valueOf("V15"))
    }

    @Test
    fun sevenOriginalGalaxStylesHaveUniqueVisualIdentities() {
        val configs = newStyles.map(::originalPlayerStyleConfig)
        assertEquals(7, configs.distinctBy { it.name }.size)
        assertEquals(7, configs.distinctBy { it.description }.size)
        assertEquals(7, configs.distinctBy { it.ornament }.size)
        configs.forEach { config ->
            assertTrue(config.name.isNotBlank())
            assertTrue(config.description.isNotBlank())
            assertTrue(config.ornament.isNotBlank())
            assertTrue(config.artScale > 0f)
            assertTrue(config.artScale <= 1f)
        }
        assertTrue(configs.map { it.cornerRadius }.distinct().size >= 5)
    }

    @Test
    fun eachNewStyleRoutesThroughItsDedicatedSceneFamily() {
        newStyles.forEach { style ->
            val portrait = resolvePlayerLayout(style, false)
            val landscape = resolvePlayerLayout(style, true)
            assertEquals(style, portrait.style)
            assertEquals(style, landscape.style)
            assertEquals(PlayerLayoutFamily.GALAX, portrait.family)
            assertEquals(PlayerLayoutFamily.GALAX, landscape.family)
            assertEquals(PlayerLayoutOrientation.PORTRAIT, portrait.orientation)
            assertEquals(PlayerLayoutOrientation.LANDSCAPE, landscape.orientation)
            assertFalse(portrait.hasSharedBackdrop)
            assertFalse(landscape.hasSharedBackdrop)
        }
    }

    @Test
    fun legacyPlayerFamiliesAreNotSilentlyRemapped() {
        assertEquals(PlayerLayoutFamily.CLASSIC, resolvePlayerLayout(PlayerDesignStyle.V1, false).family)
        assertEquals(PlayerLayoutFamily.V2, resolvePlayerLayout(PlayerDesignStyle.V2, false).family)
        assertEquals(PlayerLayoutFamily.CLASSIC, resolvePlayerLayout(PlayerDesignStyle.V3, false).family)
        assertEquals(PlayerLayoutFamily.CLASSIC, resolvePlayerLayout(PlayerDesignStyle.V4, false).family)
        assertEquals(PlayerLayoutFamily.COMPACT, resolvePlayerLayout(PlayerDesignStyle.V5, false).family)
        assertEquals(PlayerLayoutFamily.CLASSIC, resolvePlayerLayout(PlayerDesignStyle.V6, false).family)
        assertEquals(PlayerLayoutFamily.CINEMATIC, resolvePlayerLayout(PlayerDesignStyle.V7, false).family)
        assertEquals(PlayerLayoutFamily.IMMERSIVE, resolvePlayerLayout(PlayerDesignStyle.V8, false).family)
    }

    @Test
    fun onlyClassicAndV2DrawSharedBackdrops() {
        for (style in PlayerDesignStyle.entries) {
            val needsShared = style in setOf(
                PlayerDesignStyle.V1,
                PlayerDesignStyle.V2,
                PlayerDesignStyle.V3,
                PlayerDesignStyle.V4,
                PlayerDesignStyle.V6,
            )
            assertEquals(
                "Shared background leak for $style",
                needsShared,
                resolvePlayerLayout(style, false).hasSharedBackdrop,
            )
            assertEquals(
                "Landscape shared background leak for $style",
                needsShared,
                resolvePlayerLayout(style, true).hasSharedBackdrop,
            )
        }
    }

    @Test
    fun appleMusicAndSevenNewStylesRemainBackdropIndependent() {
        val isolated = listOf(PlayerDesignStyle.V5, PlayerDesignStyle.V7, PlayerDesignStyle.V8) + newStyles
        isolated.forEach { style ->
            assertFalse(resolvePlayerLayout(style, false).hasSharedBackdrop)
            assertFalse(resolvePlayerLayout(style, true).hasSharedBackdrop)
        }
        assertNotEquals(
            resolvePlayerLayout(PlayerDesignStyle.V8, false).family,
            resolvePlayerLayout(PlayerDesignStyle.V9, false).family,
        )
    }

    @Test
    fun newStyleNamesAreNotAliasesOfExistingNames() {
        val oldNames = originals.map { it.name }.toSet()
        val newNames = newStyles.map { it.name }.toSet()
        assertTrue(oldNames.intersect(newNames).isEmpty())
        assertEquals(7, newNames.size)
    }
}
