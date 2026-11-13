@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.arturo254.opentune.ui.original.adapters

import androidx.compose.runtime.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavHostController
import com.arturo254.opentune.ui.original.screens.Screens
import com.arturo254.opentune.ui.original.component.navigateToTab
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.constants.PureBlackKey

enum class RouteShelf(val key: String) { Local("local"), Cached("cached") }
fun sourceKey(key: String?): String? = key

/** Temporary equivalent: HomeScreen; original presentation: operation 06. */
@Composable
fun HomeScreen(navController: NavHostController, snackbarHostState: SnackbarHostState) {
    com.arturo254.opentune.ui.screens.HomeScreen(navController, onOpenSearchTab = { navController.navigateToTab(Screens.Search.route) }, onOpenLibraryTab = { navController.navigateToTab(Screens.Library.route) })
}

/** Temporary equivalent: HomeScreen; original presentation: operation 06. */
@Composable
fun SectionDetailScreen(navController: NavHostController, sectionId: String, homeViewModel: com.arturo254.opentune.viewmodels.HomeViewModel) {
    com.arturo254.opentune.ui.screens.HomeScreen(navController, viewModel = homeViewModel)
}

/** Temporary equivalent: HomeScreen; original presentation: operation 06. */
@Composable
fun ComingSoonScreen(navController: NavHostController, sectionTitle: String) {
    com.arturo254.opentune.ui.screens.HomeScreen(navController)
}

/** Temporary equivalent: GalaxDiscoverScreen + PrismSearchBar + UniversalSearchScreen; original presentation: operation 07. */
@Composable
fun SearchScreen(navController: NavHostController, pureBlack: Boolean) {
    SearchPresentation(navController, pureBlack)
}

/** Temporary equivalent: UniversalSearchScreen; original presentation: operation 07. */
@Composable
fun OnlineSearchResult(navController: NavHostController) {
    val pureBlack by rememberPreference(PureBlackKey, false)
    com.arturo254.opentune.ui.screens.search.UniversalSearchScreen(navController.currentBackStackEntry?.arguments?.getString("query").orEmpty(), pureBlack)
}

/** Temporary equivalent: YouTubeBrowseScreen; original presentation: operation 07. */
@Composable
fun GenreScreen(navController: NavHostController, pureBlack: Boolean) {
    com.arturo254.opentune.ui.screens.YouTubeBrowseScreen(navController)
}

/** Temporary equivalent: LibraryScreen; original presentation: operation 08/10. */
@Composable
fun LibraryHomeScreen(navController: NavHostController, pureBlack: Boolean) {
    com.arturo254.opentune.ui.screens.library.LibraryScreen(navController, onOpenSearchTab = { navController.navigateToTab(Screens.Search.route) })
}

/** Temporary equivalent: LibraryScreen; original presentation: operation 08/10. */
@Composable
fun LibrarySongsScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.library.LibraryScreen(navController, onOpenSearchTab = { navController.navigateToTab(Screens.Search.route) })
}

/** Temporary equivalent: LibraryScreen; original presentation: operation 08/10. */
@Composable
fun LibraryPlaylistsScreen(navController: NavHostController, shelf: RouteShelf? = null, source: String? = null) {
    com.arturo254.opentune.ui.screens.library.LibraryScreen(navController, onOpenSearchTab = { navController.navigateToTab(Screens.Search.route) })
}

/** Temporary equivalent: LibraryScreen; original presentation: operation 08/10. */
@Composable
fun LibraryRecentlyAddedScreen(navController: NavHostController, shelf: RouteShelf? = null) {
    com.arturo254.opentune.ui.screens.library.LibraryScreen(navController, onOpenSearchTab = { navController.navigateToTab(Screens.Search.route) })
}

/** Temporary equivalent: LibraryScreen; original presentation: operation 08/10. */
@Composable
fun LibraryAlbumsScreen(navController: NavHostController, shelf: RouteShelf? = null, source: String? = null) {
    com.arturo254.opentune.ui.screens.library.LibraryScreen(navController, onOpenSearchTab = { navController.navigateToTab(Screens.Search.route) })
}

/** Temporary equivalent: LibraryScreen; original presentation: operation 08/10. */
@Composable
fun LibraryArtistsScreen(navController: NavHostController, shelf: RouteShelf? = null) {
    com.arturo254.opentune.ui.screens.library.LibraryScreen(navController, onOpenSearchTab = { navController.navigateToTab(Screens.Search.route) })
}

/** Temporary equivalent: LibraryScreen; original presentation: operation 08/10. */
@Composable
fun LibraryImportedScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.library.LibraryScreen(navController, onOpenSearchTab = { navController.navigateToTab(Screens.Search.route) })
}

