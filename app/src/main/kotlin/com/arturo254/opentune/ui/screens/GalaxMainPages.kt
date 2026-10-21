package com.arturo254.opentune.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
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

/** Main routes share primitives, never a page template or a translucent root. */
@Composable
fun GalaxMainPages(navController: NavController, pager: PagerState, onOpenSearch: () -> Unit) {
    val scope = rememberCoroutineScope()
    val reduced = OpenTuneDesignSystem.galaxMotion.reducedMotion
    fun open(page: Int) {
        scope.launch {
            if (reduced) pager.scrollToPage(page) else pager.animateScrollToPage(page)
        }
    }
    BackHandler(pager.currentPage != 0) { open(0) }
    HorizontalPager(state = pager,
        key = { Screens.MainScreens[it].route },
        beyondViewportPageCount = 0,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) { page ->
        // Only the settled visible page spends frames on ambient animation.
        val active = pager.settledPage == page && !pager.isScrollInProgress
        when (page) {
            0 -> HomeScreen(navController, ambientActive = active,
                onOpenSearchTab = { open(1) }, onOpenLibraryTab = { open(2) })
            1 -> GalaxDiscoverScreen(navController, onOpenSearch, ambientActive = active)
            2 -> LibraryScreen(navController, ambientActive = active,
                onOpenSearchTab = { open(1) })
        }
    }
}

internal enum class NovaSpace { HORIZON, OBSERVATORY, COLLECTION, RESULTS }

/** A single slow clock per visible scene; no particle coroutines or background work. */
@Composable
internal fun rememberNovaPhase(active: Boolean): State<Float> {
    val motion = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var visible by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            visible = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val phase = remember { Animatable(0f) }
    LaunchedEffect(active, visible, motion) {
        if (!active || !visible || motion) return@LaunchedEffect
        while (currentCoroutineContext().isActive) {
            if ((currentCoroutineContext()[MotionDurationScale]?.scaleFactor ?: 1f) <= 0f) break
            phase.animateTo(1f, tween(48000, easing = LinearEasing))
            phase.snapTo(0f)
        }
    }
    return phase.asState()
}

/** Opaque roots prevent the previous pager page from leaking behind the mini player. */
@Composable
internal fun NovaBackdrop(
    space: NovaSpace,
    active: Boolean,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
) {
    val phase = rememberNovaPhase(active)
    val colors = MaterialTheme.colorScheme
    val base = if (pureBlack) Color.Black else colors.background
    val dark = base.luminance() < .4f
    val blue = if (dark) Color(0xFF79A8FF) else Color(0xFF345AB6)
    val secondary = if (dark) Color(0xFFA994E9) else Color(0xFF70509B)
    Canvas(modifier.background(base)) {
        val t = phase.value * (2f * PI.toFloat())
        val focal = when (space) {
            NovaSpace.HORIZON -> Offset(size.width * .5f, size.height * .28f)
            NovaSpace.OBSERVATORY -> Offset(size.width * .85f, size.height * .22f)
            NovaSpace.COLLECTION -> Offset(size.width * .08f, size.height * .1f)
            NovaSpace.RESULTS -> Offset(size.width * .95f, 0f)
        }
        drawRect(Brush.radialGradient(
            listOf(blue.copy(alpha = if (dark) .12f else .075f), Color.Transparent),
            center = focal, radius = size.maxDimension * .65f,
        ))
        if (space == NovaSpace.COLLECTION) {
            // Vertical light columns instead of the curved horizon used at home.
            repeat(6) { index ->
                val x = size.width * (index + .5f) / 6f
                drawLine(blue.copy(alpha = .045f), Offset(x, 0f),
                    Offset(x, size.height * .65f), 1.dp.toPx())
            }
        }
        val count = if (space == NovaSpace.RESULTS) 12 else 32
        repeat(count) { index ->
            val x = ((index * 71 + 19) % 997) / 997f * size.width
            val y = ((index * 137 + 53) % 991) / 991f * size.height * .72f
            val drift = sin(t + index) * 3.dp.toPx()
            val alpha = .10f + .11f * (.5f + .5f * cos(t + index * .7f))
            drawCircle(if (index % 4 == 0) secondary.copy(alpha = alpha) else blue.copy(alpha = alpha),
                radius = if (index % 5 == 0) 1.dp.toPx() else .65.dp.toPx(),
                center = Offset(x + drift, y))
        }
        drawRect(Brush.verticalGradient(
            0f to Color.Transparent, .62f to Color.Transparent, 1f to base,
        ))
    }
}

@Composable
internal fun NovaIcon(icon: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun NovaHeading(title: String, detail: String? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (detail != null) Text(detail.uppercase(), style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.8.sp, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
internal fun NovaArtwork(url: String?, modifier: Modifier, circular: Boolean = false) {
    Box(modifier.clip(if (circular) CircleShape else RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.music_note), null, tint = MaterialTheme.colorScheme.primary.copy(alpha = .3f),
            modifier = Modifier.size(28.dp))
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize())
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun NovaMusicRow(
    title: String, subtitle: String, artwork: String?, onClick: () -> Unit,
    modifier: Modifier = Modifier, active: Boolean = false, playing: Boolean = false,
    onMenu: (() -> Unit)? = null, circular: Boolean = false,
) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = .09f) else Color.Transparent)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onMenu)
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        NovaArtwork(artwork, Modifier.size(54.dp), circular)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2,
                overflow = TextOverflow.Ellipsis, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (active) Icon(painterResource(if (playing) R.drawable.pause else R.drawable.play),
            contentDescription = if (playing) "Pausar" else "Reproducir", tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp))
        if (onMenu != null) NovaIcon(R.drawable.more_vert, "Opciones de $title", onMenu)
        else Spacer(Modifier.width(10.dp))
    }
}

@Composable
internal fun NovaMessage(title: String, message: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null && onAction != null) OutlinedButton(onClick = onAction) { Text(action) }
    }
}

@Composable
internal fun NovaLoading(label: String) {
    Row(Modifier.fillMaxWidth().padding(28.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Entry motion is finite and honours the same reduced-motion policy as the player. */
@Composable
internal fun NovaEntrance(content: @Composable () -> Unit) {
    val reduced = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val opacity = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        if (reduced) opacity.snapTo(1f) else opacity.animateTo(1f, tween(420))
    }
    Box(Modifier.graphicsLayer { this.alpha = opacity.value; translationY = (1f - opacity.value) * 12.dp.toPx() }) {
        content()
    }
}
