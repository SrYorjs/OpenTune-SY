package com.arturo254.opentune.ui.mochy.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.arturo254.opentune.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlaylistScreenHeader(
    thumbnailUrl: String?,
    title: String,
    userName: String,
    description: String?,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onMoreClick: () -> Unit,
    onColorExtracted: (Color) -> Unit = {},
    onCoverPositioned: (androidx.compose.ui.geometry.Rect) -> Unit = {},

    headerContentAlpha: Float = 1f,
    morphProgress: Float = 0f,
    accentColor: Color? = null,
    showActions: Boolean = true,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.background

    val animatedColor by animateColorAsState(
        targetValue = accentColor ?: backgroundColor,
        animationSpec = tween(durationMillis = 600),
        label = "PlaylistHeaderColor",
    )

    val contentColor = if (animatedColor.luminance() > 0.5f) Color.Black else Color.White

    val headerHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).dp

    val coverHandedOff = morphProgress > 0.001f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)

    ) {

        if (!thumbnailUrl.isNullOrEmpty()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .graphicsLayer { alpha = if (coverHandedOff) 0f else 1f }
                    .onGloballyPositioned {
                        val pos = it.positionInWindow()
                        onCoverPositioned(
                            androidx.compose.ui.geometry.Rect(
                                pos.x, pos.y, pos.x + it.size.width, pos.y + it.size.height
                            )
                        )
                    }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (coverHandedOff) 0f else 1f }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = headerContentAlpha }
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.25f),
                        0.2f to Color.Transparent,
                        0.6f to Color.Transparent,
                        0.85f to animatedColor.copy(alpha = 0.7f),
                        1.0f to animatedColor,
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .graphicsLayer { alpha = headerContentAlpha }
        ) {

            if (userName.isNotBlank()) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (showActions) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(ACTION_ROW_HEIGHT)
                        .clip(RoundedCornerShape(50))
                        .background(contentColor.copy(alpha = 0.25f))
                        .clickable { onPlayClick() }
                        .padding(horizontal = 42.dp)
                ) {
                    AnimatedContent(targetState = isPlaying, label = "PlayPause") { playing ->
                        Icon(
                            painter = painterResource(if (playing) R.drawable.pause else R.drawable.play),
                            contentDescription = if (playing) "Pause" else "Play",
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPlaying) "Pause" else "Play",
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(ACTION_ROW_HEIGHT)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.15f))
                        .clickable { onShuffleClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = "Shuffle",
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(ACTION_ROW_HEIGHT)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.15f))
                        .clickable { onMoreClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = "Options",
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            }
        }
    }
}

private val ACTION_ROW_HEIGHT = 44.dp
