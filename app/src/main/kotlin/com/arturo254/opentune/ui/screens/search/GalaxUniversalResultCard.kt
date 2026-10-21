package com.arturo254.opentune.ui.screens.search

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.screens.GalaxEditorialSurface

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
    val scheme = MaterialTheme.colorScheme
    val highlighted = active || resolving
    GalaxEditorialSurface(
        modifier = modifier.fillMaxWidth(),
        radius = 23.dp,
        elevated = highlighted,
        accent = if (highlighted) scheme.primary else scheme.tertiary,
        onClick = if (resolving) null else onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Box(
                    modifier = Modifier.size(66.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(scheme.surfaceContainerHighest),
                ) {
                    AsyncImage(
                        model = artwork,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    if (active || resolving) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(Color.Black.copy(alpha = .36f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (resolving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    painter = painterResource(
                                        if (playing) R.drawable.pause else R.drawable.play,
                                    ),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(25.dp),
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) scheme.primary else scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                GalaxUniversalVersionAction(
                    onClick = onVersions,
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                scheme.outlineVariant.copy(alpha = .35f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        if (quality != null) {
                            GalaxUniversalInfoTag(
                                text = quality,
                                emphasized = true,
                            )
                        }
                        if (catalogOnly) {
                            GalaxUniversalInfoTag(
                                text = stringResource(R.string.universal_versions_catalog),
                                emphasized = false,
                            )
                        }
                    }
                }
                when {
                    downloaded -> GalaxUniversalStatusIcon(
                        icon = R.drawable.offline,
                        contentDescription = stringResource(R.string.offline),
                    )
                    downloading -> CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = scheme.primary,
                        strokeWidth = 2.dp,
                    )
                    onDownload != null -> GalaxUniversalDownloadAction(
                        onClick = onDownload,
                    )
                }
            }
        }
    }
}

@Composable
private fun GalaxUniversalVersionAction(
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(15.dp)
    Box(
        modifier = Modifier.size(44.dp).clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = .08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.tune),
            contentDescription = stringResource(R.string.universal_versions_action),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun GalaxUniversalDownloadAction(
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(15.dp)
    Box(
        modifier = Modifier.size(44.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.download),
            contentDescription = stringResource(R.string.download),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun GalaxUniversalStatusIcon(
    icon: Int,
    contentDescription: String,
) {
    Box(
        modifier = Modifier.size(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(23.dp),
        )
    }
}

@Composable
private fun GalaxUniversalInfoTag(
    text: String,
    emphasized: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(
                if (emphasized) scheme.primary.copy(alpha = .12f)
                else scheme.onSurface.copy(alpha = .06f),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (emphasized) scheme.primary else scheme.onSurfaceVariant,
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
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(17.dp),
                color = scheme.primary,
                strokeWidth = 2.dp,
            )
        } else {
            Box(
                Modifier.size(6.dp).clip(CircleShape)
                    .background(scheme.primary),
            )
        }
        Text(
            text = if (loading) "Cargando"
                else if (count == 1) "1 resultado"
                else count.toString() + " resultados",
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
fun GalaxUniversalNoResults(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Box(
            modifier = Modifier.size(78.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = .08f)),
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
