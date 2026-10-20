package com.arturo254.opentune.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import androidx.navigation.NavController
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.SliderStyle
import com.arturo254.opentune.constants.SliderStyleKey
import com.arturo254.opentune.constants.UseLyricsV2Key
import com.arturo254.opentune.core.designsystem.component.Artwork
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidBackdrop
import com.arturo254.opentune.core.designsystem.theme.GalaxDestinationTheme
import com.arturo254.opentune.core.designsystem.theme.GalaxScreenStyle
import com.arturo254.opentune.core.designsystem.theme.galaxScreenBackground
import com.arturo254.opentune.db.entities.LyricsEntity
import com.arturo254.opentune.di.LyricsHelperEntryPoint
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.extensions.toggleRepeatMode
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.ui.component.BigSeekBar
import com.arturo254.opentune.ui.component.LiquidGlassIconButton
import com.arturo254.opentune.ui.component.Lyrics
import com.arturo254.opentune.ui.component.LyricsV2
import com.arturo254.opentune.ui.menu.LyricsMenu
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun LyricsScreen(
    mediaMetadata: MediaMetadata,
    navController: NavController,
    onBackClick: () -> Unit,
    lyricsSyncOffset: Int,
    modifier: Modifier = Modifier,
) {
    val connection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val currentLyrics by connection.currentLyrics.collectAsState(initial = null)
    val useLyricsV2 by rememberPreference(UseLyricsV2Key, false)
    val sliderPosition = remember(mediaMetadata.id) { mutableStateOf<Long?>(null) }
    var showLyricsMenu by remember { mutableStateOf(false) }
    val backdrop = rememberLayerBackdrop()

    LaunchedEffect(mediaMetadata.id, currentLyrics) {
        if (currentLyrics == null) {
            delay(500)
            withContext(Dispatchers.IO) {
                try {
                    val lyricsHelper = EntryPointAccessors.fromApplication(
                        context.applicationContext, LyricsHelperEntryPoint::class.java,
                    ).lyricsHelper()
                    val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                    database.query { upsert(LyricsEntity(mediaMetadata.id, lyrics)) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                }
            }
        }
    }
    BackHandler(onBack = onBackClick)
    GalaxDestinationTheme(immersive = true) {
        Box(modifier.fillMaxSize()) {
            Box(Modifier.matchParentSize().layerBackdrop(backdrop).galaxScreenBackground())
            CompositionLocalProvider(LocalGalaxLiquidBackdrop provides backdrop) {
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = onBackClick) {
                            Icon(painterResource(R.drawable.expand_more), stringResource(R.string.close))
                        }
                        Artwork(mediaMetadata.thumbnailUrl, Modifier.size(48.dp), shape = GalaxScreenStyle.artworkShape)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(mediaMetadata.title, style = MaterialTheme.typography.titleMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(mediaMetadata.artists.joinToString { it.name }, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { showLyricsMenu = true }) {
                            Icon(painterResource(R.drawable.more_horiz), stringResource(R.string.more_options))
                        }
                    }
                    com.arturo254.opentune.ui.player.panels.GalaxPanelTimelineHeader(
                        title = stringResource(R.string.lyrics),
                    )
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                        val lyricContent: @Composable () -> Unit = {
                            if (useLyricsV2) LyricsV2(sliderPositionProvider = { sliderPosition.value })
                            else Lyrics(lyricsSyncOffset = lyricsSyncOffset,
                                sliderPositionProvider = { sliderPosition.value }, modifier = Modifier.fillMaxSize())
                        }
                        if (maxWidth >= 640.dp) {
                            Row(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(1f).fillMaxHeight()) { lyricContent() }
                                Box(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                                    contentAlignment = Alignment.Center) {
                                    GalaxLyricsControls(connection, mediaMetadata.id, sliderPosition)
                                }
                            }
                        } else {
                            Column(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(1f).fillMaxWidth()) { lyricContent() }
                                GalaxLyricsControls(connection, mediaMetadata.id, sliderPosition)
                            }
                        }
                    }
                }
            }
        }
        if (showLyricsMenu) Dialog(onDismissRequest = { showLyricsMenu = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            LyricsMenu(lyricsProvider = { currentLyrics }, mediaMetadataProvider = { mediaMetadata },
                onDismiss = { showLyricsMenu = false })
        }
    }
}

