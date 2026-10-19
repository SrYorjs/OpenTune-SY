package com.arturo254.opentune.ui.player.layout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.core.designsystem.component.GalaxOrbitField
import com.arturo254.opentune.core.designsystem.theme.GalaxVisualKit

@Composable
fun GalaxPlayerLandscapeScene(
    accent: Color,
    isPlaying: Boolean,
    bottomInset: Dp,
    artwork: @Composable BoxScope.() -> Unit,
    controls: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        GalaxOrbitField(
            modifier = Modifier.fillMaxSize(),
            accent = accent,
            active = isPlaying,
            intensity = .33f,
        )
        Row(
            modifier = Modifier.fillMaxSize()
                .padding(bottom = bottomInset, start = 15.dp, end = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                        .drawWithCache {
                            val halo = Brush.radialGradient(
                                0f to accent.copy(alpha = .20f),
                                1f to Color.Transparent,
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width.coerceAtLeast(1f) * .58f,
                            )
                            onDrawBehind { drawRect(halo) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    artwork()
                }
                Text(
                    text = "GALAXOT   /   AUDIO UNIVERSE",
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .75f),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .43f),
                                Color.Transparent,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    content = controls,
                )
            }
        }
    }
}
