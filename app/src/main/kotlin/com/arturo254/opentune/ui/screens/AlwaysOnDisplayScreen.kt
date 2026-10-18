@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.arturo254.opentune.ui.screens

import androidx.compose.material3.Slider
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidChrome
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AodArtShape
import com.arturo254.opentune.constants.AodArtShapeKey
import com.arturo254.opentune.constants.AodArtSizeKey
import com.arturo254.opentune.constants.AodDarknessKey
import com.arturo254.opentune.constants.AodFullscreenKey
import com.arturo254.opentune.constants.AodShowArtistKey
import com.arturo254.opentune.constants.AodShowControlsKey
import com.arturo254.opentune.constants.AodShowProgressKey
import com.arturo254.opentune.constants.AodShowTimeKey
import com.arturo254.opentune.constants.AodShowTitleKey
import com.arturo254.opentune.constants.AodStyle
import com.arturo254.opentune.constants.AodStyleKey
import com.arturo254.opentune.constants.AodSpotlightIntensityKey
import com.arturo254.opentune.constants.AodSpotlightPulseKey
import com.arturo254.opentune.constants.AodTransitionDurationKey
import com.arturo254.opentune.constants.AodControlStyleKey
import com.arturo254.opentune.constants.AodControlStyle
import com.arturo254.opentune.constants.AodTextScaleKey
import com.arturo254.opentune.constants.AodClockFormatKey
import com.arturo254.opentune.constants.AodShowClockKey
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.ui.screens.settings.toShape
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.utils.rememberPreference
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.saket.squiggles.SquigglySlider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
private fun rememberDominantColor(thumbnailUrl: String?): Color {
    val context = LocalContext.current
    var dominantColor by remember(thumbnailUrl) { mutableStateOf(Color.White) }

    LaunchedEffect(thumbnailUrl) {
        if (thumbnailUrl == null) {
            dominantColor = Color.White
            return@LaunchedEffect
        }
        runCatching {
            val req = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .allowHardware(false)
                .size(128, 128)
                .build()
            val result = context.imageLoader.execute(req)
            if (result is SuccessResult) {
                val bmp: Bitmap = (result.image as? coil3.BitmapImage)?.bitmap
                    ?: return@LaunchedEffect
                Palette.from(bmp).generate { palette ->
                    val swatch = palette?.vibrantSwatch
                        ?: palette?.dominantSwatch
                        ?: palette?.lightVibrantSwatch
                    swatch?.rgb?.let { rgb ->
                        dominantColor = Color(rgb)
                    }
                }
            }
        }
    }
    return dominantColor
}

