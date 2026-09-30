package com.arturo254.opentune.playback

internal object AutomixPolicy {
    fun shouldRefreshCurrentSeed(
        force: Boolean,
        sameSeed: Boolean,
        hasItems: Boolean,
        jobActive: Boolean,
    ): Boolean =
        force || !(sameSeed && hasItems && jobActive)

    fun shouldRun(
        autoLoadMoreEnabled: Boolean,
        repeatModeOff: Boolean,
        suppressAutoPlayback: Boolean,
        mediaItemCount: Int,
    ): Boolean =
        autoLoadMoreEnabled &&
            repeatModeOff &&
            !suppressAutoPlayback &&
            mediaItemCount > 0
}
