package com.arturo254.opentune.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import com.arturo254.opentune.ui.screens.library.LibraryScreen
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GalaxMainPages(
    navController: NavController,
    pager: PagerState,
    onOpenSearch: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val reduced = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val navigatePage: (Int) -> Unit = { page ->
        scope.launch {
            if (reduced) pager.scrollToPage(page)
            else pager.animateScrollToPage(page)
        }
    }
    BackHandler(pager.currentPage != 0) { navigatePage(0) }
    HorizontalPager(
        state = pager,
        key = { Screens.MainScreens[it].route },
        beyondViewportPageCount = 0,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) { page ->
        val active = pager.settledPage == page && !pager.isScrollInProgress
        when (page) {
            0 -> HomeScreen(
                navController = navController,
                ambientActive = active,
                onOpenSearchTab = { navigatePage(1) },
                onOpenLibraryTab = { navigatePage(2) },
            )
            1 -> GalaxDiscoverScreen(
                navController = navController,
                onOpenSearch = onOpenSearch,
                ambientActive = active,
            )
            2 -> LibraryScreen(
                navController = navController,
                ambientActive = active,
                onOpenSearchTab = { navigatePage(1) },
            )
        }
    }
}

internal enum class AsterScene { SESSION, EXPLORE, VAULT, SEARCH }

@Immutable
internal data class AsterPalette(
    val violet: Color,
    val ice: Color,
    val rose: Color,
    val base: Color,
    val panel: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
)

@Composable
internal fun asterPalette(): AsterPalette {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < .4f
    return if (dark) AsterPalette(
        violet = Color(0xFFC6B5FF),
        ice = Color(0xFF9DE4E5),
        rose = Color(0xFFF1BDCE),
        base = scheme.background,
        panel = lerp(scheme.surfaceContainer, Color(0xFF211C34), .45f),
        ink = scheme.onSurface,
        muted = scheme.onSurfaceVariant,
        line = scheme.onSurface.copy(alpha = .11f),
    ) else AsterPalette(
        violet = Color(0xFF6040A4),
        ice = Color(0xFF17686F),
        rose = Color(0xFF974465),
        base = scheme.background,
        panel = lerp(scheme.surfaceContainer, Color(0xFFF0EBF8), .45f),
        ink = scheme.onSurface,
        muted = scheme.onSurfaceVariant,
        line = scheme.onSurface.copy(alpha = .14f),
    )
}

@Composable
internal fun rememberAsterPhase(active: Boolean): State<Float> {
    val reduced = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val phase = remember { Animatable(0f) }
    LaunchedEffect(active, resumed, reduced) {
        if (!active || !resumed || reduced) return@LaunchedEffect
        while (currentCoroutineContext().isActive) {
            if ((currentCoroutineContext()[MotionDurationScale]?.scaleFactor ?: 1f) <= 0f) break
            val remaining = ((1f - phase.value) * 60000).toInt().coerceAtLeast(1)
            phase.animateTo(1f, tween(remaining, easing = LinearEasing))
            phase.snapTo(0f)
        }
    }
    return phase.asState()
}

private data class AsterStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val offset: Float,
)

@Composable
internal fun AsterBackdrop(
    scene: AsterScene,
    phase: State<Float>,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
) {
    val palette = asterPalette()
    val stars = remember(scene) {
        val random = Random(487 + scene.ordinal)
        List(if (scene == AsterScene.SEARCH) 16 else 52) {
            AsterStar(random.nextFloat(), random.nextFloat(), .4f + random.nextFloat(), random.nextFloat() * 6.28f)
        }
    }
    val base = if (pureBlack) Color.Black else palette.base
    Canvas(modifier.background(base)) {
        val t = phase.value * 2f * PI.toFloat()
        val focus = when (scene) {
            AsterScene.SESSION -> Offset(size.width * .9f, size.height * .12f)
            AsterScene.EXPLORE -> Offset(size.width * .1f, size.height * .42f)
            AsterScene.VAULT -> Offset(size.width * .8f, size.height * .04f)
            AsterScene.SEARCH -> Offset(size.width, 0f)
        }
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(palette.violet.copy(alpha = .12f), Color.Transparent),
                center = focus,
                radius = size.maxDimension * .7f,
            ),
        )
        if (scene == AsterScene.EXPLORE) {
            val path = Path().apply {
                moveTo(-size.width * .2f, size.height * .12f)
                cubicTo(size.width * .8f, size.height * .1f, size.width * .2f, size.height * .6f, size.width * 1.2f, size.height * .58f)
            }
            drawPath(path, palette.ice.copy(alpha = .065f), style = Stroke(1.dp.toPx()))
        }
        stars.forEachIndexed { index, star ->
            val center = Offset(
                size.width * star.x + sin(t + star.offset) * 3.dp.toPx(),
                size.height * star.y,
            )
            val opacity = .12f + (.5f + .5f * sin(t + star.offset)) * .2f
            val tint = if (index % 3 == 0) palette.ice else palette.violet
            drawCircle(tint.copy(alpha = opacity), star.radius.dp.toPx(), center)
            if (index % 13 == 0) {
                val length = 3.dp.toPx()
                drawLine(tint.copy(alpha = opacity * .4f), center - Offset(length, 0f), center + Offset(length, 0f), .5.dp.toPx())
                drawLine(tint.copy(alpha = opacity * .4f), center - Offset(0f, length), center + Offset(0f, length), .5.dp.toPx())
            }
        }
        drawRect(Brush.verticalGradient(0f to Color.Transparent, .45f to Color.Transparent, 1f to base))
    }
}

