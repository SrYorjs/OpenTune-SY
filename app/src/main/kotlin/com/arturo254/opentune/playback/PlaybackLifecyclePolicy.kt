package com.arturo254.opentune.playback

internal object PlaybackLifecyclePolicy {
    fun shouldStopServiceOnTaskRemoved(
        stopMusicOnTaskClearEnabled: Boolean,
        isHostSessionActive: Boolean,
        isPlaybackInactive: Boolean,
    ): Boolean =
        (isHostSessionActive && isPlaybackInactive) ||
            stopMusicOnTaskClearEnabled
}
