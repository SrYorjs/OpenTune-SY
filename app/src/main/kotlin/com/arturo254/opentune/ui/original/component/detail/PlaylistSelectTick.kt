package com.arturo254.opentune.ui.original.component.detail
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.foundation.R
private val PillContent = Color(0xFF2F2F2F)
@Composable
fun PlaylistSelectTick(checked: Boolean, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "selectTick",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(36.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .drawBehind {
                    val p = progress.coerceIn(0f, 1f)
                    val ring = 1.5.dp.toPx()
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f * (1f - p)),
                        radius = (size.minDimension - ring) / 2f,
                        style = Stroke(ring),
                    )
                    // the spring overshoots, so the disc swells a hair past its size and settles
                    drawCircle(color = Color.White, radius = size.minDimension / 2f * progress.coerceAtLeast(0f))
                },
        ) {
            Icon(
                painter = painterResource(R.drawable.original_check),
                contentDescription = if (checked) "Selected" else "Not selected",
                tint = PillContent,
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer {
                        val p = progress.coerceAtLeast(0f)
                        alpha = p.coerceAtMost(1f)
                        scaleX = 0.4f + 0.6f * p
                        scaleY = 0.4f + 0.6f * p
                    },
            )
        }
    }
}
