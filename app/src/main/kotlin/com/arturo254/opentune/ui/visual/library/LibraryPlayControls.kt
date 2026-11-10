package com.arturo254.opentune.ui.visual.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.using
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.visual.component.pressBounce
import com.arturo254.opentune.ui.visual.search.SearchHorizontalPadding
import com.arturo254.opentune.ui.visual.theme.InterFontFamily
import kotlinx.coroutines.launch

private val PillFill = Color.White.copy(alpha = 0.64f)
private val PillContent = Color(0xFF2F2F2F)
private val PillLabelStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    letterSpacing = (-0.06).em,
)

@Composable
internal fun PlayPill(isPlaying: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .pressBounce(interaction)
            .height(33.dp)
            .clip(RoundedCornerShape(50))
            .background(PillFill)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .animateContentSize(spring(dampingRatio = 0.7f, stiffness = 500f))
            .padding(start = 21.dp, end = 17.dp)
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = { glyphSwap() },
            label = "playPillGlyph",
        ) { playing ->
            Icon(
                painter = painterResource(if (playing) R.drawable.ic_untitled_pause else R.drawable.ic_untitled_play),
                contentDescription = null,
                tint = PillContent,
                modifier = Modifier.size(15.dp),
            )
        }
        Spacer(Modifier.width(9.dp))
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = { labelSwap() },
            label = "playPillLabel",
        ) { playing ->
            Text(
                text = if (playing) "Pause" else "Play",
                style = PillLabelStyle,
                color = PillContent,
            )
        }
    }
}

@Composable
internal fun CircleAction(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    spinOnClick: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val spin = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .pressBounce(interaction)
            .size(33.dp)
            .clip(CircleShape)
            .background(PillFill)
            .clickable(interactionSource = interaction, indication = null) {
                if (spinOnClick) {
                    scope.launch {
                        spin.snapTo(0f)
                        spin.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
                    }
                }
                onClick()
            }
    ) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = { glyphSwap() },
            label = "circleActionIcon",
        ) { res ->
            Icon(
                painter = painterResource(res),
                contentDescription = contentDescription,
                tint = PillContent,
                modifier = Modifier
                    .size(17.dp)
                    .graphicsLayer { rotationZ = spin.value * 360f },
            )
        }
    }
}

private fun <S> AnimatedContentTransitionScope<S>.glyphSwap(): ContentTransform =
    (scaleIn(initialScale = 0.4f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f)) + fadeIn(tween(120)))
        .togetherWith(scaleOut(targetScale = 0.4f, animationSpec = tween(110)) + fadeOut(tween(90)))
        .using(SizeTransform(clip = false))

private fun <S> AnimatedContentTransitionScope<S>.labelSwap(): ContentTransform =
    (slideInVertically(spring(dampingRatio = 0.75f, stiffness = 600f)) { it / 2 } + fadeIn(tween(140)))
        .togetherWith(slideOutVertically(tween(120)) { -it / 2 } + fadeOut(tween(100)))
        .using(SizeTransform(clip = false))

@Composable
fun LibraryPlayBar(
    onPlay: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = SearchHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayPill(isPlaying = false, onClick = onPlay)
        Spacer(modifier = Modifier.width(9.dp))
        CircleAction(
            icon = R.drawable.shuffle,
            contentDescription = "Shuffle",
            onClick = onMore,
            spinOnClick = true,
        )
    }
}
