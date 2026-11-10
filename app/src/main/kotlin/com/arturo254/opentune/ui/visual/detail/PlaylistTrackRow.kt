package com.arturo254.opentune.ui.visual.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.ui.component.ItemThumbnail
import com.arturo254.opentune.ui.visual.component.BoneColor
import com.arturo254.opentune.ui.visual.component.OnDeviceMarks
import com.arturo254.opentune.ui.visual.component.RoundedHighlight
import com.arturo254.opentune.ui.visual.theme.InterFontFamily
import com.arturo254.opentune.utils.makeTimeString

private val TrackShape = RoundedCornerShape(9.dp)
private val PlaylistInset = 36.dp
private val TrackHighlight = RoundedHighlight(cornerRadius = 100.dp, spillX = (-10).dp)
private val TrackTitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 17.sp,
    letterSpacing = (-0.03).em,
)
private val TrackSubtitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.5.sp,
    lineHeight = 14.sp,
    letterSpacing = (-0.02).em,
)

private fun Modifier.selectedRowBand(progress: () -> Float): Modifier = drawBehind {
    val p = progress()
    if (p <= 0f) return@drawBehind
    val inset = 14.dp.toPx()
    drawRoundRect(
        color = Color.White.copy(alpha = 0.08f * p.coerceAtMost(1f)),
        topLeft = Offset(inset, 0f),
        size = Size(size.width - inset * 2f, size.height),
        cornerRadius = CornerRadius(14.dp.toPx()),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistTrackRow(
    thumbnailUrl: String?,
    title: String,
    subtitle: String,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    
    coverShape: androidx.compose.ui.graphics.Shape = TrackShape,
    
    coverModifier: Modifier = Modifier,
    
    songId: String? = null,
    
    selected: Boolean = false,
    externalGestures: Boolean = false,
    
    liked: Boolean? = null,
    onLikeClick: (() -> Unit)? = null,
) {
    val band by animateFloatAsState(if (selected) 1f else 0f, tween(180), label = "selectedBand")
    val verticalPadding = if (trailing == null && liked != null && onLikeClick != null) 5.dp else 9.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .selectedRowBand { band }
            .then(
                if (externalGestures) Modifier
                else Modifier.combinedClickable(
                    interactionSource = null,
                    indication = TrackHighlight,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
            )
            .padding(start = PlaylistInset, end = 24.dp, top = verticalPadding, bottom = verticalPadding)
    ) {
        Box(
            modifier = Modifier
                .size(39.dp)
                .then(coverModifier)
                .clip(coverShape)
                .background(BoneColor)
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
                .padding(horizontal = 11.dp)
        ) {
            Text(
                text = title,
                style = TrackTitleStyle,
                color = Color.White.copy(alpha = if (isActive) 1f else 0.94f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = TrackSubtitleStyle,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OnDeviceMarks(songId, modifier = Modifier.padding(end = 2.dp))
        if (trailing == null && liked != null && onLikeClick != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onLikeClick),
            ) {
                Icon(
                    painter = painterResource(if (liked) R.drawable.favorite else R.drawable.favorite_border),
                    contentDescription = stringResource(if (liked) R.string.action_remove_like else R.string.action_like),
                    tint = if (liked) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onMenuClick)
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_horiz),
                    contentDescription = stringResource(R.string.more),
                    tint = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun OriginalDetailSongRow(
    song: Song,
    isActive: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    viewCountText: String? = null,
    showInLibraryIcon: Boolean = true,
    isSelected: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val artists = song.artists.joinToString { it.name }
    val duration = song.song.duration.takeIf { it > 0 }?.let { makeTimeString(it * 1000L) }
    val subtitle = listOfNotNull(artists.takeIf(String::isNotBlank), duration, viewCountText)
        .joinToString(" • ")
    PlaylistTrackRow(
        thumbnailUrl = song.thumbnailUrl,
        title = song.title,
        subtitle = subtitle,
        isActive = isActive,
        isPlaying = isPlaying,
        onClick = {},
        onLongClick = {},
        onMenuClick = {},
        modifier = modifier,
        songId = song.id.takeIf { showInLibraryIcon },
        selected = isSelected,
        externalGestures = true,
        trailing = { Row { trailingContent() } },
    )
}
