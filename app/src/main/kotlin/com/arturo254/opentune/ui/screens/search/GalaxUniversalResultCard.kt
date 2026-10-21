package com.arturo254.opentune.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidChrome
import com.arturo254.opentune.ui.screens.AetherInk

@Composable
fun GalaxUniversalResultCard(
    title: String,
    artist: String,
    artwork: String?,
    providers: String,
    quality: String?,
    catalogOnly: Boolean,
    active: Boolean,
    playing: Boolean,
    resolving: Boolean,
    downloaded: Boolean,
    downloading: Boolean,
    onClick: (() -> Unit)?,
    onVersions: () -> Unit,
    onDownload: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val selected = active || resolving
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        targetValue = if (selected) scheme.primary.copy(alpha = .11f)
            else scheme.surfaceContainerLow.copy(alpha = .66f),
        animationSpec = tween(350),
        label = "search-row-selection",
    )
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier.fillMaxWidth()
            .clip(shape)
            .background(bg)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(66.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(scheme.surfaceContainerHigh)
                    .then(
                        if (onClick != null && !resolving) {
                            Modifier.clickable(onClick = onClick)
                        } else Modifier
                    ),
            ) {
                AsyncImage(
                    model = artwork,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (selected) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Color.Black.copy(alpha = .39f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (resolving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(23.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                        } else {
                            Icon(
                                painter = painterResource(
                                    if (playing) R.drawable.pause else R.drawable.play
                                ),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f)
                    .then(
                        if (onClick != null && !resolving) {
                            Modifier.clickable(onClick = onClick)
                        } else Modifier
                    ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) scheme.primary else scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-.2).sp,
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AtlasResultAction(
                icon = R.drawable.tune,
                label = stringResource(R.string.universal_versions_action),
                important = true,
                onClick = onVersions,
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            scheme.outlineVariant.copy(alpha = .26f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = providers,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!quality.isNullOrBlank()) {
                        AtlasResultTag(
                            text = quality,
                            accent = true,
                        )
                    }
                    if (catalogOnly) {
                        AtlasResultTag(
                            text = stringResource(R.string.universal_versions_catalog),
                            accent = false,
                        )
                    }
                }
            }
            when {
                downloaded -> AtlasResultStatus(
                    icon = R.drawable.offline,
                    title = stringResource(R.string.offline),
                )
                downloading -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = scheme.primary,
                )
                onDownload != null -> AtlasResultAction(
                    icon = R.drawable.download,
                    label = stringResource(R.string.download),
                    important = false,
                    onClick = onDownload,
                )
            }
        }
    }
}

@Composable
private fun AtlasResultAction(
    icon: Int,
    label: String,
    important: Boolean,
    onClick: () -> Unit,
) {
    val palette = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(15.dp)
    Box(
        modifier = Modifier.size(45.dp)
            .clip(shape)
            .background(
                if (important) palette.primary.copy(alpha = .095f)
                else palette.surfaceContainerHigh,
            )
            .galaxLiquidChrome(shape, emphasis = if (important) .80f else .50f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = if (important) palette.primary else palette.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun AtlasResultStatus(
    icon: Int,
    title: String,
) {
    Box(
        modifier = Modifier.size(45.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(23.dp),
        )
    }
}

@Composable
private fun AtlasResultTag(
    text: String,
    accent: Boolean,
) {
    val palette = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.clip(RoundedCornerShape(9.dp))
            .background(
                if (accent) palette.primary.copy(alpha = .11f)
                else palette.surfaceContainerHigh,
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            color = if (accent) palette.primary else palette.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun GalaxUniversalSearchProgress(
    count: Int,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(17.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Box(
                Modifier.size(6.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Text(
            text = if (loading) "Buscando"
                else if (count == 1) "1 resultado"
                else count.toString() + " resultados",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun GalaxUniversalNoResults(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Box(
            modifier = Modifier.size(82.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = .095f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(29.dp),
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
