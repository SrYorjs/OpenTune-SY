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
 @Test fun activationSelectsTheOriginalAndRetainsRollback() {
  assertTrue(USE_ORIGINAL_PLAYER)
  assertEquals(PlayerMount.ORIGINAL, playerMount())
  assertEquals(PlayerMount.CURRENT, playerMount(false))
 }
 @Test fun oneSwitchSelectsTheOriginalMount() { assertEquals(PlayerMount.ORIGINAL, playerMount(true)) }
 @Test fun removedThemeRenderersAreAbsentFromOriginalHost() {
  val root = source("ui/original/player/BottomSheetPlayer.kt")
  val selector = source("ui/original/player/studio/PlayerSelector.kt")
  assertFalse(root.contains("GalaxPlayerSurface")); assertFalse(selector.contains("galaxPreview"))
  assertTrue(root.contains("OriginalStyleCatalog.migrate(it)"))
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
 @Test fun selectorDisposesOffscreenPreviewsAndKeepsCapabilityFilters() {
  val root = source("ui/original/player/BottomSheetPlayer.kt")
  val selector = source("ui/original/player/studio/PlayerSelector.kt")
  assertTrue(selector.contains("if (composed) {"))
  assertTrue(root.contains(".filter(C2Capabilities::actionAvailable)"))
  assertTrue(root.contains("sideEnabled = C2Capabilities.actionAvailable(menuLayout.side)"))
 }
 @Test fun oldVideoReturnIdsCannotRestoreRemovedThemes() {
  PlayerDesignStyle.entries.forEach { assertNull(resolveOriginalVideoReturn("galax:" + it.name)) }
  OriginalStyleCatalog.choices.filter { it.mochy != com.arturo254.opentune.ui.original.fullconstants.PlayerStyle.VIDEO }.forEach {
   assertEquals(it, resolveOriginalVideoReturn(it.id))
  }
 }
 @Test fun legacyVideoReturnAliasesRemainReadable() {
  assertEquals(com.arturo254.opentune.ui.original.fullconstants.PlayerStyle.TIMELINE, resolveOriginalVideoReturn("SIMPLE")?.mochy)
  assertEquals(com.arturo254.opentune.ui.original.fullconstants.PlayerStyle.FLAGSHIP, resolveOriginalVideoReturn("DEFAULT")?.mochy)
  assertNull(resolveOriginalVideoReturn("MODULE"))
  assertNull(resolveOriginalVideoReturn("mochy:VIDEO"))
 }

 @Test fun nativeMenusKeepTheirDefaultButAcceptTheOriginalFilteredHost() {
  val legacy = source("ui/player/PlayerComponents.kt")
  val apple = source("ui/player/v8/V8PlayerComponents.kt")
  listOf(legacy, apple).forEach {
   assertTrue(it.contains("onMenuOverride: (() -> Unit)? = null"))
   assertTrue(it.contains("onMenuOverride?.invoke() ?: menuState.showPlayerPanelMenu("))
  }
 }

 @Test fun unavailableMessagesHaveNoBrandOrBrokenVersionRange() {
  val messages = C1Capabilities.unavailable.values + C2Capabilities.unavailable.values
  val brand = Regex("Mochy|GalaxOT", RegexOption.IGNORE_CASE)
  messages.forEach { assertFalse(it, brand.containsMatchIn(it)); assertFalse(it, it.contains("V1?V17")) }
 }
 @Test fun ipodRootHasAnUnbrandedVisibleTitle() {
  assertEquals("Music", com.arturo254.opentune.ui.original.player.ipod.IpodPage.Main.title)
 }

}
