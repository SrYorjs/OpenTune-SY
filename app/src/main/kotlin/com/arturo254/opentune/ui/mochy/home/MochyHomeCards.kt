package com.arturo254.opentune.ui.mochy.home

import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R
import coil3.compose.AsyncImage
import androidx.compose.ui.text.font.FontFamily
import com.arturo254.opentune.ui.mochy.component.RoundedHighlight
import com.arturo254.opentune.ui.mochy.component.BoneColor
import com.arturo254.opentune.ui.mochy.component.HomeCardCornerRadius
import com.arturo254.opentune.ui.mochy.component.containerTransformSource

val HomeContentInset = 36.dp
val HomeCardSpacing = 15.dp

private val HomeTracking = (-0.06).em

val HomeLabelStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 19.sp,
    letterSpacing = HomeTracking,
)

val HomeTitleStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 28.sp,
    letterSpacing = HomeTracking,
)

private val HomeCardTitleStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 17.sp,
    letterSpacing = (-0.03).em,
)

private val HomeCardSubtitleStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 15.sp,
    letterSpacing = (-0.02).em,
)

private val HomeRankStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    letterSpacing = HomeTracking,
)

private val SectionLabelColor = Color.White.copy(alpha = 0.69f)
private val HomeRowShape = RoundedCornerShape(14.dp)
val HomeRowHeight = 54.dp


@Composable
fun homeRowHeight(): Dp {
    val linesHeight = with(LocalDensity.current) { (HomeRowTitleLineHeight.value + HomeCardSubtitleStyle.lineHeight.value).sp.toDp() }
    return maxOf(HomeRowHeight, linesHeight + 14.dp)
}

private val HomeRowTitleLineHeight = 17.sp

private val SectionTapMargin = 12.dp
private val SectionTitleHighlight = RoundedHighlight(cornerRadius = 100.dp)
private val TrackRowHighlight = RoundedHighlight(cornerRadius = 22.dp, spillX = 8.dp)


val HomeCardHighlight = RoundedHighlight(cornerRadius = HomeCardCornerRadius + 6.dp, spillX = 7.dp, spillY = 7.dp)


@Composable
fun homeCardSize(): Dp = 137.dp



@Composable
fun HomeSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = SectionTapMargin, end = SectionTapMargin, bottom = 6.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = null,
                        indication = SectionTitleHighlight,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )

            .padding(
                start = HomeContentInset - SectionTapMargin,
                end = HomeContentInset - SectionTapMargin,
                top = 4.dp,
                bottom = 4.dp,
            ),
    ) {
        if (thumbnail != null) {
            thumbnail()
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f, fill = false)) {
            if (label != null) {
                Text(
                    text = label,
                    style = HomeCardSubtitleStyle,
                    color = Color.White.copy(alpha = 0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = title,
                style = HomeLabelStyle,
                color = SectionLabelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onClick != null) {
            Spacer(Modifier.width(6.dp))
            SectionChevron()
        }
    }
}

@Composable
private fun SectionChevron() {
    Icon(
        painter = painterResource(R.drawable.chevron_right),
        contentDescription = null,
        tint = Color.White.copy(alpha = 0.55f),
        modifier = Modifier.size(18.dp),
    )
}



@Composable
fun HomeCoverCard(
    title: String,
    subtitle: String?,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    circular: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    sharedElementKey: String? = null,
    badges: @Composable RowScope.() -> Unit = {},
    cover: (@Composable (Dp) -> Unit)? = null,
) = HomeCoverCard(
    title, subtitle, thumbnailUrl, modifier, circular, isActive, { isPlaying }, sharedElementKey, badges, cover,
)

@Composable
fun HomeCoverCard(
    title: String,
    subtitle: String?,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    circular: Boolean = false,
    isActive: Boolean = false,
    isPlaying: () -> Boolean,
    sharedElementKey: String? = null,
    badges: @Composable RowScope.() -> Unit = {},
    cover: (@Composable (Dp) -> Unit)? = null,
) {
    val size = homeCardSize()
    val shape = if (circular) CircleShape else RoundedCornerShape(HomeCardCornerRadius)

    Column(modifier = modifier.width(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .containerTransformSource(key = sharedElementKey, coverUrl = thumbnailUrl)
                .clip(shape)
                .background(BoneColor),
        ) {
            if (cover != null) {
                cover(size)
            } else {
                ItemThumbnail(
                    thumbnailUrl = thumbnailUrl,
                    isActive = isActive,
                    isPlaying = isPlaying,
                    shape = shape,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = HomeCardTitleStyle,
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (circular) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!subtitle.isNullOrEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                badges()
                Text(
                    text = subtitle,
                    style = HomeCardSubtitleStyle,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeTrackRow(
    thumbnailUrl: String?,
    title: String,
    subtitle: String?,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    
    coverShape: Shape = RoundedCornerShape((HomeCardCornerRadius * 0.56f).coerceIn(0.dp, 20.dp)),
    
    coverModifier: Modifier = Modifier,
    
    songId: String? = null,
) = HomeTrackRow(
    thumbnailUrl, title, subtitle, isActive, { isPlaying }, onClick, onLongClick, onMoreClick, modifier,
    rank, coverShape, coverModifier, songId,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeTrackRow(
    thumbnailUrl: String?,
    title: String,
    subtitle: String?,
    isActive: Boolean,
    isPlaying: () -> Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    coverShape: Shape = RoundedCornerShape((HomeCardCornerRadius * 0.56f).coerceIn(0.dp, 20.dp)),
    coverModifier: Modifier = Modifier,
    songId: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(homeRowHeight())
            .combinedClickable(
                interactionSource = null,
                indication = TrackRowHighlight,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        if (rank != null) {
            Text(
                text = rank.toString(),
                style = HomeRankStyle,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.width(37.dp),
            )
        }
        Box(
            modifier = coverModifier
                .size(HomeRowHeight)
                .clip(coverShape)
                .background(BoneColor),
        ) {
            ItemThumbnail(
                thumbnailUrl = thumbnailUrl,
                isActive = isActive,
                isPlaying = isPlaying,
                shape = coverShape,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 13.dp),
        ) {
            Text(
                text = title,
                style = HomeCardTitleStyle.copy(fontSize = 15.sp),
                color = if (isActive) Color.White else Color.White.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = HomeCardSubtitleStyle,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onMoreClick != null) Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .clickable(onClick = onMoreClick),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.White.copy(alpha = 0.14f), CircleShape),
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_horiz),
                    contentDescription = "More",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}




@Composable
private fun ItemThumbnail(
    thumbnailUrl: String?,
    isActive: Boolean,
    isPlaying: () -> Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.clip(shape)) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isActive && isPlaying()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .24f)),
                contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.pause),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
