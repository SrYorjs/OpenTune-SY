package com.arturo254.opentune.ui.visual.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.flow.collect
import kotlin.math.roundToInt
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.arturo254.opentune.models.MediaMetadata
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
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
    val album: String? = null,
    val canvasEligible: Boolean = false,
)

private object PeekPageSize : PageSize {
    override fun androidx.compose.ui.unit.Density.calculateMainAxisPageSize(
        availableSpace: Int,
        pageSpacing: Int,
    ): Int = ((availableSpace - pageSpacing) * 0.8f).roundToInt()
}

private fun Modifier.heroTuck(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val tuck = 7.dp.roundToPx()
    layout(placeable.width, (placeable.height - tuck).coerceAtLeast(0)) {
        placeable.place(0, 0)
    }
}

internal enum class HeroPillKind { Play, Pause, View, Roll, Rolling }
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
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    gachaPool: List<MediaMetadata>,
    currentSongId: String?,
    isPlaying: Boolean,
    onGachaWinner: (MediaMetadata) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val peek = configuration.screenWidthDp >= 600
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    val height = (configuration.screenHeightDp * if (peek && landscape) 0.7f else 0.6f).dp
    val availablePool = remember(gachaPool) { gachaPool.filter { !it.thumbnailUrl.isNullOrEmpty() }.distinctBy { it.id } }
    val showGacha = availablePool.size >= 4
    val totalItems = items.size + if (showGacha) 1 else 0
    val gachaState = remember { GachaState() }
    val context = LocalContext.current
    val gachaSounds = remember { GachaSounds(context.applicationContext) }
    DisposableEffect(gachaSounds) { onDispose { gachaSounds.release() } }
    var gachaRolled by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(
        initialPage = if (totalItems < 2) 0 else (Int.MAX_VALUE / 2) - (Int.MAX_VALUE / 2) % totalItems,
        pageCount = { if (totalItems < 2) 1 else Int.MAX_VALUE },
    )
    val dragged by pagerState.interactionSource.collectIsDraggedAsState()
    var userSwipe by remember { mutableStateOf(false) }
    LaunchedEffect(dragged) { if (dragged) userSwipe = true }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }.collect { moving ->
            if (!moving) userSwipe = false
        }
    }
    val wipeCurve by animateFloatAsState(
        targetValue = if (userSwipe) 1f else 0f,
        animationSpec = tween(260),
        label = "heroWipeCurve",
    )
    val edgeBlur = rememberCoverBlurAvailable()
    LaunchedEffect(totalItems, active, dragged, gachaRolled) {
        if (!active || totalItems < 2 || dragged) return@LaunchedEffect
        while (true) {
            val onGacha = showGacha && pagerState.currentPage % totalItems == items.size
            if (onGacha && gachaRolled) return@LaunchedEffect
            delay(if (onGacha) HeroAdvanceMillis * 2 else HeroAdvanceMillis)
            if (scrollProgress() < 0.08f && !pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(
                    pagerState.currentPage + 1,
                    animationSpec = tween(1150, easing = CubicBezierEasing(0.32f, 0f, 0.15f, 1f)),
                )
            }
        }
    }
    ScrollAwayCover(
        progress = scrollProgress,
        follow = 0.2f,
        softFrom = 0.08f,
        modifier = modifier.heroTuck().fillMaxWidth().height(height),
    ) {
        if (items.isEmpty() && isLoading) {
            HeroBones()
        } else if (items.isEmpty()) {
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
            CompositionLocalProvider(LocalHeroPeek provides peek) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSize = if (peek) PeekPageSize else PageSize.Fill,
                pageSpacing = if (peek) 14.dp else 0.dp,
                beyondViewportPageCount = 1,
            ) { page ->
                val position = page % totalItems
                if (showGacha && position == items.size) {
                    OriginalHomeGachaCard(
                        state = gachaState,
                        sounds = gachaSounds,
                        pool = availablePool,
                        pageOffset = { (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction },
                        wipeCurve = { wipeCurve },
                        transitionBlur = edgeBlur,
                        held = {
                            !active || pagerState.currentPage != page ||
                                pagerState.isScrollInProgress || scrollProgress() >= 0.08f
                        },
                        currentSongId = currentSongId,
                        isPlaying = isPlaying,
                        onWinner = { winner ->
                            gachaRolled = true
                            onGachaWinner(winner)
                        },
                    )
                } else {
                val entry = items[position]
                PageWipe(
                    pageOffset = { (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction },
                    curve = { wipeCurve },
                    blur = edgeBlur,
                    peek = peek,
                ) {
                    val mediaLayer = rememberGraphicsLayer()
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier.fillMaxSize().then(
                                if (edgeBlur && !peek) Modifier.drawWithContent {
                                    mediaLayer.record { this@drawWithContent.drawContent() }
                                    drawLayer(mediaLayer)
                                } else Modifier
                            ),
                        ) {
                            OriginalHomeCanvas(
                                id = entry.id,
                                title = entry.title,
                                artist = entry.subtitle,
                                album = entry.album,
                                artwork = entry.artwork,
                                eligibleForCanvas = entry.canvasEligible,
                                active = active &&
                                    !pagerState.isScrollInProgress &&
                                    pagerState.currentPage == page &&
                                    scrollProgress() < 0.08f,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        if (edgeBlur && !peek) {
                            HeroBlurBand(mediaLayer, atTop = true, heightFraction = 0.46f, radius = 22f)
                            HeroBlurBand(mediaLayer, atTop = false, heightFraction = 0.5f, radius = 30f)
                        }
                    }
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
    }
}

@Composable
private fun HeroBones() {
    BonesHost(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .background(Brush.verticalGradient(listOf(BoneColor.copy(alpha = 0.12f), Color.Transparent)))
        ) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart)
                    .padding(start = HomeContentInset, bottom = 64.dp),
            ) {
                Bone(width = 72.dp, height = 14.dp)
                Spacer(Modifier.height(8.dp))
                Bone(width = 220.dp, height = 22.dp)
                Spacer(Modifier.height(13.dp))
                Bone(width = 93.dp, height = 33.dp, shape = RoundedCornerShape(50))
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
internal fun HeroText(
    label: String,
    title: String,
    pill: HeroPillKind,
    onPillClick: () -> Unit,
    modifier: Modifier = Modifier,
    pillEnabled: Boolean = true,
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
        HeroPill(kind = pill, onClick = onPillClick, enabled = pillEnabled)
    }
}

@Composable
private fun HeroPill(kind: HeroPillKind, onClick: () -> Unit, enabled: Boolean) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .pressBounce(interaction)
            .height(33.dp)
            .clip(RoundedCornerShape(50))
            .background(HeroPillFill)
            .clickable(interactionSource = interaction, indication = RemoteFocus, enabled = enabled, onClick = onClick)
    ) {
        AnimatedContent(
            targetState = kind,
            transitionSpec = { heroGlyphSwap() },
            label = "heroPill",
        ) { current ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(33.dp).padding(
                    start = when (current) {
                        HeroPillKind.Play, HeroPillKind.Pause -> 21.dp
                        HeroPillKind.View -> 17.dp
                        HeroPillKind.Roll, HeroPillKind.Rolling -> 11.dp
                    },
                    end = if (current == HeroPillKind.Roll || current == HeroPillKind.Rolling) 13.dp else 17.dp,
                ),
            ) {
                when (current) {
                    HeroPillKind.Play, HeroPillKind.Pause -> {
                        Icon(
                            painter = painterResource(
                                if (current == HeroPillKind.Pause) R.drawable.ic_untitled_pause else R.drawable.ic_untitled_play
                            ),
                            contentDescription = null,
                            tint = HeroPillContent,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                    }
                    HeroPillKind.Roll, HeroPillKind.Rolling -> {
                        GachaDie(spinning = current == HeroPillKind.Rolling)
                        Spacer(Modifier.width(8.dp))
                    }
                    HeroPillKind.View -> Unit
                }
                Text(
                    text = when (current) {
                        HeroPillKind.Play -> "Play"
                        HeroPillKind.Pause -> "Pause"
                        HeroPillKind.View -> "View"
                        HeroPillKind.Roll -> "roll!!"
                        HeroPillKind.Rolling -> "rolling!!"
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
