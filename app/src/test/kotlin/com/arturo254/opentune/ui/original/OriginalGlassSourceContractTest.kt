package com.arturo254.opentune.ui.original

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class OriginalGlassSourceContractTest {
    private fun source(path: String): String {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "app/src/main/kotlin").isDirectory }
        return File(root, "app/src/main/kotlin/com/arturo254/opentune/$path").readText()
    }

    @Test fun routeSourceCannotSampleItsOwnCapture() {
        val host = source("ui/original/adapters/ActivityGlassHost.kt")
        val source = host.substringAfter("fun ActivityGlassSourceRow(")
        assertTrue(source.contains("Modifier.glassRoot(glass)"))
        assertTrue(source.contains("Modifier.layerBackdrop(backdrop)"))
        listOf("LocalOriginalGlassState", "LocalGlassState", "LocalGalaxLiquidBackdrop", "LocalLiquidBackdrop", "LocalSky")
            .forEach { assertTrue(it, source.contains("$it provides null")) }
        assertTrue(host.substringBefore("fun ActivityGlassSourceRow(").contains("if (!routeSource && backdrop != null) Modifier.layerBackdrop(backdrop)"))
    }

    @Test fun actualActivityCapturesPagesSeparatelyFromChromeAndKeepsOpticalPreferences() {
        val activity = source("MainActivity.kt")
        val row = activity.substringAfter("adapters.ActivityGlassSourceRow(").substringBefore("// action feedback")
        assertTrue(row.contains("NavHost("))
        assertFalse(row.contains("AppNavigationBar("))
        assertFalse(row.contains("PlayerMountGate("))
        val host = source("ui/original/adapters/ActivityGlassHost.kt")
        assertTrue(host.contains("LocalGalaxLiquidVariant provides variant"))
        assertTrue(host.contains("LocalGalaxLiquidTuning provides tuning"))
        assertTrue(host.contains("!saver && !lowRam && Build.VERSION.SDK_INT >= 33"))
    }
}
