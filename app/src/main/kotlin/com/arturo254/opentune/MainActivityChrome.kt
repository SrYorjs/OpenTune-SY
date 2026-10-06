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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import com.arturo254.opentune.ui.screens.search.SpotifySearchScreen
import com.arturo254.opentune.ui.screens.search.TidalSearchScreen
import com.arturo254.opentune.ui.screens.search.AppleMusicSearchScreen
import com.arturo254.opentune.ui.screens.search.UniversalSearchScreen
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

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalTextApi::class,
)
@Composable
internal fun RowScope.MainActivityNavigationRail(
    useRail: Boolean,
    pureBlack: Boolean,
    navigationState: MainActivityNavigationState,
    playerUiState: MainActivityPlayerUiState,
    navigationEffects: MainActivityNavigationEffects,
) {
    val navigationItems = navigationState.navigationItems
    val navBackStackEntry = navigationState.navBackStackEntry
    val slimNav = navigationState.slimNav
    val navController = navigationState.navController
    val onActiveChange = navigationState.onActiveChange
    val shouldShowNavigationBar = navigationState.shouldShowNavigationBar
    val playerBottomSheetState = playerUiState.bottomSheetState
    val searchBarScrollBehavior = navigationEffects.searchBarScrollBehavior
    val coroutineScope = rememberCoroutineScope()
    val rowScope = this

                            Box {
                                rowScope.AnimatedVisibility(
                                    visible = useRail && shouldShowNavigationBar
                                ) {
                                    NavigationRail(
                                        containerColor = if(pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                        contentColor = if(pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        header = { Spacer(Modifier.height(24.dp)) }
                                    ) {
                                        navigationItems.fastForEach { screen ->
                                            val isSelected =
                                                navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true

                                            NavigationRailItem(
                                                selected = isSelected,
                                                icon = {
                                                    Icon(
                                                        painter = painterResource(
                                                            id = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                                                        ),
                                                        contentDescription = null,
                                                    )
                                                },
                                                label = {
                                                    if (!slimNav) {
                                                        Text(
                                                            text = stringResource(screen.titleId),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                },
                                                onClick = {
                                                    val wasPlayerActive = playerBottomSheetState.isExpanded

                                                    if(wasPlayerActive) {
                                                        playerBottomSheetState.collapse(spring())
                                                    }

                                                    if (screen.route == Screens.Search.route) {
                                                        onActiveChange(true)
                                                    } else if (isSelected) {
                                                        if(wasPlayerActive) return@NavigationRailItem

                                                        navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                                        coroutineScope.launch {
                                                            searchBarScrollBehavior.state.resetHeightOffset()
                                                        }
                                                    } else {
                                                        navController.navigate(screen.route) {
                                                            popUpTo(navController.graph.startDestinationId) {
                                                                saveState = true
                                                            }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalTextApi::class,
)
@Composable
internal fun MainActivityTopBar(
    useRail: Boolean,
    pureBlack: Boolean,
    latestVersionName: String,
    navigationState: MainActivityNavigationState,
    navigationEffects: MainActivityNavigationEffects,
    onShowAccountDialog: () -> Unit,
) {
    val navBackStackEntry = navigationState.navBackStackEntry
    val navController = navigationState.navController
    val accountImageUrl = navigationState.accountImageUrl
    val shouldShowTopBar = navigationState.shouldShowTopBar
    val searchBarScrollBehavior = navigationEffects.searchBarScrollBehavior
    val topAppBarScrollBehavior = navigationEffects.topAppBarScrollBehavior

                                        if (shouldShowTopBar) {
                                            val shouldUseFloatingTopBar = remember(navBackStackEntry) {
                                                navBackStackEntry?.destination?.route == Screens.Home.route ||
                                                        navBackStackEntry?.destination?.route == Screens.MoodAndGenres.route ||
                                                        navBackStackEntry?.destination?.route == Screens.Library.route
                                            }
                                            val shouldShowBlurBackground = remember(navBackStackEntry) {
                                                shouldUseFloatingTopBar
                                            }

                                            val surfaceColor = MaterialTheme.colorScheme.surface
                                            val currentScrollBehavior = if (shouldUseFloatingTopBar) searchBarScrollBehavior else topAppBarScrollBehavior

                                            Box(
                                                modifier = Modifier.offset {
                                                    IntOffset(
                                                        x = 0,
                                                        y = currentScrollBehavior.state.heightOffset.toInt()
                                                    )
                                                }
                                            ) {
                                                if (shouldShowBlurBackground) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(AppBarHeight + with(LocalDensity.current) {
                                                                WindowInsets.systemBars.getTop(
                                                                    LocalDensity.current
                                                                ).toDp()
                                                            })
                                                            .background(
                                                                Brush.verticalGradient(
                                                                    colors = listOf(
                                                                        surfaceColor.copy(alpha = 0.95f),
                                                                        surfaceColor.copy(alpha = 0.85f),
                                                                        surfaceColor.copy(alpha = 0.6f),
                                                                        Color.Transparent
                                                                    )
                                                                )
                                                            )
                                                    )
                                                }

                                                TopAppBar(
                                                    windowInsets = WindowInsets.safeDrawing.only(
                                                        (if (useRail) {
                                                            WindowInsetsSides.Right
                                                        } else {
                                                            WindowInsetsSides.Horizontal
                                                        }) + WindowInsetsSides.Top
                                                    ),
                                                    title = {
                                                        Column(
                                                            verticalArrangement = Arrangement.spacedBy(1.dp),
                                                        ) {
                                                            Text(
                                                                text = stringResource(R.string.app_name),
                                                                style = MaterialTheme.typography.headlineMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                            )
                                                            Text(
                                                                text = "MUSIC",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                            )
                                                        }
                                                    },
                                                    actions = {
                                                        LiquidGlassControlCluster(
                                                            pureBlack = pureBlack,
                                                            horizontalSpacing = 0.dp,
                                                            modifier = Modifier.padding(end = 6.dp),
                                                        ) {
                                                            androidx.compose.material3.IconButton(
                                                                modifier = Modifier.size(34.dp),
                                                                onClick = { navController.navigate("history") },
                                                            ) {
                                                                Icon(
                                                                    painter = painterResource(R.drawable.history),
                                                                    contentDescription = stringResource(R.string.history),
                                                                    modifier = Modifier.size(19.dp),
                                                                )
                                                            }

                                                            androidx.compose.material3.IconButton(
                                                                modifier = Modifier.size(34.dp),
                                                                onClick = { navController.navigate("stats") },
                                                            ) {
                                                                Icon(
                                                                    painter = painterResource(R.drawable.stats),
                                                                    contentDescription = stringResource(R.string.stats),
                                                                    modifier = Modifier.size(19.dp),
                                                                )
                                                            }

                                                            androidx.compose.material3.IconButton(
                                                                modifier = Modifier.size(34.dp),
                                                                onClick = { navController.navigate("new_release") },
                                                            ) {
                                                                Icon(
                                                                    painter = painterResource(R.drawable.notifications),
                                                                    contentDescription = stringResource(R.string.new_release_albums),
                                                                    modifier = Modifier.size(19.dp),
                                                                )
                                                            }
                                                        }

                                                        androidx.compose.material3.IconButton(
                                                            modifier = Modifier.size(42.dp),
                                                            onClick = { onShowAccountDialog() },
                                                        ) {
                                                            BadgedBox(
                                                                badge = {
                                                                    if (!Updater.isSameVersion(
                                                                            latestVersionName,
                                                                            BuildConfig.VERSION_NAME
                                                                        )
                                                                    ) {
                                                                        Badge()
                                                                    }
                                                                }
                                                            ) {
                                                                if (accountImageUrl != null) {
                                                                    AsyncImage(
                                                                        model = accountImageUrl,
                                                                        contentDescription = stringResource(R.string.account),
                                                                        modifier = Modifier
                                                                            .size(32.dp)
                                                                            .clip(CircleShape),
                                                                    )
                                                                } else {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(32.dp)
                                                                            .clip(CircleShape)
                                                                            .background(
                                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                                            ),
                                                                        contentAlignment = Alignment.Center,
                                                                    ) {
                                                                        Icon(
                                                                            painter = painterResource(R.drawable.account),
                                                                            contentDescription = stringResource(R.string.account),
                                                                            tint = MaterialTheme.colorScheme.primary,
                                                                            modifier = Modifier.size(20.dp),
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    },
                                                    scrollBehavior = if (shouldUseFloatingTopBar) {
                                                        searchBarScrollBehavior
                                                    } else {
                                                        topAppBarScrollBehavior
                                                    },
                                                    colors = TopAppBarDefaults.topAppBarColors(
                                                        containerColor = if (shouldUseFloatingTopBar) {
                                                            Color.Transparent
                                                        } else if (pureBlack) {
                                                            Color.Black
                                                        } else {
                                                            MaterialTheme.colorScheme.surface
                                                        },
                                                        scrolledContainerColor = if (shouldUseFloatingTopBar) {
                                                            Color.Transparent
                                                        } else if (pureBlack) {
                                                            Color.Black
                                                        } else {
                                                            MaterialTheme.colorScheme.surface
                                                        },
                                                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                                                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }
                                        }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalTextApi::class,
)
@Composable
internal fun MainActivitySearchBar(
    pureBlack: Boolean,
    database: MusicDatabase,
    navigationState: MainActivityNavigationState,
    playerUiState: MainActivityPlayerUiState,
    searchBarAlignmentModifier: Modifier,
) {
    val navController = navigationState.navController
    val navBackStackEntry = navigationState.navBackStackEntry
    val navigationItems = navigationState.navigationItems
    val query = navigationState.query
    val onQueryChange = navigationState.onQueryChange
    val onSearch = navigationState.onSearch
    val active = navigationState.active
    val onActiveChange = navigationState.onActiveChange
    val searchSource = navigationState.searchSource
    val searchBarFocusRequester = navigationState.searchBarFocusRequester
    val pauseSearchHistory = navigationState.pauseSearchHistory
    val playerBottomSheetState = playerUiState.bottomSheetState

                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = active || navBackStackEntry?.destination?.route?.startsWith("search/") == true,
                                            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                                            exit = fadeOut(animationSpec = tween(durationMillis = 200))
                                        ) {
                                            TopSearch(
                                                query = query,
                                                onQueryChange = onQueryChange,
                                                onSearch = onSearch,
                                                active = active,
                                                onActiveChange = onActiveChange,
                                                placeholder = {
                                                    Text(
                                                        text = stringResource(
                                                            when (searchSource) {
                                                                SearchSource.UNIVERSAL -> R.string.search_all_sources
                                                                SearchSource.LOCAL -> R.string.search_library
                                                                SearchSource.ONLINE -> R.string.search_yt_music
                                                                SearchSource.DEEZER -> R.string.search_deezer
                                                                SearchSource.SPOTIFY -> R.string.search_spotify
                                                                SearchSource.TIDAL -> R.string.search_tidal
                                                                SearchSource.APPLE_MUSIC -> R.string.search_apple_music
                                                            }
                                                        ),
                                                    )
                                                },
                                                leadingIcon = {
                                                    IconButton(
                                                        onClick = {
                                                            when {
                                                                active -> onActiveChange(false)
                                                                !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                                    navController.navigateUp()
                                                                }

                                                                else -> onActiveChange(true)
                                                            }
                                                        },
                                                        onLongClick = {
                                                            when {
                                                                active -> {}
                                                                !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                                    navController.backToMain()
                                                                }
                                                                else -> {}
                                                            }
                                                        },
                                                    ) {
                                                        Icon(
                                                            painterResource(
                                                                if (active ||
                                                                    !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }
                                                                ) {
                                                                    R.drawable.arrow_back
                                                                } else {
                                                                    R.drawable.search
                                                                },
                                                            ),
                                                            contentDescription = null,
                                                        )
                                                    }
                                                },
                                                trailingIcon = {
                                                    Row {
                                                        if (active) {
                                                            if (query.text.isNotEmpty()) {
                                                                IconButton(
                                                                    onClick = {
                                                                        onQueryChange(
                                                                            TextFieldValue(
                                                                                ""
                                                                            )
                                                                        )
                                                                    },
                                                                ) {
                                                                    Icon(
                                                                        painter = painterResource(R.drawable.close),
                                                                        contentDescription = null,
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier =
                                                    Modifier
                                                        .focusRequester(searchBarFocusRequester)
                                                        .then(searchBarAlignmentModifier),
                                                focusRequester = searchBarFocusRequester,
                                                colors = if (pureBlack && active) {
                                                    SearchBarDefaults.colors(
                                                        containerColor = Color.Black,
                                                        dividerColor = Color.DarkGray,
                                                        inputFieldColors = TextFieldDefaults.colors(
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.Gray,
                                                            focusedContainerColor = Color.Transparent,
                                                            unfocusedContainerColor = Color.Transparent,
                                                            cursorColor = Color.White,
                                                            focusedIndicatorColor = Color.Transparent,
                                                            unfocusedIndicatorColor = Color.Transparent,
                                                        )
                                                    )
                                                } else {
                                                    SearchBarDefaults.colors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                                    )
                                                }
                                            ) {
                                                SearchSourceSelector(
                                                    selected = searchSource,
                                                    onSelected = { source ->
                                                        navigationState.setSearchSource(source)
                                                    },
                                                    modifier = Modifier
                                                        .align(Alignment.CenterHorizontally)
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                )

                                                AnimatedContent(
                                                    targetState = searchSource,
                                                    transitionSpec = {
                                                        val direction =
                                                            if (targetState.ordinal >= initialState.ordinal) 1 else -1
                                                        (
                                                            slideInHorizontally(
                                                                animationSpec = spring(
                                                                    dampingRatio = 0.90f,
                                                                    stiffness = 560f,
                                                                ),
                                                                initialOffsetX = { width -> direction * width / 6 },
                                                            ) + fadeIn(tween(120))
                                                        ) togetherWith (
                                                            slideOutHorizontally(
                                                                animationSpec = tween(140),
                                                                targetOffsetX = { width -> -direction * width / 8 },
                                                            ) + fadeOut(tween(100))
                                                        )
                                                    },
                                                    label = "searchSourceSlide",
                                                    modifier =
                                                        Modifier
                                                            .fillMaxSize()
                                                            .padding(bottom = if (!playerBottomSheetState.isDismissed) MiniPlayerHeight else 0.dp)
                                                            .navigationBarsPadding(),
                                                ) { searchSource ->
                                                    when (searchSource) {
                                                        SearchSource.UNIVERSAL ->
                                                            UniversalSearchScreen(
                                                                query = query.text,
                                                                pureBlack = pureBlack,
                                                            )

                                                        SearchSource.LOCAL ->
                                                            LocalSearchScreen(
                                                                query = query.text,
                                                                navController = navController,
                                                                onDismiss = { onActiveChange(false) },
                                                                pureBlack = pureBlack,
                                                            )

                                                        SearchSource.ONLINE ->
                                                            OnlineSearchScreen(
                                                                query = query.text,
                                                                onQueryChange = onQueryChange,
                                                                navController = navController,
                                                                onSearch = { searchedQuery ->
                                                                    navController.navigate(
                                                                        "search/" + URLEncoder.encode(searchedQuery, "UTF-8")
                                                                    )
                                                                    if (!pauseSearchHistory) {
                                                                        database.query {
                                                                            insert(SearchHistory(query = searchedQuery))
                                                                        }
                                                                    }
                                                                },
                                                                onDismiss = { onActiveChange(false) },
                                                                pureBlack = pureBlack
                                                            )

                                                        SearchSource.DEEZER ->
                                                            DeezerSearchScreen(
                                                                query = query.text,
                                                                pureBlack = pureBlack,
                                                            )

                                                        SearchSource.SPOTIFY ->
                                                            SpotifySearchScreen(
                                                                query = query.text,
                                                                pureBlack = pureBlack,
                                                            )

                                                        SearchSource.TIDAL ->
                                                            TidalSearchScreen(
                                                                query = query.text,
                                                                pureBlack = pureBlack,
                                                            )

                                                        SearchSource.APPLE_MUSIC ->
                                                            AppleMusicSearchScreen(
                                                                query = query.text,
                                                                pureBlack = pureBlack,
                                                            )
                                                    }
                                                }
                                            }
                                        }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalTextApi::class,
)
@Composable
internal fun BoxScope.MainActivityFloatingToolbar(
    pureBlack: Boolean,
    useRail: Boolean,
    bottomInset: Dp,
    database: MusicDatabase,
    playerConnection: PlayerConnection?,
    navigationState: MainActivityNavigationState,
    playerUiState: MainActivityPlayerUiState,
    navigationEffects: MainActivityNavigationEffects,
) {
    val navigationItems = navigationState.navigationItems
    val navController = navigationState.navController
    val allLocalItems = navigationState.allLocalItems
    val allYtItems = navigationState.allYtItems
    val shouldShowHomeShuffleButton = navigationState.shouldShowHomeShuffleButton
    val selectedNavigationRoute = navigationState.selectedNavigationRoute
    val onActiveChange = navigationState.onActiveChange
    val active = navigationState.active
    val slimNav = navigationState.slimNav
    val playerBottomSheetState = playerUiState.bottomSheetState
    val searchBarScrollBehavior = navigationEffects.searchBarScrollBehavior
    val coroutineScope = rememberCoroutineScope()
    val floatingBarsBottomPadding = FloatingToolbarBottomPadding
    val navVisibleHeight = if (slimNav) SlimFloatingToolbarHeight else FloatingToolbarHeight
    val bottomNavigationBarHeight by animateDpAsState(
        targetValue =
            if (navigationState.shouldShowNavigationBar && !useRail) {
                navVisibleHeight
            } else {
                0.dp
            },
        animationSpec = NavigationBarAnimationSpec,
        label = "",
    )

                                if (!useRail) {
                                    val navSlideDistance =
                                        bottomInset + floatingBarsBottomPadding + navVisibleHeight

                                    Box(
                                        modifier =
                                            Modifier
                                                .align(Alignment.BottomCenter)
                                                .height(navSlideDistance)
                                                .offset {
                                                    if (bottomNavigationBarHeight == 0.dp) {
                                                        IntOffset(
                                                            x = 0,
                                                            y = navSlideDistance.roundToPx(),
                                                        )
                                                    } else {
                                                        val slideOffset =
                                                            navSlideDistance *
                                                                    playerBottomSheetState.progress.coerceIn(
                                                                        0f,
                                                                        1f,
                                                                    )
                                                        val hideOffset =
                                                            navSlideDistance *
                                                                    (1 - bottomNavigationBarHeight.coerceAtMost(
                                                                        navVisibleHeight
                                                                    ) / navVisibleHeight)
                                                        IntOffset(
                                                            x = 0,
                                                            y = (slideOffset + hideOffset).roundToPx(),
                                                        )
                                                    }
                                                },
                                    ) {
                                        FloatingNavigationToolbar(
                                            items = navigationItems,
                                            pureBlack = pureBlack,
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(
                                                    start = FloatingToolbarHorizontalPadding,
                                                    end = FloatingToolbarHorizontalPadding,
                                                    bottom = bottomInset + floatingBarsBottomPadding,
                                                )
                                                .height(navVisibleHeight),
                                            onShuffleClick = if (shouldShowHomeShuffleButton) {
                                                {
                                                    val useLocalSource = when {
                                                        allLocalItems.isNotEmpty() && allYtItems.isNotEmpty() -> Random.nextFloat() < 0.5f
                                                        allLocalItems.isNotEmpty() -> true
                                                        else -> false
                                                    }

                                                    coroutineScope.launch(Dispatchers.Main) {
                                                        if (useLocalSource) {
                                                            when (val luckyItem = allLocalItems.random()) {
                                                                is Song -> {
                                                                    playerConnection?.playQueue(
                                                                        YouTubeQueue.radio(luckyItem.toMediaMetadata())
                                                                    )
                                                                }

                                                                is Album -> {
                                                                    val albumWithSongs = withContext(Dispatchers.IO) {
                                                                        database.albumWithSongs(luckyItem.id).first()
                                                                    }

                                                                    albumWithSongs?.let {
                                                                        playerConnection?.playQueue(LocalAlbumRadio(it))
                                                                    }
                                                                }

                                                                is Artist -> Unit
                                                                is Playlist -> Unit
                                                            }
                                                        } else {
                                                            when (val luckyItem = allYtItems.random()) {
                                                                is SongItem -> {
                                                                    playerConnection?.playQueue(
                                                                        YouTubeQueue.radio(luckyItem.toMediaMetadata())
                                                                    )
                                                                }

                                                                is AlbumItem -> {
                                                                    playerConnection?.playQueue(
                                                                        YouTubeAlbumRadio(luckyItem.playlistId)
                                                                    )
                                                                }

                                                                is ArtistItem -> {
                                                                    luckyItem.radioEndpoint?.let {
                                                                        playerConnection?.playQueue(YouTubeQueue(it))
                                                                    }
                                                                }

                                                                is PlaylistItem -> {
                                                                    luckyItem.playEndpoint?.let {
                                                                        playerConnection?.playQueue(YouTubeQueue(it))
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else null,
                                            shuffleIconRes = if (shouldShowHomeShuffleButton) R.drawable.shuffle else null,
                                            shuffleContentDescription = if (shouldShowHomeShuffleButton) stringResource(R.string.shuffle) else "",
                                            onMusicRecognitionClick = if (shouldShowHomeShuffleButton) {
                                                { navController.navigate(MusicRecognitionRoute) }
                                            } else null,
                                            musicRecognitionContentDescription = if (shouldShowHomeShuffleButton) stringResource(R.string.music_recognition) else "",
                                            selectedRoute = selectedNavigationRoute,
                                            onItemClick = { screen, isSelected ->
                                                if (screen.route == Screens.Search.route) {
                                                    onActiveChange(true)
                                                } else if (isSelected && !active) {
                                                    navController.currentBackStackEntry?.savedStateHandle?.set(
                                                        "scrollToTop",
                                                        true
                                                    )
                                                    coroutineScope.launch {
                                                        searchBarScrollBehavior.state.resetHeightOffset()
                                                    }
                                                } else {
                                                    if (active) {
                                                        onActiveChange(false)
                                                    }
                                                    navigationState.setPendingNavigationRoute(screen.route)
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.startDestinationId) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
}

@Composable
internal fun BoxScope.MainActivityOverlays(
    bottomSheetPageState: BottomSheetPageState,
    navigationState: MainActivityNavigationState,
    latestVersionName: String,
    playerConnection: PlayerConnection?,
    showAccountDialog: Boolean,
    onDismissAccountDialog: () -> Unit,
) {
    BottomSheetMenu(
        state = LocalMenuState.current,
        modifier = Modifier.align(Alignment.BottomCenter),
    )

    BottomSheetPage(
        state = bottomSheetPageState,
        modifier = Modifier.align(Alignment.BottomCenter),
    )

    if (showAccountDialog) {
        AccountSettingsDialog(
            navController = navigationState.navController,
            onDismiss = onDismissAccountDialog,
            latestVersionName = latestVersionName,
        )
    }

    var sharedSong: SongItem? by remember { mutableStateOf(null) }
    sharedSong?.let { song ->
        playerConnection?.let {
            Dialog(
                onDismissRequest = { sharedSong = null },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Surface(
                    modifier = Modifier.padding(24.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = AlertDialogDefaults.TonalElevation,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        YouTubeSongMenu(
                            song = song,
                            navController = navigationState.navController,
                            onDismiss = { sharedSong = null },
                        )
                    }
                }
            }
        }
    }
}
