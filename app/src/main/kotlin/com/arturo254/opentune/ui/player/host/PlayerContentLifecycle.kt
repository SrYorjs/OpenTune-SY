package com.arturo254.opentune.ui.player.host

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.arturo254.opentune.ui.component.BottomSheetState
import kotlinx.coroutines.delay

/**
 * The visual AOD timeout belongs to PlayerHost, not to the audio service.
 * Uses exactly the legacy interaction timestamp and 100-ms loop.
 */
class PlayerContentLifecycle internal constructor(
    private val recordInteraction: () -> Unit,
) {
    fun onInteraction() = recordInteraction()
}

@Composable
fun rememberPlayerContentLifecycle(
    isExpanded: Boolean,
    aodAutoTimeoutSeconds: Int,
    onAodTimeout: () -> Unit,
): PlayerContentLifecycle {
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isAodActive by remember { mutableStateOf(false) }
    val latestOnTimeout by rememberUpdatedState(onAodTimeout)

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            lastInteractionTime = System.currentTimeMillis()
            isAodActive = false
        }
    }

    // One cancellable deadline instead of waking Compose every 100 ms.
    // Any touch, collapse or timeout-setting change restarts this effect.
    LaunchedEffect(aodAutoTimeoutSeconds, isExpanded, lastInteractionTime, isAodActive) {
        if (aodAutoTimeoutSeconds > 0 && isExpanded && !isAodActive) {
            val remaining = playerAodRemainingDelayMs(
                aodAutoTimeoutSeconds = aodAutoTimeoutSeconds,
                elapsedMs = System.currentTimeMillis() - lastInteractionTime,
            )
            delay(remaining)
            if (isExpanded && !isAodActive) {
                isAodActive = true
                latestOnTimeout()
            }
        }
    }

    return remember {
        PlayerContentLifecycle {
            lastInteractionTime = System.currentTimeMillis()
            if (isAodActive) isAodActive = false
        }
    }
}

/** Saturating milliseconds calculation: no busy timer or negative delay. */
internal fun playerAodRemainingDelayMs(aodAutoTimeoutSeconds: Int, elapsedMs: Long): Long =
    (aodAutoTimeoutSeconds.toLong().coerceAtLeast(0L) * 1_000L -
        elapsedMs.coerceAtLeast(0L)).coerceAtLeast(0L)

/** One Back priority for lyrics, queue, and player. Each sheet keeps its own animation. */
@Composable
fun PlayerHostBackHandler(
    playerSheet: BottomSheetState,
    queueSheet: BottomSheetState,
    lyricsSheet: BottomSheetState,
) {
    val destination = playerHostBackDestination(
        lyricsOpen = !lyricsSheet.isCollapsed && !lyricsSheet.isDismissed,
        queueOpen = !queueSheet.isCollapsed && !queueSheet.isDismissed,
        playerOpen = !playerSheet.isCollapsed && !playerSheet.isDismissed,
    )
    BackHandler(enabled = destination != PlayerHostBackDestination.NONE) {
        when (destination) {
            PlayerHostBackDestination.LYRICS -> lyricsSheet.collapseSoft()
            PlayerHostBackDestination.QUEUE -> queueSheet.collapseSoft()
            PlayerHostBackDestination.PLAYER -> playerSheet.collapseSoft()
            PlayerHostBackDestination.NONE -> Unit
        }
    }
}
