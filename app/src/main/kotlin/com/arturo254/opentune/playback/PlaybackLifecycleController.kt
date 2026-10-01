@file:Suppress("DEPRECATION")

package com.arturo254.opentune.playback

import android.app.Notification
import android.app.Service
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.arturo254.opentune.constants.AutoStartOnBluetoothKey
import com.arturo254.opentune.constants.PauseOnDeviceMuteKey
import com.arturo254.opentune.constants.StopMusicOnTaskClearKey
import com.arturo254.opentune.constants.WakelockKey
import com.arturo254.opentune.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class PlaybackLifecycleController(
    private val service: MusicService,
    private val player: ExoPlayer,
    private val dataStore: DataStore<Preferences>,
    private val scopeProvider: () -> CoroutineScope,
    private val isTogetherIdle: () -> Boolean,
    private val isTogetherGuest: () -> Boolean,
    private val isHostSessionActive: () -> Boolean,
    private val stopTogether: suspend () -> Unit,
    private val markTogetherIdle: () -> Unit,
    private val savePlaybackStateBlocking: () -> Unit,
    private val stopAndClearPlayback: () -> Unit,
    private val clearPresenceOnTaskRemoved: () -> Unit,
    private val foregroundNotificationProvider: () -> Notification,
    private val onError: (Throwable) -> Unit,
) {
    private var pauseOnDeviceMuteEnabled = false
    private var wasAutoPausedByDeviceMute = false
    private var autoStartOnBluetoothEnabled = false
    private var bluetoothReceiverRegistered = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var wakelockEnabled = false
    private var hasBoundClients = false
    private var idleStopJob: Job? = null

    @Volatile
    private var hasCalledStartForeground = false

    fun start() {
        wakeLock =
            (service.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "OpenTune:Playback",
                ).also {
                    it.setReferenceCounted(false)
                }

        scopeProvider().launch {
            dataStore.data
                .map { it[PauseOnDeviceMuteKey] ?: false }
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    pauseOnDeviceMuteEnabled = enabled
                    if (!enabled) {
                        wasAutoPausedByDeviceMute = false
                    } else {
                        handleDeviceMuteStateChanged()
                    }
                }
        }

        scopeProvider().launch {
            dataStore.data
                .map { it[AutoStartOnBluetoothKey] ?: false }
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    autoStartOnBluetoothEnabled = enabled
                    if (enabled) {
                        registerBluetoothReceiver()
                    } else {
                        unregisterBluetoothReceiver()
                    }
                }
        }

        scopeProvider().launch {
            dataStore.data
                .map { it[WakelockKey] ?: false }
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    wakelockEnabled = enabled
                    updateWakeLock()
                }
        }
    }

    fun prepareForPlaybackStart() {
        cancelIdleStop()
        promoteToStartedService()
        ensureStartedAsForeground()
    }

    fun ensureStartedAsForeground() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (hasCalledStartForeground) return

        val notification =
            runCatching {
                foregroundNotificationProvider()
            }.onFailure(onError)
                .getOrNull()
                ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                service.startForeground(
                    MusicService.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                service.startForeground(
                    MusicService.NOTIFICATION_ID,
                    notification,
                )
            }
            hasCalledStartForeground = true
        } catch (error: Exception) {
            onError(error)
        }
    }

    fun promoteToStartedService() {
        runCatching {
            service.startService(
                Intent(
                    service,
                    MusicService::class.java,
                ),
            )
        }.onFailure(onError)
    }

    fun cancelIdleStop() {
        idleStopJob?.cancel()
        idleStopJob = null
    }

    fun scheduleStopIfIdle() {
        if (hasBoundClients) return

        val state = player.playbackState
        val keepAlive =
            player.isPlaying ||
                (
                    player.playWhenReady &&
                        (
                            state == Player.STATE_BUFFERING ||
                                state == Player.STATE_READY
                        )
                )

        if (keepAlive) {
            cancelIdleStop()
            return
        }

        if (!isTogetherIdle()) {
            cancelIdleStop()
            return
        }

        val delayMs =
            when (state) {
                Player.STATE_READY -> 5 * 60_000L
                Player.STATE_ENDED,
                Player.STATE_IDLE,
                -> 30_000L

                else -> 60_000L
            }

        cancelIdleStop()
        idleStopJob =
            scopeProvider().launch {
                delay(delayMs)

                if (hasBoundClients) return@launch

                val currentState = player.playbackState
                val shouldKeep =
                    player.isPlaying ||
                        (
                            player.playWhenReady &&
                                (
                                    currentState ==
                                        Player.STATE_BUFFERING ||
                                        currentState ==
                                            Player.STATE_READY
                                )
                        )

                if (shouldKeep) return@launch
                if (!isTogetherIdle()) return@launch

                stopForegroundAndSelf()
            }
    }

    fun onPlayerEvents(events: Player.Events) {
        if (
            events.contains(
                Player.EVENT_DEVICE_VOLUME_CHANGED,
            )
        ) {
            handleDeviceMuteStateChanged()
        }

        if (
            events.contains(
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
            ) &&
            isDeviceMutedNow() &&
            player.playWhenReady
        ) {
            handleDeviceMuteStateChanged()
        }

        if (
            events.contains(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
            ) &&
            (
                player.playbackState ==
                    Player.STATE_IDLE ||
                    player.playbackState ==
                        Player.STATE_ENDED
            )
        ) {
            wasAutoPausedByDeviceMute = false
        }

        if (
            events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
            )
        ) {
            updateWakeLock()

            val playbackState =
                player.playbackState
            val keepAliveState =
                playbackState ==
                    Player.STATE_BUFFERING ||
                    playbackState ==
                        Player.STATE_READY

            if (
                player.playWhenReady &&
                keepAliveState
            ) {
                prepareForPlaybackStart()
            } else {
                scheduleStopIfIdle()
            }
        }
    }

    fun onBound() {
        hasBoundClients = true
        cancelIdleStop()
    }

    fun onUnbound() {
        hasBoundClients = false
        scheduleStopIfIdle()
    }

    fun onRebound() {
        hasBoundClients = true
        cancelIdleStop()
    }

    fun onTaskRemoved() {
        clearPresenceOnTaskRemoved()

        val stopMusicOnTaskClearEnabled =
            dataStore.get(
                StopMusicOnTaskClearKey,
                false,
            )

        try {
            val hostSessionActive =
                isHostSessionActive()
            val playbackInactive =
                player.playbackState ==
                    Player.STATE_IDLE ||
                    player.mediaItemCount == 0

            if (
                !PlaybackLifecyclePolicy
                    .shouldStopServiceOnTaskRemoved(
                        stopMusicOnTaskClearEnabled =
                            stopMusicOnTaskClearEnabled,
                        isHostSessionActive =
                            hostSessionActive,
                        isPlaybackInactive =
                            playbackInactive,
                    )
            ) {
                return
            }

            if (
                hostSessionActive &&
                playbackInactive
            ) {
                runCatching {
                    scopeProvider().launch {
                        stopTogether()
                    }
                }
                runCatching {
                    markTogetherIdle()
                }
                service.stopSelf()
                return
            }

            if (stopMusicOnTaskClearEnabled) {
                if (player.mediaItemCount > 0) {
                    savePlaybackStateBlocking()
                }

                runCatching {
                    stopAndClearPlayback()
                }

                removeForeground()
                service.stopSelf()
            }
        } catch (_: Exception) {
        }
    }

    fun release() {
        cancelIdleStop()
        unregisterBluetoothReceiver()

        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }

        wakeLock = null
        hasCalledStartForeground = false
    }

    private fun isDeviceMutedNow(): Boolean =
        player.isDeviceMuted ||
            player.deviceVolume <= 0

    private fun handleDeviceMuteStateChanged() {
        if (
            !pauseOnDeviceMuteEnabled ||
            isTogetherGuest()
        ) {
            wasAutoPausedByDeviceMute = false
            return
        }

        if (isDeviceMutedNow()) {
            val canPauseNow =
                player.currentMediaItem != null &&
                    player.playWhenReady &&
                    player.playbackState !=
                        Player.STATE_IDLE &&
                    player.playbackState !=
                        Player.STATE_ENDED

            if (canPauseNow) {
                player.pause()
                wasAutoPausedByDeviceMute = true
            }
            return
        }

        if (!wasAutoPausedByDeviceMute) {
            return
        }

        wasAutoPausedByDeviceMute = false

        val canResumeNow =
            player.currentMediaItem != null &&
                player.playbackState !=
                    Player.STATE_IDLE &&
                player.playbackState !=
                    Player.STATE_ENDED

        if (canResumeNow) {
            player.play()
        }
    }

    private val bluetoothReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                if (
                    intent.action !=
                    BluetoothDevice.ACTION_ACL_CONNECTED
                ) {
                    return
                }

                if (!autoStartOnBluetoothEnabled) {
                    return
                }

                val device =
                    intent.getParcelableExtra<BluetoothDevice>(
                        BluetoothDevice.EXTRA_DEVICE,
                    ) ?: return

                val isAudioDevice =
                    try {
                        val majorClass =
                            device.bluetoothClass
                                ?.majorDeviceClass

                        majorClass ==
                            BluetoothClass.Device.Major.AUDIO_VIDEO ||
                            majorClass ==
                                BluetoothClass.Device.Major.WEARABLE
                    } catch (_: SecurityException) {
                        true
                    }

                if (!isAudioDevice) return

                scopeProvider().launch {
                    delay(1_500L)
                    handleBluetoothAutoStart()
                }
            }
        }

    private fun handleBluetoothAutoStart() {
        if (isTogetherGuest()) return

        if (
            player.currentMediaItem != null &&
            player.playbackState !=
                Player.STATE_IDLE &&
            player.playbackState !=
                Player.STATE_ENDED
        ) {
            if (!player.playWhenReady) {
                player.play()
            }
            return
        }

        if (player.mediaItemCount > 0) {
            player.prepare()
            player.play()
        }
    }

    private fun registerBluetoothReceiver() {
        if (bluetoothReceiverRegistered) return

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S &&
            service.checkSelfPermission(
                android.Manifest.permission.BLUETOOTH_CONNECT,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val filter =
            IntentFilter(
                BluetoothDevice.ACTION_ACL_CONNECTED,
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            service.registerReceiver(
                bluetoothReceiver,
                filter,
                Context.RECEIVER_EXPORTED,
            )
        } else {
            service.registerReceiver(
                bluetoothReceiver,
                filter,
            )
        }

        bluetoothReceiverRegistered = true
    }

    private fun unregisterBluetoothReceiver() {
        if (!bluetoothReceiverRegistered) return

        try {
            service.unregisterReceiver(
                bluetoothReceiver,
            )
        } catch (_: Exception) {
        }

        bluetoothReceiverRegistered = false
    }

    private fun updateWakeLock() {
        val currentWakeLock =
            wakeLock ?: return
        val shouldHold =
            wakelockEnabled &&
                player.isPlaying

        if (
            shouldHold &&
            !currentWakeLock.isHeld
        ) {
            currentWakeLock.acquire()
        } else if (
            !shouldHold &&
            currentWakeLock.isHeld
        ) {
            currentWakeLock.release()
        }
    }

    private fun stopForegroundAndSelf() {
        cancelIdleStop()
        removeForeground()
        service.stopSelf()
    }

    private fun removeForeground() {
        runCatching {
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.N
            ) {
                service.stopForeground(
                    Service.STOP_FOREGROUND_REMOVE,
                )
            } else {
                service.stopForeground(true)
            }
        }

        hasCalledStartForeground = false
    }
}
