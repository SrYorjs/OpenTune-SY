package com.arturo254.opentune

import android.annotation.SuppressLint
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import com.arturo254.opentune.core.designsystem.motion.galaxEnterTransition
import com.arturo254.opentune.core.designsystem.motion.galaxExitTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.core.content.ContextCompat
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.arturo254.opentune.utils.PreferenceStore
import kotlinx.coroutines.withContext
import com.arturo254.opentune.constants.AppBarHeight
import com.arturo254.opentune.constants.AppLanguageKey
import com.arturo254.opentune.constants.CustomThemeColorKey
import com.arturo254.opentune.constants.DarkModeKey
import com.arturo254.opentune.constants.DefaultOpenTabKey
import com.arturo254.opentune.constants.DisableScreenshotKey
import com.arturo254.opentune.constants.DynamicThemeKey
import com.arturo254.opentune.constants.FloatingToolbarBottomPadding
import com.arturo254.opentune.constants.FloatingToolbarHeight
import com.arturo254.opentune.constants.FloatingToolbarHorizontalPadding
import com.arturo254.opentune.constants.HasPressedStarKey
import com.arturo254.opentune.constants.LaunchCountKey
import com.arturo254.opentune.constants.LiquidGlassNavBarKey
import com.arturo254.opentune.constants.GalaxGlassModeKey
import com.arturo254.opentune.constants.GalaxLiquidVariantKey
import com.arturo254.opentune.constants.GalaxLiquidCornerRadiusKey
import com.arturo254.opentune.constants.GalaxLiquidBlurKey
import com.arturo254.opentune.constants.GalaxLiquidRefractionHeightKey
import com.arturo254.opentune.constants.GalaxLiquidRefractionAmountKey
import com.arturo254.opentune.constants.GalaxLiquidChromaticKey
import com.arturo254.opentune.constants.GalaxLiquidTintKey
import com.arturo254.opentune.core.designsystem.glass.GlassMode
import com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidVariant
import com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidTuning
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidVariant
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidTuning
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidBackdrop
import com.arturo254.opentune.core.designsystem.liquid.galvanicRecipe
import com.arturo254.opentune.core.designsystem.liquid.parseGalaxLiquidVariant
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.arturo254.opentune.core.designsystem.glass.LocalGalaxGlassMode
import com.arturo254.opentune.core.designsystem.glass.LocalLiquidBackdrop
import com.arturo254.opentune.core.designsystem.glass.parseGalaxGlassMode
import com.arturo254.opentune.core.designsystem.glass.isGalaxGlassEnabled
import com.skydoves.cloudy.rememberSky
import com.arturo254.opentune.constants.LyricsSyncOffsetKey
import com.arturo254.opentune.constants.MiniPlayerBottomSpacing
import com.arturo254.opentune.constants.MiniPlayerHeight
import com.arturo254.opentune.constants.MiniPlayerLastAnchorKey
import com.arturo254.opentune.constants.NavigationBarAnimationSpec
import com.arturo254.opentune.constants.PauseSearchHistoryKey
import com.arturo254.opentune.constants.PureBlackKey
import com.arturo254.opentune.constants.RemindAfterKey
import com.arturo254.opentune.constants.SYSTEM_DEFAULT
import com.arturo254.opentune.constants.SlimFloatingToolbarHeight
import com.arturo254.opentune.constants.SlimNavBarKey
import com.arturo254.opentune.constants.StopMusicOnTaskClearKey
import com.arturo254.opentune.constants.UseNewMiniPlayerDesignKey
import com.arturo254.opentune.constants.AppFontKey
import com.arturo254.opentune.constants.AppFontStyle
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.SearchHistory
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.AlbumItem
import com.arturo254.opentune.innertube.models.ArtistItem
import com.arturo254.opentune.innertube.models.PlaylistItem
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.DownloadUtil
import com.arturo254.opentune.playback.MusicService
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.playback.queues.LocalAlbumRadio
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.playback.queues.YouTubeAlbumRadio
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.AccountSettingsDialog
import com.arturo254.opentune.ui.component.BottomSheetMenu
import com.arturo254.opentune.ui.component.BottomSheetPage
import com.arturo254.opentune.ui.component.COLLAPSED_ANCHOR
import com.arturo254.opentune.ui.component.DISMISSED_ANCHOR
import com.arturo254.opentune.ui.component.EXPANDED_ANCHOR
import com.arturo254.opentune.ui.component.FloatingNavigationToolbar
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.LocalBottomSheetPageState
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.LocalSky
import com.arturo254.opentune.ui.component.LocalEnableLiquidGlass
import com.arturo254.opentune.ui.component.LiquidGlassControlCluster
import com.mikepenz.markdown.m3.Markdown
import com.arturo254.opentune.constants.TogetherDisplayNameKey
import com.arturo254.opentune.ui.component.BottomSheetPageState
import com.arturo254.opentune.ui.component.MenuState
import com.arturo254.opentune.ui.component.TopSearch
import com.arturo254.opentune.ui.component.rememberBottomSheetState
import com.arturo254.opentune.ui.component.shimmer.ShimmerTheme
import com.arturo254.opentune.ui.menu.YouTubeSongMenu
import com.arturo254.opentune.ui.player.host.PlayerHost
import com.arturo254.opentune.ui.screens.LOGIN_URL_ARGUMENT
import com.arturo254.opentune.ui.screens.Screens
import com.arturo254.opentune.ui.screens.buildLoginRoute
import com.arturo254.opentune.ui.screens.musicrecognition.MusicRecognitionRoute
import com.arturo254.opentune.ui.screens.navigationBuilder
import com.arturo254.opentune.ui.screens.settings.DarkMode
import com.arturo254.opentune.ui.screens.settings.DiscordPresenceManager
import com.arturo254.opentune.ui.screens.settings.NavigationTab
import com.arturo254.opentune.ui.screens.settings.ThemePalettes
import com.arturo254.opentune.ui.theme.OpenTuneTheme
import com.arturo254.opentune.ui.theme.ColorSaver
import com.arturo254.opentune.ui.theme.DefaultThemeColor
import com.arturo254.opentune.ui.theme.ThemeSeedPalette
import com.arturo254.opentune.ui.theme.ThemeSeedPaletteCodec
import com.arturo254.opentune.ui.theme.extractThemeColor
import com.arturo254.opentune.ui.utils.appBarScrollBehavior
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.ui.utils.resetHeightOffset
import com.arturo254.opentune.utils.SyncUtils
import com.arturo254.opentune.utils.UpdateNotificationManager
import com.arturo254.opentune.utils.Updater
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.utils.reportException
import com.arturo254.opentune.utils.setAppLocale
import com.arturo254.opentune.viewmodels.HomeViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import androidx.core.graphics.toColorInt
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.arturo254.opentune.constants.EnableHapticFeedbackKey
import com.arturo254.opentune.constants.PlayerFullscreenKey

