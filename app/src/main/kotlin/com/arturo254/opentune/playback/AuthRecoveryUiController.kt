package com.arturo254.opentune.playback

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.arturo254.opentune.MainActivity
import timber.log.Timber

internal class AuthRecoveryUiController(
    private val context: Context,
) {
    @Volatile
    private var lastPrompt:
        Pair<String, Long>? = null

    fun promptLoginRecovery(
        mediaId: String,
        targetUrl: String,
    ) {
        if (!isAppInForeground()) return

        val now =
            System.currentTimeMillis()
        val previous =
            lastPrompt

        if (
            previous?.first == mediaId &&
            now - previous.second <
                PROMPT_COOLDOWN_MS
        ) {
            return
        }

        lastPrompt =
            mediaId to now

        val deepLink =
            Uri.parse(
                "OpenTune://login?url=" +
                    Uri.encode(targetUrl),
            )

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                deepLink,
                context,
                MainActivity::class.java,
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK,
                )
                addFlags(
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP,
                )
            }

        runCatching {
            context.startActivity(intent)
        }.onFailure {
            Timber.e(
                it,
                "Failed to open login recovery for %s",
                mediaId,
            )
        }
    }

    private fun isAppInForeground():
        Boolean {
        val activityManager =
            context.getSystemService(
                Context.ACTIVITY_SERVICE,
            ) as ActivityManager

        val processes =
            activityManager
                .runningAppProcesses
                ?: return false

        return processes.any {
            processInfo ->
            processInfo.importance ==
                ActivityManager
                    .RunningAppProcessInfo
                    .IMPORTANCE_FOREGROUND &&
                processInfo.processName ==
                    context.packageName
        }
    }

    private companion object {
        const val PROMPT_COOLDOWN_MS =
            10_000L
    }
}
