// NavigationBuilder.kt

package com.arturo254.opentune.ui.original.screens

import com.arturo254.opentune.ui.original.adapters.HomeScreen
import com.arturo254.opentune.ui.original.adapters.ComingSoonScreen
import com.arturo254.opentune.ui.screens.HistoryScreen
import com.arturo254.opentune.ui.screens.BrowseScreen
import com.arturo254.opentune.ui.screens.AlbumScreen
import com.arturo254.opentune.ui.screens.YouTubeBrowseScreen
import android.app.Activity
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.arturo254.opentune.db.entities.FormatEntity
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable as navigationComposable
import androidx.navigation.compose.dialog
import com.arturo254.opentune.ui.original.utils.motionEnter
import com.arturo254.opentune.ui.original.utils.motionExit
import androidx.navigation.navArgument
import com.arturo254.opentune.R
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.constants.DarkModeKey
import com.arturo254.opentune.foundation.theme.ForceDarkTheme
import com.arturo254.opentune.constants.PlaylistSortType
import com.arturo254.opentune.constants.PureBlackKey
import com.arturo254.opentune.db.entities.ArtistEntity
import com.arturo254.opentune.db.entities.SongArtistMap
import com.arturo254.opentune.db.entities.SongEntity
import com.arturo254.opentune.ui.original.component.ContainerCloseEasing
import com.arturo254.opentune.ui.original.component.ContainerCloseMillis
import com.arturo254.opentune.ui.original.component.ContainerOpenDamping
import com.arturo254.opentune.ui.original.component.ContainerOpenStiffness
import com.arturo254.opentune.ui.original.component.LocalNavAnimatedContentScope
import com.arturo254.opentune.ui.component.NavigationTitle
import com.arturo254.opentune.ui.screens.artist.ArtistAlbumsScreen
import com.arturo254.opentune.ui.screens.artist.ArtistItemsScreen
import com.arturo254.opentune.ui.screens.artist.ArtistScreen
import com.arturo254.opentune.ui.screens.artist.ArtistSongsScreen
import com.arturo254.opentune.ui.screens.playlist.AutoPlaylistScreen
import com.arturo254.opentune.ui.screens.playlist.CachePlaylistScreen
import com.arturo254.opentune.ui.screens.playlist.LocalPlaylistScreen
import com.arturo254.opentune.ui.screens.playlist.OnlinePlaylistScreen
import com.arturo254.opentune.ui.screens.playlist.TopPlaylistScreen
import com.arturo254.opentune.ui.original.adapters.GenreScreen
import com.arturo254.opentune.ui.original.adapters.OnlineSearchResult
import com.arturo254.opentune.ui.original.adapters.SearchScreen
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.ui.original.adapters.LibraryAlbumsScreen
import com.arturo254.opentune.ui.original.adapters.SectionDetailScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arturo254.opentune.viewmodels.HomeViewModel
import com.arturo254.opentune.ui.original.adapters.LibraryArtistsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.UUID

private val CoverRoutes = listOf("album/", "local_playlist/", "online_playlist/", "auto_playlist/")

private fun String?.opensFromCover(): Boolean = this != null && CoverRoutes.any { startsWith(it) }

// Home moves on the card's clocks: the open spring going out, the close ease coming back. the
// tiny threshold keeps the spring from snapping the last 1% of the scale at the end.
private val HomeRecedeSpring = spring(
    dampingRatio = ContainerOpenDamping,
    stiffness = ContainerOpenStiffness,
    visibilityThreshold = 0.001f,
)
private val HomeReturnEase = tween<Float>(ContainerCloseMillis, easing = ContainerCloseEasing)

/** Library screens recede like Home when a cover opens from them, and come back on the same clock. */
private fun libraryExitTo(route: String?): androidx.compose.animation.ExitTransition? =
    if (route.opensFromCover()) {
        scaleOut(HomeRecedeSpring, targetScale = 0.92f) + fadeOut(HomeRecedeSpring, targetAlpha = 0.45f)
    } else {
        null
    }

