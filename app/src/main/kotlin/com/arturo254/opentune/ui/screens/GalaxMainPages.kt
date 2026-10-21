package com.arturo254.opentune.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.arturo254.opentune.ui.screens.library.LibraryScreen
import kotlinx.coroutines.launch

@Composable
fun GalaxMainPages(
    navController: NavController,
    pager: PagerState,
    onOpenSearch: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    BackHandler(enabled = pager.currentPage != 0) {
        scope.launch { pager.animateScrollToPage(0) }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        HorizontalPager(state = pager,
            key = { Screens.MainScreens[it].route },
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0,
        ) { page ->
            when (page) {
                0 -> HomeScreen(
                    navController = navController,
                    ambientActive = pager.currentPage == 0,
                    onOpenSearchTab = {
                        scope.launch { pager.animateScrollToPage(1) }
                    },
                    onOpenLibraryTab = {
                        scope.launch { pager.animateScrollToPage(2) }
                    },
                )
                1 -> GalaxDiscoverScreen(
                    navController = navController,
                    onOpenSearch = onOpenSearch,
                    ambientActive = pager.currentPage == 1,
                )
                2 -> LibraryScreen(
                    navController = navController,
                    ambientActive = pager.currentPage == 2,
                    onOpenSearchTab = {
                        scope.launch { pager.animateScrollToPage(1) }
                    },
                )
            }
        }
    }
}
