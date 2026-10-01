package com.arturo254.opentune

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.util.fastAny
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arturo254.opentune.constants.DefaultOpenTabKey
import com.arturo254.opentune.constants.PauseSearchHistoryKey
import com.arturo254.opentune.constants.SearchSource
import com.arturo254.opentune.constants.SearchSourceKey
import com.arturo254.opentune.constants.SlimNavBarKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.LocalItem
import com.arturo254.opentune.db.entities.SearchHistory
import com.arturo254.opentune.innertube.models.YTItem
import com.arturo254.opentune.ui.component.BottomSheetState
import com.arturo254.opentune.ui.screens.Screens
import com.arturo254.opentune.ui.screens.settings.NavigationTab
import com.arturo254.opentune.ui.utils.appBarScrollBehavior
import com.arturo254.opentune.ui.utils.resetHeightOffset
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.viewmodels.HomeViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal data class MainActivityNavigationState(
    val navController: NavHostController,
    val accountImageUrl: String?,
    val allLocalItems: List<LocalItem>,
    val allYtItems: List<YTItem>,
    val navBackStackEntry: NavBackStackEntry?,
    val currentRoute: String?,
    val normalizedNavigationRoute: String?,
    val pendingNavigationRoute: String?,
    val setPendingNavigationRoute: (String?) -> Unit,
    val isYearInMusicScreen: Boolean,
    val isAlwaysOnDisplayScreen: Boolean,
    val navigationItems: List<Screens>,
    val slimNav: Boolean,
    val defaultOpenTab: NavigationTab,
    val pauseSearchHistory: Boolean,
    val tabOpenedFromShortcut: NavigationTab?,
    val topLevelScreens: List<String>,
    val query: TextFieldValue,
    val onQueryChange: (TextFieldValue) -> Unit,
    val active: Boolean,
    val selectedNavigationRoute: String?,
    val onActiveChange: (Boolean) -> Unit,
    val searchSource: SearchSource,
    val setSearchSource: (SearchSource) -> Unit,
    val searchBarFocusRequester: FocusRequester,
    val onSearch: (String) -> Unit,
    val openSearchImmediately: Boolean,
    val setOpenSearchImmediately: (Boolean) -> Unit,
    val shouldShowSearchBar: Boolean,
    val shouldShowNavigationBar: Boolean,
    val shouldShowHomeShuffleButton: Boolean,
    val shouldShowTopBar: Boolean,
    val currentTitleRes: Int?,
)

