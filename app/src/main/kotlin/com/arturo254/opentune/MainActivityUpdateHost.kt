package com.arturo254.opentune

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.ui.component.BottomSheetPageState
import com.arturo254.opentune.utils.UpdateNotificationManager
import com.arturo254.opentune.utils.Updater
import com.mikepenz.markdown.m3.Markdown
import kotlin.time.Duration.Companion.days

/**
 * Owns update permission/checking state and the update bottom-sheet content.
 */
@Composable
internal fun MainActivityUpdateHost(
    activity: ComponentActivity,
    bottomSheetPageState: BottomSheetPageState,
    playerConnection: PlayerConnection?,
    content: @Composable (latestVersionName: String) -> Unit,
) {
    var latestVersionName by remember { mutableStateOf(BuildConfig.VERSION_NAME) }
    val releaseNotesState = remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                playerConnection?.service?.refreshPlaybackNotification()
            }
        }

    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (System.currentTimeMillis() - Updater.lastCheckTime > 1.days.inWholeMilliseconds) {
            Updater.getLatestVersionName().onSuccess {
                latestVersionName = it
            }
        }

        UpdateNotificationManager.checkForUpdates(activity)
    }

    val updateSheetContent: @Composable ColumnScope.() -> Unit = {
        Text(
            text = stringResource(R.string.new_update_available),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 16.dp),
        )

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = {},
            shape = CircleShape,
            contentPadding = PaddingValues(
                horizontal = 5.dp,
                vertical = 5.dp,
            ),
        ) {
            Text(
                text = latestVersionName,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
        ) {
            val notes = releaseNotesState.value
            if (!notes.isNullOrBlank()) {
                Markdown(
                    content = notes,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                )
            } else {
                Text(
                    text = stringResource(R.string.release_notes_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                try {
                    uriHandler.openUri(Updater.getLatestDownloadUrl())
                } catch (_: Exception) {
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.update_text))
        }
    }

    LaunchedEffect(latestVersionName) {
        if (!Updater.isSameVersion(latestVersionName, BuildConfig.VERSION_NAME)) {
            Updater.getLatestReleaseNotes()
                .onSuccess { releaseNotesState.value = it }
                .onFailure { releaseNotesState.value = null }

            bottomSheetPageState.show(updateSheetContent)
        }
    }

    content(latestVersionName)
}
