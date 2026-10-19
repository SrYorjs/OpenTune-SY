package com.arturo254.opentune.ui.player.layout

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.component.GalaxOrbitField
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import com.arturo254.opentune.core.designsystem.theme.GalaxVisualKit
import kotlin.math.min

@Composable
fun GalaxPlayerClassicScene(
    isPlaying: Boolean,
    accent: Color,
    queuePeekHeight: Dp,
    modifier: Modifier = Modifier,
    artwork: @Composable BoxScope.() -> Unit,
    controls: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(bottom = queuePeekHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        GalaxPlayerSceneMasthead(
            accent = accent,
            active = isPlaying,
        )
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            GalaxOrbitField(
                modifier = Modifier.fillMaxSize(),
                active = isPlaying,
                intensity = .66f,
                accent = accent,
            )
            Box(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 19.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                artwork()
            }
            GalaxPlayerCornerCoordinates(
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 27.dp, bottom = 13.dp),
                accent = accent,
            )
            Box(
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(top = 15.dp, end = 26.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = .84f)),
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(topStart = 33.dp, topEnd = 33.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .drawWithCache {
                    val wash = Brush.verticalGradient(
                        listOf(
                            scheme.surfaceContainerLow.copy(alpha = .55f),
                            scheme.surface.copy(alpha = .32f),
                            scheme.surface.copy(alpha = .05f),
                        ),
                    )
                    val glow = Brush.radialGradient(
                        0f to accent.copy(alpha = .13f),
                        1f to Color.Transparent,
                        center = Offset(size.width * .87f, 0f),
                        radius = size.width.coerceAtLeast(1f) * .8f,
                    )
                    onDrawBehind {
                        drawRect(wash)
                        drawRect(glow)
                    }
                },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = controls,
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun GalaxPlayerSceneMasthead(
    accent: Color,
    active: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 15.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(29.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = .13f)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(23.dp)) {
                val radius = size.minDimension * .36f
                drawCircle(
                    accent.copy(alpha = .76f),
                    radius = radius,
                    style = Stroke(width = 1.dp.toPx()),
                )
                drawArc(
                    Color.White.copy(alpha = .76f),
                    startAngle = -100f,
                    sweepAngle = 113f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(2f * radius, 2f * radius),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
                drawCircle(accent, radius = 2.dp.toPx())
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "GALAXOT  /  PLAYER",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                color = accent,
            )
            Text(
                text = if (active) "LA MÚSICA SIGUE EN ÓRBITA" else "TU UNIVERSO MUSICAL",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                letterSpacing = .5.sp,
            )
        }
        Box(
            Modifier.clip(GalaxVisualKit.pillShape)
                .background(accent.copy(alpha = .13f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = if (active) "●  LIVE" else "◌  READY",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun GalaxPlayerCornerCoordinates(
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(4.dp).background(accent, CircleShape),
        )
        Text(
            text = "GΛLΛX • AUDIO UNIVERSE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .64f),
            letterSpacing = 1.5.sp,
        )
    }
}
