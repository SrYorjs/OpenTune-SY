package com.arturo254.opentune.ui.visual.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.ui.visual.search.SearchColors
import kotlinx.coroutines.delay

private const val FinishedLingerMs = 2500L

@Composable
private fun rememberCardVisible(running: Boolean): Boolean {
    var lingering by remember { mutableStateOf(false) }
    LaunchedEffect(running) {
        if (running) {
            lingering = true
        } else {
            delay(FinishedLingerMs)
            lingering = false
        }
    }
    return running || lingering
}

@Composable
fun LibraryProgressCard(
    title: String,
    running: Boolean,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = rememberCardVisible(running),
        enter = fadeIn(tween(220)) + expandVertically(tween(280)),
        exit = fadeOut(tween(180)) + shrinkVertically(tween(260)),
        modifier = modifier,
    ) {
        val fraction = animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(350),
            label = "libraryProgressFraction",
        )
        Column(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SearchColors.Tile)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = SearchColors.Primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = SearchColors.Primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))
            ProgressBar(fraction)
        }
    }
}

@Composable
internal fun ProgressBar(fraction: State<Float>, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White.copy(alpha = 0.16f))
            .drawBehind {
                drawRoundRect(
                    color = Color.White,
                    size = Size(size.width * fraction.value.coerceIn(0.02f, 1f), size.height),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            },
    )
}