/** coming back from a cover page: the same clock as Home; anything else keeps the default */
private fun coverReturnFrom(route: String?): androidx.compose.animation.EnterTransition? =
    if (route.opensFromCover()) {
        scaleIn(HomeReturnEase, initialScale = 0.92f) + fadeIn(HomeReturnEase, initialAlpha = 0.45f)
    } else {
        null
    }

/**
 * a screen an album or playlist can be opened from (the Library's pages, Search, an artist,
 * Home's section pages): it hands its transition scope to its covers, so the page grows out of
 * the one tapped, and it sinks back while the page is open, exactly as Home does
 */
private fun NavGraphBuilder.coverSourceComposable(
    route: String,
    arguments: List<androidx.navigation.NamedNavArgument> = emptyList(),
    content: @Composable androidx.compose.animation.AnimatedContentScope.(androidx.navigation.NavBackStackEntry) -> Unit,
) = composable(
    route = route,
    arguments = arguments,
    exitTransition = { libraryExitTo(targetState.destination.route) },
    popEnterTransition = { coverReturnFrom(initialState.destination.route) },
) { entry ->
    CompositionLocalProvider(LocalNavAnimatedContentScope provides this) {
        content(entry)
    }
}

private fun libraryReturnFrom(route: String?): androidx.compose.animation.EnterTransition =
    if (route.opensFromCover()) {
        scaleIn(HomeReturnEase, initialScale = 0.92f) + fadeIn(HomeReturnEase, initialAlpha = 0.45f)
    } else {
        fadeIn(tween(150))
    }

private typealias NavEnter =
    androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition?
private typealias NavExit =
    androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition?

/**
 * Every page of this graph goes through here rather than straight to navigation's composable(), so
 * its own transitions answer Settings > Appearance > Animation (#10): off, the page swaps in place.
 * Same parameters and defaults as navigation's (a pop transition falls back to the push one, and a
 * transition that isn't given, or gives null, falls back to the NavHost's).
 */