internal fun DrawScope.drawAsterGlyph(
    kind: Int,
    tint: Color,
    phase: Float = 0f,
) {
    val center = Offset(size.width * .67f, size.height * .5f)
    val radius = size.minDimension * .42f
    when (kind % 4) {
        0 -> rotate(-28f, center) {
            repeat(9) { ring ->
                val scale = .28f + ring * .105f
                drawOval(
                    color = tint.copy(alpha = .1f + ring * .035f),
                    topLeft = center - Offset(radius * scale, radius * scale * .38f),
                    size = Size(radius * scale * 2, radius * scale * .76f),
                    style = Stroke(if (ring == 7) 2.dp.toPx() else .8.dp.toPx()),
                )
            }
            val angle = phase * 6.283f
            val point = center + Offset(cos(angle) * radius, sin(angle) * radius * .38f)
            drawCircle(tint.copy(alpha = .12f), 8.dp.toPx(), point)
            drawCircle(tint, 2.dp.toPx(), point)
        }
        1 -> {
            repeat(17) { band ->
                val path = Path()
                for (step in 0..32) {
                    val x = size.width * .25f + step / 32f * size.width * .8f
                    val wave = sin(step / 32f * 6.28f + band * .13f + phase * 6.28f)
                    val y = size.height * .2f + band * size.height * .033f + wave * size.height * .13f
                    if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, tint.copy(alpha = .12f + band * .018f), style = Stroke(.8.dp.toPx()))
            }
        }
        2 -> rotate(24f, center) {
            repeat(8) { index ->
                val edge = radius * (.35f + index * .16f)
                drawRect(
                    tint.copy(alpha = .12f + index * .025f),
                    center - Offset(edge, edge),
                    Size(edge * 2, edge * 2),
                    style = Stroke(.8.dp.toPx()),
                )
            }
            drawCircle(tint.copy(alpha = .7f), 3.dp.toPx(), center)
        }
        else -> {
            repeat(8) { ray ->
                val angle = ray * PI.toFloat() / 4 + phase * 6.28f
                val end = center + Offset(cos(angle), sin(angle)) * radius
                drawLine(tint.copy(alpha = .28f), center, end, .8.dp.toPx())
                drawCircle(tint.copy(alpha = .7f), 2.dp.toPx(), end)
            }
            repeat(3) { ring ->
                drawCircle(tint.copy(alpha = .12f), radius * (.35f + ring * .3f), center, style = Stroke(.8.dp.toPx()))
            }
        }
    }
}

@Composable
internal fun AsterIcon(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(painterResource(icon), label, Modifier.size(21.dp))
    }
}

@Composable
internal fun AsterEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Medium,
        color = asterPalette().violet,
    )
}

@Composable
internal fun AsterHeading(
    title: String,
    detail: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val palette = asterPalette()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(3.dp).height(30.dp).background(palette.violet, CircleShape))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (!detail.isNullOrBlank()) AsterEyebrow(detail.uppercase())
            Text(
                title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-.6).sp,
            )
        }
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action, color = palette.violet) }
        }
    }
}