@Composable
internal fun rememberMainActivityNavigationState(
    activity: ComponentActivity,
    database: MusicDatabase,
): MainActivityNavigationState {
    val focusManager = LocalFocusManager.current
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = hiltViewModel()

    val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()
    val allLocalItems by homeViewModel.allLocalItems.collectAsState()
    val allYtItems by homeViewModel.allYtItems.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val normalizedNavigationRoute =
        if (currentRoute?.startsWith("search/") == true) {
            Screens.Search.route
        } else {
            currentRoute
        }

    var pendingNavigationRoute by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(normalizedNavigationRoute) {
        if (pendingNavigationRoute == normalizedNavigationRoute) {
            pendingNavigationRoute = null
        }
    }

    val isYearInMusicScreen = currentRoute == "year_in_music"
    val isAlwaysOnDisplayScreen = currentRoute == "always_on_display"
    val navigationItems = remember { Screens.MainScreens }
    val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
    val defaultOpenTab by rememberEnumPreference(DefaultOpenTabKey, NavigationTab.HOME)
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)

    val tabOpenedFromShortcut =
        remember {
            when (activity.intent?.action) {
                MainActivity.ACTION_LIBRARY -> NavigationTab.LIBRARY
                MainActivity.ACTION_SEARCH -> NavigationTab.SEARCH
                else -> null
            }
        }

    val topLevelScreens =
        remember {
            listOf(
                Screens.Home.route,
                Screens.Search.route,
                Screens.MoodAndGenres.route,
                Screens.Library.route,
                "settings",
            )
        }

    val (query, onQueryChange) =
        rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue())
        }

    var active by rememberSaveable {
        mutableStateOf(false)
    }

    val selectedNavigationRoute =
        when {
            active -> Screens.Search.route
            pendingNavigationRoute != null -> pendingNavigationRoute
            else -> normalizedNavigationRoute
        }

    val onActiveChange: (Boolean) -> Unit = { newActive ->
        active = newActive
        if (!newActive) {
            focusManager.clearFocus()
            if (
                navigationItems.fastAny {
                    it.route == navBackStackEntry?.destination?.route
                }
            ) {
                onQueryChange(TextFieldValue())
            }
        }
    }

    var searchSource by rememberEnumPreference(
        SearchSourceKey,
        SearchSource.ONLINE,
    )

    val searchBarFocusRequester = remember { FocusRequester() }

    val onSearch: (String) -> Unit = { searchText ->
        if (searchText.isNotEmpty()) {
            if (!pauseSearchHistory) {
                database.query {
                    insert(SearchHistory(query = searchText))
                }
            }

            when (searchSource) {
                SearchSource.ONLINE -> {
                    onActiveChange(false)
                    navController.navigate(
                        "search/" + URLEncoder.encode(searchText, "UTF-8"),
                    )
                }

                SearchSource.LOCAL, SearchSource.DEEZER -> focusManager.clearFocus()
            }
        }
    }

    var openSearchImmediately by remember {
        mutableStateOf(activity.intent?.action == MainActivity.ACTION_SEARCH)
    }

    val shouldShowSearchBar =
        remember(active, navBackStackEntry) {
            active ||
                navigationItems.fastAny {
                    it.route == navBackStackEntry?.destination?.route
                } ||
                navBackStackEntry?.destination?.route?.startsWith("search/") == true
        }

    val shouldShowNavigationBar =
        remember(navBackStackEntry, active) {
            navBackStackEntry?.destination?.route == null ||
                (
                    navigationItems.fastAny {
                        it.route == navBackStackEntry?.destination?.route
                    } && !active
                )
        }

    val shouldShowHomeShuffleButton =
        currentRoute == Screens.Home.route &&
            (allLocalItems.isNotEmpty() || allYtItems.isNotEmpty())

    val shouldShowTopBar =
        !active &&
            navBackStackEntry?.destination?.route in topLevelScreens &&
            navBackStackEntry?.destination?.route != "settings"

    val currentTitleRes =
        remember(navBackStackEntry) {
            when (navBackStackEntry?.destination?.route) {
                Screens.Home.route -> R.string.home
                Screens.Search.route -> R.string.search
                Screens.Library.route -> R.string.filter_library
                else -> null
            }
        }

    return MainActivityNavigationState(
        navController = navController,
        accountImageUrl = accountImageUrl,
        allLocalItems = allLocalItems,
        allYtItems = allYtItems,
        navBackStackEntry = navBackStackEntry,
        currentRoute = currentRoute,
        normalizedNavigationRoute = normalizedNavigationRoute,
        pendingNavigationRoute = pendingNavigationRoute,
        setPendingNavigationRoute = { pendingNavigationRoute = it },
        isYearInMusicScreen = isYearInMusicScreen,
        isAlwaysOnDisplayScreen = isAlwaysOnDisplayScreen,
        navigationItems = navigationItems,
        slimNav = slimNav,
        defaultOpenTab = defaultOpenTab,
        pauseSearchHistory = pauseSearchHistory,
        tabOpenedFromShortcut = tabOpenedFromShortcut,
        topLevelScreens = topLevelScreens,
        query = query,
        onQueryChange = onQueryChange,
        active = active,
        selectedNavigationRoute = selectedNavigationRoute,
        onActiveChange = onActiveChange,
        searchSource = searchSource,
        setSearchSource = { searchSource = it },
        searchBarFocusRequester = searchBarFocusRequester,
        onSearch = onSearch,
        openSearchImmediately = openSearchImmediately,
        setOpenSearchImmediately = { openSearchImmediately = it },
        shouldShowSearchBar = shouldShowSearchBar,
        shouldShowNavigationBar = shouldShowNavigationBar,
        shouldShowHomeShuffleButton = shouldShowHomeShuffleButton,
        shouldShowTopBar = shouldShowTopBar,
        currentTitleRes = currentTitleRes,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
internal data class MainActivityNavigationEffects(
    val searchBarScrollBehavior: TopAppBarScrollBehavior,
    val topAppBarScrollBehavior: TopAppBarScrollBehavior,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberMainActivityNavigationEffects(
    navigationState: MainActivityNavigationState,
    playerBottomSheetState: BottomSheetState,
): MainActivityNavigationEffects {
    val searchBarScrollBehavior =
        appBarScrollBehavior(
            canScroll = {
                navigationState.navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                    (
                        playerBottomSheetState.isCollapsed ||
                            playerBottomSheetState.isDismissed
                    )
            },
        )

    val topAppBarScrollBehavior =
        appBarScrollBehavior(
            canScroll = {
                navigationState.navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                    (
                        playerBottomSheetState.isCollapsed ||
                            playerBottomSheetState.isDismissed
                    )
            },
        )

    var previousRoute by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(navigationState.navBackStackEntry) {
        val currentRoute =
            navigationState.navBackStackEntry?.destination?.route

        val wasOnNonTopLevelScreen =
            previousRoute != null &&
                previousRoute !in navigationState.topLevelScreens &&
                previousRoute?.startsWith("search/") != true

        val isReturningToHomeOrLibrary =
            currentRoute == Screens.Home.route ||
                currentRoute == Screens.Library.route

        if (wasOnNonTopLevelScreen && isReturningToHomeOrLibrary) {
            searchBarScrollBehavior.state.resetHeightOffset()
            topAppBarScrollBehavior.state.resetHeightOffset()
        }

        previousRoute = currentRoute

        if (
            navigationState.navBackStackEntry
                ?.destination
                ?.route
                ?.startsWith("search/") == true
        ) {
            val searchQuery =
                withContext(Dispatchers.IO) {
                    val rawQuery =
                        navigationState.navBackStackEntry
                            ?.arguments
                            ?.getString("query")!!

                    if (rawQuery.contains("%")) {
                        rawQuery
                    } else {
                        URLDecoder.decode(rawQuery, "UTF-8")
                    }
                }

            navigationState.onQueryChange(
                TextFieldValue(
                    searchQuery,
                    TextRange(searchQuery.length),
                ),
            )
        } else if (
            navigationState.navigationItems.fastAny {
                it.route ==
                    navigationState.navBackStackEntry
                        ?.destination
                        ?.route
            } ||
            navigationState.navBackStackEntry
                ?.destination
                ?.route in navigationState.topLevelScreens
        ) {
            navigationState.onQueryChange(TextFieldValue())

            if (
                navigationState.navBackStackEntry
                    ?.destination
                    ?.route != Screens.Home.route
            ) {
                searchBarScrollBehavior.state.resetHeightOffset()
                topAppBarScrollBehavior.state.resetHeightOffset()
            }
        }
    }

    LaunchedEffect(navigationState.active) {
        if (navigationState.active) {
            searchBarScrollBehavior.state.resetHeightOffset()
            topAppBarScrollBehavior.state.resetHeightOffset()
            navigationState.searchBarFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(
        navigationState.shouldShowSearchBar,
        navigationState.openSearchImmediately,
    ) {
        if (
            navigationState.shouldShowSearchBar &&
            navigationState.openSearchImmediately
        ) {
            navigationState.onActiveChange(true)
            try {
                delay(100)
                navigationState.searchBarFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
            navigationState.setOpenSearchImmediately(false)
        }
    }

    return MainActivityNavigationEffects(
        searchBarScrollBehavior = searchBarScrollBehavior,
        topAppBarScrollBehavior = topAppBarScrollBehavior,
    )
}
