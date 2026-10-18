package com.arturo254.opentune.ui.screens

import android.content.Intent
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.animation.core.*
import com.arturo254.opentune.core.designsystem.theme.galaxScreenBackground
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.db.entities.SongWithStats
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.ArtistMenu
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.ComposeToImage
import com.arturo254.opentune.utils.joinByBullet
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.viewmodels.YearInMusicViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val Ink      = Color(0xFF060912)
private val Snow     = Color(0xFFFDFDFD)
private val SnowMid  = Color(0xB3FDFDFD)
private val SnowDim  = Color(0x55FDFDFD)
private val GlassHi  = Color(0x28FFFFFF)
private val GlassBorder = Color(0x18FFFFFF)

private val WelcomeA  = Color(0xFF110024)
private val WelcomeB  = Color(0xFF8A1FFF)
private val WelcomeC  = Color(0xFFFB3E7E)

private val MinutesA  = Color(0xFF000E30)
private val MinutesB  = Color(0xFF1560FF)
private val MinutesC  = Color(0xFF00CFFF)

private val SongsA    = Color(0xFF001409)
private val SongsB    = Color(0xFF1DB954)
private val SongsC    = Color(0xFFB3FF6E)

private val SpotA     = Color(0xFF200028)
private val SpotB     = Color(0xFFFF2D78)

private val ArtistA   = Color(0xFF0F001E)
private val ArtistB   = Color(0xFFBF00FF)
private val ArtistC   = Color(0xFF5500CC)

private val AlbumA    = Color(0xFF170900)
private val AlbumB    = Color(0xFFFF7A00)
private val AlbumC    = Color(0xFFFFD000)

private val PersonA   = Color(0xFF001918)
private val PersonB   = Color(0xFF00E5C3)

private val SummaryA  = Color(0xFF0E0018)
private val SummaryB  = Color(0xFF6A0DAD)
private val SummaryC  = Color(0xFF1560FF)
private val SummaryD  = Color(0xFF1DB954)

private data class InsightPersonality(
    val emoji: String,
    val title: String,
    val description: String,
    val gradTop: Color,
    val gradBot: Color,
    val accent: Color,
)

private val PersonalityDevoted = InsightPersonality(
    emoji = "🎯",
    title = "Devoted Fan",
    description = "When you find a song you love, you play it on repeat. Your loyalty to your favourites is unmatched — and that's your superpower.",
    gradTop = PersonA, gradBot = PersonB, accent = PersonB,
)
private val PersonalityExplorer = InsightPersonality(
    emoji = "🧭",
    title = "Music Explorer",
    description = "Always discovering, never settling. Your taste spans worlds, and your playlist never sounds the same twice.",
    gradTop = WelcomeA, gradBot = WelcomeB, accent = WelcomeB,
)
private val PersonalityAudiophile = InsightPersonality(
    emoji = "🎧",
    title = "True Audiophile",
    description = "Hours melt away when you're in the zone. Music isn't background noise for you — it's everything.",
    gradTop = MinutesA, gradBot = MinutesB, accent = MinutesC,
)
private val PersonalityCasual = InsightPersonality(
    emoji = "🌊",
    title = "Laid-Back Listener",
    description = "Music moves effortlessly with your life. It's always there when you need it, easy and perfectly in tune.",
    gradTop = SongsA, gradBot = SongsB, accent = SongsC,
)

private fun computePersonality(
    topSongs: List<SongWithStats>,
    totalPlayed: Long,
    totalTimeMs: Long,
): InsightPersonality {
    if (topSongs.isEmpty()) return PersonalityCasual
    val topRatio      = topSongs.first().songCountListened.toFloat() / totalPlayed.coerceAtLeast(1)
    val hoursListened = totalTimeMs / 3_600_000L
    return when {
        topRatio >= 0.25f   -> PersonalityDevoted
        hoursListened >= 50 -> PersonalityAudiophile
        topSongs.size >= 5  -> PersonalityExplorer
        else                -> PersonalityCasual
    }
}

