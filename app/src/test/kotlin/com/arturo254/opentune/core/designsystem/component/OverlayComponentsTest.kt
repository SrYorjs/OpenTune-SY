package com.arturo254.opentune.core.designsystem.component

import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayComponentsTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Source tree missing")
    private fun source(path: String) = File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun playerAndDialogsRetainTheirRadii() {
        assertEquals(16.dp, OpenTuneOverlayDimensions.collapsedPlayerTopRadius)
        assertTrue(source("ui/component/Dialog.kt").contains("OpenTuneShapeRadii.sheet"))
        assertTrue(source("ui/component/BottomSheet.kt").contains("OpenTuneOverlayDimensions.collapsedPlayerTopRadius"))
    }

    @Test
    fun existingMenuStateAndContextCallbacksRemainExternal() {
        val menu = source("ui/component/BottomSheetMenu.kt")
        val nav = source("ui/component/FloatingNavigationToolbar.kt")
        val components = source("core/designsystem/component/OverlayComponents.kt")
        assertTrue(menu.contains("OpenTuneSheet("))
        assertTrue(menu.contains("state.isVisible = false"))
        assertTrue(nav.contains("OpenTuneContextMenu("))
        listOf("fun OpenTuneSheet(", "fun OpenTuneDialog(", "fun OpenTuneMenu(",
            "fun OpenTuneContextMenu(", "fun OpenTuneMenuItem(").forEach { assertTrue(components.contains(it)) }
        assertTrue(!components.contains("MusicService"))
        assertTrue(!components.contains("NavController"))
    }
}