/**
 * Owns the visible app chrome: top bar, navigation rail, floating toolbar,
 * player surface, composition locals, menus and dialogs.
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalTextApi::class,
)
@Composable
internal fun MainActivityScaffold(
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    syncUtils: SyncUtils,
    playerConnection: PlayerConnection?,
    bottomSheetPageState: BottomSheetPageState,
    latestVersionName: String,
    pureBlack: Boolean,
    lyricsSyncOffset: Int,
    navigationState: MainActivityNavigationState,
    navigationEffects: MainActivityNavigationEffects,
    playerUiState: MainActivityPlayerUiState,
    useRail: Boolean,
    bottomInset: Dp,
    searchBarAlignmentModifier: Modifier,
) {
    val (liquidGlassNavBar) = rememberPreference(LiquidGlassNavBarKey, defaultValue = false)
    val (storedGalaxGlassMode) = rememberPreference(GalaxGlassModeKey, defaultValue = "")
    val selectedGlassMode = parseGalaxGlassMode(storedGalaxGlassMode)
    val liquidGlassSky = rememberSky()
    val (rawLiquidVariant) = rememberPreference(GalaxLiquidVariantKey, defaultValue = "")
    val liquidVariant = parseGalaxLiquidVariant(rawLiquidVariant) ?: when (selectedGlassMode) {
        GlassMode.FROSTED -> GalaxLiquidVariant.PEARL
        GlassMode.CLEAR -> GalaxLiquidVariant.CRYSTAL
        GlassMode.BLUR -> GalaxLiquidVariant.GLACIER
        GlassMode.CLASSIC -> GalaxLiquidVariant.SILK
        GlassMode.DISABLED, null -> if (liquidGlassNavBar) GalaxLiquidVariant.SILK else GalaxLiquidVariant.DISABLED
    }
    val enableGalaxGlass = liquidVariant != GalaxLiquidVariant.DISABLED
    val defaultLiquidTuning = galvanicRecipe(liquidVariant)
    val (liquidRadius) = rememberPreference(GalaxLiquidCornerRadiusKey, defaultLiquidTuning.cornerRadiusDp)
    val (liquidBlur) = rememberPreference(GalaxLiquidBlurKey, defaultLiquidTuning.blurRadiusDp)
    val (liquidHeight) = rememberPreference(GalaxLiquidRefractionHeightKey, defaultLiquidTuning.refractionHeight)
    val (liquidAmount) = rememberPreference(GalaxLiquidRefractionAmountKey, defaultLiquidTuning.refractionAmount)
    val (liquidChromatic) = rememberPreference(GalaxLiquidChromaticKey, defaultLiquidTuning.chromaticAberration)
    val (liquidTint) = rememberPreference(GalaxLiquidTintKey, defaultLiquidTuning.tintAlpha)
    val liquidTuning = if (rawLiquidVariant.isBlank()) defaultLiquidTuning else
        GalaxLiquidTuning(liquidRadius, liquidBlur, liquidHeight, liquidAmount, liquidChromatic, liquidTint).bounded()
    // Produce exactly one captured background. Importantly, its NavHost subtree
    // will explicitly receive a null Backdrop; only sibling overlay controls
    // may sample the captured layer. This prevents recursive RenderNode graphs.
    val glassContext = androidx.compose.ui.platform.LocalContext.current
    val saver = (glassContext.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager)
        ?.isPowerSaveMode == true
    val lowRam = (glassContext.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager)
        ?.isLowRamDevice == true
    val liveLiquidBackdrop = if (
        liquidVariant != GalaxLiquidVariant.DISABLED && !saver && !lowRam &&
        // Glass Studio captures a separate preview; do not nest it inside
        // an outer NavHost recording layer (Android 17 RenderThread safety).
        navigationState.currentRoute != "settings/appearance/galax_liquid" &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    ) rememberLayerBackdrop() else null
    val menuState = remember { MenuState() }

    val haptic = LocalHapticFeedback.current
    val (enableHapticFeedback) = rememberPreference(EnableHapticFeedbackKey, true)
    val customHaptic = remember(haptic, enableHapticFeedback) {
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                if (enableHapticFeedback) {
                    haptic.performHapticFeedback(hapticFeedbackType)
                }
            }
        }
    }

    val navController = navigationState.navController
    val accountImageUrl = navigationState.accountImageUrl
    val allLocalItems = navigationState.allLocalItems
    val allYtItems = navigationState.allYtItems
    val navBackStackEntry = navigationState.navBackStackEntry
    val currentRoute = navigationState.currentRoute
    val navigationItems = navigationState.navigationItems
    val slimNav = navigationState.slimNav
    val defaultOpenTab = navigationState.defaultOpenTab
    val pauseSearchHistory = navigationState.pauseSearchHistory
    val tabOpenedFromShortcut = navigationState.tabOpenedFromShortcut
    val topLevelScreens = navigationState.topLevelScreens
    val query = navigationState.query
    val onQueryChange = navigationState.onQueryChange
    val active = navigationState.active
    val selectedNavigationRoute = navigationState.selectedNavigationRoute
    val onActiveChange = navigationState.onActiveChange
    val searchBarFocusRequester = navigationState.searchBarFocusRequester
    val onSearch = navigationState.onSearch
    val shouldShowNavigationBar = navigationState.shouldShowNavigationBar
    val shouldShowHomeShuffleButton = navigationState.shouldShowHomeShuffleButton
    val shouldShowTopBar = navigationState.shouldShowTopBar
    val currentTitleRes = navigationState.currentTitleRes

    val playerBottomSheetState = playerUiState.bottomSheetState
    val playerAwareWindowInsets = playerUiState.playerAwareWindowInsets
    val searchBarScrollBehavior = navigationEffects.searchBarScrollBehavior
    val topAppBarScrollBehavior = navigationEffects.topAppBarScrollBehavior

    val galaxMotion = OpenTuneDesignSystem.galaxMotion
    val floatingBarsBottomPadding = FloatingToolbarBottomPadding
    val navVisibleHeight = if (slimNav) SlimFloatingToolbarHeight else FloatingToolbarHeight
    val bottomNavigationBarHeight by animateDpAsState(
        targetValue = if (shouldShowNavigationBar && !useRail) navVisibleHeight else 0.dp,
        animationSpec = galaxMotion.navigationBarSpec,
        label = "",
    )

    val coroutineScope = rememberCoroutineScope()
    var showAccountDialog by remember { mutableStateOf(false) }

                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalContentColor provides if (pureBlack) Color.White else contentColorFor(MaterialTheme.colorScheme.surface),
                        LocalHapticFeedback provides customHaptic,
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme,
                        LocalSyncUtils provides syncUtils,
                        LocalSky provides liquidGlassSky,
                        LocalLiquidBackdrop provides null,
                        LocalGalaxGlassMode provides selectedGlassMode,
                        LocalGalaxLiquidVariant provides liquidVariant,
                        LocalGalaxLiquidTuning provides liquidTuning,
                        LocalGalaxLiquidBackdrop provides liveLiquidBackdrop,
                        LocalEnableLiquidGlass provides enableGalaxGlass,
                        LocalBottomSheetPageState provides bottomSheetPageState,
                        LocalMenuState provides menuState,
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                        Row rowScope@ {
                            MainActivityNavigationRail(
                                useRail = useRail,
                                pureBlack = pureBlack,
                                navigationState = navigationState,
                                playerUiState = playerUiState,
                                navigationEffects = navigationEffects,
                            )

                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Scaffold(
                                    topBar = {
                                        MainActivityTopBar(
                                            useRail = useRail,
                                            pureBlack = pureBlack,
                                            latestVersionName = latestVersionName,
                                            navigationState = navigationState,
                                            navigationEffects = navigationEffects,
                                            onShowAccountDialog = { showAccountDialog = true },
                                        )
                                        MainActivitySearchBar(
                                            pureBlack = pureBlack,
                                            database = database,
                                            navigationState = navigationState,
                                            playerUiState = playerUiState,
                                            searchBarAlignmentModifier = searchBarAlignmentModifier,
                                        )
                                    },
                                    bottomBar = {
                                        PlayerHost(
                                            state = playerBottomSheetState,
                                            navController = navController,
                                            pureBlack = pureBlack,
                                            lyricsSyncOffset = lyricsSyncOffset,
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .nestedScroll(searchBarScrollBehavior.nestedScrollConnection)
                                ) {
                                    CompositionLocalProvider(
                                        LocalGalaxLiquidBackdrop provides null,
                                    ) {
                                    NavHost(
                                        navController = navController,
                                        startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                            NavigationTab.HOME -> Screens.Home
                                            NavigationTab.LIBRARY -> Screens.Library
                                            else -> Screens.Home
                                        }.route,
                                        enterTransition = {
                                            val topLevel =
                                                initialState.destination.route in topLevelScreens &&
                                                    targetState.destination.route in topLevelScreens
                                            val initialIndex = navigationItems.indexOfFirst {
                                                it.route == initialState.destination.route
                                            }
                                            val targetIndex = navigationItems.indexOfFirst {
                                                it.route == targetState.destination.route
                                            }
                                            val direction = if (targetIndex >= initialIndex) 1 else -1
                                            galaxEnterTransition(
                                                policy = galaxMotion,
                                                topLevel = topLevel,
                                                direction = direction,
                                                popping = false,
                                            )
                                        },
                                        exitTransition = {
                                            val topLevel =
                                                initialState.destination.route in topLevelScreens &&
                                                    targetState.destination.route in topLevelScreens
                                            val initialIndex = navigationItems.indexOfFirst {
                                                it.route == initialState.destination.route
                                            }
                                            val targetIndex = navigationItems.indexOfFirst {
                                                it.route == targetState.destination.route
                                            }
                                            val direction = if (targetIndex >= initialIndex) 1 else -1
                                            galaxExitTransition(
                                                policy = galaxMotion,
                                                topLevel = topLevel,
                                                direction = direction,
                                                popping = false,
                                            )
                                        },
                                        popEnterTransition = {
                                            val topLevel =
                                                (initialState.destination.route in topLevelScreens ||
                                                    initialState.destination.route?.startsWith("search/") == true) &&
                                                    targetState.destination.route in topLevelScreens
                                            val initialIndex = navigationItems.indexOfFirst {
                                                it.route == initialState.destination.route
                                            }
                                            val targetIndex = navigationItems.indexOfFirst {
                                                it.route == targetState.destination.route
                                            }
                                            val direction = if (targetIndex >= initialIndex) 1 else -1
                                            galaxEnterTransition(
                                                policy = galaxMotion,
                                                topLevel = topLevel,
                                                direction = direction,
                                                popping = true,
                                            )
                                        },
                                        popExitTransition = {
                                            val topLevel =
                                                (initialState.destination.route in topLevelScreens ||
                                                    initialState.destination.route?.startsWith("search/") == true) &&
                                                    targetState.destination.route in topLevelScreens
                                            val initialIndex = navigationItems.indexOfFirst {
                                                it.route == initialState.destination.route
                                            }
                                            val targetIndex = navigationItems.indexOfFirst {
                                                it.route == targetState.destination.route
                                            }
                                            val direction = if (targetIndex >= initialIndex) 1 else -1
                                            galaxExitTransition(
                                                policy = galaxMotion,
                                                topLevel = topLevel,
                                                direction = direction,
                                                popping = true,
                                            )
                                        },
                                        modifier = Modifier
                                            .then(
                                                if (liveLiquidBackdrop != null)
                                                    Modifier.layerBackdrop(liveLiquidBackdrop)
                                                else Modifier
                                            )
                                            .nestedScroll(
                                                if (
                                                    navigationItems.fastAny {
                                                        it.route == navBackStackEntry?.destination?.route
                                                    } ||
                                                    navBackStackEntry?.destination?.route?.startsWith("search/") == true
                                                ) {
                                                    searchBarScrollBehavior.nestedScrollConnection
                                                } else {
                                                    topAppBarScrollBehavior.nestedScrollConnection
                                                }
                                            )

                                    ) {
                                        navigationBuilder(
                                            navController,
                                            topAppBarScrollBehavior,
                                            latestVersionName
                                        )
                                    }
                                    } // NavHost is source-only; no sampling here.
                                }
                                MainActivityFloatingToolbar(
                                    pureBlack = pureBlack,
                                    useRail = useRail,
                                    bottomInset = bottomInset,
                                    database = database,
                                    playerConnection = playerConnection,
                                    navigationState = navigationState,
                                    playerUiState = playerUiState,
                                    navigationEffects = navigationEffects,
                                )
                            }
                        }

                        MainActivityOverlays(
                            bottomSheetPageState = bottomSheetPageState,
                            navigationState = navigationState,
                            latestVersionName = latestVersionName,
                            playerConnection = playerConnection,
                            showAccountDialog = showAccountDialog,
                            onDismissAccountDialog = { showAccountDialog = false },
                        )
                        }
                    }
}