private enum class InsightPage {
    Welcome, Minutes, TopSongsList, SongSpotlight,
    ArtistSpotlight, TopAlbumsList, Personality, Summary,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun YearInMusicScreen(
    navController: NavController,
    viewModel: YearInMusicViewModel = hiltViewModel(),
) {
    val context           = LocalContext.current
    val menuState         = LocalMenuState.current
    val haptic            = LocalHapticFeedback.current
    val playerConnection  = LocalPlayerConnection.current ?: return
    val coroutineScope    = rememberCoroutineScope()
    val view              = LocalView.current

    val availableYears      by viewModel.availableYears.collectAsState()
    val selectedYear        by viewModel.selectedYear.collectAsState()
    val topSongsStats       by viewModel.topSongsStats.collectAsState()
    val topSongs            by viewModel.topSongs.collectAsState()
    val topArtists          by viewModel.topArtists.collectAsState()
    val topAlbums           by viewModel.topAlbums.collectAsState()
    val totalListeningTime  by viewModel.totalListeningTime.collectAsState()
    val totalSongsPlayed    by viewModel.totalSongsPlayed.collectAsState()

    var isGeneratingImage   by remember { mutableStateOf(false) }
    var isShareCaptureMode  by remember { mutableStateOf(false) }
    var shareBounds         by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var isYearPickerOpen    by remember { mutableStateOf(false) }

    val shareArgb = Ink.toArgb()

    val pages = remember(topSongsStats, topArtists, topAlbums, totalListeningTime, totalSongsPlayed) {
        buildList {
            add(InsightPage.Welcome)
            if (totalListeningTime > 0 || totalSongsPlayed > 0) add(InsightPage.Minutes)
            if (topSongsStats.isNotEmpty()) {
                add(InsightPage.TopSongsList)
                add(InsightPage.SongSpotlight)
            }
            if (topArtists.isNotEmpty()) add(InsightPage.ArtistSpotlight)
            if (topAlbums.isNotEmpty()) add(InsightPage.TopAlbumsList)
            if (topSongsStats.isNotEmpty()) add(InsightPage.Personality)
            add(InsightPage.Summary)
        }
    }

    val pagerState = rememberPagerState { pages.size }
    val currentPage = pagerState.currentPage
    val isLastPage  = currentPage == pages.lastIndex
    val hasData     = topSongsStats.isNotEmpty() || topArtists.isNotEmpty() || topAlbums.isNotEmpty()

    LaunchedEffect(isShareCaptureMode) {
        if (isShareCaptureMode) pagerState.scrollToPage(pages.lastIndex)
    }

    com.arturo254.opentune.core.designsystem.theme.GalaxDestinationTheme(immersive = true) {
Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .onGloballyPositioned { shareBounds = it.boundsInRoot() }
    ) {

        CompositionLocalProvider(LocalInsightCapture provides isShareCaptureMode) {
HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isShareCaptureMode,
            beyondViewportPageCount = 0,
        ) { pageIndex ->
            InsightPageContent(
                page               = pages.getOrNull(pageIndex) ?: InsightPage.Summary,
                year               = selectedYear,
                totalListeningTime = totalListeningTime,
                totalSongsPlayed   = totalSongsPlayed,
                topSongsStats      = topSongsStats,
                topSongs           = topSongs,
                topArtists         = topArtists,
                topAlbums          = topAlbums,
                menuState          = menuState,
                haptic             = haptic,
                navController      = navController,
                coroutineScope     = coroutineScope,
                modifier           = Modifier.fillMaxSize(),
            )
        }
}

        if (!isShareCaptureMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(3f)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.55f),
                            1f to Color.Transparent
                        )
                    )
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top))
                    .padding(top = 6.dp)
            ) {

                InsightProgressBar(
                    totalPages  = pages.size,
                    currentPage = currentPage,
                    modifier    = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick   = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), null, tint = Snow)
                    }
                    Spacer(Modifier.weight(1f))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = stringResource(R.string.app_name),
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color      = SnowDim,
                            letterSpacing = 1.5.sp,
                        )
                        Text(
                            text       = "Insight",
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Snow,
                            letterSpacing = 0.5.sp,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    InsightYearChip(
                        year    = selectedYear,
                        onClick = { isYearPickerOpen = true },
                    )
                }
            }
        }

        if (!isShareCaptureMode && isLastPage && hasData) {
            InsightShareFab(
                isGenerating = isGeneratingImage,
                onClick = {
                    if (!isGeneratingImage) {
                        isGeneratingImage = true
                        coroutineScope.launch {
                            try {
                                isShareCaptureMode = true
                                awaitNextPreDraw(view)
                                awaitNextPreDraw(view)
                                val raw = ComposeToImage.captureViewBitmap(
                                    view = view, backgroundColor = shareArgb
                                )
                                val bounds = shareBounds
                                val uri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                    val cropped = if (bounds != null) ComposeToImage.cropBitmap(raw,
                                        bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt()) else raw
                                    val fitted = ComposeToImage.fitBitmap(cropped, 1080, 1920, shareArgb)
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        ComposeToImage.saveBitmapAsFile(context, fitted, "GalaxOS_Insight_$selectedYear")
                                    }
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        },
                                        context.getString(R.string.share_summary)
                                    )
                                )
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(context, R.string.galax_insight_share_error,
                                    android.widget.Toast.LENGTH_SHORT).show()
                            } finally {
                                isShareCaptureMode = false
                                isGeneratingImage  = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
            )
        }

        if (!isShareCaptureMode && isYearPickerOpen) {
            InsightYearPickerDialog(
                availableYears = availableYears,
                selectedYear   = selectedYear,
                onSelectYear   = { y -> viewModel.selectedYear.value = y; isYearPickerOpen = false },
                onDismiss      = { isYearPickerOpen = false },
            )
        }
    }
}
}

