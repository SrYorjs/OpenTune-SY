package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.component.GalaxOrbitField
import com.arturo254.opentune.core.designsystem.theme.GalaxVisualKit

@Composable
fun GalaxDiscoverPortal(
    title: String,
    subtitle: String,
    searchHint: String,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 17.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(35.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        scheme.primary.copy(alpha = .20f),
                        scheme.surfaceContainerLow,
                        scheme.tertiary.copy(alpha = .09f),
                    ),
                ),
            ),
    ) {
        GalaxOrbitField(
            modifier = Modifier.fillMaxSize(),
            active = false,
            intensity = .54f,
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(6.dp).background(scheme.primary, CircleShape))
                Text(
                    text = "GALAXOT  /  EXPLORACIÓN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                    letterSpacing = 1.35.sp,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                letterSpacing = (-.85).sp,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(23.dp))
                    .background(scheme.surfaceContainerHighest.copy(alpha = .79f))
                    .clickable(role = Role.Button, onClick = onSearch)
                    .padding(horizontal = 17.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(23.dp),
                )
                Text(
                    text = searchHint,
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    Modifier.clip(CircleShape)
                        .background(scheme.primary.copy(alpha = .16f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "TODOS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
fun GalaxDiscoverEditorialHeading(
    title: String,
    subtitle: String,
    number: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(40.dp)
                .clip(CircleShape)
                .background(scheme.primary.copy(alpha = .15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                color = scheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

@Composable
fun GalaxGenreSurface(
    title: String,
    swatch: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = Color(swatch).copy(alpha = 1f)
    Box(
        modifier = modifier.fillMaxWidth().height(120.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = .78f),
                        scheme.primary.copy(alpha = .37f),
                        Color(0xFF071021),
                    ),
                ),
            )
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.width * .26f
            val center = Offset(size.width * .94f, size.height * .20f)
            for (i in 0..3) {
                drawCircle(
                    Color.White.copy(alpha = .18f),
                    radius = r * (.45f + i * .40f),
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            drawArc(
                Color.White.copy(alpha = .46f),
                startAngle = -68f,
                sweepAngle = 94f,
                useCenter = false,
                topLeft = Offset(center.x-r, center.y-r),
                size = Size(r*2f,r*2f),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart)
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "GALAXOT  /  GÉNERO",
                color = Color.White.copy(alpha = .65f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = .9.sp,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
