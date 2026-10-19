package com.arturo254.opentune.ui.player.visual

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.core.designsystem.component.GalaxOrbitField
import com.arturo254.opentune.core.designsystem.theme.GalaxVisualKit
import kotlin.math.min

@Composable
fun GalaxPlayerSceneOverlay(
    index: String,
    subtitle: String,
    isPlaying: Boolean,
    landscape: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        GalaxOrbitField(
            modifier = Modifier.fillMaxSize(),
            active = isPlaying,
            accent = accent,
            intensity = if (landscape) .24f else .33f,
        )
        Row(
            modifier = Modifier.align(Alignment.TopStart)
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                    ),
                )
                .padding(
                    start = if (landscape) 23.dp else 25.dp,
                    end = 20.dp,
                    top = if (landscape) 12.dp else 20.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GalaxSceneInsignia(index = index, accent = accent)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "GALAXOT  /  $subtitle",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = .94f),
                    letterSpacing = 1.35.sp,
                )
                Text(
                    text = if (isPlaying) "●  AUDIO EN TRANSMISIÓN" else "◌  UNIVERSO EN PAUSA",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = .65f),
                    letterSpacing = .7.sp,
                )
            }
        }
        Box(
            modifier = Modifier.align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                .padding(top = 22.dp, end = 26.dp)
                .size(9.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color.White, accent.copy(alpha = .57f)),
                    ),
                ),
        )
    }
}

@Composable
private fun GalaxSceneInsignia(
    index: String,
    accent: Color,
) {
    Box(
        modifier = Modifier.size(44.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color.Black.copy(alpha = .36f)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(7.dp)) {
            val radius = min(size.width, size.height) * .38f
            drawCircle(
                color = accent.copy(alpha = .63f),
                radius = radius,
                style = Stroke(width = 1.1.dp.toPx()),
            )
            drawArc(
                color = Color.White.copy(alpha = .88f),
                startAngle = -100f,
                sweepAngle = 141f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(2 * radius, 2 * radius),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
        Text(
            text = index,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
