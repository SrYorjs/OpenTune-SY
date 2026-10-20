package com.arturo254.opentune.ui.player.v8

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Composable
fun V8ImmersiveGradientFloor(
    artworkUrl: String?,
    palette: List<Color>,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    var settledPalette by remember { mutableStateOf<List<Color>>(emptyList()) }
    LaunchedEffect(artworkUrl, palette) {
        if (artworkUrl.isNullOrBlank()) {
            settledPalette = emptyList()
        } else if (palette.isNotEmpty()) {
            settledPalette = palette
        }
    }

    val primaryTarget = palette.firstOrNull()
        ?: settledPalette.firstOrNull()
        ?: scheme.primary
    val secondaryTarget = palette.getOrNull(1)
        ?: settledPalette.getOrNull(1)
        ?: scheme.tertiary

    val primary by animateColorAsState(
        targetValue = primaryTarget,
        animationSpec = tween(durationMillis = 700),
        label = "appleImmersivePrimary",
    )
    val secondary by animateColorAsState(
        targetValue = secondaryTarget,
        animationSpec = tween(durationMillis = 760),
        label = "appleImmersiveSecondary",
    )

    Box(
        modifier = modifier.fillMaxSize().drawWithCache {
            val bottom = lerp(primary, Color(0xFF090B12), .75f)
            val middle = lerp(primary, Color(0xFF15131F), .44f)
            val supporting = lerp(secondary, Color(0xFF15111D), .57f)
            val vertical = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    .24f to Color.Transparent,
                    .40f to middle.copy(alpha = .09f),
                    .55f to supporting.copy(alpha = .28f),
                    .70f to middle.copy(alpha = .76f),
                    .84f to bottom.copy(alpha = .96f),
                    1f to bottom,
                ),
            )
            val glow = Brush.radialGradient(
                colors = listOf(
                    secondary.copy(alpha = .19f),
                    Color.Transparent,
                ),
                center = Offset(size.width * .5f, size.height * .71f),
                radius = size.width * 1.2f,
            )
            onDrawBehind {
                drawRect(vertical)
                drawRect(glow)
            }
        },
    )
}
