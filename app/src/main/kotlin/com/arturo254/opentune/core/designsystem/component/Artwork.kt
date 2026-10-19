package com.arturo254.opentune.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.theme.GalaxVisualKit
import com.arturo254.opentune.core.designsystem.theme.sizes

@Composable
fun Artwork(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    shape: Shape = GalaxVisualKit.imageShape,
    contentDescription: String? = null,
    @DrawableRes placeholderIcon: Int = R.drawable.library_music,
    placeholderSize: Dp = OpenTuneDesignSystem.sizes.searchPlaceholderIcon,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        scheme.primary.copy(alpha = .29f),
                        scheme.surfaceContainerHigh,
                        scheme.tertiary.copy(alpha = .22f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(placeholderIcon),
                contentDescription = contentDescription,
                tint = scheme.primary.copy(alpha = .8f),
                modifier = Modifier.size(placeholderSize),
            )
        }
        overlay()
    }
}
