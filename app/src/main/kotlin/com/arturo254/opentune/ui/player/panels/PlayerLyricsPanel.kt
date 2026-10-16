package com.arturo254.opentune.ui.player.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.ui.component.BottomSheet
import com.arturo254.opentune.ui.component.BottomSheetState
import com.arturo254.opentune.ui.player.LyricsScreen

/**
 * Existing LyricsScreen is the only lyrics implementation.
 * Player 2.0 only owns the sheet composition and back/collapse routing:
 * Lyrics/LyricsV2, auto-fetch, sync offset, menus and volume remain unchanged.
 * Keep the sheet absent when there is no active MediaMetadata, as before.
 */
@Composable
fun PlayerLyricsPanel(
    mediaMetadata: MediaMetadata?,
    sheetState: BottomSheetState,
    navController: NavController,
    lyricsSyncOffset: Int,
    modifier: Modifier = Modifier,
) {
    mediaMetadata?.let { metadata ->
                BottomSheet(
                    state = sheetState,
                    backgroundColor = Color.Unspecified,
                    onDismiss = { /* Optional dismiss action */ },
                    collapsedContent = {
                        // Empty collapsed content - fully hidden when collapsed
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surface.copy(
                                    alpha = sheetState.progress.coerceIn(0f, 1f)
                                )
                            )
                    ) {
                        LyricsScreen(
                            mediaMetadata = metadata,
                            onBackClick = { sheetState.collapseSoft() },
                            navController = navController,
                            lyricsSyncOffset = lyricsSyncOffset,
                            modifier = modifier,
                        )
                    }
                }
            }
}