private suspend fun awaitNextPreDraw(view: View) = suspendCancellableCoroutine<Unit> { cont ->
    val vto = view.viewTreeObserver
    val listener = object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            if (vto.isAlive) vto.removeOnPreDrawListener(this)
            cont.resume(Unit)
            return true
        }
    }
    vto.addOnPreDrawListener(listener)
    cont.invokeOnCancellation { if (vto.isAlive) vto.removeOnPreDrawListener(listener) }
    view.invalidate()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InsightPageContent(
    page: InsightPage,
    year: Int,
    totalListeningTime: Long,
    totalSongsPlayed: Long,
    topSongsStats: List<SongWithStats>,
    topSongs: List<Song>,
    topArtists: List<Artist>,
    topAlbums: List<Album>,
    menuState: com.arturo254.opentune.ui.component.MenuState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    navController: NavController,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    when (page) {
        InsightPage.Welcome ->
            WelcomePage(year = year, modifier = modifier)

        InsightPage.Minutes ->
            MinutesPage(
                totalListeningTimeMs = totalListeningTime,
                totalSongsPlayed     = totalSongsPlayed,
                modifier             = modifier,
            )

        InsightPage.TopSongsList ->
            TopSongsListPage(songs = topSongsStats, modifier = modifier)

        InsightPage.SongSpotlight ->
            SongSpotlightPage(
                song     = topSongsStats.firstOrNull(),
                modifier = modifier,
                onLongClick = {
                    topSongs.firstOrNull()?.let { entity ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            SongMenu(
                                originalSong = entity,
                                navController = navController,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    }
                },
            )

        InsightPage.ArtistSpotlight ->
            ArtistSpotlightPage(
                artist   = topArtists.firstOrNull(),
                modifier = modifier,
                onLongClick = {
                    topArtists.firstOrNull()?.let { artist ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            ArtistMenu(
                                originalArtist = artist,
                                coroutineScope = coroutineScope,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    }
                },
            )

        InsightPage.TopAlbumsList ->
            TopAlbumsPage(albums = topAlbums, modifier = modifier)

        InsightPage.Personality ->
            PersonalityPage(
                personality = computePersonality(topSongsStats, totalSongsPlayed, totalListeningTime),
                topSong     = topSongsStats.firstOrNull(),
                modifier    = modifier,
            )

        InsightPage.Summary ->
            SummaryPage(
                year               = year,
                totalListeningTime = totalListeningTime,
                totalSongsPlayed   = totalSongsPlayed,
                topSong            = topSongsStats.firstOrNull(),
                topArtist          = topArtists.firstOrNull(),
                topAlbum           = topAlbums.firstOrNull(),
                modifier           = modifier,
            )
    }
}

@Composable
private fun WelcomePage(year: Int, modifier: Modifier = Modifier) {
    GalaxInsightPage(stringResource(R.string.galax_insight_intro), year.toString(), modifier) {
        Canvas(Modifier.fillMaxWidth().height(220.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            repeat(4) { index ->
                drawOval(com.arturo254.opentune.core.designsystem.theme.GalaxScreenStyle.blue.copy(alpha = .1f + index * .03f),
                    topLeft = Offset(center.x - size.width * (.18f + index * .08f), center.y - 22.dp.toPx() - index * 14.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width * (.36f + index * .16f), 44.dp.toPx() + index * 28.dp.toPx()),
                    style = Stroke(1.dp.toPx()))
            }
            drawCircle(com.arturo254.opentune.core.designsystem.theme.GalaxScreenStyle.violet.copy(alpha = .4f), 26.dp.toPx(), center)
        }
        Text(stringResource(R.string.year_in_music), style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MinutesPage(
    totalListeningTimeMs: Long,
    totalSongsPlayed: Long,
    modifier: Modifier = Modifier,
) {
    val minutes = totalListeningTimeMs / 60_000L
    GalaxInsightPage(stringResource(R.string.total_listening_time), stringResource(R.string.year_in_music), modifier) {
        Text(minutes.toString(), style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.galax_insight_minutes), style = MaterialTheme.typography.headlineSmall)
        GalaxInsightMetric(stringResource(R.string.galax_insight_plays), totalSongsPlayed.toString())
    }
}

@Composable
private fun TopSongsListPage(songs: List<SongWithStats>, modifier: Modifier = Modifier) {
    GalaxInsightPage(stringResource(R.string.top_songs), stringResource(R.string.year_in_music), modifier) {
        songs.take(5).forEachIndexed { index, song ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text((index + 1).toString().padStart(2, '0'), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(24.dp))
                GalaxInsightArtwork(song.thumbnailUrl, Modifier.size(56.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${song.songCountListened} ${stringResource(R.string.galax_insight_plays)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongSpotlightPage(
    song: SongWithStats?,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
) {
    if (song == null) return
    GalaxInsightPage(song.title, stringResource(R.string.top_songs),
        modifier.combinedClickable(onClick = {}, onLongClick = onLongClick)) {
        GalaxInsightArtwork(song.thumbnailUrl, Modifier.fillMaxWidth().aspectRatio(1f))
        GalaxInsightMetric(stringResource(R.string.galax_insight_plays), song.songCountListened.toString())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistSpotlightPage(
    artist: Artist?,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
) {
    if (artist == null) return
    GalaxInsightPage(artist.artist.name, stringResource(R.string.top_artists),
        modifier.combinedClickable(onClick = {}, onLongClick = onLongClick)) {
        GalaxInsightArtwork(artist.artist.thumbnailUrl, Modifier.fillMaxWidth().aspectRatio(1f), circle = true)
    }
}

@Composable
private fun TopAlbumsPage(albums: List<Album>, modifier: Modifier = Modifier) {
    GalaxInsightPage(stringResource(R.string.albums), stringResource(R.string.year_in_music), modifier) {
        albums.take(5).forEachIndexed { index, album ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text((index + 1).toString().padStart(2, '0'), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(24.dp))
                GalaxInsightArtwork(album.album.thumbnailUrl, Modifier.size(64.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(album.album.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(album.artists.joinToString { it.name }, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun PersonalityPage(
    personality: InsightPersonality,
    topSong: SongWithStats?,
    modifier: Modifier = Modifier,
) {
    GalaxInsightPage(personality.title, stringResource(R.string.galax_insight_identity), modifier) {
        topSong?.let { GalaxInsightArtwork(it.thumbnailUrl, Modifier.size(160.dp)) }
        Text(personality.description, style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SummaryPage(
    year: Int,
    totalListeningTime: Long,
    totalSongsPlayed: Long,
    topSong: SongWithStats?,
    topArtist: Artist?,
    topAlbum: Album?,
    modifier: Modifier = Modifier,
) {
    GalaxInsightPage(year.toString(), stringResource(R.string.galax_insight_summary), modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GalaxInsightMetric(stringResource(R.string.galax_insight_minutes), (totalListeningTime / 60_000L).toString(), Modifier.weight(1f))
            GalaxInsightMetric(stringResource(R.string.galax_insight_plays), totalSongsPlayed.toString(), Modifier.weight(1f))
        }
        topSong?.let { song ->
            if (LocalInsightCapture.current) Row(horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                GalaxInsightArtwork(song.thumbnailUrl, Modifier.size(96.dp))
                Text(song.title, style = MaterialTheme.typography.titleLarge, maxLines = 3,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            } else {
                GalaxInsightArtwork(song.thumbnailUrl, Modifier.fillMaxWidth().aspectRatio(1f))
                Text(song.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            topArtist?.let { GalaxInsightMetric(stringResource(R.string.top_artists), it.artist.name, Modifier.weight(1f)) }
            topAlbum?.let { GalaxInsightMetric(stringResource(R.string.albums), it.album.title, Modifier.weight(1f)) }
        }
        if (topSong == null && topArtist == null && topAlbum == null)
            Text(stringResource(R.string.no_listening_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun InsightProgressBar(
    totalPages: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalPages) { index ->
            val fillFraction by animateFloatAsState(
                targetValue = when {
                    index < currentPage -> 1f
                    index == currentPage -> 1f
                    else -> 0f
                },
                animationSpec = tween(250),
                label = "segFill$index",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Snow.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Snow)
                )
            }
        }
    }
}

@Composable
private fun InsightYearChip(year: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GlassHi)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text       = year.toString(),
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color      = Snow,
            )
            Icon(
                painterResource(R.drawable.expand_more),
                contentDescription = null,
                tint     = SnowMid,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun InsightShareFab(
    isGenerating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    com.arturo254.opentune.ui.component.LiquidGlassIconButton(onClick = onClick, modifier = modifier,
        size = 56.dp, enabled = !isGenerating, accessibilityLabel = stringResource(R.string.share_summary)) {
        if (isGenerating) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        else Icon(painterResource(R.drawable.share), null)
    }
}

@Composable
private fun InsightYearPickerDialog(
    availableYears: List<Int>,
    selectedYear: Int,
    onSelectYear: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest  = onDismiss,
        containerColor    = Color(0xFF111118),
        titleContentColor = Snow,
        title = {
            Text(
                "Select year",
                fontWeight = FontWeight.ExtraBold,
                color      = Snow,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                availableYears.forEach { year ->
                    val isSelected = year == selectedYear
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected)
                                    Brush.linearGradient(listOf(WelcomeB, WelcomeC))
                                else
                                    Brush.linearGradient(listOf(GlassHi, GlassHi))
                            )
                            .border(
                                width = if (isSelected) 0.dp else 1.dp,
                                color = GlassBorder,
                                shape = RoundedCornerShape(14.dp),
                            )
                            .clickable { onSelectYear(year) }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text       = year.toString(),
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            color      = Snow,
                            fontSize   = 16.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dismiss), color = WelcomeB)
            }
        }
    )
}

@Composable
private fun rememberSafeImageRequest(data: Any?): Any? {
    val context = LocalContext.current
    return remember(data) {
        data?.let {
            ImageRequest.Builder(context)
                .data(it)
                .size(512)
                .allowHardware(false)
                .build()
        }
    }
}
private val LocalInsightCapture = staticCompositionLocalOf { false }

@Composable
private fun GalaxInsightPage(title: String, eyebrow: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val capture = LocalInsightCapture.current
    Column(modifier.fillMaxSize().galaxScreenBackground().verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
        .padding(top = if (capture) 24.dp else insets.calculateTopPadding() + 96.dp,
            bottom = if (capture) 24.dp else insets.calculateBottomPadding() + 88.dp),
        verticalArrangement = Arrangement.spacedBy(if (capture) 16.dp else 24.dp)) {
        Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary, letterSpacing = 1.5.sp)
        Text(title, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
        content()
    }
}

@Composable
private fun GalaxInsightArtwork(url: String?, modifier: Modifier, circle: Boolean = false) {
    AsyncImage(model = rememberSafeImageRequest(url), contentDescription = null, contentScale = ContentScale.Crop,
        modifier = modifier.clip(if (circle) CircleShape else com.arturo254.opentune.core.designsystem.theme.GalaxScreenStyle.artworkShape))
}

@Composable
private fun GalaxInsightMetric(label: String, value: String, modifier: Modifier = Modifier) {
    com.arturo254.opentune.ui.component.MenuSurfaceSection(modifier) {
        Column(Modifier.padding(if (LocalInsightCapture.current) 12.dp else 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