private fun NavGraphBuilder.composable(
    route: String,
    arguments: List<androidx.navigation.NamedNavArgument> = emptyList(),
    enterTransition: NavEnter? = null,
    exitTransition: NavExit? = null,
    popEnterTransition: NavEnter? = enterTransition,
    popExitTransition: NavExit? = exitTransition,
    content: @Composable androidx.compose.animation.AnimatedContentScope.(androidx.navigation.NavBackStackEntry) -> Unit,
) = navigationComposable(
    route = route,
    arguments = arguments,
    enterTransition = { motionEnter { enterTransition?.invoke(this) } },
    exitTransition = { motionExit { exitTransition?.invoke(this) } },
    popEnterTransition = { motionEnter { popEnterTransition?.invoke(this) } },
    popExitTransition = { motionExit { popExitTransition?.invoke(this) } },
    content = content,
)

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.originalNavigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    activity: Activity?,
    snackbarHostState: SnackbarHostState,
    latestVersionName: String,
) {
    composable(
        route = Screens.Home.route,
        // opening a cover: Home sinks back and dims while the card grows toward you, then comes
        // forward again on the way back, in step with the card so a back swipe scrubs both
        exitTransition = {
            if (targetState.destination.route.opensFromCover()) {
                scaleOut(HomeRecedeSpring, targetScale = 0.92f) + fadeOut(HomeRecedeSpring, targetAlpha = 0.45f)
            } else {
                null
            }
        },
        popEnterTransition = {
            if (initialState.destination.route.opensFromCover()) {
                scaleIn(HomeReturnEase, initialScale = 0.92f) + fadeIn(HomeReturnEase, initialAlpha = 0.45f)
            } else {
                fadeIn(tween(150))
            }
        },
    ) {

        CompositionLocalProvider(LocalNavAnimatedContentScope provides this) {
            HomeScreen(navController = navController, snackbarHostState = snackbarHostState)
        }
    }

    coverSourceComposable(
        route = "section_detail/{sectionId}",
        arguments = listOf(
            navArgument("sectionId") {
                type = NavType.StringType
            },
        ),
    ) { backStackEntry ->
        val sectionId = backStackEntry.arguments?.getString("sectionId") ?: return@coverSourceComposable

        val homeViewModel: HomeViewModel = hiltViewModel(navController.getBackStackEntry(Screens.Home.route))
        SectionDetailScreen(
            navController = navController,
            sectionId = sectionId,
            homeViewModel = homeViewModel
        )
    }

    coverSourceComposable("history") {
        HistoryScreen(navController = navController)
    }

    composable(
        route = "coming_soon/{sectionTitle}",
        arguments = listOf(
            navArgument("sectionTitle") {
                type = NavType.StringType
            },
        ),
    ) { backStackEntry ->
        val sectionTitle = backStackEntry.arguments?.getString("sectionTitle") ?: return@composable
        ComingSoonScreen(navController = navController, sectionTitle = sectionTitle)
    }

    coverSourceComposable(Screens.Search.route) {
        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val useDarkTheme = ForceDarkTheme
        val pureBlack = remember(pureBlackEnabled, useDarkTheme) {
            pureBlackEnabled && useDarkTheme
        }
        SearchScreen(
            navController = navController,
            pureBlack = pureBlack
        )
    }

    composable(
        route = "library",
        // a playlist opening from its cover: the Library sinks back while it grows, as Home does
        exitTransition = { libraryExitTo(targetState.destination.route) },
        popEnterTransition = { libraryReturnFrom(initialState.destination.route) },
    ) {
        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val useDarkTheme = ForceDarkTheme
        CompositionLocalProvider(LocalNavAnimatedContentScope provides this) {
            com.arturo254.opentune.ui.original.adapters.LibraryHomeScreen(
                navController = navController,
                pureBlack = remember(pureBlackEnabled, useDarkTheme) { pureBlackEnabled && useDarkTheme },
            )
        }
    }

    coverSourceComposable("library/songs") {
        com.arturo254.opentune.ui.original.adapters.LibrarySongsScreen(navController = navController)
    }

    composable(
        route = "library/playlists",
        exitTransition = { libraryExitTo(targetState.destination.route) },
        popEnterTransition = { libraryReturnFrom(initialState.destination.route) },
    ) {
        // the scope a playlist's cover needs to grow into its page, as on Home
        CompositionLocalProvider(LocalNavAnimatedContentScope provides this) {
            com.arturo254.opentune.ui.original.adapters.LibraryPlaylistsScreen(navController = navController)
        }
    }

    coverSourceComposable("library/added") {
        com.arturo254.opentune.ui.original.adapters.LibraryRecentlyAddedScreen(navController = navController)
    }

    // The Library's Local and Cached tabs open these: the same pages again, counting only that
    // tab's songs. "library/local/albums", "library/cached/albums" and so on.
    com.arturo254.opentune.ui.original.adapters.RouteShelf.entries.forEach { shelf ->
        coverSourceComposable("library/${shelf.key}/added") {
            com.arturo254.opentune.ui.original.adapters.LibraryRecentlyAddedScreen(navController = navController, shelf = shelf)
        }

        coverSourceComposable("library/${shelf.key}/albums") {
            com.arturo254.opentune.ui.original.adapters.LibraryAlbumsScreen(navController = navController, shelf = shelf)
        }

        coverSourceComposable("library/${shelf.key}/artists") {
            com.arturo254.opentune.ui.original.adapters.LibraryArtistsScreen(navController = navController, shelf = shelf)
        }

        composable(
            route = "library/${shelf.key}/playlists",
            exitTransition = { libraryExitTo(targetState.destination.route) },
            popEnterTransition = { libraryReturnFrom(initialState.destination.route) },
        ) {
            CompositionLocalProvider(LocalNavAnimatedContentScope provides this) {
                com.arturo254.opentune.ui.original.adapters.LibraryPlaylistsScreen(navController = navController, shelf = shelf)
            }
        }
    }

    // Everything one service brought in, whatever playlist it went into: Imported from Spotify.
    coverSourceComposable(
        route = "library/imported/{source}",
        arguments = listOf(navArgument("source") { type = NavType.StringType }),
    ) {
        com.arturo254.opentune.ui.original.adapters.LibraryImportedScreen(navController = navController)
    }

    // The Imported tab's Playlists cards: the playlists one service brought in.
    composable(
        route = "library/imported/{source}/playlists",
        arguments = listOf(navArgument("source") { type = NavType.StringType }),
        exitTransition = { libraryExitTo(targetState.destination.route) },
        popEnterTransition = { libraryReturnFrom(initialState.destination.route) },
    ) { entry ->
        CompositionLocalProvider(LocalNavAnimatedContentScope provides this) {
            com.arturo254.opentune.ui.original.adapters.LibraryPlaylistsScreen(
                navController = navController,
                source = com.arturo254.opentune.ui.original.adapters.sourceKey(entry.arguments?.getString("source")),
            )
        }
    }

    // the Imported tab's Albums cards: the albums one service's songs come from
    coverSourceComposable(
        route = "library/imported/{source}/albums",
        arguments = listOf(navArgument("source") { type = NavType.StringType }),
    ) {
        com.arturo254.opentune.ui.original.adapters.LibraryAlbumsScreen(
            navController = navController,
            source = com.arturo254.opentune.ui.original.adapters.sourceKey(it.arguments?.getString("source")),
        )
    }

    composable(Screens.Settings.route) {
        com.arturo254.opentune.ui.screens.settings.SettingsScreen(navController, scrollBehavior, latestVersionName)
    }

    composable("stats") {
        com.arturo254.opentune.ui.screens.StatsScreen(navController = navController)
    }

    composable("cipher_settings") {
        com.arturo254.opentune.ui.original.adapters.CipherSettingsScreen(navController = navController)
    }

    composable("playback_diagnostics") {
        com.arturo254.opentune.ui.original.adapters.PlaybackDiagnosticsScreen(navController = navController)
    }

    composable("mochy_settings") {
        // on a wide screen the list itself stays at the left (MainActivity), so this side waits for a pick
        if (com.arturo254.opentune.ui.original.utils.isWideLayout()) {
            com.arturo254.opentune.ui.original.adapters.SettingsPanePlaceholder(navController)
        } else {
            com.arturo254.opentune.ui.original.adapters.MochySettingsScreen(navController = navController)
        }
    }

    // app behaviour kept general's route, so old links and search results still land on it
    composable("general_settings") {
        com.arturo254.opentune.ui.original.adapters.AppBehaviourSettingsScreen(navController = navController)
    }

    composable("integrations_settings") {
        com.arturo254.opentune.ui.original.adapters.IntegrationsScreen(navController = navController)
    }

    composable("integrations_settings/discord") {
        com.arturo254.opentune.ui.original.adapters.DiscordScreen(navController = navController)
    }

    composable(
        route = "integrations_settings/scrobble/{site}",
        arguments = listOf(navArgument("site") { type = NavType.StringType }),
    ) { entry ->
        val site = entry.arguments?.getString("site") ?: "LASTFM"
        com.arturo254.opentune.ui.original.adapters.ScrobbleServiceScreen(navController = navController, site = site)
    }

    composable("proxy_settings") {
        com.arturo254.opentune.ui.original.adapters.ProxySettingsScreen(navController = navController)
    }

    composable("crossmix_settings") {
        com.arturo254.opentune.ui.original.adapters.CrossmixSettingsScreen(navController = navController)
    }

    composable(
        route = "developer_space?person={person}",
        arguments = listOf(navArgument("person") { type = NavType.IntType; defaultValue = 0 }),
    ) { entry ->
        val person = entry.arguments?.getInt("person") ?: 0
        com.arturo254.opentune.ui.original.adapters.DeveloperSpaceScreen(
            navController = navController,
            initialPage = person,
        )
    }

    composable("developer_playground") {
        com.arturo254.opentune.ui.original.adapters.DevPlaygroundScreen(navController = navController)
    }

    composable("storage_settings") {
        com.arturo254.opentune.ui.original.adapters.StorageScreen(navController = navController)
    }

    composable("experimental_settings") {
        com.arturo254.opentune.ui.original.adapters.ExperimentalSettingsScreen(navController = navController)
    }

    composable("import_sync") {
        com.arturo254.opentune.ui.original.adapters.ImportSyncScreen(navController = navController)
    }

    composable("tv_link") {
        com.arturo254.opentune.ui.original.adapters.TvLinkScreen(navController = navController)
    }

    composable("import_providers") {
        com.arturo254.opentune.ui.original.adapters.ImportProvidersScreen(navController = navController)
    }

    composable("import_tunemymusic") {
        com.arturo254.opentune.ui.original.adapters.TuneMyMusicImportScreen(navController = navController)
    }

    composable("import_progress") {
        com.arturo254.opentune.ui.original.adapters.ImportProgressScreen(navController = navController)
    }

    composable("youtube_sync") {
        com.arturo254.opentune.ui.original.adapters.YouTubeSyncScreen(navController = navController)
    }

    composable(
        route = "account_import/{service}?resync={resync}&then={then}",
        arguments = listOf(
            navArgument("service") { type = NavType.StringType },
            // what a Re-sync opened the page for, and the services Re-sync all goes on to
            navArgument("resync") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("then") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) {
        com.arturo254.opentune.ui.original.adapters.AccountImportScreen(navController = navController)
    }

    composable("youtube_login") {
        com.arturo254.opentune.ui.original.adapters.YouTubeLoginScreen(navController = navController)
    }

    composable("equalizer") {
        com.arturo254.opentune.ui.original.adapters.EqualizerScreen(navController = navController)
    }

    composable("gesture_settings") {
        com.arturo254.opentune.ui.original.adapters.GestureScreen(navController = navController)
    }

    composable("customisation_settings") {
        com.arturo254.opentune.ui.original.adapters.CustomisationScreen(navController = navController)
    }

    composable("audio_lab") {
        com.arturo254.opentune.ui.original.adapters.AudioLabScreen(navController = navController)
    }

    composable("audio_fx") {
        com.arturo254.opentune.ui.original.adapters.AudioFxScreen(navController = navController)
    }

    composable("audio_quality") {
        com.arturo254.opentune.ui.original.adapters.AudioQualityScreen(navController = navController)
    }

    composable("audio_pass") {
        com.arturo254.opentune.ui.original.adapters.AudioPassScreen(navController = navController)
    }

    composable("modules") {
        com.arturo254.opentune.ui.original.adapters.ModulesScreen(navController = navController)
    }

    composable(
        route = "module/{id}",
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { entry ->
        com.arturo254.opentune.ui.original.adapters.ModuleScreen(
            navController = navController,
            id = entry.arguments?.getString("id").orEmpty(),
        )
    }

    composable(
        route = "module_page/{id}/{page}",
        arguments = listOf(navArgument("id") { type = NavType.StringType }, navArgument("page") { type = NavType.StringType }),
    ) { entry ->
        com.arturo254.opentune.ui.original.adapters.ModulePageScreen(
            navController = navController,
            id = entry.arguments?.getString("id").orEmpty(),
            pageId = entry.arguments?.getString("page").orEmpty(),
        )
    }

    composable(
        route = "browse/{browseId}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
            }
        )
    ) {
        BrowseScreen(
            navController,
            scrollBehavior,
            it.arguments?.getString("browseId")
        )
    }

    composable(
        route = "search/{query}",
        arguments = listOf(
            navArgument("query") {
                type = NavType.StringType
            },
        ),
        enterTransition = {
            fadeIn(tween(250))
        },
        exitTransition = {
            when {
                targetState.destination.route.opensFromCover() -> libraryExitTo(targetState.destination.route)
                targetState.destination.route?.startsWith("search/") == true -> fadeOut(tween(200))
                else -> fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            when {
                initialState.destination.route.opensFromCover() -> coverReturnFrom(initialState.destination.route)
                initialState.destination.route?.startsWith("search/") == true -> fadeIn(tween(250))
                else -> fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            fadeOut(tween(200))
        },
    ) {
        CompositionLocalProvider(LocalNavAnimatedContentScope provides this) {
            OnlineSearchResult(navController)
        }
    }

    composable(
        route = "album/{albumId}",
        arguments = listOf(
            navArgument("albumId") {
                type = NavType.StringType
            },
        ),

        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { libraryExitTo(targetState.destination.route) ?: fadeOut(tween(200)) },
        popEnterTransition = { coverReturnFrom(initialState.destination.route) ?: fadeIn(tween(300)) },

        popExitTransition = {
            scaleOut(
                animationSpec = tween(260),
                targetScale = 0.82f,
                transformOrigin = TransformOrigin(0.5f, 0.22f),
            ) + fadeOut(tween(180))
        },
    ) {
        CompositionLocalProvider(LocalNavAnimatedContentScope provides this) {
            AlbumScreen(navController, scrollBehavior)
        }
    }

    coverSourceComposable(
        route = "artist/{artistId}",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistScreen(navController, scrollBehavior)
    }

    coverSourceComposable(
        route = "artist/{artistId}/songs",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }

    coverSourceComposable(
        route = "artist/{artistId}/albums",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            }
        )
    ) {
        ArtistAlbumsScreen(navController, scrollBehavior)
    }

    // "similar to <artist>": the artists, playlists and songs around one artist, from its page
    coverSourceComposable(
        route = "artist/{artistId}/similar",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        com.arturo254.opentune.ui.original.adapters.ArtistSimilarScreen(navController)
    }

    coverSourceComposable(
        route = "artist/{artistId}/items?browseId={browseId}&params={params}",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }

    composable(
        route = "online_playlist/{playlistId}",
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { libraryExitTo(targetState.destination.route) ?: fadeOut(tween(200)) },
        popEnterTransition = { coverReturnFrom(initialState.destination.route) ?: fadeIn(tween(300)) },

        popExitTransition = {
            scaleOut(
                animationSpec = tween(260),
                targetScale = 0.82f,
                transformOrigin = TransformOrigin(0.5f, 0.22f),
            ) + fadeOut(tween(180))
        },
    ) {
        CompositionLocalProvider(LocalNavAnimatedContentScope provides this) {
            OnlinePlaylistScreen(navController, scrollBehavior)
        }
    }

    composable(
        route = "local_playlist/{playlistId}",
        arguments = listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { libraryExitTo(targetState.destination.route) ?: fadeOut(tween(200)) },
        popEnterTransition = { coverReturnFrom(initialState.destination.route) ?: fadeIn(tween(300)) },

        popExitTransition = {
            scaleOut(
                animationSpec = tween(260),
                targetScale = 0.82f,
                transformOrigin = TransformOrigin(0.5f, 0.22f),
            ) + fadeOut(tween(180))
        },
    ) {
        CompositionLocalProvider(LocalNavAnimatedContentScope provides this) {
            LocalPlaylistScreen(navController, scrollBehavior)
        }
    }

    composable(
        route = "auto_playlist/{playlist}",
        arguments = listOf(
            navArgument("playlist") {
                type = NavType.StringType
            },
        ),
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { libraryExitTo(targetState.destination.route) ?: fadeOut(tween(200)) },
        popEnterTransition = { coverReturnFrom(initialState.destination.route) ?: fadeIn(tween(300)) },

        popExitTransition = {
            scaleOut(
                animationSpec = tween(260),
                targetScale = 0.82f,
                transformOrigin = TransformOrigin(0.5f, 0.22f),
            ) + fadeOut(tween(180))
        },
    ) {
        CompositionLocalProvider(LocalNavAnimatedContentScope provides this) {
            AutoPlaylistScreen(navController, scrollBehavior)
        }
    }

    composable(
        route = "cache_playlist/{playlist}",
        arguments = listOf(
            navArgument("playlist") {
                type = NavType.StringType
            },
        ),
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }

    composable(
        route = "top_playlist/{top}",
        arguments = listOf(
            navArgument("top") {
                type = NavType.StringType
            },
        ),
    ) {
        TopPlaylistScreen(navController, scrollBehavior)
    }

    coverSourceComposable(
        route = "genre/{browseId}?params={params}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val useDarkTheme = ForceDarkTheme
        GenreScreen(
            navController = navController,
            pureBlack = remember(pureBlackEnabled, useDarkTheme) {
                pureBlackEnabled && useDarkTheme
            },
        )
    }

    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        YouTubeBrowseScreen(navController)
    }

    coverSourceComposable("library/albums") {
        com.arturo254.opentune.ui.original.adapters.LibraryAlbumsScreen(navController = navController)
    }

    coverSourceComposable("library/artists") {
        com.arturo254.opentune.ui.original.adapters.LibraryArtistsScreen(navController = navController)
    }
}

