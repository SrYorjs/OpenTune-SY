package com.arturo254.opentune.ui.original.adapters

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.navigation.NavHostController
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.constants.PauseSearchHistoryKey
import com.arturo254.opentune.db.entities.SearchHistory
import com.arturo254.opentune.ui.screens.GalaxDiscoverScreen
import com.arturo254.opentune.ui.screens.search.PrismSearchBar
import com.arturo254.opentune.ui.screens.search.UniversalSearchScreen
import com.arturo254.opentune.utils.rememberPreference

@Composable
internal fun SearchPresentation(navController: NavHostController, pureBlack: Boolean) {
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var active by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val database = LocalDatabase.current
    val pauseHistory by rememberPreference(PauseSearchHistoryKey, false)
    val entry by navController.currentBackStackEntryFlow.collectAsState(initial = navController.currentBackStackEntry)
    val request by entry?.savedStateHandle?.getStateFlow("requestSearchFocus", false)
        ?.collectAsState() ?: remember { mutableStateOf(false) }
    LaunchedEffect(request) {
        if (request) {
            active = true
            entry?.savedStateHandle?.set("requestSearchFocus", false)
        }
    }
    Box(Modifier.fillMaxSize()) {
        GalaxDiscoverScreen(navController, onOpenSearch = { active = true }, ambientActive = !active)
        if (active) {
            PrismSearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = { text ->
                    if (text.isNotBlank() && !pauseHistory) database.query { insert(SearchHistory(query = text)) }
                    focusManager.clearFocus()
                },
                active = active,
                onActiveChange = { active = it },
                onBack = { active = false; focusManager.clearFocus() },
                focusRequester = focus,
                pureBlack = pureBlack,
            ) {
                UniversalSearchScreen(query.text, pureBlack, embedded = true)
            }
        }
    }
}
