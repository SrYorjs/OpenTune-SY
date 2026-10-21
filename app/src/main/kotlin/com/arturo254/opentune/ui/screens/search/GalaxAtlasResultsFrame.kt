package com.arturo254.opentune.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.screens.AetherInk
import com.arturo254.opentune.ui.screens.AetherMotionClock
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun AtlasResultsHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = "TODOS",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-.5).sp,
            )
        }
        Box(
            modifier = Modifier.clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = .10f))
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun AtlasResultsWarning(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = .07f))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.graphic_eq),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun AtlasResultsLoadIndicator(
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(21.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Cargando resultados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun AtlasResultsSkeleton(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(4) { index ->
            AtlasSkeletonRow(index)
        }
    }
}

@Composable
private fun AtlasSkeletonRow(
    index: Int,
) {
    val scheme = MaterialTheme.colorScheme
    AetherMotionClock(active = true, durationMillis = 1900) { phase ->
        val shimmer = phase.coerceIn(0f, 1f)
        val mid = scheme.surfaceContainerHigh
        val light = scheme.onSurface.copy(alpha = .095f)
        val shape = RoundedCornerShape(23.dp)
        Row(
            modifier = Modifier.fillMaxWidth().height(96.dp)
                .clip(shape)
                .background(scheme.surfaceContainerLow.copy(alpha = .61f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                modifier = Modifier.size(65.dp).clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(mid, light, mid),
                            startX = (shimmer - .6f) * 200f,
                            endX = (shimmer + .6f) * 200f,
                        ),
                    )
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(if (index % 2 == 0) .77f else .58f)
                        .height(13.dp).clip(RoundedCornerShape(7.dp))
                        .background(mid),
                )
                Box(
                    modifier = Modifier.fillMaxWidth(.43f)
                        .height(10.dp).clip(RoundedCornerShape(5.dp))
                        .background(mid.copy(alpha = .75f)),
                )
            }
        }
    }
}

@Composable
internal fun AtlasResultsFooter(
    hasMore: Boolean,
    loadingMore: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(top = 14.dp, bottom = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (loadingMore) {
            AtlasResultsLoadIndicator(loading = true)
        } else if (!hasMore) {
            Box(
                modifier = Modifier.size(5.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = .45f)),
            )
        }
    }
}

@Composable
internal fun AtlasSignalBars(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    AetherMotionClock(active = active, durationMillis = 11000) { phase ->
        Canvas(modifier) {
            val segments = 21
            val pitch = size.width / segments
            for (i in 0 until segments) {
                val travel = sin(i * .65 + phase * (PI * 2)).toFloat()
                val envelope = sin(i / segments.toFloat() * PI).toFloat()
                val h = size.height * (.19f + .27f * envelope * (1f + travel * .62f))
                drawLine(
                    color = if (i % 3 == 0) AetherInk.cyan.copy(alpha = .49f)
                        else AetherInk.ultramarine.copy(alpha = .50f),
                    start = androidx.compose.ui.geometry.Offset(pitch * i, size.height * .5f - h),
                    end = androidx.compose.ui.geometry.Offset(pitch * i, size.height * .5f + h),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
    }
}

@Composable
internal fun AtlasSourceSummary(
    activeProviders: Int,
    totalMatches: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AtlasSignalBars(
            active = false,
            modifier = Modifier.width(33.dp).height(25.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = totalMatches.toString() + " canciones",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = activeProviders.toString() + " fuentes en la selección",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
