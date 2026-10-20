package com.arturo254.opentune.ui.player.GalaxStudioPlayer
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.arturo254.opentune.constants.PlayerDesignStyle
@Composable
fun StudioVisualScene(style: PlayerDesignStyle,isLandscape: Boolean,isPlaying: Boolean,colors: List<Color>,queuePeekHeight: Dp,progress: Float,artwork: @Composable BoxScope.() -> Unit,controls: @Composable () -> Unit) {
    val palette = MaterialTheme.colorScheme
    val scene = StudioSceneState(isLandscape,isPlaying,progress.coerceIn(0f,1f),colors.firstOrNull() ?: palette.primary,colors.getOrNull(1) ?: palette.tertiary,queuePeekHeight)
    when(style) {
        PlayerDesignStyle.V9 -> StudioThemeV9(scene,artwork,controls)
        PlayerDesignStyle.V10 -> StudioThemeV10(scene,artwork,controls)
        PlayerDesignStyle.V11 -> StudioThemeV11(scene,artwork,controls)
        PlayerDesignStyle.V12 -> StudioThemeV12(scene,artwork,controls)
        PlayerDesignStyle.V13 -> StudioThemeV13(scene,artwork,controls)
        PlayerDesignStyle.V14 -> StudioThemeV14(scene,artwork,controls)
        PlayerDesignStyle.V15 -> StudioThemeV15(scene,artwork,controls)
        else -> StudioThemeV9(scene,artwork,controls)
    }
}
