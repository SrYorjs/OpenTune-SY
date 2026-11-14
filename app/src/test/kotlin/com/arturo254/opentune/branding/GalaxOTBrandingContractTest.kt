package com.arturo254.opentune.branding

import java.io.File
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
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
        assertTrue(manifest.contains("android:roundIcon=\"@mipmap/ic_launcher_round\""))
    }

    @Test fun brandNameIsVisibleWithoutRenamingLegalAndInternalIdentifiers() {
        val appName = source("res/values/app_name.xml")
        assertTrue(appName.contains("<string name=\"app_name\">GalaxOT</string>"))
        assertTrue(File(repo, "src/debug/res/values/app_name.xml").readText().contains("GalaxOT Debug"))
        val project = if (File("app/src/main").isDirectory) File(".") else File("..")
        val roots = listOf(File(repo, "src"), File(project, "visual-foundation/src/main/res"))
        val brand = Regex("(?i)GalaxOT|Mochy")
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        for (directory in roots) for (xml in directory.walkTopDown().filter { it.isFile && it.extension == "xml" && it.parentFile.name.startsWith("values") }) {
            val document = factory.newDocumentBuilder().parse(xml)
            val entries = document.getElementsByTagName("string")
            for (index in 0 until entries.length) {
                val entry = entries.item(index) as org.w3c.dom.Element
                val value = entry.textContent.trim()
                if (entry.getAttribute("name") == "app_name" || Regex("https?://\\S+").matches(value)) continue
                assertFalse("Visible brand in "+xml+" / "+entry.getAttribute("name"), brand.containsMatchIn(value))
            }
        }
    }

    @Test fun publicationTitlesUseTheSameGalaxOTBrand() {
        val root = if (File("fastlane").isDirectory) File(".") else File("..")
        for (locale in listOf("en-US", "es-ES")) {
            val dir = File(root, "fastlane/metadata/android/$locale")
            assertEquals("GalaxOT", File(dir, "title.txt").readText().trim())
            assertTrue(File(dir, "short_description.txt").readText().contains("GalaxOT"))
        }
    }

    @Test fun adaptiveIconContainsTheBrandmarkAndThemedMonochrome() {
        val xml = source("res/mipmap-anydpi-v26/ic_launcher.xml")
        assertTrue(xml.contains("@color/galaxot_launcher_background"))
        assertTrue(xml.contains("@mipmap/ic_launcher_foreground"))
        assertTrue(xml.contains("@mipmap/ic_launcher_monochrome"))
        assertTrue(source("res/mipmap-anydpi-v26/ic_launcher_round.xml").contains("@mipmap/ic_launcher_foreground"))
        val densities = mapOf("ldpi" to 0.75, "mdpi" to 1.0, "tvdpi" to 4.0/3.0, "hdpi" to 1.5, "xhdpi" to 2.0, "xxhdpi" to 3.0, "xxxhdpi" to 4.0)
        for ((density, scale) in densities) {
            for (name in listOf("ic_launcher", "ic_launcher_round", "ic_launcher_foreground", "ic_launcher_background", "ic_launcher_monochrome")) {
                val image = javax.imageio.ImageIO.read(resource("res/mipmap-$density/$name.png"))
                val expected = kotlin.math.round((if (name == "ic_launcher" || name == "ic_launcher_round") 48 else 108)*scale).toInt()
                assertEquals("$density/$name width", expected, image.width)
                assertEquals("$density/$name height", expected, image.height)
                assertTrue(image.colorModel.hasAlpha())
                if (name == "ic_launcher_foreground" || name == "ic_launcher_monochrome") {
                    val safe = image.width * 33.0/108.0 + 1.5
                    var visible = 0
                    for (y in 0 until image.height) for (x in 0 until image.width) {
                        val pixel = image.getRGB(x,y)
                        if ((pixel ushr 24) == 0) continue
                        visible++
                        val dx=x+0.5-image.width/2.0
                        val dy=y+0.5-image.height/2.0
                        assertTrue("Outside adaptive safe zone: $density/$name", dx*dx+dy*dy <= safe*safe)
                        if (name == "ic_launcher_monochrome") assertEquals("Monochrome must be white", 0xFFFFFF, pixel and 0xFFFFFF)
                    }
                    assertTrue("Empty identity artwork: $density/$name", visible > 10)
                }
            }
        }
    }

    @Test fun launcherBackgroundsAndOriginalIconUseRealAlphaTransparency() {
        val project = if (File("app/src/main").isDirectory) File(".") else File("..")
        val input = File(project,"assets/icon.png").readBytes()
        val copy = resource("res/drawable-nodpi/galaxot_icon_transparent.png").readBytes()
        org.junit.Assert.assertArrayEquals("The original supplied color image must be preserved", input, copy)
        val image = javax.imageio.ImageIO.read(resource("res/drawable-nodpi/galaxot_icon_transparent.png"))
        assertEquals(1254, image.width)
        assertEquals(1254, image.height)
        assertTrue(image.colorModel.hasAlpha())
        assertEquals(0, image.getRGB(0,0) ushr 24)
        assertTrue(source("res/values/galaxot_brand.xml").contains("<color name=\"galaxot_launcher_background\">#00000000</color>"))
        for (density in listOf("ldpi", "mdpi", "tvdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")) {
            val icon = javax.imageio.ImageIO.read(resource("res/mipmap-$density/ic_launcher.png"))
            assertTrue(icon.colorModel.hasAlpha())
            assertEquals(0, icon.getRGB(0,0) ushr 24)
            val background = javax.imageio.ImageIO.read(resource("res/mipmap-$density/ic_launcher_background.png"))
            for (y in 0 until background.height) for (x in 0 until background.width) assertEquals(0, background.getRGB(x,y) ushr 24)
        }
    }

    @Test fun splashUsesGalacticBackgroundAndRetainsExistingMainActivity() {
        val base = source("res/values/styles.xml")
        val v31 = source("res/values-v31/styles.xml")
        val draw = source("res/drawable/galaxot_launch_background.xml")
        assertTrue(base.contains("name=\"Theme.GalaxOT\" parent=\"Theme.OpenTune\""))
        assertTrue(base.contains("@drawable/galaxot_launch_background"))
        assertTrue(v31.contains("android:windowSplashScreenBackground"))
        assertTrue(v31.contains("@drawable/galaxot_launcher_foreground"))
        assertTrue(draw.contains("@color/galaxot_splash_background"))
        val intro = source("kotlin/com/arturo254/opentune/ui/original/launch/LaunchIntro.kt")
        assertTrue(intro.contains("R.drawable.galaxot_icon_transparent"))
        assertTrue(intro.contains("canvas.drawBitmap(bitmap, null, bounds, paint)"))
        assertFalse(intro.contains("M163.5 96.3493"))
        assertFalse(intro.contains("M97 112L108.5"))
        for (timing in listOf("FlightMillis = 660", "RiseMillis = 520", "PreludeMillis = 1_100", "0.55f, 0f, 0.1f, 1f")) assertTrue(intro.contains(timing))
        val exit = source("kotlin/com/arturo254/opentune/ui/original/utils/SplashScreenExit.kt")
        assertTrue(exit.contains("LaunchGate.onSplashGone()"))
        assertTrue(exit.contains("provider.remove()"))
        assertTrue(source("AndroidManifest.xml").contains("android:name=\".MainActivity\""))
    }

    @Test fun notificationsUseTheWhiteLogoInEveryDensity() {
        val densities = mapOf("ldpi" to .75,"mdpi" to 1.0,"tvdpi" to 4.0/3.0,"hdpi" to 1.5,"xhdpi" to 2.0,"xxhdpi" to 3.0,"xxxhdpi" to 4.0)
        for ((density, scale) in densities) {
            val image = javax.imageio.ImageIO.read(resource("res/drawable-$density/galaxot_notification.png"))
            assertEquals(kotlin.math.round(24*scale).toInt(), image.width)
            assertEquals(image.width,image.height)
            assertEquals(0,image.getRGB(0,0) ushr 24)
            var visible=0
            for (y in 0 until image.height) for (x in 0 until image.width) {
                val pixel=image.getRGB(x,y)
                if ((pixel ushr 24)==0) continue
                visible++
                assertEquals("Notification artwork must be white on transparent",0xFFFFFF,pixel and 0xFFFFFF)
            }
            assertTrue(visible>10)
        }
        for (path in listOf("kotlin/com/arturo254/opentune/utils/UpdateNotificationManager.kt","kotlin/com/arturo254/opentune/playback/MediaSessionNotificationController.kt")) {
            val code=source(path)
            assertTrue(code.contains("setSmallIcon(R.drawable.galaxot_notification)"))
            assertFalse(code.contains("setSmallIcon(R.drawable.opentune)"))
        }
    }

    @Test fun everyGeneratedIdentityResourceMatchesItsDeclaredInputsAndOutputHash() {
        val project=if(File("app/src/main").isDirectory) File(".") else File("..")
        val plan=kotlinx.serialization.json.Json.parseToJsonElement(File(project,"docs/inventario-04b/branding.json").readText()).jsonObject
        val entries=plan.getValue("outputs").jsonArray
        assertTrue("Missing branding variants",entries.size>=98)
        for (entry in entries) {
            val item=entry.jsonObject
            val path=item.getValue("output").jsonPrimitive.content
            val input=item.getValue("input").jsonPrimitive.content
            assertTrue(input in setOf("assets/icon.png","assets/Notification_Logo.png"))
            val file=File(project,path)
            val image=javax.imageio.ImageIO.read(file)
            assertEquals(path,item.getValue("width").jsonPrimitive.content.toInt(),image.width)
            assertEquals(path,item.getValue("height").jsonPrimitive.content.toInt(),image.height)
            val digest=java.security.MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString(""){"%02x".format(it.toInt() and 0xff)}
            assertEquals(path,item.getValue("sha256").jsonPrimitive.content,digest)
        }
        for (module in listOf(File(repo,"src"),File(project,"visual-foundation/src"))) {
            for (file in module.walkTopDown().filter{it.isFile && it.extension=="xml" && it.path.contains("res")}) {
                val text=file.readText()
                assertFalse("Original mascot in "+file,Regex("163\\.5[ ,]+96\\.3493|167\\.25[ ,]+89\\.854").containsMatchIn(text))
            }
        }
    }
}
