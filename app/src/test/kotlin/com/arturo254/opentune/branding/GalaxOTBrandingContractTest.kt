package com.arturo254.opentune.branding

import java.io.File
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 4.5.1: verify that GalaxOT is an in-place update of OpenTune and
 * that Android adaptive, legacy and splash branding ship actual image files.
 */
class GalaxOTBrandingContractTest {
    private val repo = if (File("app/src/main").isDirectory) File("app") else File(".")
    private fun source(path: String) = File(repo, "src/main/$path").readText()
    private fun resource(path: String) = File(repo, "src/main/$path")

    @Test fun keepsExactOriginalAppIdNamespaceAndTogetherDeepLink() {
        val gradle = File(repo, "build.gradle.kts").readText()
        val manifest = source("AndroidManifest.xml")
        assertTrue(gradle.contains("applicationId = \"com.Arturo254.opentune\""))
        assertTrue(gradle.contains("namespace = \"com.arturo254.opentune\""))
        assertTrue(manifest.contains("android:scheme=\"opentune\""))
        assertTrue(manifest.contains("android:host=\"together\""))
        assertTrue(manifest.contains("android:label=\"@string/app_name\""))
        assertTrue(manifest.contains("android:theme=\"@style/Theme.GalaxOT\""))
        assertTrue(manifest.contains("android:roundIcon=\"@mipmap/ic_launcher\""))
    }

    @Test fun brandNameIsVisibleWithoutRenamingLegalAndInternalIdentifiers() {
        val appName = source("res/values/app_name.xml")
        assertTrue(appName.contains("<string name=\"app_name\">GalaxOT</string>"))
        assertFalse(appName.contains(">OpenTune<"))
        assertTrue(File(repo, "src/debug/res/values/app_name.xml").readText().contains("GalaxOT Debug"))
        assertTrue(source("res/values/strings.xml").contains("GalaxOT"))
        assertTrue(source("res/values-es/strings.xml").contains("GalaxOT"))
        assertTrue(source("res/values/opentune_strings.xml").contains("GalaxOT"))
        assertTrue(source("res/values-es/opentune_strings.xml").contains("GalaxOT"))
    }

    @Test fun adaptiveIconContainsTheBrandmarkAndThemedMonochrome() {
        val xml = source("res/mipmap-anydpi-v26/ic_launcher.xml")
        assertTrue(xml.contains("@color/galaxot_launcher_background"))
        assertTrue(xml.contains("@drawable/galaxot_launcher_foreground"))
        assertTrue(xml.contains("@drawable/galaxot_launcher_monochrome"))
        for (path in listOf(
            "drawable-nodpi/galaxot_logo.webp",
            "drawable-nodpi/galaxot_launcher_foreground.webp",
            "drawable-nodpi/galaxot_launcher_monochrome.webp",
        )) {
            val bytes = resource("res/$path").readBytes()
            assertTrue("$path must contain real brand art", bytes.size > 1_000)
            assertEquals("RIFF", String(bytes, 0, 4, StandardCharsets.US_ASCII))
            assertEquals("WEBP", String(bytes, 8, 4, StandardCharsets.US_ASCII))
        }
        for (density in listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")) {
            for (name in listOf(
                "ic_launcher.png",
                "ic_launcher_foreground.png",
                "ic_launcher_background.png",
                "ic_launcher_monochrome.png",
            )) {
                val file = resource("res/mipmap-$density/$name")
                assertTrue("$density/$name missing", file.isFile)
                val bytes = file.readBytes()
                assertTrue(bytes.size > 100)
                assertEquals(0x89, bytes[0].toInt() and 0xff)
                assertEquals('P'.code, bytes[1].toInt())
                assertEquals('N'.code, bytes[2].toInt())
                assertEquals('G'.code, bytes[3].toInt())
            }
        }
    }

    @Test fun splashUsesGalacticBackgroundAndRetainsExistingMainActivity() {
        val base = source("res/values/styles.xml")
        val v31 = source("res/values-v31/styles.xml")
        val draw = source("res/drawable/galaxot_launch_background.xml")
        val manifest = source("AndroidManifest.xml")
        assertTrue(base.contains("name=\"Theme.GalaxOT\" parent=\"Theme.OpenTune\""))
        assertTrue(base.contains("@drawable/galaxot_launch_background"))
        assertTrue(v31.contains("android:windowSplashScreenBackground"))
        assertTrue(v31.contains("android:windowSplashScreenAnimatedIcon"))
        assertTrue(v31.contains("@drawable/galaxot_launcher_foreground"))
        assertTrue(draw.contains("@color/galaxot_splash_background"))
        assertTrue(manifest.contains("android:name=\".MainActivity\""))
        assertFalse(manifest.contains("android:scheme=\"galaxot\""))
    }
}
