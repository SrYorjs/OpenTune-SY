package com.arturo254.opentune.ui.original.adapters
import android.content.Context
import android.content.ClipboardManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.arturo254.opentune.extensions.getQueueWindows
import com.arturo254.opentune.extensions.metadata
import com.arturo254.opentune.ui.player.queue.calculateQueuePlaybackState
import com.arturo254.opentune.LocalDownloadUtil
import com.arturo254.opentune.constants.*
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidBackdrop
import com.arturo254.opentune.ui.component.LocalBottomSheetPageState
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.original.component.BottomSheetState
import com.arturo254.opentune.ui.original.player.studio.inert
import com.arturo254.opentune.ui.original.player.rememberMusicVolume
import com.arturo254.opentune.ui.original.player.styles.StyleActions
import com.arturo254.opentune.ui.player.GalaxStudioPlayer.*
import com.arturo254.opentune.ui.player.PlayerControlsContent
import com.arturo254.opentune.ui.player.PlayerBackground
import com.arturo254.opentune.ui.player.actions.rememberPlayerActions
import com.arturo254.opentune.ui.player.layout.*
import com.arturo254.opentune.ui.player.preferences.rememberPlayerPresentationPreferences
import com.arturo254.opentune.ui.player.state.*
import com.arturo254.opentune.ui.player.v7.V7PlayerScene
import com.arturo254.opentune.ui.player.v8.V8PlayerScene
import com.arturo254.opentune.ui.player.v8.V8PlayerControlsContent
import com.arturo254.opentune.ui.player.visual.PlayerArtworkAtmosphere
import com.arturo254.opentune.ui.player.visual.rememberPlayerVisualState
import com.arturo254.opentune.utils.rememberPreference
import com.kyant.backdrop.backdrops.LayerBackdrop
/** All versions route to existing scenes/controls. Preview only blocks input and ambient movement. */
@Composable internal fun GalaxPlayerSurface(
 design: PlayerDesignStyle, navController: NavController, sheet: BottomSheetState,
 preview: Boolean = false, styleActions: StyleActions,
) {
 val original = LocalOriginalPlayerConnection.current ?: return
 val connection = original.galaxConnection
 val metadata by original.mediaMetadata.collectAsState()
 val transport by original.uiState.transportState.collectAsState()
 val progress by original.progressState.collectAsState()
 val song by original.currentSong.collectAsState(initial = null)
 val queue by original.queueSnapshot.collectAsState()
 val nextUp = remember(queue, transport.shuffleModeEnabled, transport.repeatMode) {
  val windows = connection.player.getQueueWindows()
  val order = calculateQueuePlaybackState(windows.size, queue.currentIndex, transport.shuffleModeEnabled, transport.repeatMode)
  order.upNextIndices.firstOrNull()?.let { windows.getOrNull(it)?.mediaItem?.metadata }
 }
 val format by connection.currentFormat.collectAsState(initial = null)
 val downloadUtil = LocalDownloadUtil.current
 val download by remember(metadata?.id) { downloadUtil.getDownload(metadata?.id.orEmpty()) }.collectAsState(initial = null)
 val offline = remember(metadata?.id, download?.state) { metadata?.id?.let(downloadUtil::getOfflineCoverUri) }
 val visuals = rememberPlayerVisualState(metadata, download?.state, offline, downloadUtil::getOfflineAnimationUri, true)
 val state = rememberPlayerState(PlayerStateInput(metadata, transport.playbackState, transport.isPlaying,
  progress.position, progress.duration, offlineArtworkUrl = offline, canSkipPrevious = transport.canSkipPrevious,
  canSkipNext = transport.canSkipNext, isLiked = song?.song?.liked == true,
  shuffleModeEnabled = transport.shuffleModeEnabled, repeatMode = transport.repeatMode))
 val actions = rememberPlayerActions(connection)
 val context = LocalContext.current
 val clipboard = remember(context) { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
 val menu = LocalMenuState.current
 val pages = LocalBottomSheetPageState.current
 val presentation = rememberPlayerPresentationPreferences()
 val disableBlur by rememberPreference(DisableBlurKey, false)
 val blurRadius by rememberPreference(BlurRadiusKey, 36f)
 val image by rememberPreference(PlayerCustomImageUriKey, "")
 val customBlur by rememberPreference(PlayerCustomBlurKey, 0f)
 val contrast by rememberPreference(PlayerCustomContrastKey, 1f)
 val brightness by rememberPreference(PlayerCustomBrightnessKey, 1f)
 val volume = rememberMusicVolume(listening = !preview)
 val nested = remember { object : NestedScrollConnection {} }
 var pending by remember(metadata?.id) { mutableStateOf<Long?>(null) }
 val seek: (Long) -> Unit = { pending = it }
 val finish: () -> Unit = { pending?.let { actions.seekTo(it, metadata?.id) }; pending = null }
 val moving = transport.isPlaying && !preview && !OpenTuneDesignSystem.galaxMotion.reducedMotion
 val accent = visuals.gradientColors.firstOrNull() ?: MaterialTheme.colorScheme.primary
 val art: @Composable BoxScope.() -> Unit = {
  if (design == PlayerDesignStyle.V17) StudioVinJaxArtwork(visuals.artworkUrl, visuals.canvasArtwork, moving, Modifier.fillMaxSize())
  else AsyncImage(model = visuals.artworkUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
 }
 val controls: @Composable () -> Unit = {
  metadata?.let { meta ->
   when {
    design == PlayerDesignStyle.V8 -> V8PlayerControlsContent(
     mediaMetadata = meta, playbackState = state.playbackState, isPlaying = state.isPlaying,
     isLoading = state.isLoading, repeatMode = state.repeatMode, canSkipPrevious = state.canSkipPrevious,
     canSkipNext = state.canSkipNext, textBackgroundColor = Color.White, sliderPosition = pending,
     position = progress.position, duration = progress.duration, playerActions = actions,
     liked = state.isLiked, shuffleModeEnabled = state.shuffleModeEnabled, navController = navController,
     state = sheet, menuState = menu, bottomSheetPageState = pages, onSliderValueChange = seek,
     onSliderValueChangeFinished = finish, currentFormat = format, nextUpMetadata = nextUp,
     onExpandQueue = { styleActions.onQueue?.invoke() }, playerVolume = volume.shown,
     onVolumeChange = { volume.drag(it); volume.release() }, legacyMain = true, onMenuOverride = styleActions.onOtherMenu)
    design.ordinal >= PlayerDesignStyle.V9.ordinal -> GalaxOriginalPlayerControls(
     style = design, state = state, actions = actions, accent = accent, positionMs = progress.position,
     durationMs = progress.duration, pendingSeekMs = pending, onSeek = seek, onSeekFinished = finish,
     onMenu = styleActions.onOtherMenu, onQueue = { styleActions.onQueue?.invoke() }, onLyrics = { styleActions.onLyrics?.invoke() })
    else -> PlayerControlsContent(
     mediaMetadata = meta, playerDesignStyle = design, sliderStyle = presentation.sliderStyle,
     playbackState = state.playbackState, isPlaying = state.isPlaying, isLoading = state.isLoading,
     repeatMode = state.repeatMode, canSkipPrevious = state.canSkipPrevious, canSkipNext = state.canSkipNext,
     textButtonColor = Color.White, iconButtonColor = Color.White, textBackgroundColor = Color.White,
     icBackgroundColor = accent, sliderPosition = pending, position = progress.position,
     duration = progress.duration, playerConnection = connection, navController = navController,
     state = sheet, menuState = menu, bottomSheetPageState = pages, clipboardManager = clipboard,
     context = context, onSliderValueChange = seek, onSliderValueChangeFinished = finish, currentFormat = format, onMenuOverride = styleActions.onOtherMenu)
   }
  }
 }
 BoxWithConstraints(Modifier.fillMaxSize().then(if (preview) Modifier.inert() else Modifier)) {
  val landscape = maxWidth > maxHeight
  OriginalStudioGlassScope(background = {
   PlayerArtworkAtmosphere(visuals.artworkUrl, visuals.gradientColors, moving, disableBlur, Modifier.fillMaxSize())
  }, modifier = Modifier.fillMaxSize()) {
   val backdrop = LocalGalaxLiquidBackdrop.current as? LayerBackdrop
   when (galaxSceneFamily(design)) {
    GalaxSceneFamily.STUDIO ->
     StudioVisualScene(design, landscape, moving, visuals.gradientColors, 0.dp, state.progressFraction, art, controls)
    GalaxSceneFamily.VINJAX -> VinJaxPlayerScene(landscape, moving, state.progressFraction, accent, 0.dp, art, controls)
    GalaxSceneFamily.IMMERSIVE -> StudioThemeV16(landscape, visuals.artworkUrl, visuals.canvasArtwork, moving,
     disableBlur, 0.dp, backdrop, true, visuals.gradientColors, nested) { controls() }
    GalaxSceneFamily.APPLE -> V8PlayerScene(landscape, visuals.artworkUrl, visuals.canvasArtwork, moving,
     disableBlur, 0.dp, nested, backdrop, true, legacyMain = true, gradientColors = visuals.gradientColors) { controls() }
    GalaxSceneFamily.LEGACY -> {
     val classic: @Composable () -> Unit = {
      PlayerBackground(presentation.backgroundStyle, metadata, visuals.gradientColors, disableBlur,
       blurRadius, image, customBlur, contrast, brightness)
      if (landscape) GalaxPlayerLandscapeScene(accent, moving, 0.dp, art, { controls() }, Modifier.fillMaxSize())
      else GalaxPlayerClassicScene(moving, accent, 0.dp, Modifier.fillMaxSize(), art) { controls() }
     }
     val cinematic: @Composable () -> Unit = {
      V7PlayerScene(landscape, visuals.artworkUrl, disableBlur, moving, 0.dp, nested,
       backdrop, true, visuals.gradientColors) { controls() }
     }
     PlayerLayout(design, landscape, classic, classic, classic, classic, cinematic, cinematic, cinematic, cinematic)
    }
   }
  }
 }
}
