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
import kotlinx.coroutines.isActive

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

    LaunchedEffect(aodAutoTimeoutSeconds, isExpanded, lastInteractionTime, isAodActive) {
        if (aodAutoTimeoutSeconds > 0 && isExpanded && !isAodActive) {
            delay(100L)
            while (isActive) {
                delay(100L)
                val elapsedSeconds =
                    (System.currentTimeMillis() - lastInteractionTime) / 1000f
                if (elapsedSeconds >= aodAutoTimeoutSeconds) {
                    isAodActive = true
                    latestOnTimeout()
                    break
                }
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
