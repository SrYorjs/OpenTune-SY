package com.arturo254.opentune.ui.player.skin

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.arturo254.opentune.constants.PlayerBackgroundStyle
import com.arturo254.opentune.constants.PlayerButtonsStyle
import com.arturo254.opentune.constants.PlayerDesignStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualSkinTest {
    private val colors = lightColorScheme(
        secondary = Color(0xFF008B8B),
        onSecondary = Color.Yellow,
        onBackground = Color(0xFF121212),
        onSurface = Color(0xFF313131),
        onPrimary = Color(0xFF414141),
        surface = Color(0xFFEFEFEF),
        surfaceContainer = Color(0xFFDEDEDE),
    )

    private fun skin(
        style: PlayerDesignStyle = PlayerDesignStyle.V8,
        background: PlayerBackgroundStyle = PlayerBackgroundStyle.DEFAULT,
        buttons: PlayerButtonsStyle = PlayerButtonsStyle.DEFAULT,
        dark: Boolean = false,
        pureBlack: Boolean = false,
    ) = resolveVisualSkin(style, background, buttons, dark, pureBlack, colors)

    @Test
    fun v7ForcesLegibleWhiteAndBlackForEveryBackgroundAndButtonMode() {
        for (background in PlayerBackgroundStyle.entries) {
            for (buttons in PlayerButtonsStyle.entries) {
                val result = skin(PlayerDesignStyle.V7, background, buttons)
                assertEquals(Color.White, result.textBackgroundColor)
                assertEquals(Color.Black, result.iconBackgroundColor)
                assertEquals(Color.White, result.textButtonColor)
                assertEquals(Color.Black, result.iconButtonColor)
                assertEquals(Color.Black, result.sheetBackgroundColor(1f))
            }
        }
    }

    @Test
    fun materialDefaultAndAlternativeColorsMatchTheOldPlayer() {
        val normal = skin()
        assertEquals(colors.secondary, normal.onBackgroundColor)
        assertEquals(colors.onBackground, normal.textBackgroundColor)
        assertEquals(colors.surface, normal.iconBackgroundColor)
        assertEquals(colors.onBackground, normal.textButtonColor)
        assertEquals(colors.surface, normal.iconButtonColor)
        val alternative = skin(
            background = PlayerBackgroundStyle.BLUR,
            buttons = PlayerButtonsStyle.SECONDARY,
        )
        assertEquals(Color.White, alternative.textBackgroundColor)
        assertEquals(Color.Black, alternative.iconBackgroundColor)
        assertEquals(colors.secondary, alternative.textButtonColor)
        assertEquals(colors.onSecondary, alternative.iconButtonColor)
        assertEquals(colors.onPrimary, alternative.onBackgroundColor)
        assertEquals(colors.onSurface, skin(
            background = PlayerBackgroundStyle.BLUR, dark = true
        ).onBackgroundColor)
    }

    @Test
    fun pureBlackOnlyAppliesToDarkThemeAndKeepsQueueContrast() {
        assertFalse(skin(pureBlack = true, dark = false).useBlackBackground)
        val black = skin(pureBlack = true, dark = true)
        assertTrue(black.useBlackBackground)
        assertEquals(Color.Black, black.queueSurfaceColor)
        assertEquals(Color.White, black.queueOnBackgroundColor)
        assertEquals(Color.Black, black.playerBackgroundColor(1f, true))
        assertEquals(colors.surfaceContainer, black.playerBackgroundColor(1f, false))
        assertEquals(Color.Black, black.sheetBackgroundColor(1f))
        val secondary = skin(
            buttons = PlayerButtonsStyle.SECONDARY, pureBlack = true, dark = true
        )
        assertEquals(colors.secondary, secondary.queueTextButtonColor)
        assertEquals(colors.onSecondary, secondary.queueIconButtonColor)
    }

    @Test
    fun paletteExtractionTriggersOnlyForTheOriginalFiveBackgroundModes() {
        val paletteModes = setOf(
            PlayerBackgroundStyle.GRADIENT,
            PlayerBackgroundStyle.COLORING,
            PlayerBackgroundStyle.BLUR_GRADIENT,
            PlayerBackgroundStyle.GLOW,
            PlayerBackgroundStyle.GLOW_ANIMATED,
        )
        for (bg in PlayerBackgroundStyle.entries) {
            assertEquals("background mode: $bg", bg in paletteModes,
                skin(background = bg).usesArtworkPalette)
        }
    }

    @Test
    fun sheetFadeIsPreservedAcrossV7BlurGradientAndPureBlack() {
        val v7 = skin(style = PlayerDesignStyle.V7)
        assertEquals(0f, v7.sheetBackgroundColor(0f).alpha, 0.00001f)
        // ARGB Color quantizes sRGB alpha to 8-bit; tolerate < 1/255.
        assertEquals(0.5f, v7.sheetBackgroundColor(0.1f).alpha, 0.004f)
        assertEquals(1f, v7.sheetBackgroundColor(0.2f).alpha, 0.00001f)
        val blur = skin(background = PlayerBackgroundStyle.BLUR, dark = true, pureBlack = true)
        assertEquals(colors.surface, blur.sheetBackgroundColor(1f))
        val gradient = skin(background = PlayerBackgroundStyle.GRADIENT,
            dark = true, pureBlack = true)
        assertEquals(colors.surface, gradient.sheetBackgroundColor(1f))
        val other = skin(background = PlayerBackgroundStyle.COLORING,
            dark = true, pureBlack = true)
        assertEquals(Color.Black, other.sheetBackgroundColor(1f))
    }

    @Test
    fun paletteIsDerivedFromCurrentMaterialColorsAndUpdatesOnChange() {
        val one = skin()
        val changed = resolveVisualSkin(
            PlayerDesignStyle.V8, PlayerBackgroundStyle.DEFAULT,
            PlayerButtonsStyle.DEFAULT, false, false,
            colors.copy(secondary = Color.Magenta, onBackground = Color.Cyan),
        )
        assertEquals(colors.secondary, one.onBackgroundColor)
        assertEquals(Color.Magenta, changed.onBackgroundColor)
        assertEquals(Color.Cyan, changed.textBackgroundColor)
    }

    @Test
    fun everySavedVersionAndBackgroundIsResolvable() {
        for (style in PlayerDesignStyle.entries)
            for (background in PlayerBackgroundStyle.entries)
                for (buttons in PlayerButtonsStyle.entries) {
                    val result = skin(style, background, buttons)
                    assertEquals(style, result.designStyle)
                    assertEquals(background, result.backgroundStyle)
                    assertEquals(buttons, result.buttonStyle)
                }
    }
}
