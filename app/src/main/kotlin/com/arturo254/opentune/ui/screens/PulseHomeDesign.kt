package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AppBarHeight
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidBackdrop
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidEffect
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.sin

internal data class PulseLayout(val columns: Int, val coverWidth: Dp, val compactHeight: Boolean)

internal fun pulseLayout(width: Float, height: Float, fontScale: Float): PulseLayout {
    val available = (width - 40f).coerceAtLeast(0f)
    val scale = fontScale.coerceAtLeast(1f)
    return PulseLayout(
        columns = (available / (300f * scale)).toInt().coerceIn(1, 3),
        coverWidth = (available / if (available < 500f) 2.25f else 4.25f).coerceIn(80f, 210f).dp,
        compactHeight = height < 480f,
    )
}

/** Full-window atmosphere; inset and constrain only the foreground. */
@Composable
internal fun PulseScreenFrame(
    active: Boolean,
    content: @Composable (PulseLayout, PaddingValues) -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    val inherited = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val top =
        (inherited.calculateTopPadding() - AppBarHeight).coerceAtLeast(
            WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
        )
    val direction = LocalLayoutDirection.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontal =
            inherited.calculateLeftPadding(direction) + inherited.calculateRightPadding(direction)
        val width = (maxWidth - horizontal).coerceIn(0.dp, 1120.dp)
        val height = (maxHeight - top - inherited.calculateBottomPadding()).coerceAtLeast(0.dp)
        val layout = pulseLayout(width.value, height.value, LocalDensity.current.fontScale)
        // A glass consumer must never be part of its own recorded source.
        PulseAtmosphere(active, Modifier.matchParentSize().layerBackdrop(backdrop))
        CompositionLocalProvider(LocalGalaxLiquidBackdrop provides backdrop) {
            Box(
                Modifier.fillMaxSize()
                    .padding(
                        start = inherited.calculateStartPadding(direction),
                        end = inherited.calculateEndPadding(direction),
                    ),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(Modifier.widthIn(max = 1120.dp).fillMaxSize()) {
                    content(
                        layout,
                        PaddingValues(
                            top = top,
                            bottom = inherited.calculateBottomPadding() + 32.dp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PulseAtmosphere(active: Boolean, modifier: Modifier) {
    val phase = rememberNovaPhase(active)
    val colors = MaterialTheme.colorScheme
    Canvas(modifier.background(colors.background)) {
        if (size.minDimension <= 0f) return@Canvas
        val t = phase.value * 6.28318f
        val focus = Offset(size.width * .95f, size.height * .15f)
        drawRect(
            Brush.radialGradient(
                listOf(colors.secondary.copy(alpha = .14f), Color.Transparent),
                focus,
                size.maxDimension * .75f,
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(colors.primary.copy(alpha = .10f), Color.Transparent),
                Offset(0f, size.height * .55f),
                size.maxDimension * .6f,
            )
        )
        repeat(5) { index ->
            val drift = sin(t + index * .2f) * 8.dp.toPx()
            val path =
                Path().apply {
                    moveTo(-size.width * .2f, size.height * .15f + index * 15.dp.toPx())
                    cubicTo(
                        size.width * .3f,
                        size.height * .45f + drift,
                        size.width * .45f,
                        -size.height * .15f,
                        size.width * 1.2f,
                        size.height * .3f + index * 24.dp.toPx(),
                    )
                }
            drawPath(
                path,
                colors.primary.copy(alpha = .025f + index * .008f),
                style = Stroke((1f + index * .4f).dp.toPx()),
            )
        }
        repeat(30) { index ->
            val x = ((index * 83 + 31) % 997) / 997f * size.width
            val y = ((index * 173 + 47) % 991) / 991f * size.height
            val alpha = .10f + .10f * (.5f + .5f * sin(t + index))
            drawCircle(
                colors.primary.copy(alpha = alpha),
                if (index % 6 == 0) 1.3.dp.toPx() else .7.dp.toPx(),
                Offset(x + sin(t + index) * 3.dp.toPx(), y),
            )
        }
        // Feather the atmosphere into the window background beside the transparent navigation rail.
        drawRect(
            Brush.horizontalGradient(
                0f to colors.background,
                .08f to Color.Transparent,
                .92f to Color.Transparent,
                1f to colors.background,
            )
        )
    }
}

@Composable
internal fun PulseHeader(account: String?, onNavigate: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "GALAXOT",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.8.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.pulse_for_you),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Box {
            IconButton(
                onClick = { open = true },
                modifier =
                    Modifier.size(48.dp)
                        .galaxLiquidEffect(shapeOverride = RoundedCornerShape(18.dp)),
            ) {
                if (account.isNullOrBlank())
                    Icon(
                        painterResource(R.drawable.person),
                        stringResource(R.string.account),
                        Modifier.size(23.dp),
                    )
                else
                    AsyncImage(
                        account,
                        stringResource(R.string.account),
                        Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
            }
            DropdownMenu(open, onDismissRequest = { open = false }) {
                listOf(
                        R.string.account to "account",
                        R.string.history to "history",
                        R.string.stats to "stats",
                        R.string.new_release_albums to "new_release",
                        R.string.settings to "settings",
                    )
                    .forEach { (label, route) ->
                        DropdownMenuItem(
                            text = { Text(stringResource(label)) },
                            onClick = {
                                open = false
                                onNavigate(route)
                            },
                        )
                    }
            }
        }
    }
}

@Composable
internal fun PulseSearch(onSearch: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 24.dp)
            .fillMaxWidth()
            .galaxLiquidEffect(shapeOverride = RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onSearch)
            .heightIn(min = 54.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painterResource(R.drawable.search),
            null,
            Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.pulse_search),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun PulseFeature(
    title: String?,
    subtitle: String?,
    artwork: String?,
    playing: Boolean,
    onPlay: () -> Unit,
    onMenu: (() -> Unit)?,
    compactHeight: Boolean = false,
) {
    val scale = LocalDensity.current.fontScale
    BoxWithConstraints(
        Modifier.padding(horizontal = 24.dp)
            .fillMaxWidth()
            .galaxLiquidEffect(shapeOverride = RoundedCornerShape(28.dp))
    ) {
        val split = maxWidth / scale >= 310.dp
        val artSize = if (compactHeight) 100.dp else if (maxWidth >= 700.dp) 200.dp else 126.dp
        val art: @Composable () -> Unit = {
            Box(Modifier.size(artSize).clip(RoundedCornerShape(22.dp))) {
                PulseCoverArt(artwork, Modifier.fillMaxSize())
                if (onMenu != null)
                    IconButton(
                        onClick = onMenu,
                        modifier =
                            Modifier.align(Alignment.TopEnd)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = .85f),
                                    RoundedCornerShape(bottomStart = 16.dp),
                                ),
                    ) {
                        Icon(
                            painterResource(R.drawable.more_vert),
                            stringResource(R.string.pulse_options),
                        )
                    }
            }
        }
        val copy: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    stringResource(
                        if (title == null) R.string.pulse_start else R.string.pulse_spotlight
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    title ?: stringResource(R.string.pulse_welcome),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle ?: stringResource(R.string.pulse_welcome_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = onPlay,
                        modifier = Modifier.weight(1f, fill = false).heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            painterResource(if (playing) R.drawable.pause else R.drawable.play),
                            null,
                            Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(
                                if (title == null) R.string.pulse_explore
                                else if (playing) R.string.pulse_pause else R.string.pulse_play
                            )
                        )
                    }
                }
            }
        }
        if (split)
            Row(
                Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                art()
                Box(Modifier.weight(1f)) { copy() }
            }
        else
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Small windows and enlarged text share a compact horizontal artwork strip.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))) {
                        PulseCoverArt(artwork, Modifier.fillMaxSize())
                    }
                    Text(
                        stringResource(R.string.pulse_daily),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    if (onMenu != null)
                        IconButton(onClick = onMenu) {
                            Icon(
                                painterResource(R.drawable.more_vert),
                                stringResource(R.string.pulse_options),
                            )
                        }
                }
                copy()
            }
    }
}

