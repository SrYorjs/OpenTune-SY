package com.arturo254.opentune.ui.visual.search

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import com.arturo254.opentune.ui.visual.component.BlurEffectCache

@Composable
fun OriginalSearchTopResult(
    item: UnifiedTrackSearchResult,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val artwork = item.recommendedVariant?.artworkUrl
        ?: item.variants.firstNotNullOfOrNull { it.artworkUrl }
    val artist = item.identity.artists.joinToString { it.name }
    val detail = listOfNotNull(
        artist.takeIf { it.isNotBlank() },
        item.identity.durationMs?.let { originalSearchDuration(it) },
    ).joinToString("  •  ")

    Box(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = SearchHorizontalPadding)
            .clip(RoundedCornerShape(16.dp))
            .background(SearchColors.Tile)
            .searchCardBorder(16.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        if (artwork != null) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().graphicsLayer {
                    alpha = 0.55f
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = BlurEffectCache.get(
                            70f, android.graphics.Shader.TileMode.CLAMP
                        )
                    }
                },
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.55f to Color.Black.copy(alpha = 0.62f),
                        1f to Color.Black.copy(alpha = 0.88f),
                    )
                )
            )
        }
        Column(Modifier.padding(16.dp)) {
            SearchArtwork(url = artwork, size = 108.dp, corner = 8.dp)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.identity.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        ),
                        color = SearchColors.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = SearchColors.Secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier.size(46.dp).clip(CircleShape)
                        .background(Color.White).clickable(onClick = onClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(
                            if (isActive && isPlaying) R.drawable.ic_untitled_pause
                            else R.drawable.play
                        ),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun OriginalSearchResultRow(
    item: UnifiedTrackSearchResult,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val artwork = item.recommendedVariant?.artworkUrl
        ?: item.variants.firstNotNullOfOrNull { it.artworkUrl }
    val subtitle = listOfNotNull(
        item.identity.artists.joinToString { it.name }.takeIf { it.isNotBlank() },
        item.identity.durationMs?.let { originalSearchDuration(it) },
    ).joinToString("  •  ")
    Column {
        Row(
            modifier = Modifier.fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = SearchHorizontalPadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchArtwork(url = artwork, size = 46.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.identity.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (isActive) Color(0xFF7FD1FF) else SearchColors.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = SearchColors.Secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        SearchRule()
    }
}

@Composable
fun OriginalSearchCollectionRow(
    title: String,
    subtitle: String,
    artwork: String?,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
                .padding(horizontal = SearchHorizontalPadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchArtwork(url = artwork, size = 46.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    ),
                    color = SearchColors.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = SearchColors.Secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        SearchRule()
    }
}

private fun originalSearchDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    return (seconds / 60).toString() + ":" + (seconds % 60).toString().padStart(2, '0')
}
