package com.arturo254.opentune.ui.screens

import com.arturo254.opentune.core.designsystem.theme.galaxScreenBackground
import com.arturo254.opentune.ui.component.LiquidGlassIconButton
import com.arturo254.opentune.ui.screens.GalaxMediaHeader
import com.arturo254.opentune.ui.mochy.theme.MochyTheme
import com.arturo254.opentune.ui.mochy.detail.DetailActionRow
import com.arturo254.opentune.ui.mochy.detail.DetailCircleButton
import com.arturo254.opentune.ui.mochy.detail.DetailSecondaryAction
import com.arturo254.opentune.ui.mochy.detail.DetailTrackRow
import com.arturo254.opentune.ui.mochy.detail.PlaylistScreenHeader
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.arturo254.opentune.ui.screens.GalaxTopAppBar as TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalDownloadUtil
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AppBarHeight
import com.arturo254.opentune.constants.DisableBlurKey
import com.arturo254.opentune.constants.HideExplicitKey
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.playback.ExoDownloadService
import com.arturo254.opentune.playback.queues.LocalAlbumRadio
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.NavigationTitle
import com.arturo254.opentune.ui.component.SongListItem
import com.arturo254.opentune.ui.component.YouTubeGridItem
import com.arturo254.opentune.ui.component.shimmer.ButtonPlaceholder
import com.arturo254.opentune.ui.component.shimmer.ListItemPlaceHolder
import com.arturo254.opentune.ui.component.shimmer.ShimmerHost
import com.arturo254.opentune.ui.component.shimmer.TextPlaceholder
import com.arturo254.opentune.ui.menu.AlbumMenu
import com.arturo254.opentune.ui.menu.SelectionSongMenu
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.menu.YouTubeAlbumMenu
import com.arturo254.opentune.ui.theme.PlayerColorExtractor
import com.arturo254.opentune.ui.utils.ItemWrapper
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.viewmodels.AlbumUiState
import com.arturo254.opentune.viewmodels.AlbumViewModel
import com.valentinilk.shimmer.shimmer

suspend fun downloadAlbumCover(
    context: android.content.Context,
    imageUrl: String?,
    albumTitle: String,
    quality: Int = 1024,
): Boolean {
    if (imageUrl.isNullOrEmpty()) return false

    return try {
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(Size(quality, quality))
            .allowHardware(false)
            .build()

        val result = context.imageLoader.execute(request)
        val bitmap = result.image?.toBitmap() ?: return false

        val qualityLabel = when (quality) {
            512 -> "baja"
            768 -> "media"
            1024 -> "alta"
            else -> "custom"
        }

        val filename = "${albumTitle.replace("/", "_")}_${qualityLabel}_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/OpenTune")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val imageUri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return false

        context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, outputStream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(imageUri, contentValues, null, null)
        }

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

