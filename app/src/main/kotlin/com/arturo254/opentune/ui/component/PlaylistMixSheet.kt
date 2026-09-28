package com.arturo254.opentune.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.PlaylistSong
import com.arturo254.opentune.playback.MixTrackAnalysis
import com.arturo254.opentune.playback.MixTransition
import com.arturo254.opentune.playback.MixTransitionCurve
import com.arturo254.opentune.playback.PlaylistMixEngine
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.playback.queues.ListQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

enum class PlaylistMixMode {
    SMART,
    MANUAL,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistMixSheet(
    playlistName: String,
    songs: List<PlaylistSong>,
    playerConnection: PlayerConnection,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(PlaylistMixMode.SMART) }
    var analysisResults by remember { mutableStateOf<List<MixTrackAnalysis>>(emptyList()) }
    var manualTransitions by remember(songs) {
        mutableStateOf(
            songs.zipWithNext { current, next ->
                MixTransition(
                    outgoingSongId = current.song.id,
                    incomingSongId = next.song.id,
                    durationMs = 7_000,
                    curve = MixTransitionCurve.BLEND,
                    bpmDelta = null,
                    gainDelta = null,
                )
            }
        )
    }
    var analyzing by remember { mutableStateOf(true) }
    var starting by remember { mutableStateOf(false) }

