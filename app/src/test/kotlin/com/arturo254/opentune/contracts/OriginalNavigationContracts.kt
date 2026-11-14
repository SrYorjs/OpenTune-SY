package com.arturo254.opentune.contracts

import java.io.File
import org.junit.Assert.*

/** Contracts follow the active host and its delegates, rather than obsolete file placement. */
object OriginalNavigationContracts {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Kotlin source root missing")
    fun source(path: String) = File(root, "com/arturo254/opentune/$path").readText()

    fun routesAndStacks() {
        val expected = listOf("home", "search_input", "library", "settings")
        assertEquals(expected, com.arturo254.opentune.ui.screens.Screens.MainScreens.map { it.route })
        assertEquals(expected, com.arturo254.opentune.ui.original.screens.Screens.MainScreens.map { it.route })
        val registered = com.arturo254.opentune.ui.original.screens.OriginalNavigationRoutes
        assertTrue(registered.containsAll(expected + listOf("search/{query}", "album/{albumId}", "artist/{artistId}", "local_playlist/{playlistId}")))
        val extra = source("ui/screens/NavigationBuilder.kt")
        assertTrue(extra.contains("originalNavigationBuilder("))
        assertTrue(extra.contains("composable(\"search\")"))
        assertTrue(extra.contains("settings/appearance/player_studio"))
        assertTrue(extra.contains("settings/appearance/galax_liquid"))
        val nav = source("ui/original/component/AppNavigation.kt")
        listOf("saveState = true", "restoreState = true", "launchSingleTop = true", "popBackStack(screen.route", "savedStateHandle?.set(\"scrollToTop\", true)", "backStack.asReversed()", ".navHighlight(selectedIndex, navigationItems.size)").forEach {
            assertTrue("Missing tab behavior: $it", nav.contains(it))
        }
        assertTrue(source("MainActivity.kt").contains("navController.onTabTapped("))
    }

    fun singleGlassRenderer() {
        assertTrue(source("ui/component/FloatingNavigationToolbar.kt").contains("AppNavigationBar("))
        val nav = source("ui/original/component/AppNavigation.kt")
        assertTrue(nav.contains("FrostedChromeGlass("))
        assertTrue(nav.contains("onItemClick(screen, isSelected)"))
        assertTrue(source("ui/original/component/FrostedGlass.kt").contains("if (OriginalLiquidChrome(pureBlack, modifier)) return"))
        val bridge = source("ui/original/component/OriginalChromeSurface.kt")
        assertTrue(bridge.contains("LocalGalaxLiquidVariant.current"))
        assertTrue(bridge.contains("modifier.galaxLiquidEffect("))
        assertFalse(nav.contains("drawBackdrop("))
        assertFalse(source("ui/component/FloatingNavigationToolbar.kt").contains("drawBackdrop("))
        assertTrue(source("ui/player/visual/PlayerGlassEffect.kt").contains("galaxGlassEffect("))
        val host = source("ui/original/adapters/ActivityGlassHost.kt")
        listOf("LocalGalaxLiquidVariant provides variant", "LocalGalaxLiquidTuning provides tuning", "LocalGalaxLiquidBackdrop provides backdrop", "LocalLiquidBackdrop provides null").forEach { assertTrue(host.contains(it)) }
    }

    fun backgroundIsSeparate() {
        val host = source("ui/original/adapters/ActivityGlassHost.kt")
        assertTrue(source("ui/original/adapters/ActivityThemeHost.kt").contains("ActivityGlassHost(pureBlack, content)"))
        assertTrue(host.contains("Modifier.matchParentSize().glassRoot(glass)"))
        assertTrue(host.contains("Modifier.layerBackdrop(backdrop)"))
        assertTrue(host.indexOf(".glassRoot(glass)") < host.indexOf("            content()"))
        assertFalse(host.contains("NavHost("))
        assertFalse(host.contains("AppNavigationBar("))
        assertFalse(source("MainActivity.kt").contains(".glassRoot(glassState"))
        assertTrue(source("ui/original/adapters/UpdateRuntimeAdapters.kt").contains("ActivityGlassHost(pureBlack)"))
    }
}
