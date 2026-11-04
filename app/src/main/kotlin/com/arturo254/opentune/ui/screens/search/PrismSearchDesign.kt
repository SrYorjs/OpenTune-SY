package com.arturo254.opentune.ui.screens.search

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
import com.arturo254.opentune.constants.PureBlackKey
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidBackdrop
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidEffect
import com.arturo254.opentune.ui.screens.rememberNovaPhase
import com.arturo254.opentune.utils.rememberPreference
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.sin

internal data class PrismLayout(val columns: Int, val tileWidth: Dp, val compactHeight: Boolean)

internal fun prismLayout(width: Float, height: Float, fontScale: Float): PrismLayout {
    val available = (width.coerceIn(0f, 1120f) - 48f).coerceAtLeast(0f)
    val columns =
        ((available + 12f) / (150f * fontScale.coerceAtLeast(1f) + 12f)).toInt().coerceIn(1, 4)
    return PrismLayout(
        columns,
        ((available - 12f * (columns - 1)) / columns).coerceAtLeast(0f).dp,
        height < 480f,
    )
}

/** Only the atmosphere is recorded. No refracting control is in its own source. */
internal val LocalPrismBackdrop = staticCompositionLocalOf<Backdrop?> { null }
private val LocalPrismCompact = staticCompositionLocalOf { false }
internal val LocalPrismSceneActive = staticCompositionLocalOf { true }

@Composable
internal fun PrismScreenFrame(
    active: Boolean = true,
    reserveAppBar: Boolean = false,
    pureBlack: Boolean = false,
    insetTop: Boolean = true,
    content: @Composable (PrismLayout, PaddingValues) -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    val shared = LocalPrismBackdrop.current
    val inherited = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val direction = LocalLayoutDirection.current
    val top =
        if (!insetTop) 0.dp
        else
            (inherited.calculateTopPadding() - if (reserveAppBar) 0.dp else AppBarHeight)
                .coerceAtLeast(WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding())
    val (blackPreference) = rememberPreference(PureBlackKey, false)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontal =
            inherited.calculateLeftPadding(direction) + inherited.calculateRightPadding(direction)
        val width = (maxWidth - horizontal).coerceIn(0.dp, 1120.dp)
        val layout =
            prismLayout(
                width.value,
                (maxHeight - top - inherited.calculateBottomPadding()).value,
                LocalDensity.current.fontScale,
            )
        if (shared == null)
            PrismAtmosphere(
                active && LocalPrismSceneActive.current,
                pureBlack || blackPreference,
                Modifier.matchParentSize().layerBackdrop(backdrop),
            )
        CompositionLocalProvider(
            LocalGalaxLiquidBackdrop provides (shared ?: backdrop),
            LocalPrismCompact provides layout.compactHeight,
        ) {
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
                            top = top + if (insetTop) 12.dp else 0.dp,
                            bottom = inherited.calculateBottomPadding() + 24.dp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PrismAtmosphere(active: Boolean, pureBlack: Boolean, modifier: Modifier) {
    val phase = rememberNovaPhase(active)
    val colors = MaterialTheme.colorScheme
    val base = if (pureBlack) Color.Black else colors.background
    val violet = lerp(colors.tertiary, Color(0xFFAD8BFA), .45f)
    val mint = lerp(colors.secondary, Color(0xFF6EDBC6), .45f)
    Canvas(modifier.background(base)) {
        if (size.minDimension <= 0f) return@Canvas
        val t = phase.value * 6.28318f
        drawRect(
            Brush.radialGradient(
                listOf(violet.copy(alpha = .15f), Color.Transparent),
                Offset(size.width * .78f, size.height * .14f),
                size.maxDimension * .65f,
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(mint.copy(alpha = .07f), Color.Transparent),
                Offset(size.width * .25f, size.height * .65f),
                size.maxDimension * .6f,
            )
        )
        repeat(4) { i ->
            val x = size.width * (.50f + i * .12f)
            val path =
                Path().apply {
                    moveTo(x, -size.height * .1f)
                    lineTo(x - size.width * .32f, size.height * .45f)
                    lineTo(x + size.width * .45f, size.height * 1.1f)
                }
            drawPath(path, violet.copy(alpha = .035f + .008f * i), style = Stroke(1.dp.toPx()))
        }
        repeat(32) { i ->
            val x = ((i * 179 + 29) % 991) / 991f * size.width
            val y = ((i * 89 + 113) % 997) / 997f * size.height
            val p = Offset(x + sin(t + i) * 2.dp.toPx(), y + sin(t * .5f + i) * 3.dp.toPx())
            drawCircle(
                if (i % 3 == 0) mint.copy(alpha = .22f)
                else violet.copy(alpha = .15f + .05f * sin(t + i)),
                if (i % 7 == 0) 1.7.dp.toPx() else .8.dp.toPx(),
                p,
            )
        }
        drawRect(
            Brush.horizontalGradient(
                0f to base,
                .07f to Color.Transparent,
                .93f to Color.Transparent,
                1f to base,
            )
        )
    }
}

@Composable
internal fun PrismDiscoveryMasthead(compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp)) {
        val showCrystal = maxWidth / LocalDensity.current.fontScale >= 260.dp
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 7.dp),
            ) {
                Text(
                    stringResource(R.string.prism_search_label),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.7.sp,
                    color = colors.tertiary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.prism_explore),
                    style =
                        if (compact) MaterialTheme.typography.headlineLarge
                        else MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.prism_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            if (showCrystal)
                Canvas(Modifier.size(if (compact) 68.dp else 88.dp)) {
                    val violet = lerp(colors.tertiary, Color(0xFFBD8AFF), .55f)
                    val mint = lerp(colors.secondary, Color(0xFF79E4CC), .6f)
                    fun point(x: Float, y: Float) = Offset(size.width * x, size.height * y)
                    fun facet(a: Offset, b: Offset, c: Offset, color: Color) {
                        val path =
                            Path().apply {
                                moveTo(a.x, a.y)
                                lineTo(b.x, b.y)
                                lineTo(c.x, c.y)
                                close()
                            }
                        drawPath(
                            path,
                            Brush.linearGradient(listOf(color, color.copy(alpha = .40f)), a, c),
                        )
                        drawPath(
                            path,
                            colors.onSurface.copy(alpha = .25f),
                            style = Stroke(.65.dp.toPx()),
                        )
                    }
                    val peak = point(.64f, .07f)
                    val left = point(.10f, .56f)
                    val center = point(.54f, .57f)
                    val right = point(.90f, .43f)
                    val foot = point(.38f, .95f)
                    drawCircle(
                        Brush.radialGradient(
                            listOf(violet.copy(alpha = .2f), Color.Transparent),
                            point(.5f, .52f),
                            size.minDimension * .51f,
                        ),
                        size.minDimension * .51f,
                        point(.5f, .52f),
                    )
                    facet(peak, left, center, violet)
                    facet(peak, center, right, mint)
                    facet(left, foot, center, lerp(violet, colors.background, .38f))
                    facet(center, foot, right, lerp(mint, violet, .55f))
                    drawLine(colors.onSurface.copy(alpha = .75f), peak, center, 1.dp.toPx())
                }
        }
    }
}

