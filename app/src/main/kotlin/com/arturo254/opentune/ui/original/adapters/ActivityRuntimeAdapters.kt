@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.arturo254.opentune.ui.original.adapters

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.arturo254.opentune.BuildConfig
import com.arturo254.opentune.constants.AccountNameKey
import com.arturo254.opentune.constants.AppLanguageKey
import com.arturo254.opentune.constants.SYSTEM_DEFAULT
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.ui.original.launch.AppIcon
import com.arturo254.opentune.ui.original.screens.Screens
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get
import com.arturo254.opentune.utils.rememberPreference
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.Locale
import com.arturo254.opentune.ui.original.component.PopupSheetState
import com.arturo254.opentune.ui.original.component.navigateToTab

val LocalRuntimeNavigation = staticCompositionLocalOf<NavHostController> { error("No navigation host") }

fun NavController.requestSearchFocus() {
    navigateToTab(Screens.Search.route)
    currentBackStackEntry?.savedStateHandle?.set("requestSearchFocus", true)
}

fun MusicDatabase.eventCount() = events().map { it.size }

fun Context.isTelevision(): Boolean =
    (getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

object AppLanguage {
    fun wrapActivityContext(context: Context): Context {
        val language = context.dataStore.get(AppLanguageKey, SYSTEM_DEFAULT)
        if (language == SYSTEM_DEFAULT || language.isBlank()) return context
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language))
        return context.createConfigurationContext(configuration)
    }
}

object AppIcons {
    fun launchedFrom(intent: Intent?): AppIcon? =
        if (intent?.action == Intent.ACTION_MAIN) AppIcon(com.arturo254.opentune.R.mipmap.ic_launcher, null) else null
    fun splashShows(icon: AppIcon?): Boolean = icon != null && Build.VERSION.SDK_INT >= 33
    fun syncSplash(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 33) {
            val theme = activity.packageManager.getActivityInfo(activity.componentName, 0).themeResource
            if (theme != 0) activity.splashScreen.setSplashScreenTheme(theme)
        }
    }
}

object ProfileStore {
    fun copyPhoto(context: Context, uri: Uri): String {
        val destination = File(context.filesDir, "profile-${java.util.UUID.randomUUID()}")
        requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
            destination.outputStream().use { input.copyTo(it) }
        }
        return Uri.fromFile(destination).toString()
    }
}

object TransferRestore {
    fun takeRestoredNote(context: Context): String? {
        val file = File(context.noBackupFilesDir, "transfer_restored.json")
        if (!file.exists()) return null
        val note = runCatching { org.json.JSONObject(file.readText()) }.getOrNull()
        file.delete()
        note ?: return null
        val missing = note.optInt("missingLocal")
        return if (missing > 0) "Welcome back! Your library is here, but $missing local ${if (missing == 1) "song wasn't" else "songs weren't"} found on this phone."
        else "Welcome back! Your whole library is here."
    }
}

/** The existing account editor completes the first-run profile until operation 16. */
@Composable
fun SetupWizardContainer(isVisible: Boolean, onSetupCompleted: (String, Uri?) -> Unit, content: @Composable () -> Unit) {
    if (isVisible) {
        val name by rememberPreference(AccountNameKey, "")
        com.arturo254.opentune.ui.screens.settings.AccountSettings(
            LocalRuntimeNavigation.current,
            onClose = { onSetupCompleted(name, null) },
            latestVersionName = BuildConfig.VERSION_NAME,
        )
    } else content()
}

fun showsSettingsPane(backStack: List<NavBackStackEntry>, route: String?): Boolean =
    route != "settings" && (route?.startsWith("settings/") == true || route == "mochy_settings") &&
        backStack.any { it.destination.route == "settings" }

fun settingsPaneSelection(backStack: List<NavBackStackEntry>): String? = backStack.lastOrNull()?.destination?.route

@Composable
fun SettingsListPane(navController: NavHostController, selectedRoute: String?) {
    com.arturo254.opentune.ui.screens.settings.SettingsScreen(
        navController, TopAppBarDefaults.pinnedScrollBehavior(), BuildConfig.VERSION_NAME,
    )
}

/** Native sharing is the existing equivalent until the original share presentation (12). */
object SongShare {
    internal var pending by mutableStateOf<MediaMetadata?>(null)
    fun open(song: MediaMetadata) { pending = song }
}

/** Retains the original prompt scheduler and uses the current support/about presentation (16). */
internal fun PopupSheetState.showSupportSheet() {
    show {
        com.arturo254.opentune.ui.screens.settings.AboutScreen(
            LocalRuntimeNavigation.current, TopAppBarDefaults.pinnedScrollBehavior(),
        )
    }
}

@Composable
fun SongShareHost() {
    val context = LocalContext.current
    val song = SongShare.pending
    LaunchedEffect(song) {
        if (song != null) {
            val link = when {
                song.spotifyTrackId != null -> "https://open.spotify.com/track/${song.spotifyTrackId}"
                song.id.matches(Regex("[A-Za-z0-9_-]{11}")) -> "https://music.youtube.com/watch?v=${Uri.encode(song.id)}"
                song.id.startsWith("https://") -> song.id
                else -> ""
            }
            val text = "${song.title} — ${song.artists.joinToString { it.name }}\n$link"
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }, null))
            SongShare.pending = null
        }
    }
}
