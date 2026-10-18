package com.arturo254.opentune.core.designsystem.glass

import com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidVariant
import com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidTuning
import com.arturo254.opentune.core.designsystem.liquid.galvanicRecipe
import com.arturo254.opentune.core.designsystem.liquid.galvanicFinish
import com.arturo254.opentune.core.designsystem.liquid.parseGalaxLiquidVariant
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxGlassSelectionTest {
    private val root = listOf(File("src/main"), File("app/src/main"))
        .firstOrNull(File::isDirectory) ?: error("Android main source root unavailable")
    private fun source(path: String) = File(root, "kotlin/com/arturo254/opentune/$path").readText()

    @Test fun fourteenOpticalVariantsPlusOffRoundTrip() {
        assertEquals(15, GalaxLiquidVariant.entries.size)
        for (variant in GalaxLiquidVariant.entries) {
            assertEquals(variant, parseGalaxLiquidVariant(variant.name))
            assertEquals(variant, parseGalaxLiquidVariant(variant.name.lowercase()))
        }
        assertNull(parseGalaxLiquidVariant("invalid"))
        assertNull(parseGalaxLiquidVariant(null))
    }

    @Test fun tenOriginalAndFourKyantInspiredProfilesHaveBoundedParameters() {
        val recipes = GalaxLiquidVariant.entries
            .filter { it != GalaxLiquidVariant.DISABLED }
            .map(::galvanicRecipe)
        assertEquals(14, recipes.size)
        assertEquals(14, recipes.distinct().size)
        for (recipe in recipes) assertEquals(recipe, recipe.bounded())
        assertEquals(0f, galvanicRecipe(GalaxLiquidVariant.DISABLED).blurRadiusDp, 0f)
    }

    @Test fun dedicatedScreenAutoPersistsVariantAndSixOpticalSliders() {
        val appearance = source("ui/screens/settings/AppearanceSettings.kt")
        val studio = source("ui/screens/settings/GalaxLiquidSettingsScreen.kt")
        val preview = source("core/designsystem/liquid/GalaxLiquidPlayground.kt")
        val controls = source("core/designsystem/liquid/GalaxLiquidControls.kt")
        val navigation = source("ui/screens/NavigationBuilder.kt")
        val keys = source("constants/PreferenceKeys.kt")
        assertTrue(appearance.contains("settings/appearance/galax_liquid"))
        assertTrue(navigation.contains("GalaxLiquidSettingsScreen(navController)"))
        assertTrue(studio.contains("LaunchedEffect(loaded, state)"))
        assertTrue(studio.contains("delay(120)"))
        assertTrue(studio.contains("settings[GalaxLiquidVariantKey] = snapshot.variant.name"))
        assertTrue(studio.contains("settings[GalaxLiquidRefractionAmountKey] = tuning.refractionAmount"))
        assertTrue(studio.contains("settings[GalaxLiquidChromaticKey] = tuning.chromaticAberration"))
        assertTrue(studio.contains("onChange = { state = state.copy(tuning = it.bounded()) }"))
        assertTrue(preview.contains("variant = variant"))
        assertTrue(preview.contains("tuning = tuning"))
        assertTrue(preview.contains("onChange(tuning.copy("))
        assertTrue(controls.contains("tuning = tuning"))
        assertTrue(keys.contains("galax_liquid_variant_v2"))
    }

    @Test fun tenOriginalMaterialsHaveIndependentOpticalAndFinishSignatures() {
        val original = GalaxLiquidVariant.entries.take(10)
        assertEquals(10, original.map(::galvanicRecipe).distinct().size)
        assertEquals(10, original.map(::galvanicFinish).distinct().size)
        val crystal = galvanicRecipe(GalaxLiquidVariant.CRYSTAL)
        val glacier = galvanicRecipe(GalaxLiquidVariant.GLACIER)
        val prism = galvanicRecipe(GalaxLiquidVariant.PRISM)
        assertTrue(glacier.blurRadiusDp > crystal.blurRadiusDp * 10f)
        assertTrue(prism.refractionAmount > glacier.refractionAmount * 5f)
        val renderer = source("core/designsystem/liquid/GalaxLiquidRenderer.kt")
        assertTrue(renderer.contains("galvanicFinish(variant)"))
        assertTrue(renderer.contains("shadow = {"))
        assertTrue(renderer.contains("finish.depthEffect"))
    }

    @Test fun userPhotoAndInteractionAreConnectedToTheRealBackdrop() {
        val studio = source("ui/screens/settings/GalaxLiquidSettingsScreen.kt")
        val preview = source("core/designsystem/liquid/GalaxLiquidPlayground.kt")
        val controls = source("core/designsystem/liquid/GalaxLiquidControls.kt")
        val keys = source("constants/PreferenceKeys.kt")
        assertTrue(keys.contains("galax_liquid_preview_background_uri"))
        assertTrue(studio.contains("ActivityResultContracts.OpenDocument()"))
        assertTrue(studio.contains("takePersistableUriPermission("))
        assertTrue(studio.contains("settings[GalaxLiquidPreviewBackgroundUriKey] = uri.toString()"))
        assertTrue(studio.contains("selectedBackgroundUri = settings[GalaxLiquidPreviewBackgroundUriKey]"))
        assertTrue(preview.contains("coil3.compose.AsyncImage("))
        assertTrue(preview.contains(".layerBackdrop(backdrop)"))
        assertTrue(preview.contains("detectDragGestures"))
        assertTrue(controls.contains("collectIsPressedAsState()"))
        assertTrue(controls.contains("liquidToggleThumb"))
        assertTrue(controls.contains("liquidSliderThumb"))
        assertTrue(controls.contains("liquidTabsIndicator"))
    }

    @Test fun rendererKeepsValuesInSafeRange() {
        val values = GalaxLiquidTuning(-20f, 200f, 40f, -20f, 5f, 4f).bounded()
        assertEquals(0f, values.cornerRadiusDp, 0f)
        assertEquals(32f, values.blurRadiusDp, 0f)
        assertEquals(1f, values.refractionHeight, 0f)
        assertEquals(0f, values.refractionAmount, 0f)
        assertEquals(1f, values.chromaticAberration, 0f)
        assertEquals(.65f, values.tintAlpha, 0f)
    }
}
