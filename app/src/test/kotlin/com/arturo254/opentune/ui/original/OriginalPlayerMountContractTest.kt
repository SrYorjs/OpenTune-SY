package com.arturo254.opentune.ui.original
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.constants.PlayerBackgroundStyleKey
import com.arturo254.opentune.ui.original.adapters.*
class OriginalPlayerMountContractTest {
 private fun source(relative: String): String {
  val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
   .first { File(it, "app/src/main/kotlin").isDirectory }
  return File(root, "app/src/main/kotlin/com/arturo254/opentune/" + relative).readText()
 }
 @Test fun safetySwitchKeepsTheCurrentMountByDefault() {
  assertFalse(USE_ORIGINAL_PLAYER)
  assertEquals(PlayerMount.CURRENT, playerMount())
  assertEquals(PlayerMount.CURRENT, playerMount(false))
 }
 @Test fun oneSwitchSelectsTheOriginalMount() { assertEquals(PlayerMount.ORIGINAL, playerMount(true)) }
 @Test fun allSeventeenRouteToTheirRealSceneFamilies() {
  val counts = PlayerDesignStyle.entries.groupingBy(::galaxSceneFamily).eachCount()
  assertEquals(mapOf(GalaxSceneFamily.LEGACY to 7, GalaxSceneFamily.APPLE to 1,
   GalaxSceneFamily.STUDIO to 7, GalaxSceneFamily.IMMERSIVE to 1, GalaxSceneFamily.VINJAX to 1), counts)
  assertEquals(GalaxSceneFamily.APPLE, galaxSceneFamily(PlayerDesignStyle.V8))
  assertEquals(GalaxSceneFamily.IMMERSIVE, galaxSceneFamily(PlayerDesignStyle.V16))
  assertEquals(GalaxSceneFamily.VINJAX, galaxSceneFamily(PlayerDesignStyle.V17))
 }
 @Test fun originalEditorCannotOverwriteTheSavedGalaxBackground() {
  assertEquals("playerBackgroundStyle", PlayerBackgroundStyleKey.name)
  assertNotEquals(PlayerBackgroundStyleKey, OriginalClassicBackgroundStyleKey)
  val preferences = mutablePreferencesOf(PlayerBackgroundStyleKey to "BLUR")
  preferences[OriginalClassicBackgroundStyleKey] = "COVER_GRADIENT"
  assertEquals("BLUR", preferences[PlayerBackgroundStyleKey])
 }
 @Test fun theActualActivityCallsTheGateAndRetainsTheCurrentPlayer() {
  val activity = source("MainActivity.kt")
  val gate = source("ui/original/adapters/PlayerMountGate.kt")
  assertTrue(activity.contains("ui.original.adapters.PlayerMountGate("))
  assertTrue(activity.contains("lyricsSyncOffset = lyricsSyncOffset"))
  assertTrue(gate.contains("PlayerMount.CURRENT -> current()"))
  assertTrue(gate.contains("ui.original.player.BottomSheetPlayer("))
 }
 @Test fun unifiedSelectorUsesNativeRenderersAndBlocksPreviewInput() {
  val root = source("ui/original/player/BottomSheetPlayer.kt")
  val renderer = source("ui/original/adapters/GalaxPlayerSurface.kt")
  assertTrue(root.contains("styles = playerStyles"))
  assertTrue(root.contains("galaxPreview = { design -> GalaxPlayerSurface("))
  assertTrue(renderer.contains("if (preview) Modifier.inert()"))
  listOf("StudioVisualScene(", "StudioThemeV16(", "VinJaxPlayerScene(", "V8PlayerScene(",
   "V8PlayerControlsContent(", "PlayerControlsContent(", "PlayerLayout(").forEach { assertTrue(it, renderer.contains(it)) }
  assertTrue(root.contains(".filter(C2Capabilities::actionAvailable)"))
  assertTrue(root.contains("sideEnabled = C2Capabilities.actionAvailable(menuLayout.side)"))
 }
 @Test fun videoReturnKeepsEveryGalaxThemeAndBothPreferenceNamespaces() {
  OriginalStyleCatalog.galax.forEach { choice ->
   val preferences = mutablePreferencesOf(com.arturo254.opentune.constants.PlayerDesignStyleKey to choice.galax!!.name)
   val returnId = choice.id
   OriginalStyleCatalog.select(preferences, OriginalStyleChoice(mochy = com.arturo254.opentune.ui.original.fullconstants.PlayerStyle.VIDEO))
   assertEquals(choice.galax!!.name, preferences[com.arturo254.opentune.constants.PlayerDesignStyleKey])
   val restored = resolveOriginalVideoReturn(returnId)
   assertEquals(choice, restored)
   OriginalStyleCatalog.select(preferences, restored!!)
   assertEquals(choice, OriginalStyleCatalog.read(preferences))
  }
 }
 @Test fun legacyVideoReturnAliasesRemainReadable() {
  assertEquals(com.arturo254.opentune.ui.original.fullconstants.PlayerStyle.TIMELINE, resolveOriginalVideoReturn("SIMPLE")?.mochy)
  assertEquals(com.arturo254.opentune.ui.original.fullconstants.PlayerStyle.FLAGSHIP, resolveOriginalVideoReturn("DEFAULT")?.mochy)
  assertNull(resolveOriginalVideoReturn("MODULE"))
  assertNull(resolveOriginalVideoReturn("mochy:VIDEO"))
 }

 @Test fun nativeMenusKeepTheirDefaultButAcceptTheOriginalFilteredHost() {
  val renderer = source("ui/original/adapters/GalaxPlayerSurface.kt")
  val legacy = source("ui/player/PlayerComponents.kt")
  val apple = source("ui/player/v8/V8PlayerComponents.kt")
  assertEquals(2, renderer.split("onMenuOverride = styleActions.onOtherMenu").size - 1)
  listOf(legacy, apple).forEach {
   assertTrue(it.contains("onMenuOverride: (() -> Unit)? = null"))
   assertTrue(it.contains("onMenuOverride?.invoke() ?: menuState.showPlayerPanelMenu("))
  }
 }

}
