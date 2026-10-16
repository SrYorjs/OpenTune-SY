package com.arturo254.opentune.ui.player.panels

import androidx.navigation.NavController
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.ui.component.BottomSheetPageState
import com.arturo254.opentune.ui.component.BottomSheetState
import com.arturo254.opentune.ui.component.MenuState
import com.arturo254.opentune.ui.menu.PlayerMenu
import com.arturo254.opentune.ui.utils.ShowMediaInfo

/**
 * One integration point for the existing PlayerMenu and media-information sheet.
 *
 * PlayerMenu still owns its options (including volume, DSP, playback speed and
 * other secondary actions). MenuState/BottomSheetPageState own visibility.
 * The currently selected MediaMetadata is captured at the click that opens
 * the menu, so changing tracks does not retarget an already-open panel.
 */
fun MenuState.showPlayerPanelMenu(
    metadata: MediaMetadata?,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    bottomSheetPageState: BottomSheetPageState,
    isQueueTrigger: Boolean = false,
    detailsMediaId: String? = metadata?.id,
) {
    if (metadata == null) return
    show {
        PlayerMenu(
            mediaMetadata = metadata,
            navController = navController,
            playerBottomSheetState = playerBottomSheetState,
            isQueueTrigger = isQueueTrigger,
            onShowDetailsDialog = {
                detailsMediaId?.takeIf { it.isNotBlank() }?.let { mediaId ->
                    bottomSheetPageState.show { ShowMediaInfo(mediaId) }
                }
            },
            onDismiss = this@showPlayerPanelMenu::dismiss,
        )
    }
}
