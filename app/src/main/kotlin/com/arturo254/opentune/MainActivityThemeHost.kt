package com.arturo254.opentune

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.arturo254.opentune.constants.GalaxReduceMotionKey
import com.arturo254.opentune.constants.LyricsSyncOffsetKey
import com.arturo254.opentune.constants.PureBlackKey
import com.arturo254.opentune.constants.ThumbnailCornerRadiusKey
import com.arturo254.opentune.core.designsystem.theme.ProvideOpenTuneDesignSystem
import com.arturo254.opentune.foundation.theme.ForceDarkTheme
import com.arturo254.opentune.foundation.theme.OriginalTheme
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.utils.rememberPreference

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun MainActivityThemeHost(
    activity: ComponentActivity,
    playerConnection: PlayerConnection?,
    systemUiController: MainActivitySystemUiController,
    content: @Composable (
        pureBlack: Boolean,
        lyricsSyncOffset: Int,
    ) -> Unit,
) {
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val lyricsSyncOffset by rememberPreference(LyricsSyncOffsetKey, defaultValue = 0)
    val reduceMotion by rememberPreference(GalaxReduceMotionKey, defaultValue = false)
    val artworkCornerRadius by rememberPreference(ThumbnailCornerRadiusKey, defaultValue = 16f)

    LaunchedEffect(systemUiController) {
        systemUiController.setSystemBarAppearance(ForceDarkTheme)
    }

    OriginalTheme(pureBlack = pureBlack) {
        ProvideOpenTuneDesignSystem(
            artworkRadiusDp = artworkCornerRadius,
            reduceMotion = reduceMotion,
        ) {
            content(pureBlack, lyricsSyncOffset)
        }
    }
}
