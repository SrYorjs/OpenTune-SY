package com.arturo254.opentune.ui.player.skin

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.arturo254.opentune.constants.PlayerBackgroundStyle
import com.arturo254.opentune.constants.PlayerButtonsStyle
import com.arturo254.opentune.constants.PlayerDesignStyle

/**
 * Player 2.0 visual-only theme. The selected layout remains in PlayerLayout,
 * audio state in PlayerState, and commands in PlayerActions.
 *
 * Tokens follow the current Material theme: dynamic wallpaper colors and
 * dark/light palette changes are reflected without creating a second theme.
 */
@Immutable
data class VisualSkin(
    val designStyle: PlayerDesignStyle,
    val backgroundStyle: PlayerBackgroundStyle,
    val buttonStyle: PlayerButtonsStyle,
    val useDarkTheme: Boolean,
    val useBlackBackground: Boolean,
    val onBackgroundColor: Color,
    val textBackgroundColor: Color,
    val iconBackgroundColor: Color,
    val textButtonColor: Color,
    val iconButtonColor: Color,
    val queueOnBackgroundColor: Color,
    val queueSurfaceColor: Color,
    val queueTextButtonColor: Color,
    val queueIconButtonColor: Color,
    val normalSurfaceColor: Color,
    val surfaceContainerColor: Color,
    val usesArtworkPalette: Boolean,
) {
    /** Opacity over the collapsed-to-expanded position, same as pre-4.6. */
    fun playerBackgroundColor(progress: Float, aboveCollapsed: Boolean): Color {
        val alpha = progress.coerceIn(0f, 1f)
        return if (useBlackBackground && aboveCollapsed) Color.Black.copy(alpha = alpha)
        else surfaceContainerColor.copy(alpha = alpha)
    }

    /**
     * The overlay sheet preserves V7's black and BLUR/GRADIENT's
     * Material surface, and retains the old 20%-of-expansion fade.
     */
    fun sheetBackgroundColor(progress: Float): Color {
        val p = progress.coerceIn(0f, 1f)
        val fade = if (p < 0.2f) ((0.2f - p) / 0.2f).coerceIn(0f, 1f) else 0f
        val base = when {
            designStyle == PlayerDesignStyle.V7 -> Color.Black
            backgroundStyle == PlayerBackgroundStyle.BLUR ||
                backgroundStyle == PlayerBackgroundStyle.GRADIENT -> normalSurfaceColor
            useBlackBackground -> Color.Black
            else -> normalSurfaceColor
        }
        return base.copy(alpha = 1f - fade)
    }
}

/** Pure, testable mapping; NEVER reads preferences or owns mutable state. */
fun resolveVisualSkin(
    designStyle: PlayerDesignStyle,
    backgroundStyle: PlayerBackgroundStyle,
    buttonStyle: PlayerButtonsStyle,
    useDarkTheme: Boolean,
    pureBlack: Boolean,
    colors: ColorScheme,
): VisualSkin {
    val black = useDarkTheme && pureBlack
    val text = if (designStyle == PlayerDesignStyle.V7 ||
        backgroundStyle != PlayerBackgroundStyle.DEFAULT) Color.White else colors.onBackground
    val icon = if (designStyle == PlayerDesignStyle.V7 ||
        backgroundStyle != PlayerBackgroundStyle.DEFAULT) Color.Black else colors.surface
    val buttons = if (designStyle == PlayerDesignStyle.V7) {
        Color.White to Color.Black
    } else if (buttonStyle == PlayerButtonsStyle.SECONDARY) {
        colors.secondary to colors.onSecondary
    } else {
        text to icon
    }
    val queueOn = if (black) Color.White else colors.onSurface
    val queueSurface = if (black) Color.Black else colors.surface
    val queueButtons = if (buttonStyle == PlayerButtonsStyle.SECONDARY) {
        colors.secondary to colors.onSecondary
    } else {
        queueOn to queueSurface
    }
    return VisualSkin(
        designStyle = designStyle,
        backgroundStyle = backgroundStyle,
        buttonStyle = buttonStyle,
        useDarkTheme = useDarkTheme,
        useBlackBackground = black,
        onBackgroundColor =
            if (backgroundStyle == PlayerBackgroundStyle.DEFAULT) colors.secondary
            else if (useDarkTheme) colors.onSurface else colors.onPrimary,
        textBackgroundColor = text,
        iconBackgroundColor = icon,
        textButtonColor = buttons.first,
        iconButtonColor = buttons.second,
        queueOnBackgroundColor = queueOn,
        queueSurfaceColor = queueSurface,
        queueTextButtonColor = queueButtons.first,
        queueIconButtonColor = queueButtons.second,
        normalSurfaceColor = colors.surface,
        surfaceContainerColor = colors.surfaceContainer,
        usesArtworkPalette = backgroundStyle == PlayerBackgroundStyle.GRADIENT ||
            backgroundStyle == PlayerBackgroundStyle.COLORING ||
            backgroundStyle == PlayerBackgroundStyle.BLUR_GRADIENT ||
            backgroundStyle == PlayerBackgroundStyle.GLOW ||
            backgroundStyle == PlayerBackgroundStyle.GLOW_ANIMATED,
    )
}

/**
 * One skin per Player view; updating the original preferences swaps tokens
 * immediately. Glass rendering stays delegated to the existing Design System.
 */
@Composable
fun rememberVisualSkin(
    designStyle: PlayerDesignStyle,
    backgroundStyle: PlayerBackgroundStyle,
    buttonStyle: PlayerButtonsStyle,
    useDarkTheme: Boolean,
    pureBlack: Boolean,
): VisualSkin {
    val colors = MaterialTheme.colorScheme
    return remember(designStyle, backgroundStyle, buttonStyle, useDarkTheme, pureBlack, colors) {
        resolveVisualSkin(designStyle, backgroundStyle, buttonStyle, useDarkTheme, pureBlack, colors)
    }
}
