package com.arturo254.opentune.sources.navidrome

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavidromeIntegrationTest {
    private val root = listOf(
        File("src/main/kotlin"),
        File("app/src/main/kotlin"),
    ).firstOrNull(File::isDirectory) ?: error("Kotlin source folder unavailable")

    private fun source(path: String): String =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun navidromeIsRegisteredAsIndependentStreamingAndDownloadSource() {
        val module = source("sources/MusicSourceModule.kt")
        val coordinator = source("sources/playback/SourcePlaybackCoordinator.kt")
        val source = source("sources/navidrome/NavidromeMusicSource.kt")
        assertTrue(module.contains("bindNavidromeMusicSource"))
        assertTrue(module.contains("bindNavidromeGateway"))
        assertTrue(coordinator.contains("ProviderId.NAVIDROME -> SourcePlaybackRoute.NAVIDROME_DIRECT"))
        assertTrue(source.contains("SourceCapability.STREAM"))
        assertTrue(source.contains("SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD"))
        assertTrue(source.contains("search3"))
        assertTrue(source.contains("streamUrl"))
        assertTrue(source.contains("downloadUrl"))
    }

    @Test
    fun navidromeConnectionKeepsSecretsOutOfRepositories() {
        val panel = source("ui/screens/settings/NavidromeConnectionPanel.kt")
        val integration = source("ui/screens/settings/IntegrationScreen.kt")
        val store = source("sources/navidrome/NavidromeAccountStore.kt")
        assertTrue(integration.contains("NavidromeConnectionPanel()"))
        assertTrue(panel.contains("PasswordVisualTransformation()"))
        assertTrue(store.contains("AndroidKeyStore"))
        assertTrue(store.contains("AES/GCM/NoPadding"))
        assertFalse(store.contains("putString(\"password\", verified.password)"))
        assertTrue(store.contains("validateNavidromeUrl"))
    }
}