internal data class LocalAudioMetadata(
    val title: String,
    val artist: String,
    val album: String?,
    val durationSeconds: Int,
    val mimeType: String?,
    val bitrate: Int?,
    val sampleRate: Int?,
    val embeddedArtwork: ByteArray?,
    val albumArtist: String? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
)

internal fun extractAudioMetadata(
    context: android.content.Context,
    uri: Uri,
): LocalAudioMetadata {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            ?: uri.lastPathSegment?.substringBeforeLast('.') ?: "Unknown"
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            ?: "Unknown Artist"
        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
        val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            ?: context.contentResolver.getType(uri)
        val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            ?.toIntOrNull()
        val sampleRate = try {

            if (android.os.Build.VERSION.SDK_INT >= 31) {
                retriever.extractMetadata(38)?.toIntOrNull()
            } else null
        } catch (_: Exception) { null }
        val embeddedArtwork = try { retriever.embeddedPicture } catch (_: Exception) { null }
        // track and disc tags often read "3/12", the part before the slash is the number
        fun number(key: Int) = retriever.extractMetadata(key)?.substringBefore('/')?.trim()?.toIntOrNull()
        LocalAudioMetadata(
            title = title,
            artist = artist,
            album = album,
            durationSeconds = (durationMs / 1000).toInt(),
            mimeType = mimeType,
            bitrate = bitrate,
            sampleRate = sampleRate,
            embeddedArtwork = embeddedArtwork,
            albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
            trackNumber = number(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER),
            discNumber = number(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER),
            year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.take(4)?.toIntOrNull(),
        )
    } catch (e: Exception) {

        LocalAudioMetadata(
            title = uri.lastPathSegment?.substringBeforeLast('.') ?: "Unknown",
            artist = "Unknown Artist",
            album = null,
            durationSeconds = 0,
            mimeType = context.contentResolver.getType(uri),
            bitrate = null,
            sampleRate = null,
            embeddedArtwork = null,
        )
    } finally {
        retriever.release()
    }
}

