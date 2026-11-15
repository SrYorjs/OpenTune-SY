package com.arturo254.opentune.ui.original.adapters
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.ui.original.component.BottomSheetState
import kotlinx.coroutines.flow.MutableStateFlow
/** Active original mount. Set false to restore the retained current mount without mounting this tree. */
internal const val USE_ORIGINAL_PLAYER = true
internal enum class PlayerMount { CURRENT, ORIGINAL }
internal fun playerMount(original: Boolean = USE_ORIGINAL_PLAYER) = if (original) PlayerMount.ORIGINAL else PlayerMount.CURRENT
@Composable fun PlayerMountGate(
 state: BottomSheetState,
 navController: NavController,
 pureBlack: Boolean,
 modifier: Modifier = Modifier,
 current: @Composable () -> Unit,
) {
 when (playerMount()) {
  PlayerMount.CURRENT -> current()
  PlayerMount.ORIGINAL -> OriginalStudioHost(pageActive = true) {
   val connection = LocalPlayerConnection.current ?: return@OriginalStudioHost
   val scope = rememberCoroutineScope()
   val visible = remember { MutableStateFlow(true) }
   val original = remember(connection, scope) { OriginalPlayerConnection(connection, scope, visible) }
   CompositionLocalProvider(LocalOriginalPlayerConnection provides original) {
    com.arturo254.opentune.ui.original.player.BottomSheetPlayer(state, navController, modifier, pureBlack)
   }
  }
 }
}
