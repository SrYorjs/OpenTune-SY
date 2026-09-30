package com.arturo254.opentune.playback

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.media3.exoplayer.ExoPlayer
import com.arturo254.opentune.constants.EqualizerBandLevelsMbKey
import com.arturo254.opentune.constants.EqualizerBassBoostEnabledKey
import com.arturo254.opentune.constants.EqualizerBassBoostStrengthKey
import com.arturo254.opentune.constants.EqualizerEnabledKey
import com.arturo254.opentune.constants.EqualizerOutputGainEnabledKey
import com.arturo254.opentune.constants.EqualizerOutputGainMbKey
import com.arturo254.opentune.constants.EqualizerSelectedProfileIdKey
import com.arturo254.opentune.constants.EqualizerVirtualizerEnabledKey
import com.arturo254.opentune.constants.EqualizerVirtualizerStrengthKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class AudioEffectsController(
    private val context: Context,
    private val playerProvider: () -> ExoPlayer,
    private val dataStore: DataStore<Preferences>,
    private val scopeProvider: () -> CoroutineScope,
    private val ioScopeProvider: () -> CoroutineScope,
) {
    val eqCapabilities = MutableStateFlow<EqCapabilities?>(null)
    val internalDspActive = MutableStateFlow(false)

    private val desiredEqSettings =
        MutableStateFlow(
            EqSettings(
                enabled = false,
                bandLevelsMb = emptyList(),
                outputGainEnabled = false,
                outputGainMb = 0,
                bassBoostEnabled = false,
                bassBoostStrength = 0,
                virtualizerEnabled = false,
                virtualizerStrength = 0,
            ),
        )

    private var isSessionOpened = false
    private var openedSessionId: Int? = null
    private var effectsSessionId: Int? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    fun start() {
        scopeProvider().launch {
            dataStore.data
                .map(::readSettingsFromPrefs)
                .distinctUntilChanged()
                .collectLatest { settings ->
                    desiredEqSettings.value = settings
                    internalDspActive.value =
                        settings.enabled ||
                            settings.outputGainEnabled ||
                            settings.bassBoostEnabled ||
                            settings.virtualizerEnabled

                    val sessionId = playerProvider().audioSessionId
                    if (sessionId > 0) {
                        ensureEffects(sessionId)
                    }
                    applySettingsToEffects(settings)
                }
        }
    }

    fun previewEnabled(enabled: Boolean) {
        previewSettings(
            desiredEqSettings.value.copy(
                enabled = enabled,
            ),
        )
    }

    fun previewBandLevels(levelsMb: List<Int>) {
        previewSettings(
            desiredEqSettings.value.copy(
                bandLevelsMb = levelsMb,
            ),
        )
    }

    fun previewOutputGain(
        enabled: Boolean,
        gainMb: Int,
    ) {
        previewSettings(
            desiredEqSettings.value.copy(
                outputGainEnabled = enabled,
                outputGainMb =
                    gainMb.coerceIn(-1500, 1500),
            ),
        )
    }

    fun previewBassBoost(
        enabled: Boolean,
        strength: Int,
    ) {
        previewSettings(
            desiredEqSettings.value.copy(
                bassBoostEnabled = enabled,
                bassBoostStrength =
                    strength.coerceIn(0, 1000),
            ),
        )
    }

    fun previewVirtualizer(
        enabled: Boolean,
        strength: Int,
    ) {
        previewSettings(
            desiredEqSettings.value.copy(
                virtualizerEnabled = enabled,
                virtualizerStrength =
                    strength.coerceIn(0, 1000),
            ),
        )
    }

    private fun previewSettings(settings: EqSettings) {
        desiredEqSettings.value = settings
        internalDspActive.value =
            settings.enabled ||
                settings.outputGainEnabled ||
                settings.bassBoostEnabled ||
                settings.virtualizerEnabled

        val sessionId =
            playerProvider().audioSessionId
        if (sessionId > 0) {
            ensureEffects(sessionId)
        }
        applySettingsToEffects(settings)
    }

    fun applyFlatPreset() {
        ioScopeProvider().launch {
            val caps = eqCapabilities.value
            val bandCount =
                caps?.bandCount
                    ?: runCatching { equalizer?.numberOfBands?.toInt() }.getOrNull()
                    ?: 0
            val encoded =
                encodeBandLevelsMb(List(bandCount.coerceAtLeast(0)) { 0 })

            dataStore.edit { prefs ->
                prefs[EqualizerEnabledKey] = true
                prefs[EqualizerBandLevelsMbKey] = encoded
                prefs[EqualizerSelectedProfileIdKey] = "flat"
            }
        }
    }

    fun applySystemPreset(presetIndex: Int) {
        scopeProvider().launch {
            ensureEffects(playerProvider().audioSessionId)
            val eq = equalizer ?: return@launch
            val maxPreset =
                runCatching { eq.numberOfPresets.toInt() }.getOrNull() ?: 0
            if (presetIndex !in 0 until maxPreset) return@launch

            runCatching { eq.usePreset(presetIndex.toShort()) }.getOrNull()
                ?: return@launch

            val bandCount =
                runCatching { eq.numberOfBands.toInt() }.getOrNull() ?: 0
            val levels =
                (0 until bandCount).map { band ->
                    runCatching {
                        eq.getBandLevel(band.toShort()).toInt()
                    }.getOrNull() ?: 0
                }

            val encoded = encodeBandLevelsMb(levels)
            if (encoded.isBlank()) return@launch

            ioScopeProvider().launch {
                dataStore.edit { prefs ->
                    prefs[EqualizerEnabledKey] = true
                    prefs[EqualizerBandLevelsMbKey] = encoded
                    prefs[EqualizerSelectedProfileIdKey] = "system:$presetIndex"
                }
            }
        }
    }

    fun openSession() {
        if (isSessionOpened) return
        val sessionId = playerProvider().audioSessionId
        if (sessionId <= 0) return

        isSessionOpened = true
        openedSessionId = sessionId
        ensureEffects(sessionId)
        sendOpenBroadcast(sessionId)
    }

    fun closeSession() {
        if (!isSessionOpened) return
        isSessionOpened = false

        val sessionId = openedSessionId ?: playerProvider().audioSessionId
        openedSessionId = null
        release()
        if (sessionId > 0) sendCloseBroadcast(sessionId)
    }

    fun onAudioSessionIdChanged(newSessionId: Int) {
        val oldSessionId = openedSessionId
        if (
            !isSessionOpened ||
            newSessionId <= 0 ||
            oldSessionId == null ||
            oldSessionId <= 0 ||
            oldSessionId == newSessionId
        ) {
            return
        }

        sendCloseBroadcast(oldSessionId)
        openedSessionId = newSessionId
        ensureEffects(newSessionId)
        sendOpenBroadcast(newSessionId)
    }

    fun release() {
        effectsSessionId = null
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        eqCapabilities.value = null
    }

    private fun ensureEffects(sessionId: Int) {
        if (sessionId <= 0) return
        if (effectsSessionId == sessionId && equalizer != null) return

        release()
        effectsSessionId = sessionId
        equalizer = runCatching { Equalizer(1_000, sessionId) }.getOrNull()
        bassBoost = runCatching { BassBoost(1_000, sessionId) }.getOrNull()
        virtualizer = runCatching { Virtualizer(1_000, sessionId) }.getOrNull()
        loudnessEnhancer =
            runCatching { LoudnessEnhancer(sessionId) }.getOrNull()

        equalizer?.let(::updateCapabilities)
        applySettingsToEffects(desiredEqSettings.value)
    }

    private fun applySettingsToEffects(settings: EqSettings) {
        val eq = equalizer ?: return
        val caps = eqCapabilities.value
        val bandCount = caps?.bandCount ?: eq.numberOfBands.toInt()
        val minMb =
            caps?.minBandLevelMb
                ?: runCatching { eq.bandLevelRange.getOrNull(0)?.toInt() }.getOrNull()
                ?: -1500
        val maxMb =
            caps?.maxBandLevelMb
                ?: runCatching { eq.bandLevelRange.getOrNull(1)?.toInt() }.getOrNull()
                ?: 1500

        val levels =
            AudioEffectsPolicy.resampleLevelsByIndex(
                settings.bandLevelsMb,
                bandCount,
            )

        runCatching { eq.enabled = settings.enabled }

        for (band in 0 until bandCount) {
            val levelMb =
                levels.getOrNull(band)?.coerceIn(minMb, maxMb) ?: 0
            runCatching {
                eq.setBandLevel(band.toShort(), levelMb.toShort())
            }
        }

        bassBoost?.let { effect ->
            runCatching { effect.enabled = settings.bassBoostEnabled }
            runCatching {
                effect.setStrength(settings.bassBoostStrength.toShort())
            }
        }

        virtualizer?.let { effect ->
            runCatching { effect.enabled = settings.virtualizerEnabled }
            runCatching {
                effect.setStrength(settings.virtualizerStrength.toShort())
            }
        }

        loudnessEnhancer?.let { effect ->
            val gainMb =
                if (settings.outputGainEnabled) {
                    settings.outputGainMb.coerceIn(-1500, 1500)
                } else {
                    0
                }
            runCatching { effect.setTargetGain(gainMb) }
            runCatching { effect.enabled = settings.outputGainEnabled }
        }
    }

    private fun updateCapabilities(eq: Equalizer) {
        val bandCount = eq.numberOfBands.toInt().coerceAtLeast(0)
        val range = runCatching { eq.bandLevelRange }.getOrNull()
        val minMb = range?.getOrNull(0)?.toInt() ?: -1500
        val maxMb = range?.getOrNull(1)?.toInt() ?: 1500
        val centers =
            (0 until bandCount).map { band ->
                (runCatching {
                    eq.getCenterFreq(band.toShort())
                }.getOrNull() ?: 0) / 1000
            }
        val presets =
            (0 until eq.numberOfPresets.toInt()).map { index ->
                runCatching {
                    eq.getPresetName(index.toShort()).toString()
                }.getOrNull() ?: ("Preset " + (index + 1))
            }

        eqCapabilities.value =
            EqCapabilities(
                bandCount = bandCount,
                minBandLevelMb = minMb,
                maxBandLevelMb = maxMb,
                centerFreqHz = centers,
                systemPresets = presets,
            )
    }

    private fun readSettingsFromPrefs(prefs: Preferences): EqSettings =
        EqSettings(
            enabled = prefs[EqualizerEnabledKey] ?: false,
            bandLevelsMb =
                decodeBandLevelsMb(prefs[EqualizerBandLevelsMbKey]),
            outputGainEnabled =
                prefs[EqualizerOutputGainEnabledKey] ?: false,
            outputGainMb = prefs[EqualizerOutputGainMbKey] ?: 0,
            bassBoostEnabled =
                prefs[EqualizerBassBoostEnabledKey] ?: false,
            bassBoostStrength =
                (prefs[EqualizerBassBoostStrengthKey] ?: 0)
                    .coerceIn(0, 1000),
            virtualizerEnabled =
                prefs[EqualizerVirtualizerEnabledKey] ?: false,
            virtualizerStrength =
                (prefs[EqualizerVirtualizerStrengthKey] ?: 0)
                    .coerceIn(0, 1000),
        )

    private fun decodeBandLevelsMb(raw: String?): List<Int> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            EqualizerJson.json.decodeFromString<List<Int>>(raw)
        }.getOrNull() ?: emptyList()
    }

    private fun encodeBandLevelsMb(levelsMb: List<Int>): String =
        runCatching {
            EqualizerJson.json.encodeToString(levelsMb)
        }.getOrNull().orEmpty()

    private fun sendOpenBroadcast(sessionId: Int) {
        context.sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(
                    AudioEffect.EXTRA_CONTENT_TYPE,
                    AudioEffect.CONTENT_TYPE_MUSIC,
                )
            },
        )
    }

    private fun sendCloseBroadcast(sessionId: Int) {
        context.sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            },
        )
    }
}
