package com.arturo254.opentune

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.arturo254.opentune.constants.StopMusicOnTaskClearKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.playback.DownloadUtil
import com.arturo254.opentune.playback.MusicService
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.ui.component.BottomSheetPageState
import com.arturo254.opentune.ui.screens.settings.DiscordPresenceManager
import com.arturo254.opentune.utils.SyncUtils
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    private lateinit var serviceBindingController: MainActivityServiceBindingController
    private lateinit var deepLinkController: MainActivityDeepLinkController
    private lateinit var systemUiController: MainActivitySystemUiController

    private val playerConnection: PlayerConnection?
        get() = serviceBindingController.playerConnection

    override fun onStart() {
        super.onStart()
        serviceBindingController.bind()
        deepLinkController.onPlayerConnectionAvailable()
    }

    override fun onStop() {
        serviceBindingController.safeUnbind()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && !isChangingConfigurations) {
            try { DiscordPresenceManager.stop() } catch (_: Exception) {}
        }

        val shouldStopOnTaskClear =
            if (!isFinishing) {
                false
            } else {
                dataStore.get(StopMusicOnTaskClearKey, false)
            }

        if (shouldStopOnTaskClear) {
            serviceBindingController.safeUnbind()
            stopService(Intent(this, MusicService::class.java))
            serviceBindingController.clearPlayerConnectionReference()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkController.handleIntent(intent)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        deepLinkController =
            MainActivityDeepLinkController(
                context = this,
                scope = lifecycleScope,
                playerConnectionProvider = { serviceBindingController.playerConnection },
            )

        serviceBindingController =
            MainActivityServiceBindingController(
                context = this,
                database = database,
                scope = lifecycleScope,
                onConnected = deepLinkController::onPlayerConnectionAvailable,
            )

        systemUiController =
            MainActivitySystemUiController(
                activity = this,
                scope = lifecycleScope,
            )
        systemUiController.configureInitialWindow()
        systemUiController.initializeLocale()
        systemUiController.observeScreenshotProtection()

        setContent {
            val bottomSheetPageState = remember { BottomSheetPageState() }

            MainActivityUpdateHost(
                activity = this@MainActivity,
                bottomSheetPageState = bottomSheetPageState,
                playerConnection = playerConnection,
            ) { latestVersionName ->
                MainActivityThemeHost(
                    activity = this@MainActivity,
                    playerConnection = playerConnection,
                    systemUiController = systemUiController,
                ) { pureBlack, lyricsSyncOffset ->
                MainActivityShell(
                    activity = this@MainActivity,
                    database = database,
                    downloadUtil = downloadUtil,
                    syncUtils = syncUtils,
                    playerConnection = playerConnection,
                    deepLinkController = deepLinkController,
                    systemUiController = systemUiController,
                    bottomSheetPageState = bottomSheetPageState,
                    latestVersionName = latestVersionName,
                    pureBlack = pureBlack,
                    lyricsSyncOffset = lyricsSyncOffset,
                )
            }
            }
        }
    }

    companion object {
        const val ACTION_SEARCH = "com.arturo254.opentune.action.SEARCH"
        const val ACTION_LIBRARY = "com.arturo254.opentune.action.LIBRARY"
        const val ACTION_DOWNLOAD_QUEUE = "com.arturo254.opentune.action.DOWNLOAD_QUEUE"
    }
}
