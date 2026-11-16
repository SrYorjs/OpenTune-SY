package com.arturo254.opentune.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import com.arturo254.opentune.extensions.setOffloadEnabled

internal class PlaybackEngine(
    private val context: Context,
    private val streamResolver: StreamResolver,
) {
    fun createPlayer(): ExoPlayer =
        ExoPlayer
            .Builder(context)
            .setMediaSourceFactory(createMediaSourceFactory())
            .setLoadControl(createFastStartLoadControl())
            .setRenderersFactory(createRenderersFactory())
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(defaultAudioAttributes(), false)
            .setSeekBackIncrementMs(5_000)
            .setSeekForwardIncrementMs(5_000)
            .setDeviceVolumeControlEnabled(true)
            .build()

    fun createOverlapPlayer(): ExoPlayer =
        ExoPlayer
            .Builder(context)
            .setMediaSourceFactory(createMediaSourceFactory())
            .setLoadControl(createFastStartLoadControl())
            .setRenderersFactory(createRenderersFactory())
            .setHandleAudioBecomingNoisy(false)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(defaultAudioAttributes(), false)
            .setSeekBackIncrementMs(5_000)
            .setSeekForwardIncrementMs(5_000)
            .build()

    fun updateAudioOffload(
        player: ExoPlayer,
        enabled: Boolean,
    ) {
        runCatching {
            val builder =
                player.trackSelectionParameters.buildUpon()
            val preferencesClass =
                Class.forName(
                    "androidx.media3.common.AudioOffloadPreferences",
                )
            val builderClass =
                Class.forName(
                    "androidx.media3.common.AudioOffloadPreferences\$Builder",
                )

            val modeFieldName =
                if (enabled) {
                    "AUDIO_OFFLOAD_MODE_ENABLED"
                } else {
                    "AUDIO_OFFLOAD_MODE_DISABLED"
                }
            val mode =
                preferencesClass
                    .getField(modeFieldName)
                    .getInt(null)

            val preferencesBuilder =
                builderClass
                    .getDeclaredConstructor()
                    .newInstance()

            builderClass
                .getMethod(
                    "setAudioOffloadMode",
                    Int::class.javaPrimitiveType,
                ).invoke(preferencesBuilder, mode)

            val preferences =
                builderClass
                    .getMethod("build")
                    .invoke(preferencesBuilder)

            builder.javaClass.methods
                .firstOrNull { method ->
                    method.name ==
                        "setAudioOffloadPreferences" &&
                        method.parameterTypes.size == 1
                }?.invoke(builder, preferences)

            player.trackSelectionParameters =
                builder.build()
        }

        player.setOffloadEnabled(enabled)
    }

    private fun createFastStartLoadControl(): DefaultLoadControl =
        DefaultLoadControl
            .Builder()
            .setBufferDurationsMs(
                8_000,
                45_000,
                750,
                1_500,
            ).setPrioritizeTimeOverSizeThresholds(true)
            .build()

    private fun createMediaSourceFactory() =
        DefaultMediaSourceFactory(
            streamResolver.createDataSourceFactory(),
            playbackExtractorsFactory(),
        ).setLoadErrorHandlingPolicy(PlaybackLoadErrorHandlingPolicy())

    private fun createRenderersFactory() =
        object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ) = DefaultAudioSink
                .Builder(this@PlaybackEngine.context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(
                    enableAudioTrackPlaybackParams,
                ).setAudioProcessorChain(
                    DefaultAudioSink.DefaultAudioProcessorChain(
                        SilenceSkippingAudioProcessor(
                            1_500_000L,
                            0.35f,
                            500_000L,
                            10,
                            150.toShort(),
                        ),
                        SonicAudioProcessor(),
                    ),
                ).build()
        }

    private fun defaultAudioAttributes(): AudioAttributes =
        AudioAttributes
            .Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
}

internal fun playbackExtractorsFactory(): ExtractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()

internal class PlaybackLoadErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy() {
    override fun getRetryDelayMsFor(info: LoadErrorHandlingPolicy.LoadErrorInfo): Long =
        if (generateSequence<Throwable>(info.exception) { it.cause }.take(32).any {
            it is PlaybackResolutionDataSourceException || it is PlaybackLoadCancelledException
        }) C.TIME_UNSET else super.getRetryDelayMsFor(info)
}
