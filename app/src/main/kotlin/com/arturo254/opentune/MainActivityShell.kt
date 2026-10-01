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
import com.arturo254.opentune.constants.SearchSource
import com.arturo254.opentune.constants.SearchSourceKey
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
import com.arturo254.opentune.ui.component.SearchSourceSelector
import com.arturo254.opentune.ui.component.rememberBottomSheetState
import com.arturo254.opentune.ui.component.shimmer.ShimmerTheme
import com.arturo254.opentune.ui.menu.YouTubeSongMenu
import com.arturo254.opentune.ui.player.BottomSheetPlayer
import com.arturo254.opentune.ui.screens.LOGIN_URL_ARGUMENT
import com.arturo254.opentune.ui.screens.Screens
import com.arturo254.opentune.ui.screens.buildLoginRoute
import com.arturo254.opentune.ui.screens.musicrecognition.MusicRecognitionRoute
import com.arturo254.opentune.ui.screens.navigationBuilder
import com.arturo254.opentune.ui.screens.search.DeezerSearchScreen
import com.arturo254.opentune.ui.screens.search.LocalSearchScreen
import com.arturo254.opentune.ui.screens.search.OnlineSearchScreen
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
 * Main Compose shell extracted from MainActivity.
 * Owns adaptive navigation chrome, player surface, menus and dialogs.
 */
@Composable
internal fun MainActivityShell(
    activity: ComponentActivity,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    syncUtils: SyncUtils,
    playerConnection: PlayerConnection?,
    deepLinkController: MainActivityDeepLinkController,
    systemUiController: MainActivitySystemUiController,
    bottomSheetPageState: BottomSheetPageState,
    latestVersionName: String,
    pureBlack: Boolean,
    lyricsSyncOffset: Int,
) {
                BoxWithConstraints(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface
                            )
                ) {
                    val density = LocalDensity.current
                    val windowsInsets = WindowInsets.systemBars
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }

                    val useRail = currentWindowAdaptiveInfo().windowSizeClass
                        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

                    val navigationState =
                        rememberMainActivityNavigationState(
                            activity = activity,
                            database = database,
                        )

                    val bottomNavPadding =
                        if (navigationState.shouldShowNavigationBar && !useRail) {
                            if (navigationState.slimNav) {
                                SlimFloatingToolbarHeight
                            } else {
                                FloatingToolbarHeight
                            }
                        } else {
                            0.dp
                        }

                    val playerUiState =
                        rememberMainActivityPlayerUiState(
                            playerConnection = playerConnection,
                            systemUiController = systemUiController,
                            bottomInset = bottomInset,
                            bottomNavPadding = bottomNavPadding,
                            shouldShowNavigationBar = navigationState.shouldShowNavigationBar,
                            useRail = useRail,
                            maxHeight = maxHeight,
                            windowsInsets = windowsInsets,
                            isYearInMusicScreen = navigationState.isYearInMusicScreen,
                            isAlwaysOnDisplayScreen = navigationState.isAlwaysOnDisplayScreen,
                        )
                    val playerBottomSheetState = playerUiState.bottomSheetState

                    val navigationEffects =
                        rememberMainActivityNavigationEffects(
                            navigationState = navigationState,
                            playerBottomSheetState = playerBottomSheetState,
                        )
                    val searchBarScrollBehavior = navigationEffects.searchBarScrollBehavior
                    val topAppBarScrollBehavior = navigationEffects.topAppBarScrollBehavior
                    val shouldShowTopBar = navigationState.shouldShowTopBar
                    val currentTitleRes = navigationState.currentTitleRes

                    LaunchedEffect(navigationState.navController) {
                        deepLinkController.attachNavController(
                            navController = navigationState.navController,
                            initialIntent = activity.intent,
                        )
                    }

                    var showStarDialog by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        delay(3000)

                        withContext(Dispatchers.IO) {
                            val current = activity.dataStore[LaunchCountKey] ?: 0
                            val newCount = current + 1
                            activity.dataStore.edit { prefs ->
                                prefs[LaunchCountKey] = newCount
                            }
                        }

                        val shouldShow = withContext(Dispatchers.IO) {
                            val hasPressed = activity.dataStore[HasPressedStarKey] ?: false
                            val remindAfter = activity.dataStore[RemindAfterKey] ?: 3
                            !hasPressed && (activity.dataStore[LaunchCountKey] ?: 0) >= remindAfter
                        }

                        if (shouldShow) {
                            var waited = 0L
                            val waitStep = 500L
                            val maxWait = 30_000L
                            while (bottomSheetPageState.isVisible && waited < maxWait) {
                                delay(waitStep)
                                waited += waitStep
                            }
                            showStarDialog = true
                        }
                    }

                    MainActivityScaffold(
                        database = database,
                        downloadUtil = downloadUtil,
                        syncUtils = syncUtils,
                        playerConnection = playerConnection,
                        bottomSheetPageState = bottomSheetPageState,
                        latestVersionName = latestVersionName,
                        pureBlack = pureBlack,
                        lyricsSyncOffset = lyricsSyncOffset,
                        navigationState = navigationState,
                        navigationEffects = navigationEffects,
                        playerUiState = playerUiState,
                        useRail = useRail,
                        bottomInset = bottomInset,
                        searchBarAlignmentModifier = Modifier.align(Alignment.TopCenter),
                    )


                }
}
