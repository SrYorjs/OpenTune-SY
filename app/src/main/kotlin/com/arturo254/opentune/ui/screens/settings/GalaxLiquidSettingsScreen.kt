package com.arturo254.opentune.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.GalaxGlassModeKey
import com.arturo254.opentune.constants.GalaxLiquidVariantKey
import com.arturo254.opentune.constants.GalaxLiquidCornerRadiusKey
import com.arturo254.opentune.constants.GalaxLiquidBlurKey
import com.arturo254.opentune.constants.GalaxLiquidRefractionHeightKey
import com.arturo254.opentune.constants.GalaxLiquidRefractionAmountKey
import com.arturo254.opentune.constants.GalaxLiquidChromaticKey
import com.arturo254.opentune.constants.GalaxLiquidTintKey
import com.arturo254.opentune.constants.LiquidGlassNavBarKey
import com.arturo254.opentune.core.designsystem.glass.GlassMode
import com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidOpticsControls
import com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidPreview
import com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidTuning
import com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidVariant
import com.arturo254.opentune.core.designsystem.liquid.galvanicRecipe
import com.arturo254.opentune.core.designsystem.liquid.parseGalaxLiquidVariant
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.utils.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class LiquidStudioState(
    val variant: GalaxLiquidVariant,
    val tuning: GalaxLiquidTuning,
)

/**
 * One canonical editor state:
 * selector -> preview + samples + sliders all update in the same composition.
 * A single atomic DataStore edit publishes changes to the application's
 * global Glass provider after a short debounce for continuous drag gestures.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalaxLiquidSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember {
        mutableStateOf(LiquidStudioState(GalaxLiquidVariant.DISABLED, galvanicRecipe(GalaxLiquidVariant.DISABLED)))
    }
    var loaded by remember { mutableStateOf(false) }

    suspend fun persist(snapshot: LiquidStudioState) {
        val tuning = snapshot.tuning.bounded()
        context.dataStore.edit { settings ->
            settings[GalaxLiquidVariantKey] = snapshot.variant.name
            settings[GalaxGlassModeKey] = if (snapshot.variant == GalaxLiquidVariant.DISABLED) GlassMode.DISABLED.name else GlassMode.CLASSIC.name
            settings[LiquidGlassNavBarKey] = snapshot.variant != GalaxLiquidVariant.DISABLED
            settings[GalaxLiquidCornerRadiusKey] = tuning.cornerRadiusDp
            settings[GalaxLiquidBlurKey] = tuning.blurRadiusDp
            settings[GalaxLiquidRefractionHeightKey] = tuning.refractionHeight
            settings[GalaxLiquidRefractionAmountKey] = tuning.refractionAmount
            settings[GalaxLiquidChromaticKey] = tuning.chromaticAberration
            settings[GalaxLiquidTintKey] = tuning.tintAlpha
        }
    }

    LaunchedEffect(context) {
        val settings = context.dataStore.data.first()
        val previouslySaved = parseGalaxLiquidVariant(settings[GalaxLiquidVariantKey])
        val selected = previouslySaved ?: when (settings[GalaxGlassModeKey]) {
            GlassMode.FROSTED.name -> GalaxLiquidVariant.PEARL
            GlassMode.CLEAR.name -> GalaxLiquidVariant.CRYSTAL
            GlassMode.BLUR.name -> GalaxLiquidVariant.GLACIER
            GlassMode.DISABLED.name -> GalaxLiquidVariant.DISABLED
            else -> if (settings[LiquidGlassNavBarKey] == true) GalaxLiquidVariant.SILK else GalaxLiquidVariant.DISABLED
        }
        val defaults = galvanicRecipe(selected)
        val tuning = if (previouslySaved != null) GalaxLiquidTuning(
            cornerRadiusDp = settings[GalaxLiquidCornerRadiusKey] ?: defaults.cornerRadiusDp,
            blurRadiusDp = settings[GalaxLiquidBlurKey] ?: defaults.blurRadiusDp,
            refractionHeight = settings[GalaxLiquidRefractionHeightKey] ?: defaults.refractionHeight,
            refractionAmount = settings[GalaxLiquidRefractionAmountKey] ?: defaults.refractionAmount,
            chromaticAberration = settings[GalaxLiquidChromaticKey] ?: defaults.chromaticAberration,
            tintAlpha = settings[GalaxLiquidTintKey] ?: defaults.tintAlpha,
        ).bounded() else defaults
        state = LiquidStudioState(selected, tuning)
        loaded = true
    }

    LaunchedEffect(loaded, state) {
        if (loaded) {
            // Dragging the six sliders remains local and immediate; only I/O is
            // debounced. Cancellation drops obsolete writes when values change.
            delay(120)
            persist(state)
        }
    }

    val exit: () -> Unit = {
        // Flush pending slider changes before leaving the screen.
        scope.launch {
            if (loaded) persist(state)
            navController.navigateUp()
        }
    }
    BackHandler(onBack = exit)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Galax Liquid Glass") },
                navigationIcon = {
                    IconButton(onClick = exit) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        }
    ) { padding ->
        if (!loaded) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("Preparando Glass Studio...", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Column(
                Modifier.fillMaxSize()
                    .padding(padding)
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                    .padding(horizontal = 15.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GalaxLiquidPreview(
                    variant = state.variant,
                    tuning = state.tuning,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("14 estilos + Desactivado", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Selecciona un estilo: el vidrio cambia al instante.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(onClick = {
                            state = state.copy(tuning = galvanicRecipe(state.variant))
                        }) { Text("Restablecer") }
                    }
                    GalaxLiquidVariant.entries.chunked(2).forEach { options ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEach { candidate ->
                                val selected = state.variant == candidate
                                Card(
                                    modifier = Modifier.weight(1f).clickable {
                                        state = LiquidStudioState(candidate, galvanicRecipe(candidate))
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainer,
                                    ),
                                    border = BorderStroke(
                                        if (selected) 1.5.dp else .5.dp,
                                        if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant,
                                    ),
                                ) {
                                    Text(
                                        candidate.title,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 15.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            if (options.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    GalaxLiquidOpticsControls(
                        tuning = state.tuning,
                        onChange = { state = state.copy(tuning = it.bounded()) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Los cambios se guardan automáticamente. La vista previa utiliza el mismo perfil que la barra de navegación y el MiniPlayer.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
            }
        }
    }
}