@Composable
internal fun PrismHeading(
    title: String,
    subtitle: String? = null,
    eyebrow: String? = null,
    onBack: (() -> Unit)? = null,
) {
    val compact = LocalPrismCompact.current
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = if (compact) 4.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
    ) {
        if (onBack != null)
            IconButton(
                onClick = onBack,
                modifier =
                    Modifier.size(48.dp)
                        .galaxLiquidEffect(shapeOverride = RoundedCornerShape(16.dp)),
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    stringResource(R.string.back_button_desc),
                )
            }
        if (eyebrow != null)
            Text(
                eyebrow,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
            )
        Text(
            title,
            style =
                if (compact) MaterialTheme.typography.headlineMedium
                else MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        if (!subtitle.isNullOrBlank())
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
internal fun PrismSectionTitle(title: String, detail: String? = null) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (!detail.isNullOrBlank())
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
internal fun PrismStatus(
    title: String,
    detail: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (!detail.isNullOrBlank())
            Text(
                detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        if (actionLabel != null && onAction != null)
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
internal fun PrismArtwork(url: String?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.background(
            Brush.linearGradient(listOf(colors.tertiaryContainer, colors.secondaryContainer))
        )
    ) {
        Canvas(Modifier.matchParentSize()) {
            repeat(4) { i ->
                val offset = size.width * (.1f + i * .22f)
                drawLine(
                    colors.onTertiaryContainer.copy(alpha = .12f + i * .03f),
                    Offset(offset, 0f),
                    Offset(offset - size.width * .6f, size.height),
                    size.width * .12f,
                )
            }
        }
        if (!url.isNullOrBlank())
            AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
internal fun PrismCollectionTile(
    title: String,
    subtitle: String?,
    artwork: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PrismArtwork(
            artwork,
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)),
        )
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank())
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
    }
}

@Composable
internal fun PrismGenreTile(
    title: String,
    tint: Color,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val ink = lerp(colors.tertiary, tint, .55f)
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainer.copy(alpha = .75f))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.linearGradient(listOf(ink.copy(alpha = .23f), Color.Transparent)))
            val center = Offset(size.width * .86f, size.height * .22f)
            repeat(4) { i ->
                val r = size.width * (.22f + i * .08f)
                val path =
                    Path().apply {
                        moveTo(center.x, center.y - r)
                        lineTo(center.x + r * .65f, center.y)
                        lineTo(center.x, center.y + r)
                        lineTo(center.x - r * .65f, center.y)
                        close()
                    }
                drawPath(path, ink.copy(alpha = .24f - i * .04f), style = Stroke(1.dp.toPx()))
            }
        }
        val compact = LocalPrismCompact.current
        Column(
            Modifier.fillMaxWidth().heightIn(min = if (compact) 96.dp else 144.dp).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 24.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    (index + 1).toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
                Icon(
                    painterResource(R.drawable.arrow_forward),
                    null,
                    Modifier.size(18.dp),
                    tint = colors.onSurfaceVariant,
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
