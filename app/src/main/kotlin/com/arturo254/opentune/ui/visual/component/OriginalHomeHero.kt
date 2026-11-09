package com.arturo254.opentune.ui.visual.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

data class OriginalHomeHeroItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val artwork: String?,
    val label: String,
    val isCurrent: Boolean,
    val isPlaying: Boolean,
    val opensPage: Boolean = false,
    val onOpen: () -> Unit,
)

private enum class HeroPillKind { Play, Pause, View }
private const val HeroAdvanceMillis = 5500L
private const val HeroDim = 0.36f
private val HeroLabelColor = Color.White.copy(alpha = 0.69f)
private val HeroPillContent = Color(0xFF2F2F2F)
private val HeroPillFill = Color.White.copy(alpha = 0.64f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OriginalHomeHero(
    items: List<OriginalHomeHeroItem>,
    active: Boolean,
    scrollProgress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val height = (LocalConfiguration.current.screenHeightDp * 0.6f).dp
    val pagerState = rememberPagerState(
        initialPage = if (items.size < 2) 0 else (Int.MAX_VALUE / 2) - (Int.MAX_VALUE / 2) % items.size,
        pageCount = { if (items.size < 2) 1 else Int.MAX_VALUE },
    )
    LaunchedEffect(items.size, active) {
        if (!active || items.size < 2) return@LaunchedEffect
        while (true) {
            delay(HeroAdvanceMillis)
            if (scrollProgress() < 0.08f && !pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }
    Box(modifier = modifier.fillMaxWidth().height(height)) {
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().background(Color(0xFF161616))) {
                Column(
                    modifier = Modifier.align(Alignment.BottomStart)
                        .padding(start = HomeContentInset, end = HomeContentInset, bottom = 64.dp)
                ) {
                    Text("Your music", style = HomeLabelStyle, color = HeroLabelColor)
                    Text("Play something to discover your next favorite",
                        style = HomeTitleStyle, color = Color.White, maxLines = 2)
                }
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { page ->
                val entry = items[page % items.size]
                PageWipe(
                    pageOffset = { pagerState.currentPage - page + pagerState.currentPageOffsetFraction },
                    curve = { 0f },
                    blur = true,
                ) {
                    AsyncImage(
                        model = entry.artwork,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    HeroScrims()
                    Box(modifier = Modifier.matchParentSize()) {
                        HeroText(
                            label = entry.label,
                            title = if (entry.subtitle.isNotEmpty()) "${entry.title} - ${entry.subtitle}" else entry.title,
                            pill = when {
                                entry.opensPage -> HeroPillKind.View
                                entry.isCurrent && entry.isPlaying -> HeroPillKind.Pause
                                else -> HeroPillKind.Play
                            },
                            onPillClick = entry.onOpen,
                            modifier = Modifier.align(Alignment.BottomStart),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.HeroScrims() {
    Box(
        modifier = Modifier
            .matchParentSize()
            .drawWithCache {
                val topEnd = 0.463f
                val bottomStart = 0.533f
                val bottomEnd = 0.989f
                val scrim = Brush.verticalGradient(
                    0f to heroScrimBlack(0.86f),
                    0.475f * topEnd to heroScrimBlack(0.59f),
                    topEnd to heroScrimBlack(0f),
                    bottomStart to heroScrimBlack(0f),
                    bottomStart + 0.457f * (bottomEnd - bottomStart) to heroScrimBlack(0.59f),
                    bottomEnd to heroScrimBlack(1f),
                    1f to heroScrimBlack(1f),
                    startY = 0f,
                    endY = size.height,
                )
                onDrawBehind { drawRect(scrim) }
            }
    )
}

private fun heroScrimBlack(gradient: Float): Color =
    Color.Black.copy(alpha = 1f - (1f - HeroDim) * (1f - gradient))

@Composable
private fun HeroText(
    label: String,
    title: String,
    pill: HeroPillKind,
    onPillClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(start = HomeContentInset, end = HomeContentInset, bottom = 64.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = HomeLabelStyle, color = HeroLabelColor, maxLines = 1)
        }
        AnimatedContent(
            targetState = title,
            transitionSpec = { heroLabelSwap() },
            label = "heroTitle",
        ) { text ->
            Text(
                text = text,
                style = HomeTitleStyle,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(11.dp))
        HeroPill(kind = pill, onClick = onPillClick)
    }
}

@Composable
private fun HeroPill(kind: HeroPillKind, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .pressBounce(interaction)
            .height(33.dp)
            .clip(RoundedCornerShape(50))
            .background(HeroPillFill)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        AnimatedContent(
            targetState = kind,
            transitionSpec = { heroGlyphSwap() },
            label = "heroPill",
        ) { current ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(33.dp).padding(
                    start = if (current == HeroPillKind.View) 17.dp else 21.dp,
                    end = 17.dp,
                ),
            ) {
                if (current != HeroPillKind.View) {
                    Icon(
                        painter = painterResource(if (current == HeroPillKind.Pause) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        tint = HeroPillContent,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                }
                Text(
                    text = when (current) {
                        HeroPillKind.Play -> "Play"
                        HeroPillKind.Pause -> "Pause"
                        HeroPillKind.View -> "View"
                    },
                    style = HomeLabelStyle,
                    color = HeroPillContent,
                )
                if (current == HeroPillKind.View) {
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward),
                        contentDescription = null,
                        tint = HeroPillContent,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

private fun <S> AnimatedContentTransitionScope<S>.heroGlyphSwap(): ContentTransform =
    (scaleIn(initialScale = 0.6f, animationSpec = spring(dampingRatio = 0.55f, stiffness = 650f)) + fadeIn(tween(130)))
        .togetherWith(scaleOut(targetScale = 0.6f, animationSpec = tween(110)) + fadeOut(tween(90)))
        .using(SizeTransform(clip = false) { _, _ -> spring(dampingRatio = 0.75f, stiffness = 520f) })

private fun <S> AnimatedContentTransitionScope<S>.heroLabelSwap(): ContentTransform =
    (slideInVertically(spring(dampingRatio = 0.8f, stiffness = 420f)) { it / 3 } + fadeIn(tween(220)))
        .togetherWith(slideOutVertically(tween(160)) { -it / 3 } + fadeOut(tween(140)))
        .using(SizeTransform(clip = false))