private data class AodMediaState(
    val id: String?,
    val thumbnailUrl: String?,
    val title: String,
    val artist: String,
)

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun AlwaysOnDisplayScreen(navController: NavController) {
    val playerConnection = LocalPlayerConnection.current ?: run {
        LaunchedEffect(Unit) { navController.navigateUp() }
        return
    }

    val view = LocalView.current
    val context = LocalContext.current
    val activity = context as? Activity
    val activityWindow = activity?.window
    val dialogWindow = (view.parent as? DialogWindowProvider)?.window
    val window = dialogWindow ?: activityWindow

    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    val (fullscreenMode) = rememberPreference(AodFullscreenKey, true)

    DisposableEffect(Unit, fullscreenMode) {
        view.keepScreenOn = true

        if (fullscreenMode && window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            view.keepScreenOn = false
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }
    }

    val (rawStyle) = rememberPreference(AodStyleKey, AodStyle.CLASSIC.name)
    val (rawShape) = rememberPreference(AodArtShapeKey, AodArtShape.ROUNDED.name)
    val (darkness) = rememberPreference(AodDarknessKey, 0.55f)
    val (artSizeFrac) = rememberPreference(AodArtSizeKey, 0.65f)
    val (showTitle) = rememberPreference(AodShowTitleKey, true)
    val (showArtist) = rememberPreference(AodShowArtistKey, true)
    val (showTime) = rememberPreference(AodShowTimeKey, true)
    val (showProgress) = rememberPreference(AodShowProgressKey, true)
    val (showControls) = rememberPreference(AodShowControlsKey, true)
    val (spotlightIntensity) = rememberPreference(AodSpotlightIntensityKey, 0.75f)
    val (spotlightPulse) = rememberPreference(AodSpotlightPulseKey, true)
    val (transitionDuration) = rememberPreference(AodTransitionDurationKey, 700)
    val (rawControlStyle) = rememberPreference(AodControlStyleKey, AodControlStyle.ROUNDED.name)
    val (textScale) = rememberPreference(AodTextScaleKey, 1.0f)
    val (showClock) = rememberPreference(AodShowClockKey, false)
    val (clockFormat24h) = rememberPreference(AodClockFormatKey, true)

    val aodStyle =
        remember(rawStyle) { runCatching { AodStyle.valueOf(rawStyle) }.getOrDefault(AodStyle.CLASSIC) }
    val artShape =
        remember(rawShape) { runCatching { AodArtShape.valueOf(rawShape) }.getOrDefault(AodArtShape.ROUNDED) }
    val controlStyle = remember(rawControlStyle) {
        runCatching { AodControlStyle.valueOf(rawControlStyle) }.getOrDefault(AodControlStyle.ROUNDED)
    }

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val canSkipPrev by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    var position by rememberSaveable(mediaMetadata?.id) { mutableLongStateOf(playerConnection.player.currentPosition) }
    var duration by rememberSaveable(mediaMetadata?.id) { mutableLongStateOf(playerConnection.player.duration) }
    var sliderPosition by remember(mediaMetadata?.id) { mutableStateOf<Long?>(null) }

    LaunchedEffect(mediaMetadata?.id, playbackState, isPlaying, lifecycle) {
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            if (playbackState == STATE_READY) do {
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
                sliderPosition?.let {
                    if (abs(position - it) <= 1_500L) sliderPosition = null
                }
                delay(200L)
            } while (isActive && isPlaying)
        }
    }

    val displayPosition = sliderPosition ?: position
    val progressFraction = remember(displayPosition, duration) {
        if (duration > 0L && duration != C.TIME_UNSET)
            (displayPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    }
    val artistText = remember(mediaMetadata?.artists) {
        mediaMetadata?.artists?.joinToString(", ") { it.name }.orEmpty()
    }

    val mediaState = remember(mediaMetadata?.id, mediaMetadata?.thumbnailUrl, mediaMetadata?.title, artistText) {
        AodMediaState(
            id = mediaMetadata?.id,
            thumbnailUrl = mediaMetadata?.thumbnailUrl,
            title = mediaMetadata?.title.orEmpty(),
            artist = artistText,
        )
    }

    val dominantColor = rememberDominantColor(mediaState.thumbnailUrl)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp.dp
    val artSizeDp: Dp = remember(artSizeFrac, screenWidthDp, isLandscape) {
        val base = if (isLandscape) screenWidthDp * 0.35f else screenWidthDp * artSizeFrac
        base.coerceIn(100.dp, screenWidthDp)
    }

    val onSeekChange: (Float) -> Unit = { f ->
        if (duration > 0L && duration != C.TIME_UNSET)
            sliderPosition = (f * duration).toLong().coerceIn(0L, duration)
    }
    val onSeekFinished: () -> Unit = {
        sliderPosition?.let { playerConnection.player.seekTo(it); position = it }
        sliderPosition = null
    }

    val contentModifier = if (fullscreenMode) Modifier.fillMaxSize()
    else Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()

    var clockText by remember { mutableStateOf("") }
    val clockPattern = if (clockFormat24h) "HH:mm" else "hh:mm a"
    LaunchedEffect(showClock, clockPattern, lifecycle) {
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            if (showClock) {
                val formatter = SimpleDateFormat(clockPattern, Locale.getDefault())
                while (isActive) {
                    clockText = formatter.format(Date())
                    delay((60_000L - System.currentTimeMillis() % 60_000L).coerceAtLeast(1L))
                }
            }
        }
    }

    val commonParams = AodCommonParams(
        mediaState = mediaState,
        dominantColor = dominantColor,
        progressFraction = progressFraction,
        displayPosition = displayPosition,
        duration = duration,
        isPlaying = isPlaying,
        canSkipPrev = canSkipPrev,
        canSkipNext = canSkipNext,
        showTitle = showTitle,
        showArtist = showArtist,
        showTime = showTime,
        showProgress = showProgress,
        showControls = showControls,
        showClock = showClock,
        clockText = clockText,
        textScale = textScale,
        controlStyle = controlStyle,
        transitionDuration = transitionDuration,
        onSeekChange = onSeekChange,
        onSeekFinished = onSeekFinished,
        onSkipPrev = { playerConnection.seekToPrevious() },
        onPlayPause = { playerConnection.player.togglePlayPause() },
        onSkipNext = { playerConnection.seekToNext() },
        onCollapse = { navController.navigateUp() },
        isLandscape = isLandscape,
        fullscreen = fullscreenMode,
        contentModifier = contentModifier,
    )

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        when (aodStyle) {
            AodStyle.BACKGROUND -> BackgroundAodLayout(
                params = commonParams,
                artSizeDp = artSizeDp,
                artShape = artShape,
                darkness = darkness,
            )

            AodStyle.MINIMAL -> MinimalAodLayout(params = commonParams)
            AodStyle.SPOTLIGHT -> SpotlightAodLayout(
                params = commonParams,
                artSizeDp = artSizeDp,
                artShape = artShape,
                darkness = darkness,
                spotlightIntensity = spotlightIntensity,
                spotlightPulse = spotlightPulse,
            )
            AodStyle.LARGE -> LargeAodLayout(
                params = commonParams,
                artShape = artShape,
            )
            AodStyle.CLASSIC -> ClassicAodLayout(
                params = commonParams,
                artSizeDp = artSizeDp,
                artShape = artShape,
            )
        }
    }
}

