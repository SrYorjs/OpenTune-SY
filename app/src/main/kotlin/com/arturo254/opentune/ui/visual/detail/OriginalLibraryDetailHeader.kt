package com.arturo254.opentune.ui.visual.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.arturo254.opentune.ui.visual.component.ScrollAwayCover
import com.arturo254.opentune.ui.visual.component.containerTransformCover
import com.arturo254.opentune.ui.visual.component.containerTransformTarget
import com.arturo254.opentune.ui.visual.theme.InterFontFamily
import com.arturo254.opentune.ui.utils.resize

private val DetailPageInset = 36.dp
private val OriginalDetailTitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.06).em,
)
private val OriginalDetailStatsStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = (-0.02).em,
)

@Composable
fun OriginalLibraryDetailHeader(
    title: String,
    label: String,
    artworkUrls: List<String?>,
    modifier: Modifier = Modifier,
    artist: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary,
    facts: List<String> = emptyList(),
    details: @Composable ColumnScope.() -> Unit = {},
    supporting: @Composable ColumnScope.() -> Unit = {},
    actions: @Composable ColumnScope.() -> Unit = {},
    sharedElementKey: String? = null,
    listState: LazyListState? = null,
    onCoverPositioned: (Rect) -> Unit = {},
) {
    val handedCover = remember(sharedElementKey) { containerTransformCover(sharedElementKey) }
    val coverUrl = handedCover ?: artworkUrls.firstOrNull { !it.isNullOrBlank() }
    val height = (LocalConfiguration.current.screenHeightDp * 0.60f).dp
    val density = LocalDensity.current
    val heightPx = with(density) { height.toPx() }
    val progress by remember(listState, heightPx) {
        derivedStateOf {
            val list = listState
            if (list == null) 0f
            else if (list.firstVisibleItemIndex > 0) 1f
            else (list.firstVisibleItemScrollOffset / heightPx).coerceIn(0f, 1f)
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .containerTransformTarget(sharedElementKey)
            .onGloballyPositioned { coordinates ->
                onCoverPositioned(
                    Rect(
                        coordinates.positionInWindow(),
                        Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat()),
                    )
                )
            },
    ) {
        DetailCoverBackground(
            thumbnailUrl = coverUrl,
            modifier = Modifier.matchParentSize(),
        )
        if (!coverUrl.isNullOrBlank()) {
            ScrollAwayCover(
                progress = { progress },
                follow = -0.8f,
                modifier = Modifier.matchParentSize(),
            ) {
                AsyncImage(
                    model = coverUrl.resize(1200, 1200),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        alpha = if (artist) 0.72f else 1f
                    },
                )
            }
        }
        Box(
            modifier = Modifier.matchParentSize().drawWithCache {
                val shade = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.18f),
                    0.26f to Color.Transparent,
                    0.61f to Color.Black.copy(alpha = 0.42f),
                    1f to Color.Black.copy(alpha = 0.84f),
                )
                onDrawBehind { drawRect(shade) }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(
                    start = DetailPageInset + 1.dp,
                    end = DetailPageInset,
                    top = 90.dp,
                    bottom = 14.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (artist && !coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = coverUrl.resize(300, 300),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                )
                Spacer(Modifier.heightIn(min = 8.dp))
            }
            Text(
                text = title,
                style = OriginalDetailTitleStyle,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            details()
            if (facts.isNotEmpty()) {
                Text(
                    text = facts.joinToString("  •  "),
                    style = OriginalDetailStatsStyle,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            supporting()
            Spacer(Modifier.heightIn(min = 10.dp))
            actions()
        }
    }
}
