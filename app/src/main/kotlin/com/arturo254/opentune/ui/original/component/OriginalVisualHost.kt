package com.arturo254.opentune.ui.original.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arturo254.opentune.ui.original.constants.AnimationSpeedKey
import com.arturo254.opentune.ui.original.constants.BlurQualityKey
import com.arturo254.opentune.ui.original.constants.ElementBlurKey
import com.arturo254.opentune.ui.original.constants.EnableElementBlurAnimatorKey
import com.arturo254.opentune.ui.original.utils.AppMotion
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference

val LocalOriginalSceneActive = staticCompositionLocalOf { true }

internal fun originalAmbientMotionAllowed(
    sceneActive: Boolean,
    resumed: Boolean,
    enabled: Boolean,
    durationScale: Float,
): Boolean = sceneActive && resumed && enabled && durationScale > 0f

@Composable
fun rememberOriginalAmbientMotion(): Boolean {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    return originalAmbientMotionAllowed(
        LocalOriginalSceneActive.current,
        lifecycle.isAtLeast(Lifecycle.State.RESUMED),
        AppMotion.enabled,
        AppMotion.durationScale.scaleFactor,
    )
}

@Composable
fun OriginalVisualHost(reduceMotion: Boolean, content: @Composable () -> Unit) {
    val animations by rememberPreference(EnableElementBlurAnimatorKey, true)
    val speed by rememberPreference(AnimationSpeedKey, 1f)
    val elementBlur by rememberPreference(ElementBlurKey, true)
    val quality by rememberEnumPreference(BlurQualityKey, DefaultBlurQuality)
    SideEffect {
        AppMotion.enabled = animations && !reduceMotion
        AppMotion.speed = if (speed.isFinite()) speed.coerceIn(0.5f, 2f) else 1f
        AppMotion.elementBlur = elementBlur && !reduceMotion
    }
    CompositionLocalProvider(LocalBlurQuality provides quality, content = content)
}