private data class AodCommonParams(
    val mediaState: AodMediaState,
    val dominantColor: Color,
    val progressFraction: Float,
    val displayPosition: Long,
    val duration: Long,
    val isPlaying: Boolean,
    val canSkipPrev: Boolean,
    val canSkipNext: Boolean,
    val showTitle: Boolean,
    val showArtist: Boolean,
    val showTime: Boolean,
    val showProgress: Boolean,
    val showControls: Boolean,
    val showClock: Boolean,
    val clockText: String,
    val textScale: Float,
    val controlStyle: AodControlStyle,
    val transitionDuration: Int,
    val onSeekChange: (Float) -> Unit,
    val onSeekFinished: () -> Unit,
    val onSkipPrev: () -> Unit,
    val onPlayPause: () -> Unit,
    val onSkipNext: () -> Unit,
    val onCollapse: () -> Unit,
    val isLandscape: Boolean,
    val fullscreen: Boolean,
    val contentModifier: Modifier,
)

@Composable
private fun ClassicAodLayout(
    params: AodCommonParams,
    artSizeDp: Dp,
    artShape: AodArtShape,
) {
    GalaxAodLayout(params, artSizeDp, artShape)
}

@Composable
private fun BackgroundAodLayout(
    params: AodCommonParams,
    artSizeDp: Dp,
    artShape: AodArtShape,
    darkness: Float,
) {
    GalaxAodLayout(params, artSizeDp, artShape, backgroundArt = true, darkness = darkness)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinimalAodLayout(params: AodCommonParams) {
    GalaxAodLayout(params, 0.dp, AodArtShape.ROUNDED, minimal = true)
}

@Composable
private fun LargeAodLayout(
    params: AodCommonParams,
    artShape: AodArtShape,
) {
    GalaxAodLayout(params, 340.dp, artShape, large = true)
}

@Composable
private fun SpotlightAodLayout(
    params: AodCommonParams,
    artSizeDp: Dp,
    artShape: AodArtShape,
    darkness: Float,
    spotlightIntensity: Float,
    spotlightPulse: Boolean,
) {
    GalaxAodLayout(params, artSizeDp, artShape, spotlight = true, darkness = darkness,
        intensity = spotlightIntensity, pulse = spotlightPulse)
}

@Composable
private fun SyncedArtImage(
    mediaState: AodMediaState,
    sizeDp: Dp,
    shape: AodArtShape,
    duration: Int,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = mediaState,
        transitionSpec = {
            fadeIn(tween(duration)) togetherWith fadeOut(tween((duration * 0.85f).toInt()))
        },
        label = "aod_art_synced"
    ) { state ->
        AsyncImage(
            model = state.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(sizeDp)
                .clip(shape.toShape())
        )
    }
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")

@Composable
private fun TimeRow(
    displayPosition: Long,
    duration: Long,
    textScale: Float,
    isLandscape: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            makeTimeString(displayPosition), color = Color.White.copy(0.38f),
            style = MaterialTheme.typography.labelSmall,
            fontSize = ((if (isLandscape) 12f else 10f) * textScale).sp
        )
        if (duration > 0L && duration != C.TIME_UNSET)
            Text(
                makeTimeString(duration), color = Color.White.copy(0.38f),
                style = MaterialTheme.typography.labelSmall,
                fontSize = ((if (isLandscape) 12f else 10f) * textScale).sp
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AodControlButtons(
    isPlaying: Boolean,
    canSkipPrev: Boolean,
    canSkipNext: Boolean,
    onSkipPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    controlStyle: AodControlStyle,
    accentColor: Color,
    isLandscape: Boolean = false,
    minimal: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically) {
        AodButton(R.drawable.skip_previous, canSkipPrev, 48.dp, 24.dp, false, controlStyle, accentColor,
            onClick = onSkipPrev)
        AodButton(if (isPlaying) R.drawable.pause else R.drawable.play, true, 64.dp, 30.dp, false,
            controlStyle, accentColor, isPlayButton = true,
            playBg = if (controlStyle == AodControlStyle.ACCENT) accentColor.copy(alpha = .25f) else Color.White.copy(alpha = .12f),
            playTint = Color.White, onClick = onPlayPause)
        AodButton(R.drawable.skip_next, canSkipNext, 48.dp, 24.dp, false, controlStyle, accentColor,
            onClick = onSkipNext)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AodButton(
    iconRes: Int,
    enabled: Boolean,
    baseSize: Dp,
    iconSize: Dp,
    isExpanded: Boolean,
    controlStyle: AodControlStyle,
    accentColor: Color,
    isPlayButton: Boolean = false,
    playBg: Color = Color.White,
    playTint: Color = Color.Black,
    onClick: () -> Unit,
    onAnimEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem.galaxMotion
    val scale = animateFloatAsState(if (pressed && enabled) motion.pressScale else 1f,
        animationSpec = motion.pressSpec, label = "aodPress")
    val shape = if (controlStyle == AodControlStyle.SQUARE) RoundedCornerShape(16.dp) else CircleShape
    val background = if (isPlayButton) playBg else if (controlStyle == AodControlStyle.ACCENT)
        accentColor.copy(alpha = .10f) else Color.White.copy(alpha = .04f)
    Box(modifier.size(baseSize).graphicsLayer { scaleX = scale.value; scaleY = scale.value; alpha = if (enabled) 1f else .3f }
        .clip(shape).background(background).galaxLiquidChrome(shape)
        .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(painterResource(iconRes), stringResource(when (iconRes) {
            R.drawable.skip_previous -> R.string.player_a11y_previous
            R.drawable.skip_next -> R.string.next_song
            R.drawable.pause -> R.string.player_a11y_pause
            else -> R.string.play
        }), tint = if (isPlayButton) playTint else Color.White, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun CollapseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(0.09f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(painterResource(R.drawable.expand_more), null,
            tint = Color.White.copy(0.70f),
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun GalaxAodLayout(params: AodCommonParams, artSize: Dp, artShape: AodArtShape,
    minimal: Boolean = false, backgroundArt: Boolean = false, large: Boolean = false,
    spotlight: Boolean = false, darkness: Float = .55f, intensity: Float = .75f, pulse: Boolean = false) {
    val backdrop = rememberLayerBackdrop()
    val motion = com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem.galaxMotion
    val lifecycleState by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val phase: State<Float> = if (spotlight && pulse && params.isPlaying && !motion.reducedMotion &&
        lifecycleState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
        rememberInfiniteTransition(label = "aodAmbient").animateFloat(.82f, 1f,
            infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Reverse), label = "aodAmbientPhase")
    } else remember { mutableStateOf(1f) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().layerBackdrop(backdrop)) {
            GalaxAodBackdrop(params.mediaState.thumbnailUrl, params.dominantColor, backgroundArt,
                minimal, darkness, if (spotlight) intensity else .35f, phase)
        }
        CompositionLocalProvider(com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidBackdrop provides backdrop) {
            BoxWithConstraints(params.contentModifier.padding(24.dp)) {
                val coverSize = (if (large) maxWidth * .82f else artSize)
                    .coerceAtMost(if (params.isLandscape) maxWidth * .43f else maxHeight * .48f)
                    .coerceAtLeast(80.dp)
                if (params.isLandscape && !minimal) Row(Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        SyncedArtImage(params.mediaState, coverSize, artShape,
                            if (motion.reducedMotion) 0 else params.transitionDuration)
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally) { GalaxAodMetadata(params) }
                } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)) {
                    if (!minimal) SyncedArtImage(params.mediaState, coverSize, artShape,
                        if (motion.reducedMotion) 0 else params.transitionDuration)
                    GalaxAodMetadata(params)
                }
                if (!params.fullscreen) CollapseButton(params.onCollapse, Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

@Composable
private fun GalaxAodBackdrop(url: String?, accent: Color, art: Boolean, minimal: Boolean,
    darkness: Float, intensity: Float, phase: State<Float>) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (art && !url.isNullOrBlank()) {
            val context = LocalContext.current
            val image = remember(url, context) { ImageRequest.Builder(context).data(url).size(384).build() }
            AsyncImage(image, null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = (1f - darkness.coerceIn(0f, 1f)) * .38f })
        }
        if (!minimal) Canvas(Modifier.fillMaxSize().graphicsLayer { scaleX = phase.value; scaleY = phase.value }) {
            val center = Offset(size.width * .5f, size.height * .3f)
            val radius = size.width * .9f
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .18f * intensity.coerceIn(0f, 1f)), Color.Transparent),
                center, radius), radius, center)
        }
    }
}

@Composable
private fun GalaxAodMetadata(params: AodCommonParams) {
    Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (params.showClock) Text(params.clockText, color = Color.White.copy(alpha = .65f),
            fontSize = (32f * params.textScale).sp, fontWeight = FontWeight.Light)
        if (params.showTitle) Text(params.mediaState.title, color = Color.White,
            fontSize = (24f * params.textScale).sp, fontWeight = FontWeight.SemiBold,
            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        if (params.showArtist) Text(params.mediaState.artist, color = Color.White.copy(alpha = .58f),
            fontSize = (15f * params.textScale).sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        if (params.showProgress) Slider(value = params.progressFraction, onValueChange = params.onSeekChange,
            onValueChangeFinished = params.onSeekFinished,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = .15f)), modifier = Modifier.fillMaxWidth())
        if (params.showTime) TimeRow(params.displayPosition, params.duration, params.textScale, params.isLandscape)
        if (params.showControls) AodControlButtons(params.isPlaying, params.canSkipPrev, params.canSkipNext,
            params.onSkipPrev, params.onPlayPause, params.onSkipNext, params.controlStyle, params.dominantColor,
            params.isLandscape)
    }
}
