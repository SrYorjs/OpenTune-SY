@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.arturo254.opentune.ui.original.adapters

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.arturo254.opentune.BuildConfig
import com.arturo254.opentune.constants.EnableUpdateNotificationKey
import com.arturo254.opentune.utils.ReleaseInfo
import com.arturo254.opentune.utils.Updater
import com.arturo254.opentune.utils.UpdateNotificationManager
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get

const val ActionOpenUpdate = "com.arturo254.opentune.action.OPEN_UPDATE"
data class RuntimeRelease(val info: ReleaseInfo) { val version: String get() = info.tagName.removePrefix("v") }
sealed interface RuntimeUpdateState {
    data object Loading : RuntimeUpdateState
    data object Current : RuntimeUpdateState
    data class Available(val release: RuntimeRelease) : RuntimeUpdateState
    data class Failed(val error: Throwable) : RuntimeUpdateState
}

fun getAutoUpdateCheckSetting(context: Context) = context.getSharedPreferences("update_settings", Context.MODE_PRIVATE).getBoolean("auto_check", true)
fun getUpdateNotificationsSetting(context: Context) = context.dataStore.get(EnableUpdateNotificationKey, false)
fun saveUpdateAvailableState(context: Context, available: Boolean) {
    context.getSharedPreferences("update_settings", Context.MODE_PRIVATE).edit().putBoolean("available", available).apply()
}
fun isNewerVersion(candidate: String, installed: String): Boolean {
    val a = candidate.removePrefix("v").split(Regex("[^0-9]+" )).mapNotNull(String::toIntOrNull)
    val b = installed.removePrefix("v").split(Regex("[^0-9]+" )).mapNotNull(String::toIntOrNull)
    for (index in 0 until maxOf(a.size, b.size)) {
        val difference = (a.getOrNull(index) ?: 0).compareTo(b.getOrNull(index) ?: 0)
        if (difference != 0) return difference > 0
    }
    return false
}
fun isNewerThanInstalled(version: String) = isNewerVersion(version, BuildConfig.VERSION_NAME)
suspend fun getLatestRelease(): Result<RuntimeRelease> = Updater.getLatestReleaseInfo().map(::RuntimeRelease)
fun maybeNotifyUpdate(context: Context, release: RuntimeRelease) {
    if (isNewerThanInstalled(release.version)) UpdateNotificationManager.checkForUpdates(context)
}

@Composable
fun rememberUpdateState(): State<RuntimeUpdateState> = produceState<RuntimeUpdateState>(RuntimeUpdateState.Loading) {
    value = getLatestRelease().fold(
        { if (isNewerThanInstalled(it.version)) RuntimeUpdateState.Available(it) else RuntimeUpdateState.Current },
        { RuntimeUpdateState.Failed(it) },
    )
}

@Composable
fun UpdateNotificationOpener(requests: Int, onOpenDevSpace: (Int) -> Unit) {
    val navController = LocalRuntimeNavigation.current
    LaunchedEffect(requests) { if (requests > 0) navController.navigate("settings/update") { launchSingleTop = true } }
}

/** Uses the current release screen, including its download and failure states (operation 16). */
@Composable
fun UpdatePromptSheet(release: RuntimeRelease, userName: String, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    val navController = LocalRuntimeNavigation.current
    Dialog(onDismissRequest = onSnooze, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val pureBlack by com.arturo254.opentune.utils.rememberPreference(com.arturo254.opentune.constants.PureBlackKey, false)
        ActivityGlassHost(pureBlack) {
            com.arturo254.opentune.ui.screens.settings.UpdateScreen(navController, TopAppBarDefaults.pinnedScrollBehavior())
        }
    }
}
