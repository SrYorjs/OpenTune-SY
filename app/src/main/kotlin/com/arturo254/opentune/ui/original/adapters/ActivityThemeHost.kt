package com.arturo254.opentune.ui.original.adapters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.arturo254.opentune.constants.EnableHapticFeedbackKey
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
    val hapticsEnabled by rememberPreference(EnableHapticFeedbackKey, true)
    val platformHaptics = LocalHapticFeedback.current
    val guardedHaptics = remember(platformHaptics, hapticsEnabled) {
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                if (hapticsEnabled) platformHaptics.performHapticFeedback(hapticFeedbackType)
            }
        }
    }
    OriginalTheme(darkTheme = darkTheme, pureBlack = pureBlack) {
        CompositionLocalProvider(LocalHapticFeedback provides guardedHaptics) {
        ProvideOpenTuneDesignSystem(artworkRadiusDp = radius, reduceMotion = reduceMotion) {
            OriginalVisualHost(reduceMotion) {
                MeasuredWindowLayout { ActivityGlassHost(pureBlack, content) }
            }
        }
        }
    }
}
