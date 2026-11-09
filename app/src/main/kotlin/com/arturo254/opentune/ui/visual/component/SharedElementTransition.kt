package com.arturo254.opentune.ui.visual.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedContentScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.homeSharedElement(
    key: String?,
    cornerRadius: Dp = HomeCardCornerRadius,
    coverUrl: String? = null,
): Modifier {
    if (key == null) return this
    
    if (key.startsWith("playlist-") || key.startsWith("album-")) return containerTransformSource(key, cornerRadius, coverUrl)
    val sharedTransitionScope = LocalSharedTransitionScope.current ?: return this
    val animatedVisibilityScope = LocalNavAnimatedContentScope.current ?: return this
    return with(sharedTransitionScope) {
        this@homeSharedElement.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}

fun coverTransitionKey(item: com.arturo254.opentune.innertube.models.YTItem): String? = when (item) {
    is com.arturo254.opentune.innertube.models.AlbumItem -> "album-${item.id}"
    is com.arturo254.opentune.innertube.models.PlaylistItem -> "playlist-${item.id}"
    else -> null
}