@Composable
internal fun AsterArtwork(
    url: String?,
    modifier: Modifier,
    circular: Boolean = false,
) {
    val palette = asterPalette()
    val shape = if (circular) CircleShape else RoundedCornerShape(12.dp)
    Box(
        modifier = modifier.clip(shape).background(palette.panel),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.linearGradient(listOf(palette.violet.copy(alpha = .18f), palette.ice.copy(alpha = .08f))))
            drawAsterGlyph(0, palette.violet.copy(alpha = .5f))
        }
        Icon(
            painter = painterResource(R.drawable.music_note),
            contentDescription = null,
            tint = palette.violet,
            modifier = Modifier.size(24.dp),
        )
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
internal fun AsterChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = asterPalette()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) palette.violet.copy(alpha = .17f) else palette.panel.copy(alpha = .65f))
            .border(1.dp, if (selected) palette.violet.copy(alpha = .5f) else palette.line, RoundedCornerShape(14.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (selected) Box(Modifier.size(5.dp).background(palette.violet, CircleShape))
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) palette.violet else palette.muted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
internal fun AsterPlaybackMark(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = asterPalette()
    Icon(
        painterResource(if (playing) R.drawable.pause else R.drawable.play),
        contentDescription = if (playing) "Pausar" else "Reproducir",
        tint = palette.violet,
        modifier = modifier.size(22.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AsterMusicRow(
    title: String,
    subtitle: String,
    artwork: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    playing: Boolean = false,
    onMenu: (() -> Unit)? = null,
    circular: Boolean = false,
    number: Int? = null,
) {
    val palette = asterPalette()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) palette.violet.copy(alpha = .1f) else Color.Transparent)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onMenu)
            .padding(start = 12.dp, end = 2.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (number != null) {
            Text(
                text = number.toString().padStart(2, '0'),
                modifier = Modifier.width(24.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (active) palette.violet else palette.muted,
            )
        }
        Box {
            AsterArtwork(artwork, Modifier.size(52.dp), circular)
            if (active) {
                Box(
                    Modifier.matchParentSize().background(Color.Black.copy(alpha = .45f), if (circular) CircleShape else RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) { AsterPlaybackMark(playing) }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = if (active) palette.violet else palette.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onMenu != null) AsterIcon(R.drawable.more_vert, "Opciones de $title", onMenu)
        else Spacer(Modifier.width(12.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AsterRecordCard(
    title: String,
    subtitle: String,
    artwork: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMenu: (() -> Unit)? = null,
    active: Boolean = false,
    playing: Boolean = false,
    busy: Boolean = false,
    circular: Boolean = false,
    index: Int? = null,
) {
    val palette = asterPalette()
    Column(
        modifier = modifier
            .width(164.dp)
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onMenu),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            AsterArtwork(artwork, Modifier.fillMaxSize(), circular)
            if (index != null) {
                Surface(
                    color = palette.base.copy(alpha = .94f),
                    shape = RoundedCornerShape(bottomEnd = 12.dp),
                    modifier = Modifier.align(Alignment.TopStart),
                ) {
                    Text(
                        index.toString().padStart(2, '0'),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.violet,
                    )
                }
            }
            if (active || busy) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(8.dp).size(38.dp)
                        .background(palette.base.copy(alpha = .94f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = palette.violet)
                    else AsterPlaybackMark(playing)
                }
            }
        }
        Column(Modifier.padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun AsterMessage(
    title: String,
    message: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val palette = asterPalette()
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp)
            .border(1.dp, palette.line, RoundedCornerShape(24.dp))
            .background(palette.panel.copy(alpha = .6f), RoundedCornerShape(24.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Canvas(Modifier.size(54.dp)) { drawAsterGlyph(3, palette.violet) }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = palette.muted)
        if (action != null && onAction != null) {
            FilledTonalButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
internal fun AsterLoading(label: String) {
    val palette = asterPalette()
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = palette.violet)
            Text(label, style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }
        repeat(3) { index ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(50.dp).background(palette.panel, RoundedCornerShape(12.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(Modifier.fillMaxWidth(.7f - index * .1f).height(11.dp).background(palette.line, CircleShape))
                    Box(Modifier.fillMaxWidth(.4f).height(8.dp).background(palette.line.copy(alpha = .06f), CircleShape))
                }
            }
        }
    }
}

@Composable
internal fun AsterEntrance(content: @Composable () -> Unit) {
    val reduced = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        if (reduced) progress.snapTo(1f)
        else progress.animateTo(1f, tween(520))
    }
    Box(Modifier.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 16.dp.toPx()
    }) { content() }
}

@Composable
internal fun LazyItemScope.asterItemMotion(modifier: Modifier = Modifier): Modifier {
    return if (OpenTuneDesignSystem.galaxMotion.reducedMotion) modifier
    else modifier.animateItem()
}