/** Temporary equivalent: ArtistScreen; original presentation: operation 09. */
@Composable
fun ArtistSimilarScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.artist.ArtistScreen(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: ContentSettings; original presentation: operation 16. */
@Composable
fun CipherSettingsScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.ContentSettings(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: DebugSettings; original presentation: operation 16. */
@Composable
fun PlaybackDiagnosticsScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.DebugSettings(navController)
}

/** Temporary equivalent: SettingsScreen; original presentation: operation 16. */
@Composable
fun SettingsPanePlaceholder(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.SettingsScreen(navController, TopAppBarDefaults.pinnedScrollBehavior(), com.arturo254.opentune.BuildConfig.VERSION_NAME)
}

/** Temporary equivalent: SettingsScreen; original presentation: operation 16. */
@Composable
fun MochySettingsScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.SettingsScreen(navController, TopAppBarDefaults.pinnedScrollBehavior(), com.arturo254.opentune.BuildConfig.VERSION_NAME)
}

/** Temporary equivalent: ContentSettings; original presentation: operation 16. */
@Composable
fun AppBehaviourSettingsScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.ContentSettings(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: IntegrationScreen; original presentation: operation 16. */
@Composable
fun IntegrationsScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.IntegrationScreen(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: DiscordSettings; original presentation: operation 16. */
@Composable
fun DiscordScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.DiscordSettings(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: LastFMSettings; original presentation: operation 16. */
@Composable
fun ScrobbleServiceScreen(navController: NavHostController, site: String) {
    com.arturo254.opentune.ui.screens.settings.LastFMSettings(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: PrivacySettings; original presentation: operation 16. */
@Composable
fun ProxySettingsScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.PrivacySettings(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: PlayerSettings; original presentation: operation 16. */
@Composable
fun CrossmixSettingsScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.PlayerSettings(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: AboutScreen; original presentation: operation 16. */
@Composable
fun DeveloperSpaceScreen(navController: NavHostController, initialPage: Int) {
    com.arturo254.opentune.ui.screens.settings.AboutScreen(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: DebugSettings; original presentation: operation 16. */
@Composable
fun DevPlaygroundScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.DebugSettings(navController)
}

/** Temporary equivalent: StorageSettings; original presentation: operation 16. */
@Composable
fun StorageScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.StorageSettings(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: DebugSettings; original presentation: operation 16. */
@Composable
fun ExperimentalSettingsScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.DebugSettings(navController)
}

/** Temporary equivalent: BackupAndRestore; original presentation: operation 16. */
@Composable
fun ImportSyncScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.BackupAndRestore(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: AndroidAutoSettings; original presentation: operation 16. */
@Composable
fun TvLinkScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.AndroidAutoSettings(navController, TopAppBarDefaults.pinnedScrollBehavior(), context = androidx.compose.ui.platform.LocalContext.current)
}

/** Temporary equivalent: IntegrationScreen; original presentation: operation 16. */
@Composable
fun ImportProvidersScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.IntegrationScreen(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: BackupAndRestore; original presentation: operation 16. */
@Composable
fun TuneMyMusicImportScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.BackupAndRestore(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: BackupAndRestore; original presentation: operation 16. */
@Composable
fun ImportProgressScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.BackupAndRestore(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: AccountSettings; original presentation: operation 16. */
@Composable
fun YouTubeSyncScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.AccountSettings(navController, onClose = { navController.navigateUp() }, latestVersionName = com.arturo254.opentune.BuildConfig.VERSION_NAME)
}

/** Temporary equivalent: LoginScreen; original presentation: operation 16. */
@Composable
fun YouTubeLoginScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.LoginScreen(navController)
}

/** Temporary equivalent: AccountSettings; original presentation: operation 16. */
@Composable
fun AccountImportScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.AccountSettings(navController, onClose = { navController.navigateUp() }, latestVersionName = com.arturo254.opentune.BuildConfig.VERSION_NAME)
}

/** Temporary equivalent: PlayerSettings; original presentation: operation 16. */
@Composable
fun EqualizerScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.PlayerSettings(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: PlayerSettings; original presentation: operation 16. */
@Composable
fun GestureScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.PlayerSettings(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: AppearanceSettings; original presentation: operation 16. */
@Composable
fun CustomisationScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.AppearanceSettings(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: PlayerSettings; original presentation: operation 16. */
@Composable
fun AudioLabScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.PlayerSettings(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: PlayerSettings; original presentation: operation 16. */
@Composable
fun AudioFxScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.PlayerSettings(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: PlayerSettings; original presentation: operation 16. */
@Composable
fun AudioQualityScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.PlayerSettings(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: PlayerSettings; original presentation: operation 16. */
@Composable
fun AudioPassScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.PlayerSettings(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: IntegrationScreen; original presentation: operation 16. */
@Composable
fun ModulesScreen(navController: NavHostController) {
    com.arturo254.opentune.ui.screens.settings.IntegrationScreen(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: IntegrationScreen; original presentation: operation 16. */
@Composable
fun ModuleScreen(navController: NavHostController, id: String) {
    com.arturo254.opentune.ui.screens.settings.IntegrationScreen(navController, TopAppBarDefaults.pinnedScrollBehavior())
}

/** Temporary equivalent: IntegrationScreen; original presentation: operation 16. */
@Composable
fun ModulePageScreen(navController: NavHostController, id: String, pageId: String) {
    com.arturo254.opentune.ui.screens.settings.IntegrationScreen(navController, TopAppBarDefaults.pinnedScrollBehavior())
}
