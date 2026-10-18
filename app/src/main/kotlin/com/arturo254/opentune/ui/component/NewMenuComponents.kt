package com.arturo254.opentune.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.LocalIndication
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidChrome
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidVariant
import com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidVariant
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NewActionButton(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val animatedBackground by animateColorAsState(
        targetValue = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "background"
    )

    val animatedContent by animateColorAsState(
        targetValue = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "content"
    )

    val liquid = LocalGalaxLiquidVariant.current != GalaxLiquidVariant.DISABLED
    val shape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (liquid && pressed && enabled) .962f else 1f,
        animationSpec = spring(dampingRatio = .68f, stiffness = 480f),
        label = "liquidActionPress",
    )
    Card(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .then(if (liquid) Modifier.galaxLiquidChrome(shape, emphasis = .85f) else Modifier)
            .clickable(enabled = enabled, interactionSource = interactionSource,
                indication = LocalIndication.current, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (liquid) Color.Transparent else animatedBackground
        ),
        shape = shape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (liquid) 0.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = animatedContent,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

@Composable
fun NewMenuItem(
    headlineContent: @Composable () -> Unit,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val motion = com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem.galaxMotion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) motion.pressScale else 1f,
        animationSpec = motion.pressSpec, label = "menuRowPress")
    Row(
        modifier.graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else .45f }
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled && onClick != null, interactionSource = interaction,
                indication = LocalIndication.current) { onClick?.invoke() }
            .heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        leadingContent?.let { leading ->
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .09f)) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) { leading() }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.bodyLarge) { headlineContent() }
            supportingContent?.let { supporting ->
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                ) { androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.bodySmall) { supporting() } }
            }
        }
        trailingContent?.invoke()
    }
}

@Composable
fun NewMenuSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
fun NewActionGrid(
    actions: List<NewAction>,
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    val rows = actions.chunked(columns)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { action ->
                    NewActionButton(
                        icon = action.icon,
                        text = action.text,
                        onClick = action.onClick,
                        modifier = Modifier.weight(1f),
                        enabled = action.enabled,
                        backgroundColor = if (action.backgroundColor != Color.Unspecified) action.backgroundColor else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (action.contentColor != Color.Unspecified) action.contentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

data class NewAction(
    val icon: @Composable () -> Unit,
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val backgroundColor: Color = Color.Unspecified,
    val contentColor: Color = Color.Unspecified
)

@Composable
fun NewMenuContent(
    headerContent: @Composable (() -> Unit)? = null,
    actionGrid: @Composable (() -> Unit)? = null,
    menuItems: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        headerContent?.invoke()

        actionGrid?.invoke()

        if (headerContent != null && actionGrid != null) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        menuItems?.invoke()
    }
}

@Composable
fun NewIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val animatedBackground by animateColorAsState(
        targetValue = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "background"
    )

    val animatedContent by animateColorAsState(
        targetValue = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "content"
    )

    val liquid = LocalGalaxLiquidVariant.current != GalaxLiquidVariant.DISABLED
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (liquid && pressed && enabled) .90f else 1f,
        animationSpec = spring(dampingRatio = .67f, stiffness = 510f),
        label = "liquidIconPress",
    )
    Card(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .then(if (liquid) Modifier.galaxLiquidChrome(CircleShape, emphasis = 1.1f) else Modifier)
            .clickable(enabled = enabled, interactionSource = interactionSource,
                indication = LocalIndication.current, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (liquid) Color.Transparent else animatedBackground
        ),
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (liquid) 0.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}

@Composable
fun NewMenuContainer(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        content()
    }
}

@Composable
fun MenuSurfaceSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (LocalGalaxLiquidVariant.current != GalaxLiquidVariant.DISABLED)
            Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth().then(
            if (LocalGalaxLiquidVariant.current != GalaxLiquidVariant.DISABLED)
                Modifier.galaxLiquidChrome(RoundedCornerShape(24.dp))
            else Modifier
        ),
    ) {
        Column(content = content)
    }
}
