package com.arturo254.opentune.ui.original.adapters

import androidx.compose.runtime.staticCompositionLocalOf
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.innertube.YouTube
import okhttp3.OkHttpClient

val LocalOriginalPlayerConnection = staticCompositionLocalOf<OriginalPlayerConnection?> { null }

fun OriginalPlayerConnection.togglePlayPause() = player.togglePlayPause()

fun OkHttpClient.Builder.followAppProxy(): OkHttpClient.Builder = proxy(YouTube.proxy)