    LaunchedEffect(songs) {
        analyzing = true
        analysisResults =
            withContext(Dispatchers.IO) {
                PlaylistMixEngine.analyze(songs)
            }
        analyzing = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 42.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f))
            )
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.playlist_mix_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = playlistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            LiquidGlassCard(
                cornerRadius = 26.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(8.dp),
                ) {
                    MixModeButton(
                        text = stringResource(R.string.playlist_mix_smart),
                        selected = mode == PlaylistMixMode.SMART,
                        modifier = Modifier.weight(1f),
                        onClick = { mode = PlaylistMixMode.SMART },
                    )
                    MixModeButton(
                        text = stringResource(R.string.playlist_mix_manual),
                        selected = mode == PlaylistMixMode.MANUAL,
                        modifier = Modifier.weight(1f),
                        onClick = { mode = PlaylistMixMode.MANUAL },
                    )
                }
            }

            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "playlistMixMode",
            ) { selectedMode ->
                when (selectedMode) {
                    PlaylistMixMode.SMART ->
                        SmartMixPanel(
                            analysis = analysisResults,
                            analyzing = analyzing,
                        )

                    PlaylistMixMode.MANUAL ->
                        ManualMixPanel(
                            songs = songs,
                            transitions = manualTransitions,
                            onTransitionChange = { index, transition ->
                                manualTransitions =
                                    manualTransitions.toMutableList().apply {
                                        if (index in indices) this[index] = transition
                                    }
                            },
                        )
                }
            }

            LiquidGlassPillButton(
                enabled = songs.size > 1 && !starting,
                onClick = {
                    if (starting) return@LiquidGlassPillButton
                    starting = true
                    scope.launch {
                        val plan =
                            withContext(Dispatchers.Default) {
                                when (mode) {
                                    PlaylistMixMode.SMART ->
                                        PlaylistMixEngine.buildSmartPlan(songs)

                                    PlaylistMixMode.MANUAL -> {
                                        val analyzed =
                                            if (analysisResults.size == songs.size) {
                                                analysisResults
                                            } else {
                                                PlaylistMixEngine.analyze(songs)
                                            }
                                        PlaylistMixEngine.rebuildWithManualTransitions(
                                            analyzed = analyzed,
                                            transitions = manualTransitions,
                                        )
                                    }
                                }
                            }

                        if (plan.orderedTracks.isNotEmpty()) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title =
                                        if (mode == PlaylistMixMode.SMART) {
                                            "Smart Mix · " + playlistName
                                        } else {
                                            "Manual Mix · " + playlistName
                                        },
                                    items = plan.toMediaItems(),
                                )
                            )
                            onDismiss()
                        } else {
                            starting = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                if (starting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.mix),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text =
                            if (mode == PlaylistMixMode.SMART) {
                                stringResource(R.string.playlist_mix_start_smart)
                            } else {
                                stringResource(R.string.playlist_mix_start_manual)
                            },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun MixModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        targetValue =
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            } else {
                Color.Transparent
            },
        label = "mixModeContainer",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.985f,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 600f),
        label = "mixModeScale",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .border(
                0.75.dp,
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f)
                },
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color =
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SmartMixPanel(
    analysis: List<MixTrackAnalysis>,
    analyzing: Boolean,
) {
    val knownTempo = analysis.count { it.bpm != null }
    val avgBpm =
        analysis
            .mapNotNull { it.bpm }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.roundToInt()

    LiquidGlassCard(
        cornerRadius = 26.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)),
                ) {
                    if (analyzing) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.graphic_eq),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.playlist_mix_smart_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.playlist_mix_smart_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MixMetric(
                    label = "BPM",
                    value = avgBpm?.toString() ?: "—",
                    modifier = Modifier.weight(1f),
                )
                MixMetric(
                    label = stringResource(R.string.playlist_mix_analyzed),
                    value = knownTempo.toString() + "/" + analysis.size.toString(),
                    modifier = Modifier.weight(1f),
                )
            }

            if (analysis.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(188.dp),
                ) {
                    itemsIndexed(
                        items = analysis,
                        key = { _, item -> item.playlistSong.song.id },
                    ) { index, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.48f)
                                )
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(26.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.playlistSong.song.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = item.playlistSong.song.artists.joinToString { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = item.bpm?.roundToInt()?.let { it.toString() + " BPM" } ?: "—",
                                style = MaterialTheme.typography.labelMedium,
                                color =
                                    if (item.bpm != null) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualMixPanel(
    songs: List<PlaylistSong>,
    transitions: List<MixTransition>,
    onTransitionChange: (Int, MixTransition) -> Unit,
) {
    LiquidGlassCard(
        cornerRadius = 26.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.playlist_mix_manual_console),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.playlist_mix_manual_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.height(310.dp),
            ) {
                itemsIndexed(
                    items = transitions,
                    key = { index, item ->
                        item.outgoingSongId + ">" + item.incomingSongId + ":" + index
                    },
                ) { index, transition ->
                    val outgoing = songs.getOrNull(index)
                    val incoming = songs.getOrNull(index + 1)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.48f)
                            )
                            .border(
                                0.75.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                                RoundedCornerShape(20.dp),
                            )
                            .padding(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = outgoing?.song?.title.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "→ " + incoming?.song?.title.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Text(
                                text = (transition.durationMs / 1000f)
                                    .let { String.format("%.1fs", it) },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Slider(
                            value = transition.durationMs / 1000f,
                            onValueChange = { seconds ->
                                onTransitionChange(
                                    index,
                                    transition.copy(
                                        durationMs =
                                            (seconds * 1000f)
                                                .roundToInt()
                                                .coerceIn(1_500, 20_000)
                                    ),
                                )
                            },
                            valueRange = 2f..14f,
                            steps = 11,
                            colors = SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                thumbColor = MaterialTheme.colorScheme.primary,
                            ),
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MixTransitionCurve.entries.forEach { option ->
                                val selected = transition.curve == option
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (selected) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                            } else {
                                                Color.Transparent
                                            }
                                        )
                                        .clickable {
                                            onTransitionChange(
                                                index,
                                                transition.copy(curve = option),
                                            )
                                        }
                                        .padding(vertical = 8.dp),
                                ) {
                                    Text(
                                        text =
                                            when (option) {
                                                MixTransitionCurve.FADE -> "Fade"
                                                MixTransitionCurve.BLEND -> "Blend"
                                                MixTransitionCurve.RISE -> "Rise"
                                                MixTransitionCurve.MELT -> "Melt"
                                            },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight =
                                            if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color =
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MixMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.52f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
