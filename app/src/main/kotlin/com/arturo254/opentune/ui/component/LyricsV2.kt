/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254) & WTTexe!
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.component

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.LyricsClickKey
import com.arturo254.opentune.constants.LyricsLineSpacingKey
import com.arturo254.opentune.constants.LyricsRomanizeJapaneseKey
import com.arturo254.opentune.constants.LyricsRomanizeKoreanKey
import com.arturo254.opentune.constants.LyricsScrollKey
import com.arturo254.opentune.constants.LyricsTextSizeKey
import com.arturo254.opentune.constants.PlayerBackgroundStyle
import com.arturo254.opentune.constants.PlayerBackgroundStyleKey
import com.arturo254.opentune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.arturo254.opentune.lyrics.LyricsEntry
import com.arturo254.opentune.lyrics.LyricsUtils.findCurrentLineIndex
import com.arturo254.opentune.lyrics.LyricsUtils.isChinese
import com.arturo254.opentune.lyrics.LyricsUtils.isJapanese
import com.arturo254.opentune.lyrics.LyricsUtils.isKorean
import com.arturo254.opentune.lyrics.LyricsUtils.isTtml
import com.arturo254.opentune.lyrics.LyricsUtils.parseLyrics
import com.arturo254.opentune.lyrics.LyricsUtils.parseTtml
import com.arturo254.opentune.lyrics.LyricsUtils.romanizeJapanese
import com.arturo254.opentune.lyrics.LyricsUtils.romanizeKorean
import com.arturo254.opentune.lyrics.WordTimestamp
import com.arturo254.opentune.ui.component.shimmer.ShimmerHost
import com.arturo254.opentune.ui.component.shimmer.TextPlaceholder
import com.arturo254.opentune.ui.utils.smoothFadingEdge
import com.arturo254.opentune.utils.ComposeToImage
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs


// ──────────────────────────────────────────────────────────────────────
// Constants
// ──────────────────────────────────────────────────────────────────────

/** Lead time offset for LRC-style line-synced lyrics (ms). */
private const val LRC_LEAD_MS = 300L

/** Lead time offset for TTML word-synced lyrics (ms). */
private const val TTML_LEAD_MS = 0L

/** Extra offset so animations land visually on beat instead of slightly late. */
private const val VISUAL_TUNING_OFFSET_MS = 150L

/** Seconds to wait before auto-scroll resumes after manual scroll. */
private const val MANUAL_SCROLL_TIMEOUT_MS = 3000L

/** How far (in items) the current line can drift before we hard-snap instead of animating. */
private const val SCROLL_SNAP_DISTANCE_THRESHOLD = 15

/** Vertical bias for where the active line sits in the viewport (from the top). */
private const val SCROLL_TARGET_BIAS = 0.35f

/** Maximum number of lines that can be selected for sharing at once. */
private const val MAX_SELECTION_LIMIT = 5

/** How much the active line scales up ("pops") relative to inactive lines. */
private const val ACTIVE_LINE_SCALE = 1.035f

/** Duration for the alpha crossfade between active/inactive lines. */
private const val LINE_ALPHA_TRANSITION_MS = 380

/** Max blur radius (dp) applied to lines far from the active one — Android 12+ (API 31+) only. */
private const val MAX_LINE_BLUR_RADIUS_DP = 2.2f

/** How much blur radius grows per line of distance from the active line. */
private const val BLUR_PER_LINE_DISTANCE = 0.6f

/** Glow build-up / decay timing so it feels like it fades rather than snapping off. */
private const val GLOW_ATTACK_MS = 90
private const val GLOW_DECAY_MS = 320

/** Apple-Music-style easing for smooth deceleration. */
private val V2Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

/** Liquid fill easing: fast attack, very smooth deceleration (Apple Music-like). */
private val LiquidFillEasing = CubicBezierEasing(0.0f, 0.0f, 0.15f, 1.0f)

/** Sentinel entry prepended so auto-scroll has headroom above the first line. */
private val HEAD_LYRICS_ENTRY = LyricsEntry(time = 0L, text = "")


// ──────────────────────────────────────────────────────────────────────
// Main Composable
// ──────────────────────────────────────────────────────────────────────


