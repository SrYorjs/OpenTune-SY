package com.arturo254.opentune.ui.visual.library

import com.arturo254.opentune.ui.visual.component.containerTransformSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.visual.component.HomeTrackRow
import com.arturo254.opentune.ui.visual.theme.InterFontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.em
import com.arturo254.opentune.ui.visual.search.SearchColors
import com.arturo254.opentune.ui.visual.search.SearchHorizontalPadding
import com.arturo254.opentune.ui.visual.search.SearchTitleBlockHeight
import com.arturo254.opentune.ui.utils.resize
import kotlinx.coroutines.launch

val LibraryTitleBlockHeight: Dp = SearchTitleBlockHeight + 16.dp

@Composable
fun LibraryRule(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SearchHorizontalPadding)
            .height(1.dp)
            .background(SearchColors.Divider),
    )
}

@Composable
fun LibrarySectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = SearchColors.Primary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = SearchHorizontalPadding),
    )
}

@Composable
fun LibraryGridArtwork(
    url: String?,
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp,
    round: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(if (round) CircleShape else RoundedCornerShape(corner))
            .background(SearchColors.TileHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url.resize(400, 400),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                tint = SearchColors.Secondary,
                modifier = Modifier.fillMaxSize(0.35f),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryPinnedTile(
    thumbnailUrl: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    
    sharedElementKey: String? = null,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .containerTransformSource(key = sharedElementKey, cornerRadius = 18.dp, coverUrl = thumbnailUrl)
            .clip(RoundedCornerShape(18.dp))
            .background(SearchColors.TileHigh)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl.resize(300, 300),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                tint = SearchColors.Secondary,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun LibraryCoverStack(
    covers: List<String?>,
    modifier: Modifier = Modifier,
    tile: Dp = 46.dp,
    round: Boolean = false,
    front: (@Composable (Modifier) -> Unit)? = null,
) {
    val shown = remember(covers) { covers.filterNotNull().take(2) }
    if (shown.isEmpty() && front == null) return
    val back = if (front != null) shown.firstOrNull() else shown.getOrNull(1)
    val corner = if (round) tile / 2 else tile * 0.22f

    Box(modifier = modifier.size(width = tile * 1.55f, height = tile)) {
        if (back != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(tile)
                    .graphicsLayer {
                        alpha = 0.55f
                        rotationZ = if (round) 0f else -9f
                    },
            ) {
                LibraryGridArtwork(url = back, corner = corner, round = round, modifier = Modifier.fillMaxSize())
            }
        }
        val frontModifier = Modifier
            .align(Alignment.CenterEnd)
            .size(tile)
            .graphicsLayer { rotationZ = if (round) 0f else 7f }
        if (front != null) {
            front(frontModifier.clip(if (round) CircleShape else RoundedCornerShape(corner)))
        } else {
            Box(modifier = frontModifier) {
                LibraryGridArtwork(url = shown[0], corner = corner, round = round, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private val LibraryCountStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    lineHeight = 36.sp,
    letterSpacing = (-0.03).em,
)

@Composable
private fun LibraryCardLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = SearchColors.Primary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun LibraryCount(count: Int) {
    Text(
        text = count.toString(),
        color = SearchColors.Secondary,
        style = LibraryCountStyle,
        maxLines = 1,
    )
}

@Composable
fun LibraryEmptyState(modifier: Modifier = Modifier, text: String = "Nothing to show here") {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 56.dp, horizontal = SearchHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = SearchColors.Secondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun LibraryCategoryCard(
    title: String,
    count: Int,
    covers: List<String?>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    
    roundCovers: Boolean = false,
) {
    Box(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SearchColors.Tile)
            .clickable(onClick = onClick),
    ) {
        LibraryCoverStack(
            covers = covers,
            round = roundCovers,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 12.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 11.dp, end = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.width(6.dp))
                }
                LibraryCardLabel(title)
            }
            LibraryCount(count)
        }
    }
}

@Composable
fun LibraryWideCard(
    title: String,
    count: Int,
    covers: List<String?>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    
    sharedElementKey: String? = null,
    
    art: (@Composable (Modifier) -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .containerTransformSource(
                key = sharedElementKey,
                cornerRadius = 18.dp,
                coverUrl = covers.firstOrNull { it != null },
            )
            .clip(RoundedCornerShape(18.dp))
            .background(SearchColors.Tile)
            .clickable(onClick = onClick),
    ) {
        LibraryCoverStack(
            covers = covers,
            tile = 60.dp,
            front = art,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 14.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 14.dp, end = 110.dp),
        ) {
            LibraryCardLabel(title)
            LibraryCount(count)
        }
    }
}

@Composable
fun LibraryListRow(
    title: String,
    subtitle: String?,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    
    round: Boolean = false,
    
    sharedElementKey: String? = null,
    
    songId: String? = null,
) {
    HomeTrackRow(
        thumbnailUrl = thumbnailUrl,
        title = title,
        subtitle = subtitle,
        isActive = isActive,
        isPlaying = isPlaying,
        onClick = onClick,
        onLongClick = onLongClick,
        onMoreClick = null,
        coverShape = if (round) CircleShape else LibraryRowCoverShape,
        songId = songId,
        coverModifier = Modifier.containerTransformSource(
            key = sharedElementKey,
            cornerRadius = if (round) LibraryRowCoverSize / 2 else LibraryRowCoverCorner,
            coverUrl = thumbnailUrl,
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SearchHorizontalPadding, vertical = 3.dp),
    )
}

private val LibraryRowCoverCorner = 14.dp
private val LibraryRowCoverSize = 54.dp
private val LibraryRowCoverShape = RoundedCornerShape(LibraryRowCoverCorner)

@Composable
fun LibraryLetterHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        color = SearchColors.Primary,
        style = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = (-0.03).em,
        ),
        modifier = modifier.padding(horizontal = SearchHorizontalPadding, vertical = 10.dp),
    )
}
