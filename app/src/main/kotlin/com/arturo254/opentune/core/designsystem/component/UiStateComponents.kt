package com.arturo254.opentune.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.theme.shapes
import com.arturo254.opentune.ui.component.shimmer.ListItemPlaceHolder
import com.arturo254.opentune.ui.component.shimmer.ShimmerHost

/** STANDARD matches the original EmptyPlaceholder, COMPACT the search label,
 * and SETTINGS the previous settings-search empty card. */
enum class OpenTuneEmptyPresentation { STANDARD, COMPACT, SETTINGS }

@Composable
fun OpenTuneEmptyState(
    @DrawableRes icon: Int?,
    text: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    presentation: OpenTuneEmptyPresentation = OpenTuneEmptyPresentation.STANDARD,
    settingsCardShape: Shape = OpenTuneDesignSystem.shapes.large,
) {
    if (presentation == OpenTuneEmptyPresentation.COMPACT) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(horizontal = 8.dp, vertical = 16.dp),
        )
        return
    }

    if (presentation == OpenTuneEmptyPresentation.SETTINGS) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = settingsCardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                supportingText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        return
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(96.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                ) {
                    Image(
                        painter = painterResource(icon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.size(48.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            supportingText?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Explicit progress semantics belong to the caller; no background jobs here. */
@Composable
fun OpenTuneLoadingState(
    modifier: Modifier = Modifier,
    indicatorSize: Dp = 28.dp,
    verticalPadding: Dp = 28.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = verticalPadding),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(indicatorSize))
    }
}

/** Retry is supplied by the host screen; this component never retries itself. */
@Composable
fun OpenTuneErrorState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onRetry != null) {
            Button(onClick = onRetry) { Text(actionLabel) }
        }
    }
}

/** Exactly one shimmer implementation; legacy placeholders remain reusable. */
@Composable
fun OpenTuneShimmerState(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ShimmerHost(modifier = modifier, content = content)
}

@Composable
fun OpenTuneListShimmer(
    modifier: Modifier = Modifier,
    count: Int = 4,
) {
    OpenTuneShimmerState(modifier = modifier) {
        repeat(count.coerceIn(0, 20)) {
            ListItemPlaceHolder()
        }
    }
}
