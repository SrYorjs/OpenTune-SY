package com.arturo254.opentune.ui.visual.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalDownloadUtil
import com.arturo254.opentune.R
import kotlinx.coroutines.flow.map

fun isImportedFile(songId: String): Boolean =
    songId.startsWith("LOCAL_") ||
        songId.startsWith("content://") ||
        songId.startsWith("file://")

@Composable
fun rememberOnDevice(songId: String?): Boolean {
    if (songId == null) return false
    if (isImportedFile(songId)) return true
    val downloadUtil = LocalDownloadUtil.current
    val downloaded by remember(songId) {
        downloadUtil.downloads.map { it[songId]?.state == Download.STATE_COMPLETED }
    }.collectAsState(initial = downloadUtil.downloads.value[songId]?.state == Download.STATE_COMPLETED)
    return downloaded
}

@Composable
fun OnDeviceMarks(songId: String?, modifier: Modifier = Modifier) {
    if (songId == null || !rememberOnDevice(songId)) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        OnDeviceIcon()
    }
}

@Composable
fun OnDeviceIcon(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(17.dp)
            .background(Color.White.copy(alpha = 0.45f), CircleShape),
    ) {
        Icon(
            painter = painterResource(R.drawable.arrow_downward),
            contentDescription = "On this device",
            tint = Color.Black.copy(alpha = 0.8f),
            modifier = Modifier.size(12.dp),
        )
    }
}
