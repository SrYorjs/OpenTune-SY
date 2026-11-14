package com.arturo254.opentune.ui.original.adapters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arturo254.opentune.constants.GalaxReduceMotionKey
import com.arturo254.opentune.constants.ThumbnailCornerRadiusKey
import com.arturo254.opentune.core.designsystem.theme.ProvideOpenTuneDesignSystem
import com.arturo254.opentune.foundation.theme.OriginalTheme
import com.arturo254.opentune.ui.original.component.OriginalVisualHost
import com.arturo254.opentune.ui.original.utils.MeasuredWindowLayout
import com.arturo254.opentune.utils.rememberPreference

@Composable
fun ActivityThemeHost(darkTheme: Boolean, pureBlack: Boolean, content: @Composable () -> Unit) {
    val reduceMotion by rememberPreference(GalaxReduceMotionKey, false)
    val radius by rememberPreference(ThumbnailCornerRadiusKey, 16f)
    OriginalTheme(darkTheme = darkTheme, pureBlack = pureBlack) {
        ProvideOpenTuneDesignSystem(artworkRadiusDp = radius, reduceMotion = reduceMotion) {
            OriginalVisualHost(reduceMotion) {
                MeasuredWindowLayout { ActivityGlassHost(pureBlack, content) }
            }
        }
    }
}