@Composable
private fun GalaxLyricsControls(connection: PlayerConnection, mediaId: String, pending: MutableState<Long?>) {
    val playing by connection.isPlaying.collectAsState()
    val repeatMode by connection.repeatMode.collectAsState()
    val shuffle by connection.shuffleModeEnabled.collectAsState()
    val canSkipPrevious by connection.canSkipPrevious.collectAsState()
    val canSkipNext by connection.canSkipNext.collectAsState()
    val buffering by connection.playbackState.collectAsState()
    val volume = connection.service.playerVolume.collectAsState()
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GalaxLyricsProgress(connection, mediaId, pending)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = { connection.player.toggleRepeatMode() }) {
                Icon(painterResource(if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat),
                    stringResource(R.string.player_a11y_repeat),
                    tint = if (repeatMode == Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
            }
            LiquidGlassIconButton(onClick = { connection.player.seekToPrevious() }, enabled = canSkipPrevious,
                accessibilityLabel = stringResource(R.string.player_a11y_previous)) {
                Icon(painterResource(R.drawable.skip_previous), null)
            }
            LiquidGlassIconButton(onClick = { connection.player.togglePlayPause() }, size = 64.dp,
                accessibilityLabel = stringResource(if (playing) R.string.player_a11y_pause else R.string.play)) {
                if (buffering == Player.STATE_BUFFERING) CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp)
                else Icon(painterResource(if (playing) R.drawable.pause else R.drawable.play), null, Modifier.size(32.dp))
            }
            LiquidGlassIconButton(onClick = { connection.player.seekToNext() }, enabled = canSkipNext,
                accessibilityLabel = stringResource(R.string.next_song)) {
                Icon(painterResource(R.drawable.skip_next), null)
            }
            IconButton(onClick = { connection.player.shuffleModeEnabled = !shuffle }) {
                Icon(painterResource(R.drawable.shuffle), stringResource(R.string.shuffle),
                    tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(R.drawable.volume_off), stringResource(R.string.minimum_volume), Modifier.size(18.dp))
            BigSeekBar(progressProvider = volume::value,
                onProgressChange = { connection.service.playerVolume.value = it },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).height(28.dp))
            Icon(painterResource(R.drawable.volume_up), stringResource(R.string.maximum_volume), Modifier.size(18.dp))
        }
    }
}

@Composable
private fun GalaxLyricsProgress(connection: PlayerConnection, mediaId: String, pending: MutableState<Long?>) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val playing by connection.isPlaying.collectAsState()
    val playbackState by connection.playbackState.collectAsState()
    var position by remember(mediaId) { mutableLongStateOf(connection.player.currentPosition.coerceAtLeast(0L)) }
    var duration by remember(mediaId) { mutableLongStateOf(0L) }
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.Standard)
    LaunchedEffect(mediaId, playing, playbackState, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            do {
                position = connection.player.currentPosition.coerceAtLeast(0L)
                duration = connection.player.duration.coerceAtLeast(0L)
                delay(200)
            } while (isActive && playing)
        }
    }
    val displayed = (pending.value ?: position).coerceIn(0L, duration.coerceAtLeast(1L))
    Column {
        StyledPlaybackSlider(sliderStyle = sliderStyle, value = displayed.toFloat(),
            valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
            onValueChange = { if (duration > 0) pending.value = it.toLong() },
            onValueChangeFinished = {
                pending.value?.let { connection.player.seekTo(it); position = it }
                pending.value = null
            }, activeColor = MaterialTheme.colorScheme.primary, isPlaying = playing,
            modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(makeTimeString(displayed), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(makeTimeString(duration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
