package com.arturo254.opentune.playback

internal data class StreamRefreshSignals(
    val badHttpStatusError: Boolean = false,
    val fileNotFoundError: Boolean = false,
    val readPositionOutOfRangeError: Boolean = false,
    val httpStatusCode: Int? = null,
    val resolutionTimeoutError: Boolean = false,
)

internal object ErrorRecoveryPolicy {
    const val MAX_CONSECUTIVE_ERRORS = 5

    private val refreshableHttpStatuses =
        setOf(403, 404, 410, 416, 429, 500, 502, 503)

    fun shouldAttemptStreamRefresh(signals: StreamRefreshSignals): Boolean =
        signals.badHttpStatusError ||
            signals.resolutionTimeoutError ||
            signals.fileNotFoundError ||
            signals.readPositionOutOfRangeError ||
            signals.httpStatusCode in refreshableHttpStatuses

    fun looksLikeSilenceProcessorFailure(
        skipSilenceEnabled: Boolean,
        causeText: String,
    ): Boolean {
        if (!skipSilenceEnabled) return false
        val normalized = causeText.lowercase()
        return "silenceskippingaudioprocessor" in normalized || "silence" in normalized
    }

    fun nextConsecutiveErrorCount(current: Int): Int =
        current + 2

    fun shouldSkipToNext(
        nextConsecutiveErrorCount: Int,
        hasNextItem: Boolean,
    ): Boolean =
        nextConsecutiveErrorCount <= MAX_CONSECUTIVE_ERRORS && hasNextItem
}

internal class TrackRecoveryAttempts(private val now: () -> Long = System::currentTimeMillis) {
    private var activeId: String? = null
    private var attempts = 0
    private var lastAt = 0L

    fun transition(mediaId: String?) {
        if (activeId != mediaId) {
            activeId = mediaId
            attempts = 0
            lastAt = 0L
        }
    }

    fun allow(mediaId: String): Boolean {
        transition(mediaId)
        val current = now()
        if (current - lastAt > 45_000L) attempts = 0
        if (attempts >= 2) return false
        attempts++
        lastAt = current
        return true
    }
}
