package com.arturo254.opentune.ui.screens

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.arturo254.opentune.viewmodels.HomeViewModel
import com.arturo254.opentune.viewmodels.MoodAndGenresViewModel

/** Stable navigation entry; the obsolete Atlas layout has been removed. */
@Composable
fun GalaxDiscoverScreen(
    navController: NavController,
    onOpenSearch: () -> Unit,
    genresModel: MoodAndGenresViewModel = hiltViewModel(),
    home: HomeViewModel = hiltViewModel(),
    ambientActive: Boolean = true,
) {
    NovaDiscoveryExperience(navController, onOpenSearch, genresModel, home, ambientActive)
}
