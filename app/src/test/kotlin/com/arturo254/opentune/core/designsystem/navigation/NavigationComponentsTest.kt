package com.arturo254.opentune.core.designsystem.navigation

import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationComponentsTest {
    private val root =
        listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
            .firstOrNull(File::isDirectory) ?: error("Sources unavailable")

    @Test
    fun floatingToolbarPreservesPreviousDimensions() {
        assertEquals(58.dp, OpenTuneNavigationDimensions.floatingHeight)
        assertEquals(30.dp, OpenTuneNavigationDimensions.floatingRadius)
        assertEquals(5.dp, OpenTuneNavigationDimensions.floatingHorizontalPadding)
        assertEquals(4.dp, OpenTuneNavigationDimensions.floatingVerticalPadding)
        assertEquals(320.dp, OpenTuneNavigationDimensions.compactLabelThreshold)
    }

    @Test
    fun legacyToolbarUsesSharedLayoutAndRouteCallbacksRemainExternal() {
        val src = File(root,"com/arturo254/opentune/ui/component/FloatingNavigationToolbar.kt").readText()
        assertTrue(src.contains("OpenTuneNavigationDimensions.floatingHeight"))
        assertTrue(src.contains("OpenTuneNavigationDimensions.floatingRadius"))
        assertTrue(src.contains("onItemClick: (Screens, Boolean) -> Unit"))
        val components = File(root,"com/arturo254/opentune/core/designsystem/navigation/NavigationComponents.kt").readText()
        assertTrue(components.contains("onNavigate: (String) -> Unit"))
        // Comments/docs can mention NavController; only executable dependency is disallowed.
        assertTrue(!components.contains("import androidx.navigation.NavController"))
        assertTrue(!components.contains("NavController("))
    }
}
