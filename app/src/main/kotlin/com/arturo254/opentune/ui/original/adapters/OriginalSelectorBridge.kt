package com.arturo254.opentune.ui.original.adapters
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import com.arturo254.opentune.ui.original.player.studio.*
import com.arturo254.opentune.utils.dataStore
import kotlinx.coroutines.launch
/** Original-only selector, with legacy preference migration before selection. */
@Composable fun OriginalSelectorBridge(
 studio: PlayerStudioState,
 pageActive: Boolean,
 onSet: (OriginalStyleChoice, Float) -> Unit,
 onEdit: (OriginalStyleChoice) -> Unit,
 modifier: Modifier = Modifier,
) {
 val store = LocalContext.current.dataStore
 val scope = rememberCoroutineScope()
 val selected by store.data.collectAsState(initial = null)
 LaunchedEffect(store) { store.edit { OriginalStyleCatalog.migrate(it) } }
 val choice = selected?.let(OriginalStyleCatalog::read) ?: return
 LaunchedEffect(studio, choice) {
  require(studio.columns == OriginalStyleCatalog.columns) { "Use rememberOriginalPlayerStudioState()" }
  if (studio.stage == StudioStage.PLAYER) studio.livePage = OriginalStyleCatalog.choices.indexOf(choice)
 }
 OriginalStudioHost(pageActive) { PlayerSelectorOverlay(studio, OriginalStyleCatalog.choices,
  onSet = { index, velocity ->
   val next = OriginalStyleCatalog.choices[index]
   scope.launch { store.edit { OriginalStyleCatalog.select(it, next) }; onSet(next, velocity) }
  },
  onEdit = { index -> onEdit(OriginalStyleCatalog.choices[index]) }, modifier = modifier) }
}

@Composable fun rememberOriginalPlayerStudioState(): PlayerStudioState = rememberPlayerStudioState(OriginalStyleCatalog.columns)