@SuppressLint("UnusedBoxWithConstraintsScope", "LocalContextGetResourceValueCall",
    "StringFormatInvalid"
)
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LyricsV2(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    // ── Preferences ──
    val (lyricsClick) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll) = rememberPreference(LyricsScrollKey, defaultValue = true)
    val (lyricsTextSize) = rememberPreference(LyricsTextSizeKey, defaultValue = 26f)
    val (lyricsLineSpacing) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.3f)
    val (romanizeJapanese) = rememberPreference(LyricsRomanizeJapaneseKey, defaultValue = true)
    val (romanizeKorean) = rememberPreference(LyricsRomanizeKoreanKey, defaultValue = true)
    // Font is now resolved once, app-wide, in Theme.kt (see AppFontStyle) and
    // flows down automatically through MaterialTheme.typography — passing null
    // here just falls back to whatever the theme's typography already carries.
    val lyricsFontFamily: FontFamily? = null
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)

    // ── Text colour derived from background style ──
    val textColor = if (playerBackground == PlayerBackgroundStyle.DEFAULT)
        MaterialTheme.colorScheme.onBackground
    else
        Color.White

    val inactiveAlpha = 0.35f

    // ── Selection mode state ──
    var isSelectionModeActive by rememberSaveable { mutableStateOf(false) }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var showShareCarouselSheet by remember { mutableStateOf(false) }
    var shareDialogData by remember {
        mutableStateOf<Triple<String, String, String>?>(null)
    }

    // ── Lyrics data ──
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = currentLyrics?.lyrics

    // ── Parse lyrics into entries ──
    val isSynced = remember(lyrics) { lyrics != null && (lyrics!!.startsWith("[") || isTtml(lyrics!!)) }
    val isTtmlFormat = remember(lyrics) { lyrics != null && isTtml(lyrics!!) }

    val lyricsEntries: List<LyricsEntry> = remember(lyrics) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) return@remember emptyList()
        val parsed = when {
            isTtml(lyrics!!) -> parseTtml(lyrics!!)
            lyrics!!.startsWith("[") -> parseLyrics(lyrics!!)
            else -> lyrics!!.lines()
                .filter { it.isNotBlank() }
                .map { line -> LyricsEntry(time = -1L, text = line.trim()) }
        }
        if (parsed.isNotEmpty() && parsed.first().time >= 0) {
            listOf(HEAD_LYRICS_ENTRY) + parsed
        } else {
            parsed
        }
    }

    // ── Synthesize word timings for LRC entries that lack them ──
    val entriesWithWords: List<LyricsEntry> = remember(lyricsEntries) {
        if (lyricsEntries.isEmpty()) return@remember emptyList()
        lyricsEntries.mapIndexed { index, entry ->
            if (entry.words != null || entry.time < 0 || entry.text.isBlank()) {
                entry // Already has word timings (TTML) or is non-synced
            } else {
                // Synthesize word-level timings for this LRC line
                val nextEntryTime = if (index < lyricsEntries.lastIndex) {
                    lyricsEntries[index + 1].time
                } else {
                    entry.time + 5000L // 5s fallback for last line
                }
                val lineDurationMs = (nextEntryTime - entry.time).coerceAtLeast(500L)
                val lineStartSec = entry.time / 1000.0

                val isCjkText = isJapanese(entry.text) || isChinese(entry.text) || isKorean(entry.text)
                val tokens = if (isCjkText) {
                    val chars = mutableListOf<String>()
                    var currentWord = StringBuilder()
                    entry.text.forEach { char ->
                        if (char.isWhitespace()) {
                            if (currentWord.isNotEmpty()) {
                                chars.add(currentWord.toString())
                                currentWord.clear()
                            }
                            chars.add(char.toString())
                        } else if (isJapanese(char.toString()) || isChinese(char.toString()) || isKorean(char.toString())) {
                            if (currentWord.isNotEmpty()) {
                                chars.add(currentWord.toString())
                                currentWord.clear()
                            }
                            chars.add(char.toString())
                        } else {
                            currentWord.append(char)
                        }
                    }
                    if (currentWord.isNotEmpty()) {
                        chars.add(currentWord.toString())
                    }

                    // Group spaces onto the preceding word
                    val groupedTokens = mutableListOf<String>()
                    chars.forEach { c ->
                        if (c.isBlank()) {
                            if (groupedTokens.isNotEmpty()) {
                                groupedTokens[groupedTokens.lastIndex] = groupedTokens.last() + c
                            }
                        } else {
                            groupedTokens.add(c)
                        }
                    }
                    groupedTokens
                } else {
                    entry.text.split(Regex("\\s+"))
                }
                if (tokens.isEmpty()) return@mapIndexed entry

                // Weight each token by character count for proportional distribution
                val totalChars = tokens.sumOf { it.length }.coerceAtLeast(1)
                val words = mutableListOf<WordTimestamp>()
                var currentOffsetMs = 0.0

                tokens.forEachIndexed { wordIdx, token ->
                    val weight = token.length.toDouble() / totalChars
                    val wordDurMs = lineDurationMs * weight
                    val wordStartSec = lineStartSec + (currentOffsetMs / 1000.0)
                    val wordEndSec = wordStartSec + (wordDurMs / 1000.0)

                    val wordText = if (wordIdx < tokens.lastIndex && !isCjkText) "$token " else token
                    words.add(
                        WordTimestamp(
                            text = wordText,
                            startTime = wordStartSec,
                            endTime = wordEndSec,
                        )
                    )
                    currentOffsetMs += wordDurMs
                }
                entry.copy(words = words)
            }
        }
    }

    // ── Romanization ──
    LaunchedEffect(entriesWithWords, romanizeJapanese, romanizeKorean) {
        if (!romanizeJapanese && !romanizeKorean) return@LaunchedEffect
        entriesWithWords.forEach { entry ->
            if (entry.text.isBlank() || entry.romanizedTextFlow.value != null) return@forEach
            scope.launch(Dispatchers.Default) {
                val romanized = when {
                    romanizeJapanese && isJapanese(entry.text) -> romanizeJapanese(entry.text)
                    romanizeKorean && isKorean(entry.text) -> romanizeKorean(entry.text)
                    else -> null
                }
                if (romanized != null) entry.romanizedTextFlow.value = romanized
            }
        }
    }

    // ── Playback position tracking ──
    val leadMs = if (isTtmlFormat) TTML_LEAD_MS else LRC_LEAD_MS
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var currentLineIndex by remember { mutableIntStateOf(0) }

    // Frame-synced position loop. Ties updates to the actual display refresh rate
    // (via withFrameMillis) instead of a fixed 16ms delay, so word-sweep animations
    // stay in sync on 90Hz/120Hz screens too, and don't drift over a long song.
    LaunchedEffect(entriesWithWords, isSynced) {
        if (!isSynced || entriesWithWords.isEmpty()) return@LaunchedEffect
        while (isActive) {
            withFrameMillis { }
            val sliderPos = sliderPositionProvider()
            val pos = sliderPos ?: player.currentPosition
            currentPositionMs = pos + leadMs + VISUAL_TUNING_OFFSET_MS
            currentLineIndex = findCurrentLineIndex(entriesWithWords, currentPositionMs, 0L)
        }
    }

    // ── Scroll State ──
    val listState = rememberLazyListState()
    var isManualScrolling by remember { mutableStateOf(false) }
    var lastManualScrollTime by remember { mutableLongStateOf(0L) }

    // Detect manual scrolling
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!isSelectionModeActive && source == NestedScrollSource.UserInput) {
                    isManualScrolling = true
                    lastManualScrollTime = System.currentTimeMillis()
                }
                return Offset.Zero
            }
        }
    }

    // Resume auto-scroll after timeout
    LaunchedEffect(isManualScrolling, lastManualScrollTime) {
        if (isManualScrolling) {
            kotlinx.coroutines.delay(MANUAL_SCROLL_TIMEOUT_MS)
            isManualScrolling = false
        }
    }

    // Auto-scroll to active line
    LaunchedEffect(currentLineIndex, isManualScrolling, lyricsScroll) {
        if (!lyricsScroll || isManualScrolling || !isSynced) return@LaunchedEffect
        if (currentLineIndex < 0 || currentLineIndex >= entriesWithWords.size) return@LaunchedEffect
        listState.smoothScrollToActiveLine(currentLineIndex)
    }

    BackHandler(enabled = isSelectionModeActive) {
        isSelectionModeActive = false
        selectedIndices.clear()
    }

    LaunchedEffect(showMaxSelectionToast) {
        if (showMaxSelectionToast) {
            Toast.makeText(
                context,
                context.getString(R.string.max_selection_limit, MAX_SELECTION_LIMIT),
                Toast.LENGTH_SHORT
            ).show()
            showMaxSelectionToast = false
        }
    }

    // ── Keep screen alive ──
    val activity = context as? android.app.Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // ── Render ──
    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
    ) {
        if (lyrics == LYRICS_NOT_FOUND) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
            return@BoxWithConstraints
        }

        if (lyrics == null) {
            ShimmerHost {
                repeat(6) {
                    TextPlaceholder()
                }
            }
            return@BoxWithConstraints
        }

        if (entriesWithWords.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
            return@BoxWithConstraints
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .smoothFadingEdge(vertical = 80.dp)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(
                items = entriesWithWords,
                key = { index, entry -> "${index}_${entry.time}" }
            ) { index, item ->
                if (item == HEAD_LYRICS_ENTRY) {
                    Spacer(modifier = Modifier.height(120.dp))
                    return@itemsIndexed
                }

                // ── Agent-based positioning ──
                // v1 or null -> Start, v2 -> End, others -> Center
                val textAlign = when (item.agent?.lowercase()) {
                    "v1", null -> TextAlign.Start
                    "v2" -> TextAlign.End
                    else -> TextAlign.Center
                }
                val horizontalAlignment = when (item.agent?.lowercase()) {
                    "v1", null -> Alignment.Start
                    "v2" -> Alignment.End
                    else -> Alignment.CenterHorizontally
                }

                val isActive = isSynced && index == currentLineIndex
                val isPast = isSynced && index < currentLineIndex

                // Distance-based alpha for non-active lines
                val distanceFromActive = if (isSynced) abs(index - currentLineIndex) else 0
                // For word-synced lines, each word handles its own alpha independently
                // so we use 1f for active lines to not double-dim
                val lineAlpha = when {
                    !isSynced -> 0.9f
                    isActive -> 1f
                    else -> (inactiveAlpha - (distanceFromActive - 1) * 0.03f)
                        .coerceIn(0.15f, inactiveAlpha)
                }
                // Word-synced lines: pass 1f alpha so individual words control their own
                val wordLineAlpha = if (item.words != null && isSynced) 1f else lineAlpha

                // Background vocal detection
                val isAllBackground = item.words?.all { it.isBackground || it.text.isBlank() } == true

                val isSelected = selectedIndices.contains(index)

                // ── Apple-Music-style line transition: smooth crossfade, gentle "pop" ──
                // scale on the active line, and a soft depth-of-field blur on lines far
                // from focus (mirrors the karaoke/soft-focus feel of Apple Music lyrics).
                val animatedLineAlpha by animateFloatAsState(
                    targetValue = wordLineAlpha,
                    animationSpec = tween(durationMillis = LINE_ALPHA_TRANSITION_MS, easing = V2Easing),
                    label = "lineAlpha",
                )
                val animatedLineScale by animateFloatAsState(
                    targetValue = if (isActive) ACTIVE_LINE_SCALE else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                    label = "lineScale",
                )
                val targetLineBlur = if (isSynced && distanceFromActive > 0) {
                    (distanceFromActive * BLUR_PER_LINE_DISTANCE).coerceIn(0f, MAX_LINE_BLUR_RADIUS_DP)
                } else {
                    0f
                }
                val animatedLineBlur by animateFloatAsState(
                    targetValue = targetLineBlur,
                    animationSpec = tween(durationMillis = LINE_ALPHA_TRANSITION_MS, easing = V2Easing),
                    label = "lineBlur",
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isSelected && isSelectionModeActive)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else
                                Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(
                            start = if (isAllBackground) 24.dp else 12.dp,
                            end = 12.dp,
                            top = if (index == 0 || (index == 1 && entriesWithWords[0] == HEAD_LYRICS_ENTRY)) 0.dp else (lyricsLineSpacing * 8).dp,
                            bottom = (lyricsLineSpacing * 8).dp,
                        )
                        .graphicsLayer {
                            scaleX = animatedLineScale
                            scaleY = animatedLineScale
                            // RenderEffect blur requires API 31+; older devices simply keep
                            // the alpha/scale animation with no blur (graceful fallback).
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && animatedLineBlur > 0.05f) {
                                renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                    animatedLineBlur,
                                    animatedLineBlur,
                                    android.graphics.Shader.TileMode.CLAMP,
                                ).asComposeRenderEffect()
                            }
                        }
                        .alpha(animatedLineAlpha)
                        .combinedClickable(
                            enabled = true,
                            onClick = {
                                if (isSelectionModeActive) {
                                    if (isSelected) {
                                        selectedIndices.remove(index)
                                        if (selectedIndices.isEmpty()) {
                                            isSelectionModeActive = false
                                        }
                                    } else {
                                        if (selectedIndices.size < MAX_SELECTION_LIMIT) {
                                            selectedIndices.add(index)
                                        } else {
                                            showMaxSelectionToast = true
                                        }
                                    }
                                } else if (lyricsClick && isSynced && item.time > 0) {
                                    player.seekTo(item.time)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionModeActive) {
                                    isSelectionModeActive = true
                                    selectedIndices.add(index)
                                } else if (!isSelected && selectedIndices.size < MAX_SELECTION_LIMIT) {
                                    selectedIndices.add(index)
                                } else if (!isSelected) {
                                    showMaxSelectionToast = true
                                }
                            }
                        ),
                    horizontalAlignment = horizontalAlignment,
                ) {
                    if (item.words != null && isSynced) {
                        // ── Word-synced rendering ──
                        LyricsLineV2(
                            words = item.words!!,
                            isActive = isActive,
                            isPast = isPast,
                            currentPositionMs = currentPositionMs,
                            textColor = textColor,
                            inactiveAlpha = inactiveAlpha,
                            baseFontSize = lyricsTextSize,
                            isLineAllBackground = isAllBackground,
                            textAlign = textAlign,
                            lyricsFontFamily = lyricsFontFamily,
                        )
                    } else {
                        // ── Plain text rendering ──
                        val plainTextAlpha by animateFloatAsState(
                            targetValue = if (isActive) 1f else inactiveAlpha,
                            animationSpec = tween(durationMillis = LINE_ALPHA_TRANSITION_MS, easing = V2Easing),
                            label = "plainTextAlpha",
                        )
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = if (isAllBackground) (lyricsTextSize * 0.82f).sp else lyricsTextSize.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                fontStyle = if (isAllBackground) FontStyle.Italic else FontStyle.Normal,
                                lineHeight = (lyricsTextSize * lyricsLineSpacing).sp,
                                fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily,
                            ),
                            color = textColor.copy(alpha = plainTextAlpha),
                            textAlign = textAlign,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // ── Romanization ──
                    val romanizedText by item.romanizedTextFlow.collectAsState()
                    if (romanizedText != null) {
                        Text(
                            text = romanizedText!!,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (lyricsTextSize * 0.55f).sp,
                                lineHeight = (lyricsTextSize * 0.75f).sp,
                                fontWeight = FontWeight.Normal,
                                fontStyle = if (isAllBackground) FontStyle.Italic else FontStyle.Normal,
                                fontFamily = lyricsFontFamily ?: MaterialTheme.typography.bodyMedium.fontFamily,
                            ),
                            color = textColor.copy(alpha = if (isActive) 0.75f else inactiveAlpha * 0.7f),
                            textAlign = textAlign,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = (lyricsTextSize * 0.3f).dp),
                        )
                    }
                }
            }

            // Bottom spacer for overscroll
            item {
                Spacer(modifier = Modifier.height(300.dp))
            }
        }

        // ── Resume auto-scroll button ──
        if (isManualScrolling && isSynced) {
            androidx.compose.material3.FilledTonalButton(
                onClick = {
                    isManualScrolling = false
                    scope.launch {
                        listState.smoothScrollToActiveLine(currentLineIndex)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(
                    text = "Resume",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (isSelectionModeActive) {
            mediaMetadata?.let { metadata ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    isSelectionModeActive = false
                                    selectedIndices.clear()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.close),
                                contentDescription = stringResource(R.string.cancel),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .background(
                                    color = if (selectedIndices.isNotEmpty())
                                        Color.White.copy(alpha = 0.9f)
                                    else
                                        Color.White.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .clickable(enabled = selectedIndices.isNotEmpty()) {
                                    if (selectedIndices.isNotEmpty()) {
                                        val sortedIndices = selectedIndices.sorted()
                                        val selectedLyricsText = sortedIndices
                                            .mapNotNull { entriesWithWords.getOrNull(it)?.text }
                                            .joinToString("\n")

                                        if (selectedLyricsText.isNotBlank()) {
                                            shareDialogData = Triple(
                                                selectedLyricsText,
                                                metadata.title ?: "",
                                                metadata.artists.joinToString { it.name }
                                            )
                                            showShareCarouselSheet = true
                                        }
                                        isSelectionModeActive = false
                                        selectedIndices.clear()
                                    }
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.share),
                                contentDescription = stringResource(R.string.share_selected),
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.share),
                                color = Color.Black,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // ── Carrusel de estilos para compartir letras ──────────────────────────────
        if (showShareCarouselSheet) {
            shareDialogData?.let { (lyricText, title, artist) ->
                mediaMetadata?.let { metadata ->
                    LyricsShareCarouselSheet(
                        lyricText = lyricText,
                        mediaMetadata = metadata,
                        initialConfig = LyricsCardConfig(),
                        onDismiss = {
                            showShareCarouselSheet = false
                            shareDialogData = null
                        },
                        onShare = { config ->
                            showShareCarouselSheet = false
                            shareDialogData = null
                            showProgressDialog = true

                            scope.launch {
                                runCatching {
                                    val bitmap = ComposeToImage.createLyricsImageWithConfig(
                                        context = context,
                                        coverArtUrl = metadata.thumbnailUrl,
                                        songTitle = title,
                                        artistName = artist,
                                        lyrics = lyricText,
                                        config = config,
                                        outputSize = 1080,
                                    )

                                    val uri = ComposeToImage.saveBitmapAsFile(
                                        context = context,
                                        bitmap = bitmap,
                                        fileName = "lyrics_${System.currentTimeMillis()}",
                                    )

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }

                                    context.startActivity(
                                        Intent.createChooser(shareIntent, null)
                                    )
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        "No se pudo compartir la imagen",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                showProgressDialog = false
                            }
                        },
                        onSave = { config ->
                            showProgressDialog = true

                            scope.launch {
                                runCatching {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        val bitmap = ComposeToImage.createLyricsImageWithConfig(
                                            context = context,
                                            coverArtUrl = metadata.thumbnailUrl,
                                            songTitle = title,
                                            artistName = artist,
                                            lyrics = lyricText,
                                            config = config,
                                            outputSize = 1080,
                                        )

                                        ComposeToImage.saveBitmapAsFile(
                                            context = context,
                                            bitmap = bitmap,
                                            fileName = "lyrics_saved_${System.currentTimeMillis()}",
                                        )
                                    }
                                }.onSuccess {
                                    Toast.makeText(
                                        context,
                                        "Imagen guardada",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        "No se pudo guardar la imagen",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                showProgressDialog = false
                            }

                            showShareCarouselSheet = false
                            shareDialogData = null
                        },
                    )
                }
            }
        }
    }

    // ── Progress indicator while an image is being generated/shared/saved ──
    if (showProgressDialog) {
        BasicAlertDialog(onDismissRequest = {}) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.padding(32.dp)) {
                    Text(
                        text = stringResource(R.string.generating_image) + "\n" + stringResource(R.string.please_wait),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Smoothly scrolls this list so [index] lands near the top of the viewport
 * (biased by [SCROLL_TARGET_BIAS]). If the target is far outside the current
 * viewport, it first snaps close to it to avoid a long, janky scroll animation.
 */
private suspend fun LazyListState.smoothScrollToActiveLine(index: Int) {
    val viewportHeight = layoutInfo.viewportSize.height
    val targetOffset = (viewportHeight * SCROLL_TARGET_BIAS).toInt()

    if (abs(index - firstVisibleItemIndex) > SCROLL_SNAP_DISTANCE_THRESHOLD) {
        scrollToItem((index - 2).coerceAtLeast(0), 0)
    }
    animateScrollToItem(index = index, scrollOffset = -targetOffset)
}


// ──────────────────────────────────────────────────────────────────────
// Line-level composable: renders words with fluid fill animation
// ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricsLineV2(
    words: List<WordTimestamp>,
    isActive: Boolean,
    isPast: Boolean,
    currentPositionMs: Long,
    textColor: Color,
    inactiveAlpha: Float,
    baseFontSize: Float,
    isLineAllBackground: Boolean,
    textAlign: TextAlign,
    lyricsFontFamily: FontFamily?,
) {
    val arrangement = when (textAlign) {
        TextAlign.Center -> Arrangement.Center
        TextAlign.End -> Arrangement.End
        else -> Arrangement.Start
    }

    // Split words into main and background
    val mainWords = words.filter { !it.isBackground }
    val bgWords = words.filter { it.isBackground }

    // 1. Render main words first (if any)
    if (mainWords.isNotEmpty()) {
        WordsRow(
            words = mainWords,
            startIndex = 0,
            arrangement = arrangement,
            isActive = isActive,
            isPast = isPast,
            currentPositionMs = currentPositionMs,
            textColor = textColor,
            inactiveAlpha = inactiveAlpha,
            fontSize = if (isLineAllBackground) baseFontSize * 0.82f else baseFontSize,
            isBackground = isLineAllBackground,
            lyricsFontFamily = lyricsFontFamily,
        )
    }

    // 2. Render background words explicitly on a new line, noticeably smaller
    if (bgWords.isNotEmpty()) {
        if (mainWords.isNotEmpty()) Spacer(modifier = Modifier.height(4.dp))
        WordsRow(
            words = bgWords,
            startIndex = mainWords.size,
            arrangement = arrangement,
            isActive = isActive,
            isPast = isPast,
            currentPositionMs = currentPositionMs,
            textColor = textColor,
            inactiveAlpha = inactiveAlpha,
            fontSize = baseFontSize * 0.65f, // ~65% size of main text
            isBackground = true, // Force dimmer styling inside AnimatedWordV2
            lyricsFontFamily = lyricsFontFamily,
        )
    }
}

/**
 * Renders a single flow-row of words (either the main vocal line or the
 * background-vocal line), sharing the same layout/space/newline handling
 * so the two call sites in [LyricsLineV2] don't duplicate logic.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordsRow(
    words: List<WordTimestamp>,
    startIndex: Int,
    arrangement: Arrangement.Horizontal,
    isActive: Boolean,
    isPast: Boolean,
    currentPositionMs: Long,
    textColor: Color,
    inactiveAlpha: Float,
    fontSize: Float,
    isBackground: Boolean,
    lyricsFontFamily: FontFamily?,
) {
    FlowRow(
        modifier = if (isBackground) {
            Modifier.fillMaxWidth().alpha(0.85f) // Slightly dimmer overall
        } else {
            Modifier.fillMaxWidth()
        },
        horizontalArrangement = arrangement,
    ) {
        words.forEachIndexed { wordIndex, word ->
            when (word.text) {
                " " -> Text(
                    text = " ",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = fontSize.sp,
                        fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily,
                    ),
                    color = Color.Transparent,
                )

                "\n" -> Spacer(modifier = Modifier.fillMaxWidth())

                else -> AnimatedWordV2(
                    word = word,
                    wordIndex = startIndex + wordIndex,
                    isLineActive = isActive,
                    isLinePast = isPast,
                    currentPositionMs = currentPositionMs,
                    textColor = textColor,
                    inactiveAlpha = inactiveAlpha,
                    fontSize = fontSize,
                    isBackground = isBackground,
                    lyricsFontFamily = lyricsFontFamily,
                )
            }
        }
    }
}


// ──────────────────────────────────────────────────────────────────────
// Word-level composable: liquid fill sweep + glow + bounce
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedWordV2(
    word: WordTimestamp,
    wordIndex: Int,
    isLineActive: Boolean,
    isLinePast: Boolean,
    currentPositionMs: Long,
    textColor: Color,
    inactiveAlpha: Float,
    fontSize: Float,
    isBackground: Boolean,
    lyricsFontFamily: FontFamily?,
) {
    val wordStartMs = (word.startTime * 1000).toLong()
    val wordEndMs = (word.endTime * 1000).toLong()
    val wordDuration = (wordEndMs - wordStartMs).coerceAtLeast(1L)

    val isWordComplete = currentPositionMs >= wordEndMs
    val isWordActive = currentPositionMs in wordStartMs until wordEndMs

    // Perfect linear progress [0..1] that matches individual word timings
    val progress = when {
        isWordComplete -> 1f
        currentPositionMs <= wordStartMs -> 0f
        else -> ((currentPositionMs - wordStartMs).toFloat() / wordDuration).coerceIn(0f, 1f)
    }

    // ── Bounce and Float animation ──
    // Subtle scale up peaking halfway through the word. Exact timing sync!
    val sinProgress = kotlin.math.sin(progress * kotlin.math.PI).toFloat()
    val wordScale = 1f + (0.015f * sinProgress)

    // Float is only applied when the word is actively sung, making it pop from the line.
    // We use animateFloatAsState so that when it finishes (and drops to 0f),
    // it smoothly decays back into place rather than a harsh mathematical snap.
    val targetFloat = if (isWordActive) -4f * sinProgress else 0f
    val floatOffset by animateFloatAsState(
        targetValue = targetFloat,
        animationSpec = tween(
            durationMillis = if (isWordActive) 50 else 350,
            easing = FastOutSlowInEasing
        )
    )

    // ── Glow intensity ──
    // Make glow build up faster: reach max intensity at 50% progress.
    val glowProgress = (progress * 2f).coerceAtMost(1f)
    val targetGlowAlpha = if (isWordActive) glowProgress * 0.45f else 0f
    val targetGlowRadius = if (isWordActive) glowProgress * 14f else 0f

    // Ease the glow in fast but let it fade out slowly ("afterglow") instead of
    // cutting instantly the moment a word finishes — a softer, more elegant shimmer.
    val glowAlpha by animateFloatAsState(
        targetValue = targetGlowAlpha,
        animationSpec = tween(
            durationMillis = if (isWordActive) GLOW_ATTACK_MS else GLOW_DECAY_MS,
            easing = FastOutSlowInEasing,
        ),
        label = "glowAlpha",
    )
    val glowRadius by animateFloatAsState(
        targetValue = targetGlowRadius,
        animationSpec = tween(
            durationMillis = if (isWordActive) GLOW_ATTACK_MS else GLOW_DECAY_MS,
            easing = FastOutSlowInEasing,
        ),
        label = "glowRadius",
    )

    val actualFontSize = if (isBackground) fontSize * 0.85f else fontSize
    val fontWeight = FontWeight.SemiBold // Consistent weight — no thin→bold jump

    // ── Two-layer rendering: dim base + liquid fill overlay ──
    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = floatOffset * density
                scaleX = wordScale
                scaleY = wordScale
            }
    ) {
        // Layer 1: Base text (always dimmed)
        Text(
            text = word.text,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = actualFontSize.sp,
                fontWeight = fontWeight,
                fontStyle = FontStyle.Normal,
                lineHeight = (actualFontSize * 1.35f).sp,
                fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily,
            ),
            color = textColor.copy(alpha = if (isBackground) inactiveAlpha * 0.7f else inactiveAlpha),
        )

        // Layer 2: Filled overlay with liquid sweep mask + glow
        if (isWordComplete || isWordActive || isLinePast) {
            Text(
                text = word.text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = actualFontSize.sp,
                    fontWeight = fontWeight,
                    fontStyle = FontStyle.Normal,
                    lineHeight = (actualFontSize * 1.35f).sp,
                    fontFamily = lyricsFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily,
                    shadow = if (glowAlpha > 0f) {
                        Shadow(
                            color = textColor.copy(alpha = glowAlpha),
                            offset = Offset.Zero,
                            blurRadius = glowRadius.coerceAtLeast(1f),
                        )
                    } else null,
                ),
                color = textColor.copy(
                    alpha = if (isBackground) 0.75f else 1f
                ),
                modifier = if (isWordActive && !isWordComplete) {
                    Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val edgeWidth = 11.dp.toPx() // wider edge = softer, more liquid sweep
                            val center = (size.width + edgeWidth * 2) * progress - edgeWidth
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Black, Color.Transparent),
                                    startX = center - edgeWidth,
                                    endX = center + edgeWidth,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                } else {
                    Modifier
                }
            )
        }
    }
}