@Composable
private fun QualitySelectionDialog(
    onDismiss: () -> Unit,
    onQualitySelected: (Int) -> Unit,
) {
    var selectedQuality by remember { mutableStateOf(1024) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Selecciona la calidad",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .selectable(
                            selected = selectedQuality == 512,
                            onClick = { selectedQuality = 512 },
                            role = Role.RadioButton
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = selectedQuality == 512,
                        onClick = { selectedQuality = 512 }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Baja (512x512)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "~50 KB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .selectable(
                            selected = selectedQuality == 768,
                            onClick = { selectedQuality = 768 },
                            role = Role.RadioButton
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = selectedQuality == 768,
                        onClick = { selectedQuality = 768 }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Media (768x768)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "~100 KB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .selectable(
                            selected = selectedQuality == 1024,
                            onClick = { selectedQuality = 1024 },
                            role = Role.RadioButton
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = selectedQuality == 1024,
                        onClick = { selectedQuality = 1024 }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Alta (1024x1024)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "~200 KB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onQualitySelected(selectedQuality)
                    onDismiss()
                }
            ) {
                Text("Descargar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface

    var downloadingCover by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }

    LaunchedEffect(albumWithSongs?.album?.thumbnailUrl) {
        val thumbnailUrl = albumWithSongs?.album?.thumbnailUrl
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .size(Size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE))
                .allowHardware(false)
                .build()

            val result = runCatching {
                context.imageLoader.execute(request)
            }.getOrNull()

            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap)
                            .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                            .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                            .generate()
                    }

                    val extractedColors = PlayerColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = fallbackColor
                    )
                    gradientColors = extractedColors
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val wrappedSongs = remember(albumWithSongs, hideExplicit) {
        val filteredSongs = if (hideExplicit) {
            albumWithSongs?.songs?.filter { !it.song.explicit } ?: emptyList()
        } else {
            albumWithSongs?.songs ?: emptyList()
        }
        filteredSongs.map { item -> ItemWrapper(item) }.toMutableStateList()
    }

    var selection by remember { mutableStateOf(false) }

    if (selection) {
        BackHandler {
            selection = false
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableStateOf(Download.STATE_STOPPED) }

    LaunchedEffect(albumWithSongs) {
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                                downloads[it]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    val lazyListState = rememberLazyListState()

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    val transparentAppBar by remember {
        derivedStateOf {
            !disableBlur && !selection && !showTopBarTitle
        }
    }

    MochyTheme {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {

        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            val albumWithSongs = albumWithSongs
            val hasSongs = albumWithSongs?.songs?.isNotEmpty() == true
            if (hasSongs) {

                item(key = "header") {
Column(
    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
) {
    PlaylistScreenHeader(
        thumbnailUrl = albumWithSongs.album.thumbnailUrl,
        title = albumWithSongs.album.title,
        userName = albumWithSongs.artists.joinToString { it.name },
        description = null,
        isPlaying = isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id,
        onPlayClick = {
            playerConnection.service.getAutomix(playlistId)
            playerConnection.playQueue(LocalAlbumRadio(albumWithSongs))
        },
        onShuffleClick = {
            playerConnection.service.getAutomix(playlistId)
            playerConnection.playQueue(
                LocalAlbumRadio(albumWithSongs.copy(songs = albumWithSongs.songs.shuffled()))
            )
        },
        onMoreClick = {
            menuState.show {
                AlbumMenu(
                    originalAlbum = Album(albumWithSongs.album, albumWithSongs.artists),
                    navController = navController,
                    onDismiss = menuState::dismiss,
                )
            }
        },
        accentColor = gradientColors.firstOrNull(),
        showActions = false,
        modifier = Modifier.padding(top = systemBarsTopPadding + AppBarHeight),
    )
    Text(
        text = listOfNotNull(
            albumWithSongs.album.year?.toString(),
            pluralStringResource(R.plurals.n_song, wrappedSongs.size, wrappedSongs.size),
            albumWithSongs.songs.sumOf { it.song.duration }.takeIf { it > 0 }
                ?.let { makeTimeString(it * 1000L) },
        ).joinToString("  ·  "),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp),
    )
    Row(modifier = Modifier.padding(horizontal = 24.dp)) {
        albumWithSongs.artists.forEachIndexed { index, artist ->
            Text(
                text = artist.name + if (index == albumWithSongs.artists.lastIndex) "" else ", ",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable { navController.navigate("artist/${artist.id}") },
            )
        }
    }
        DetailActionRow(
            isPlaying = isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id,
            onPlayClick = {
                if (mediaMetadata?.album?.id == albumWithSongs.album.id) {
                    playerConnection.player.togglePlayPause()
                } else {
                    playerConnection.service.getAutomix(playlistId)
                    playerConnection.playQueue(LocalAlbumRadio(albumWithSongs))
                }
            },
            onShuffleClick = {
                playerConnection.service.getAutomix(playlistId)
                playerConnection.playQueue(
                    LocalAlbumRadio(albumWithSongs.copy(songs = albumWithSongs.songs.shuffled()))
                )
            },
            secondaryAction = DetailSecondaryAction.DownloadAction(
                state = downloadState,
                onClick = {
                    when (downloadState) {
                        Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                            albumWithSongs.songs.forEach { song ->
                                DownloadService.sendRemoveDownload(
                                    context, ExoDownloadService::class.java, song.id, false
                                )
                            }
                        }
                        else -> {
                            albumWithSongs.songs.forEach { song ->
                                val request = DownloadRequest.Builder(song.id, song.id.toUri())
                                    .setCustomCacheKey(song.id)
                                    .setData(song.song.title.toByteArray())
                                    .build()
                                DownloadService.sendAddDownload(
                                    context, ExoDownloadService::class.java, request, false
                                )
                            }
                        }
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            extraAction = {
                DetailCircleButton(
                    icon = if (albumWithSongs.album.bookmarkedAt != null)
                        R.drawable.favorite else R.drawable.favorite_border,
                    contentDescription = stringResource(R.string.save),
                    onClick = { database.query { update(albumWithSongs.album.toggleLike()) } },
                )
                DetailCircleButton(
                    icon = R.drawable.more_vert,
                    contentDescription = null,
                    onClick = {
                        menuState.show {
                            AlbumMenu(
                                originalAlbum = Album(albumWithSongs.album, albumWithSongs.artists),
                                navController = navController,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                )
            },
        )
        TextButton(onClick = { showQualityDialog = true }, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.download))
        }
}}

                item(key = "songs_header") {
                    NavigationTitle(
                        title = stringResource(R.string.songs),
                    )
                }

                itemsIndexed(
                    items = wrappedSongs,
                    key = { _, song -> song.item.id },
                ) { index, songWrapper ->
                    if (!selection) {
                        DetailTrackRow(
                            thumbnailUrl = songWrapper.item.thumbnailUrl,
                            title = songWrapper.item.title,
                            subtitle = songWrapper.item.artists.joinToString { it.name },
                            isActive = songWrapper.item.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            onClick = {
                                if (songWrapper.item.id == mediaMetadata?.id) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playerConnection.service.getAutomix(playlistId)
                                    playerConnection.playQueue(
                                        LocalAlbumRadio(albumWithSongs, startIndex = index)
                                    )
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selection = true
                                wrappedSongs.forEach { it.isSelected = false }
                                songWrapper.isSelected = true
                            },
                            onMenuClick = {
                                menuState.show {
                                    SongMenu(
                                        originalSong = songWrapper.item,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                            modifier = Modifier.animateItem(),
                        )
                    } else {
                    SongListItem(
                        song = songWrapper.item,
                        albumIndex = index + 1,
                        isActive = songWrapper.item.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        showInLibraryIcon = true,
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        SongMenu(
                                            originalSong = songWrapper.item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                                onLongClick = {}
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                )
                            }
                        },
                        isSelected = songWrapper.isSelected && selection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (!selection) {
                                        if (songWrapper.item.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.service.getAutomix(playlistId)
                                            playerConnection.playQueue(
                                                LocalAlbumRadio(albumWithSongs, startIndex = index),
                                            )
                                        }
                                    } else {
                                        songWrapper.isSelected = !songWrapper.isSelected
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (!selection) {
                                        selection = true
                                    }
                                    wrappedSongs.forEach { it.isSelected = false }
                                    songWrapper.isSelected = true
                                },
                            ),
                    )
                    }
                }

                if (otherVersions.isNotEmpty()) {
                    item(key = "other_versions_header") {
                        NavigationTitle(
                            title = stringResource(R.string.other_versions),
                        )
                    }
                    item(key = "other_versions_list") {
                        LazyRow {
                            items(
                                items = otherVersions.distinctBy { it.id },
                                key = { it.id },
                            ) { item ->
                                YouTubeGridItem(
                                    item = item,
                                    isActive = mediaMetadata?.album?.id == item.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = scope,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = { navController.navigate("album/${item.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeAlbumMenu(
                                                        albumItem = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        }
                    }
                }
            } else {
                when (val state = uiState) {
                    AlbumUiState.Loading,
                    AlbumUiState.Content -> {
                        item(key = "shimmer") {
                            ShimmerHost {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = systemBarsTopPadding + AppBarHeight),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 8.dp, bottom = 20.dp)
                                            .size(240.dp)
                                            .shimmer()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.onSurface)
                                    )

                                    TextPlaceholder(
                                        height = 28.dp,
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .padding(horizontal = 32.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    TextPlaceholder(
                                        height = 20.dp,
                                        modifier = Modifier.fillMaxWidth(0.4f)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 48.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        repeat(3) {
                                            TextPlaceholder(
                                                height = 32.dp,
                                                modifier = Modifier.width(70.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .shimmer()
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface)
                                        )
                                        ButtonPlaceholder(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        )
                                        ButtonPlaceholder(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .shimmer()
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))
                                }

                                repeat(6) {
                                    ListItemPlaceHolder()
                                }
                            }
                        }
                    }

                    AlbumUiState.Empty -> {
                        item(key = "empty") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = systemBarsTopPadding + AppBarHeight)
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_album),
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.empty_album_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    is AlbumUiState.Error -> {
                        item(key = "error") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = systemBarsTopPadding + AppBarHeight)
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (state.isNotFound) stringResource(R.string.album_not_found) else stringResource(R.string.error_unknown),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (state.isNotFound) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (state.isNotFound) stringResource(R.string.album_not_found_desc) else stringResource(R.string.error_unknown),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.retry() }) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                }
            }
        }

        val topAppBarColors = if (transparentAppBar) {
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            )
        } else {
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            )
        }

        TopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            colors = topAppBarColors,
            scrollBehavior = scrollBehavior,
            title = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    Text(
                        text = pluralStringResource(R.plurals.n_song, count, count),
                        style = MaterialTheme.typography.titleLarge
                    )
                } else if (showTopBarTitle) {
                    Text(
                        text = albumWithSongs?.album?.title.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (selection) {
                            selection = false
                        } else {
                            navController.navigateUp()
                        }
                    },
                    onLongClick = {
                        if (!selection) {
                            navController.backToMain()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (selection) R.drawable.close else R.drawable.arrow_back
                        ),
                        contentDescription = null
                    )
                }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    IconButton(
                        onClick = {
                            if (count == wrappedSongs.size) {
                                wrappedSongs.forEach { it.isSelected = false }
                            } else {
                                wrappedSongs.forEach { it.isSelected = true }
                            }
                        },
                        onLongClick = {}
                    ) {
                        Icon(
                            painter = painterResource(
                                if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                            ),
                            contentDescription = null
                        )
                    }

                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionSongMenu(
                                    songSelection = wrappedSongs.filter { it.isSelected }
                                        .map { it.item },
                                    onDismiss = menuState::dismiss,
                                    clearAction = { selection = false }
                                )
                            }
                        },
                        onLongClick = {}
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                }
            }
        )

        if (showQualityDialog) {
            QualitySelectionDialog(
                onDismiss = { showQualityDialog = false },
                onQualitySelected = { quality ->
                    downloadingCover = true
                    scope.launch {
                        try {
                            val success = downloadAlbumCover(
                                context = context,
                                imageUrl = albumWithSongs?.album?.thumbnailUrl,
                                albumTitle = albumWithSongs?.album?.title ?: "album",
                                quality = quality
                            )

                            val qualityLabel = when (quality) {
                                512 -> "Baja (512x512)"
                                768 -> "Media (768x768)"
                                1024 -> "Alta (1024x1024)"
                                else -> "Custom"
                            }

                            if (success) {

                            }
                        } finally {
                            downloadingCover = false
                        }
                    }
                }
            )
        }
    }
    }
}

@Composable
private fun MetadataChip(
    icon: Int,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
