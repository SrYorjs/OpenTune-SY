package com.arturo254.opentune.ui.visual.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R

@Composable
fun OriginalPlayerSongInfoRow(
    title: String,
    artist: String,
    liked: Boolean,
    onLike: () -> Unit,
    onExpandMenu: () -> Unit,
    onSongInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
    menuExpanded: Boolean = false,
    onTitlePositioned: (androidx.compose.ui.geometry.Rect) -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned { onTitlePositioned(it.boundsInRoot()) }
                .clickable(indication = null, interactionSource = null, onClick = onSongInfoClick),
        ) {
            FadingOverflowText(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
            )
            if (artist.isNotBlank()) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                        .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            PressScaleActionButton(
                icon = if (liked) R.drawable.favorite else R.drawable.favorite_border,
                tint = if (liked) Color.White else Color.White.copy(alpha = 0.85f),
                containerColor = if (liked) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.15f),
                onClick = onLike,
            )
            val chevronRotation by animateFloatAsState(
                targetValue = if (menuExpanded) 180f else 0f,
                animationSpec = tween(270, easing = FastOutSlowInEasing),
                label = "playerChevronRotation",
            )
            PressScaleActionButton(
                icon = R.drawable.expand_more,
                tint = Color.White,
                containerColor = Color.White.copy(alpha = 0.15f),
                onClick = onExpandMenu,
                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
            )
        }
    }
}

private val TitleEdgeFade = 10.dp

private fun Modifier.trailingFade(cut: (shownWidth: Float) -> Boolean = { true }): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val fade = Brush.horizontalGradient(
            0f to Color.Black,
            0.82f to Color.Black,
            1f to Color.Transparent,
        )
        onDrawWithContent {
            drawContent()
            if (cut(size.width)) drawRect(brush = fade, blendMode = BlendMode.DstIn)
        }
    }

@Composable
internal fun FadingOverflowText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var fullWidth by remember { mutableIntStateOf(0) }
    Text(
        text = text,
        style = style,
        maxLines = 1,
        softWrap = false,
        color = color,
        onTextLayout = { fullWidth = it.size.width },
        modifier = modifier
            .trailingFade { shown -> fullWidth > shown + 0.5f }
            .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
    )
}

@Composable
fun PressScaleActionButton(
    icon: Int,
    tint: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    boldIcon: Boolean = false,
    badgeText: String? = null,
    hasShadow: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 34.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.54f, stiffness = 720f),
        label = "pressScaleActionButton"
    )

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = scale; scaleY = scale }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (hasShadow) Modifier.shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                        else Modifier
                    )
                    .clip(CircleShape)
                    .background(containerColor)
            ) {

                if (boldIcon) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(icon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(tint),
                        modifier = Modifier
                            .size(18.dp)
                            .offset(x = 0.45.dp)
                    )
                }
                androidx.compose.foundation.Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(tint),
                    modifier = Modifier.size(18.dp)
                )
            }
            if (badgeText != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(tint)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.Black,
                        fontSize = 7.sp,
                        lineHeight = 7.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
