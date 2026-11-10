package com.arturo254.opentune.ui.visual.player

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class TrackInfo(
    val mediaId: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val thumbnailUrl: String? = null,
    val liked: Boolean = false,
)

@Immutable
data class QueueItemData(
    val uid: Int,
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
    val duration: Long = 0L,
    val isAutoplay: Boolean = false,
)

@Immutable
data class QueueState(
    val items: List<QueueItemData> = emptyList(),
    val currentIndex: Int = -1,
    val title: String = "",
)