/** Original routes are registered once; the host adds its additional destinations. */
internal val OriginalNavigationRoutes = setOf(
    "home",
    "search_input",
    "library",
    "settings",
    "section_detail/{sectionId}",
    "history",
    "coming_soon/{sectionTitle}",
    "library/songs",
    "library/playlists",
    "library/added",
    "library/local/added",
    "library/cached/added",
    "library/local/albums",
    "library/cached/albums",
    "library/local/artists",
    "library/cached/artists",
    "library/local/playlists",
    "library/cached/playlists",
    "library/imported/{source}",
    "library/imported/{source}/playlists",
    "library/imported/{source}/albums",
    "stats",
    "cipher_settings",
    "playback_diagnostics",
    "mochy_settings",
    "general_settings",
    "integrations_settings",
    "integrations_settings/discord",
    "integrations_settings/scrobble/{site}",
    "proxy_settings",
    "crossmix_settings",
    "developer_space?person={person}",
    "developer_playground",
    "storage_settings",
    "experimental_settings",
    "import_sync",
    "tv_link",
    "import_providers",
    "import_tunemymusic",
    "import_progress",
    "youtube_sync",
    "account_import/{service}?resync={resync}&then={then}",
    "youtube_login",
    "equalizer",
    "gesture_settings",
    "customisation_settings",
    "audio_lab",
    "audio_fx",
    "audio_quality",
    "audio_pass",
    "modules",
    "module/{id}",
    "module_page/{id}/{page}",
    "browse/{browseId}",
    "search/{query}",
    "album/{albumId}",
    "artist/{artistId}",
    "artist/{artistId}/songs",
    "artist/{artistId}/albums",
    "artist/{artistId}/similar",
    "artist/{artistId}/items?browseId={browseId}&params={params}",
    "online_playlist/{playlistId}",
    "local_playlist/{playlistId}",
    "auto_playlist/{playlist}",
    "cache_playlist/{playlist}",
    "top_playlist/{top}",
    "genre/{browseId}?params={params}",
    "youtube_browse/{browseId}?params={params}",
    "library/albums",
    "library/artists",
)
