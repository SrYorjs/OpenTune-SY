package com.arturo254.opentune.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.arturo254.opentune.MainActivity
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.MediaSessionConstants.CommandToggleLike
import com.arturo254.opentune.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.arturo254.opentune.constants.MediaSessionConstants.CommandToggleShuffle
import com.arturo254.opentune.constants.MediaSessionConstants.CommandToggleStartRadio
import com.arturo254.opentune.extensions.SilentHandler
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.utils.CoilBitmapLoader
import com.arturo254.opentune.widget.PlayerWidgetActions
import com.arturo254.opentune.widget.PlayerWidgetState
import com.arturo254.opentune.widget.PlayerWidgetUpdater
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class MediaSessionNotificationController(
    private val service: MusicService,
    private val player: ExoPlayer,
    private val mediaLibrarySessionCallback:
        MediaLibrarySessionCallback,
    private val scopeProvider: () -> CoroutineScope,
    private val lifecycleController:
        PlaybackLifecycleController,
    private val isCurrentSongLiked: () -> Boolean,
    private val hasCurrentSong: () -> Boolean,
    private val toggleLike: () -> Unit,
    private val toggleLibrary: () -> Unit,
    private val toggleStartRadio: () -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    lateinit var session: MediaLibraryService.MediaLibrarySession
        private set

    fun start() {
        createNotificationChannel()

        mediaLibrarySessionCallback.apply {
            toggleLike =
                this@MediaSessionNotificationController
                    .toggleLike
            toggleStartRadio =
                this@MediaSessionNotificationController
                    .toggleStartRadio
            toggleLibrary =
                this@MediaSessionNotificationController
                    .toggleLibrary
        }

        session =
            MediaLibraryService.MediaLibrarySession
                .Builder(
                    service,
                    player,
                    mediaLibrarySessionCallback,
                ).setSessionActivity(
                    PendingIntent.getActivity(
                        service,
                        0,
                        Intent(
                            service,
                            MainActivity::class.java,
                        ),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).setBitmapLoader(
                    CoilBitmapLoader(
                        service,
                        scopeProvider(),
                    ),
                ).build()

        service.setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                service,
                { MusicService.NOTIFICATION_ID },
                MusicService.CHANNEL_ID,
                R.string.music_player,
            ).apply {
                setSmallIcon(R.drawable.opentune)
            },
        )

        updateCustomLayout()

        val sessionToken =
            SessionToken(
                service,
                ComponentName(
                    service,
                    MusicService::class.java,
                ),
            )

        val controllerFuture =
            MediaController
                .Builder(
                    service,
                    sessionToken,
                ).buildAsync()

        controllerFuture.addListener(
            {
                runCatching {
                    controllerFuture.get()
                }.onFailure(onError)
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun createForegroundNotification(): Notification {
        val contentIntent =
            PendingIntent.getActivity(
                service,
                0,
                Intent(
                    service,
                    MainActivity::class.java,
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat
            .Builder(
                service,
                MusicService.CHANNEL_ID,
            ).setSmallIcon(
                R.drawable.opentune_monochrome,
            ).setContentTitle(
                service.getString(
                    R.string.music_player,
                ),
            ).setContentText(
                service.getString(
                    R.string.app_name,
                ),
            ).setContentIntent(
                contentIntent,
            ).setCategory(
                Notification.CATEGORY_SERVICE,
            ).setPriority(
                NotificationCompat.PRIORITY_LOW,
            ).setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun updateCustomLayout() {
        if (!::session.isInitialized) return

        try {
            val hasSong = hasCurrentSong()
            val liked = isCurrentSongLiked()

            val customLayout =
                listOf(
                    CommandButton
                        .Builder()
                        .setDisplayName(
                            service.getString(
                                if (liked) {
                                    R.string.action_remove_like
                                } else {
                                    R.string.action_like
                                },
                            ),
                        ).setIconResId(
                            if (liked) {
                                R.drawable.favorite
                            } else {
                                R.drawable.favorite_border
                            },
                        ).setSessionCommand(
                            CommandToggleLike,
                        ).setEnabled(hasSong)
                        .build(),
                    CommandButton
                        .Builder()
                        .setDisplayName(
                            service.getString(
                                when (
                                    player.repeatMode
                                ) {
                                    Player.REPEAT_MODE_OFF ->
                                        R.string.repeat_mode_off

                                    Player.REPEAT_MODE_ONE ->
                                        R.string.repeat_mode_one

                                    Player.REPEAT_MODE_ALL ->
                                        R.string.repeat_mode_all

                                    else ->
                                        R.string.repeat_mode_off
                                },
                            ),
                        ).setIconResId(
                            when (
                                player.repeatMode
                            ) {
                                Player.REPEAT_MODE_OFF ->
                                    R.drawable.repeat

                                Player.REPEAT_MODE_ONE ->
                                    R.drawable.repeat_one_on

                                Player.REPEAT_MODE_ALL ->
                                    R.drawable.repeat_on

                                else ->
                                    R.drawable.repeat
                            },
                        ).setSessionCommand(
                            CommandToggleRepeatMode,
                        ).build(),
                    CommandButton
                        .Builder()
                        .setDisplayName(
                            service.getString(
                                if (
                                    player.shuffleModeEnabled
                                ) {
                                    R.string.action_shuffle_off
                                } else {
                                    R.string.action_shuffle_on
                                },
                            ),
                        ).setIconResId(
                            if (
                                player.shuffleModeEnabled
                            ) {
                                R.drawable.shuffle_on
                            } else {
                                R.drawable.shuffle
                            },
                        ).setSessionCommand(
                            CommandToggleShuffle,
                        ).build(),
                    CommandButton
                        .Builder()
                        .setDisplayName(
                            service.getString(
                                R.string.start_radio,
                            ),
                        ).setIconResId(
                            R.drawable.radio,
                        ).setSessionCommand(
                            CommandToggleStartRadio,
                        ).setEnabled(hasSong)
                        .build(),
                )

            session.setCustomLayout(
                customLayout,
            )
        } catch (error: Exception) {
            onError(error)
        }
    }

    fun handleWidgetAction(
        action: String?,
    ) {
        when (action) {
            PlayerWidgetActions.ACTION_PLAY_PAUSE -> {
                if (!player.playWhenReady) {
                    lifecycleController
                        .prepareForPlaybackStart()
                }

                player.togglePlayPause()
                updateWidgets()
            }

            PlayerWidgetActions.ACTION_NEXT -> {
                if (player.hasNextMediaItem()) {
                    lifecycleController
                        .prepareForPlaybackStart()

                    player.seekToNext()
                    player.playWhenReady = true
                }

                updateWidgets()
            }

            PlayerWidgetActions.ACTION_PREVIOUS -> {
                if (
                    player.hasPreviousMediaItem()
                ) {
                    lifecycleController
                        .prepareForPlaybackStart()

                    player.seekToPrevious()
                    player.playWhenReady = true
                }

                updateWidgets()
            }

            PlayerWidgetActions.ACTION_REFRESH ->
                updateWidgets()
        }
    }

    fun updateWidgets() {
        scopeProvider().launch(SilentHandler) {
            val state =
                PlayerWidgetState.fromPlayer(
                    player,
                    service,
                )

            PlayerWidgetUpdater.update(
                service,
                state,
            )
        }
    }

    fun release() {
        if (::session.isInitialized) {
            runCatching {
                session.release()
            }
        }
    }

    private fun createNotificationChannel() {
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) {
            return
        }

        try {
            service
                .getSystemService(
                    NotificationManager::class.java,
                )?.createNotificationChannel(
                    NotificationChannel(
                        MusicService.CHANNEL_ID,
                        service.getString(
                            R.string.music_player,
                        ),
                        NotificationManager.IMPORTANCE_LOW,
                    ),
                )
        } catch (error: Exception) {
            onError(error)
        }
    }
}
