package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AppBarHeight
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidBackdrop
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidEffect
import com.arturo254.opentune.db.entities.Song
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Measured from the available window, including split-screen and display scaling. */
internal data class OrbitLayout(val columns: Int, val artworkDp: Float, val sideBySide: Boolean)

internal fun orbitLayout(width: Float, height: Float, fontScale: Float): OrbitLayout {
    val usable = width.coerceAtLeast(0f)
    val scaled = usable / fontScale.coerceAtLeast(1f)
    return OrbitLayout(
        columns = (scaled / 320f).toInt().coerceIn(1, 3),
        artworkDp = minOf(usable * .64f, height * .34f, 260f).coerceAtLeast(0f),
        sideBySide = scaled >= 600f,
    )
}

/** A reusable edge-to-edge scene: only content receives safe insets, never the sky. */
@Composable
internal fun OrbitScreenFrame(
    active: Boolean,
    content: @Composable (OrbitLayout, PaddingValues) -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    // The legacy player host reserves a toolbar even when the page owns its header.
    val topInset =
        (insets.calculateTopPadding() - AppBarHeight).coerceAtLeast(
            WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
        )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val direction = androidx.compose.ui.platform.LocalLayoutDirection.current
        val left = insets.calculateLeftPadding(direction)
        val right = insets.calculateRightPadding(direction)
        val width = (maxWidth - left - right).coerceAtLeast(0.dp)
        val height = (maxHeight - topInset - insets.calculateBottomPadding()).coerceAtLeast(0.dp)
        val layout =
            orbitLayout(minOf(width.value, 1120f), height.value, LocalDensity.current.fontScale)
        // The sampled layer contains ONLY the sky, never a glass consumer.
        OrbitSky(active, Modifier.matchParentSize().layerBackdrop(backdrop))
        CompositionLocalProvider(LocalGalaxLiquidBackdrop provides backdrop) {
            Box(
                Modifier.fillMaxSize()
                    .padding(
                        start = insets.calculateStartPadding(direction),
                        end = insets.calculateEndPadding(direction),
                    ),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(Modifier.widthIn(max = 1120.dp).fillMaxSize()) {
                    content(
                        layout,
                        PaddingValues(
                            top = topInset,
                            bottom = insets.calculateBottomPadding() + 24.dp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun OrbitSky(active: Boolean, modifier: Modifier) {
    val phase = rememberNovaPhase(active)
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < .4f
    val violet = if (dark) Color(0xFFA993FF) else Color(0xFF6850B8)
    val cyan = if (dark) Color(0xFF78DDE5) else Color(0xFF257480)
    Canvas(modifier.background(colors.background)) {
        if (size.minDimension <= 0f) return@Canvas
        val time = phase.value * 2f * PI.toFloat()
        val focus = Offset(size.width * .72f, size.height * .24f)
        drawRect(
            Brush.radialGradient(
                listOf(violet.copy(alpha = .19f), Color.Transparent),
                focus,
                size.maxDimension * .67f,
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(cyan.copy(alpha = .10f), Color.Transparent),
                Offset(size.width * .08f, size.height * .55f),
                size.maxDimension * .5f,
            )
        )
        repeat(3) { index ->
            val radius = size.minDimension * (.32f + index * .15f)
            drawOval(
                violet.copy(alpha = .08f),
                focus - Offset(radius, radius * .52f),
                Size(radius * 2, radius * 1.04f),
                style = Stroke(.7.dp.toPx()),
            )
        }
        repeat(52) { index ->
            val x = ((index * 71 + 19) % 997) / 997f * size.width
            val y = ((index * 137 + 53) % 991) / 991f * size.height
            val center =
                Offset(x + sin(time + index) * 3.dp.toPx(), y + cos(time + index) * 2.dp.toPx())
            val alpha = .12f + .18f * (.5f + .5f * sin(time + index * .7f))
            val tint = if (index % 3 == 0) cyan else violet
            drawCircle(
                tint.copy(alpha = alpha),
                if (index % 7 == 0) 1.4.dp.toPx() else .7.dp.toPx(),
                center,
            )
            if (index % 13 == 0) {
                drawLine(
                    tint.copy(alpha = alpha * .5f),
                    center - Offset(4.dp.toPx(), 0f),
                    center + Offset(4.dp.toPx(), 0f),
                    .6.dp.toPx(),
                )
                drawLine(
                    tint.copy(alpha = alpha * .5f),
                    center - Offset(0f, 4.dp.toPx()),
                    center + Offset(0f, 4.dp.toPx()),
                    .6.dp.toPx(),
                )
            }
        }
    }
}

@Composable
internal fun OrbitMasthead(account: String?, onNavigate: (String) -> Unit, onSearch: () -> Unit) {
    var accountMenuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                "GALAXOT",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 3.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.home),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        OrbitIcon(R.drawable.search, stringResource(R.string.orbit_search), onSearch)
        Box(
            Modifier.size(48.dp)
                .galaxLiquidEffect(shapeOverride = RoundedCornerShape(50))
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.orbit_account),
                    onClick = { accountMenuOpen = true },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (account.isNullOrBlank())
                Icon(
                    painterResource(R.drawable.person),
                    stringResource(R.string.orbit_account),
                    Modifier.size(22.dp),
                )
            else NovaArtwork(account, Modifier.size(36.dp), circular = true)
            DropdownMenu(
                expanded = accountMenuOpen,
                onDismissRequest = { accountMenuOpen = false },
            ) {
                listOf(
                        R.string.orbit_account to "account",
                        R.string.history to "history",
                        R.string.stats to "stats",
                        R.string.new_release_albums to "new_release",
                        R.string.settings to "settings",
                    )
                    .forEach { (label, route) ->
                        DropdownMenuItem(
                            text = { Text(stringResource(label)) },
                            onClick = {
                                accountMenuOpen = false
                                onNavigate(route)
                            },
                        )
                    }
            }
        }
    }
}

@Composable
internal fun OrbitIcon(icon: Int, label: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).galaxLiquidEffect(shapeOverride = RoundedCornerShape(50)),
    ) {
        Icon(painterResource(icon), label, Modifier.size(21.dp))
    }
}

@Composable
internal fun OrbitOpening(
    song: Song?,
    layout: OrbitLayout,
    playing: Boolean,
    onPlay: () -> Unit,
    onMenu: () -> Unit,
) {
    val record: @Composable () -> Unit = { OrbitRecord(song?.thumbnailUrl, layout.artworkDp.dp) }
    val details: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.orbit_daily),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.sp,
            )
            Text(
                song?.title ?: stringResource(R.string.orbit_welcome),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-1).sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song?.artists?.joinToString { it.name }
                    ?: stringResource(R.string.orbit_welcome_detail),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f, fill = false).heightIn(min = 48.dp),
                ) {
                    Icon(
                        painterResource(if (playing) R.drawable.pause else R.drawable.play),
                        null,
                        Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(
                            if (song == null) R.string.orbit_explore
                            else if (playing) R.string.orbit_pause else R.string.orbit_listen
                        )
                    )
                }
                if (song != null)
                    OrbitIcon(R.drawable.more_vert, stringResource(R.string.orbit_options), onMenu)
            }
        }
    }
    // Natural text height keeps large fonts and short windows scrollable.
    if (layout.sideBySide)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            Box(Modifier.weight(1f)) { details() }
            record()
        }
    else
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { record() }
            details()
        }
}