@Composable
internal fun PulseCoverArt(url: String?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.background(
            Brush.linearGradient(
                listOf(
                    colors.primaryContainer,
                    colors.secondaryContainer,
                    colors.surfaceContainerHigh,
                )
            )
        )
    ) {
        Canvas(Modifier.matchParentSize()) {
            if (size.minDimension <= 0f) return@Canvas
            repeat(9) { index ->
                val x = size.width * (.18f + index * .08f)
                val h = size.height * (.15f + .45f * kotlin.math.abs(sin(index * .7f)))
                drawLine(
                    colors.onPrimaryContainer.copy(alpha = .25f + index * .025f),
                    Offset(x, (size.height - h) / 2),
                    Offset(x, (size.height + h) / 2),
                    size.width * .025f,
                    StrokeCap.Round,
                )
            }
        }
        if (!url.isNullOrBlank())
            AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PulseShortcuts(
    shuffling: Boolean,
    enabled: Boolean,
    onShuffle: () -> Unit,
    onLibrary: () -> Unit,
    onRecognize: () -> Unit,
) {
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        val columns = if (maxWidth / scale >= 280.dp) 3 else 2
        val tile = Modifier.width((maxWidth - 8.dp * (columns - 1)) / columns)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PulseShortcut(
                R.drawable.shuffle,
                stringResource(R.string.pulse_shuffle),
                onShuffle,
                tile,
                enabled && !shuffling,
                shuffling,
            )
            PulseShortcut(
                R.drawable.music_note,
                stringResource(R.string.pulse_library),
                onLibrary,
                tile,
            )
            PulseShortcut(
                R.drawable.graphic_eq,
                stringResource(R.string.pulse_recognize),
                onRecognize,
                tile,
            )
        }
    }
}

@Composable
private fun PulseShortcut(
    icon: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .65f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically),
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        else
            Icon(
                painterResource(icon),
                null,
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color =
                MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled || busy) 1f else .4f),
        )
    }
}

@Composable
internal fun PulseHeading(title: String, detail: String? = null) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (!detail.isNullOrBlank())
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
internal fun PulseShelfLayout(content: @Composable (Dp) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        content(pulseLayout(maxWidth.value, 700f, LocalDensity.current.fontScale).coverWidth)
    }
}
