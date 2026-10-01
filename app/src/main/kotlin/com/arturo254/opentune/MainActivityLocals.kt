package com.arturo254.opentune

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.playback.DownloadUtil
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.utils.SyncUtils

val LocalDatabase =
    staticCompositionLocalOf<MusicDatabase> {
        error("No database provided")
    }

val LocalPlayerConnection =
    staticCompositionLocalOf<PlayerConnection?> {
        error("No PlayerConnection provided")
    }

val LocalPlayerAwareWindowInsets =
    compositionLocalOf<WindowInsets> {
        error("No WindowInsets provided")
    }

val LocalDownloadUtil =
    staticCompositionLocalOf<DownloadUtil> {
        error("No DownloadUtil provided")
    }

val LocalSyncUtils =
    staticCompositionLocalOf<SyncUtils> {
        error("No SyncUtils provided")
    }