@Composable
private fun OrbitRecord(artwork: String?, diameter: Dp) {
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            if (size.minDimension <= 0f) return@Canvas
            drawCircle(
                Brush.radialGradient(listOf(accent.copy(alpha = .22f), Color.Transparent)),
                size.minDimension / 2,
            )
            drawCircle(
                accent.copy(alpha = .24f),
                size.minDimension * .47f,
                style = Stroke(1.dp.toPx()),
            )
            drawCircle(
                accent.copy(alpha = .10f),
                size.minDimension * .41f,
                style = Stroke(.7.dp.toPx()),
            )
            drawCircle(accent, 3.dp.toPx(), Offset(size.width * .83f, size.height * .17f))
        }
        NovaArtwork(artwork, Modifier.fillMaxSize(.72f), circular = true)
        Box(
            Modifier.size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .padding(5.dp)
                .background(accent.copy(alpha = .55f), CircleShape)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OrbitActions(
    shuffling: Boolean,
    enabled: Boolean,
    onShuffle: () -> Unit,
    onLibrary: () -> Unit,
    onRecognize: () -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OrbitAction(
            R.drawable.shuffle,
            stringResource(R.string.orbit_shuffle),
            onShuffle,
            enabled && !shuffling,
            shuffling,
        )
        OrbitAction(R.drawable.music_note, stringResource(R.string.orbit_library), onLibrary)
        OrbitAction(R.drawable.graphic_eq, stringResource(R.string.orbit_recognize), onRecognize)
    }
}

@Composable
private fun OrbitAction(
    icon: Int,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    busy: Boolean = false,
) {
    Row(
        Modifier.galaxLiquidEffect(shapeOverride = RoundedCornerShape(50))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        else Icon(painterResource(icon), null, Modifier.size(18.dp))
        Text(title, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun OrbitShelfLayout(content: @Composable (Dp) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        val available = (maxWidth - 48.dp).coerceAtLeast(1.dp)
        val count = (available.value / (152f * fontScale)).toInt().coerceIn(2, 6)
        content(((available - 16.dp * (count - 1)) / count).coerceIn(72.dp, 220.dp))
    }
}
