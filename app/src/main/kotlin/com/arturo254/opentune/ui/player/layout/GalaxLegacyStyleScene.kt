package com.arturo254.opentune.ui.player.layout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.core.designsystem.component.GalaxOrbitField
import kotlin.math.min
import kotlin.math.sin

@Immutable
data class LegacyPlayerIdentity(
    val name: String,
    val tagline: String,
    val mark: String,
    val artworkScale: Float,
    val cornerDp: Dp,
    val panelDp: Dp,
)

fun legacyPlayerIdentity(style: PlayerDesignStyle): LegacyPlayerIdentity = when (style) {
    PlayerDesignStyle.V1 -> LegacyPlayerIdentity(
        "CLÁSICO", "TU MÚSICA, SIEMPRE", "EST. 01", .76f, 19.dp, 21.dp,
    )
    PlayerDesignStyle.V2 -> LegacyPlayerIdentity(
        "MODERNO", "UNA NUEVA FRECUENCIA", "MOD / 02", .74f, 38.dp, 35.dp,
    )
    PlayerDesignStyle.V3 -> LegacyPlayerIdentity(
        "MÍNIMO", "MENOS ES MÁS", "MIN / 03", .68f, 3.dp, 5.dp,
    )
    PlayerDesignStyle.V4 -> LegacyPlayerIdentity(
        "CINEMA", "SONIDO EN PANTALLA GRANDE", "FILM / 04", .89f, 8.dp, 11.dp,
    )
    PlayerDesignStyle.V6 -> LegacyPlayerIdentity(
        "EXPRESIVO", "CADA NOTA TIENE VIDA", "EXP / 06", .73f, 50.dp, 42.dp,
    )
    else -> LegacyPlayerIdentity(
        "GALAXOT", "TU UNIVERSO MUSICAL", "GALAXOT", .75f, 20.dp, 25.dp,
    )
}

@Composable
fun GalaxLegacyStyleScene(
    style: PlayerDesignStyle,
    isLandscape: Boolean,
    isPlaying: Boolean,
    accent: Color,
    queuePeekHeight: Dp,
    artwork: @Composable BoxScope.() -> Unit,
    controls: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val identity = remember(style) { legacyPlayerIdentity(style) }
    Box(
        modifier = modifier.fillMaxSize()
            .background(scheme.background)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
    ) {
        GalaxLegacyBackdrop(
            style = style,
            accent = accent,
            modifier = Modifier.fillMaxSize(),
        )
        if (style == PlayerDesignStyle.V4) {
            GalaxOrbitField(
                modifier = Modifier.fillMaxSize(),
                active = isPlaying,
                accent = accent,
                intensity = .17f,
            )
        }
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize()
                    .padding(
                        start = 15.dp, end = 16.dp,
                        bottom = queuePeekHeight + 12.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                BoxWithConstraints(
                    modifier = Modifier.weight(1.02f).fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    val artworkSize = min(maxWidth.value * identity.artworkScale, maxHeight.value * .89f).dp
                    LegacyArtworkFrame(style, identity, Modifier.size(artworkSize), artwork)
                }
                Column(
                    modifier = Modifier.weight(.98f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    GalaxLegacyHeader(style, identity, accent)
                    LegacyControlPanel(style, identity, accent, controls)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(bottom = queuePeekHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                GalaxLegacyHeader(style, identity, accent)
                BoxWithConstraints(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val artworkSize = min(maxWidth.value * identity.artworkScale, maxHeight.value * .91f).dp
                    LegacyArtworkFrame(style, identity, Modifier.size(artworkSize), artwork)
                }
                LegacyControlPanel(style, identity, accent, controls)
                Spacer(Modifier.height(13.dp))
            }
        }
    }
}

@Composable
private fun LegacyArtworkFrame(
    style: PlayerDesignStyle,
    identity: LegacyPlayerIdentity,
    modifier: Modifier = Modifier,
    artwork: @Composable BoxScope.() -> Unit,
) {
    val outline: Shape = when (style) {
        PlayerDesignStyle.V6 -> RoundedCornerShape(topStart=70.dp, topEnd=14.dp, bottomStart=19.dp, bottomEnd=70.dp)
        PlayerDesignStyle.V2 -> RoundedCornerShape(42.dp)
        else -> RoundedCornerShape(identity.cornerDp)
    }
    Box(
        modifier = modifier.clip(outline)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        artwork()
        if (style == PlayerDesignStyle.V4) {
            Canvas(Modifier.fillMaxSize()) {
                val line = 6.dp.toPx()
                drawRect(Color.Black.copy(alpha = .39f), Offset(0f, 0f), Size(size.width, line))
                drawRect(Color.Black.copy(alpha = .39f), Offset(0f, size.height - line), Size(size.width, line))
                drawLine(Color.White.copy(alpha = .62f), Offset(size.width*.12f, 10.dp.toPx()),
                    Offset(size.width*.88f,10.dp.toPx()), 1.dp.toPx())
            }
        }
        if (style == PlayerDesignStyle.V3) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Color.White.copy(alpha = .44f),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, 1.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun GalaxLegacyHeader(
    style: PlayerDesignStyle,
    identity: LegacyPlayerIdentity,
    accent: Color,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = if (style == PlayerDesignStyle.V3) 14.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                identity.mark,
                color = if (style == PlayerDesignStyle.V3) scheme.onSurfaceVariant else accent,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = if (style == PlayerDesignStyle.V3) 2.9.sp else 1.7.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "GALAXOT / AUDIO",
                color = scheme.onSurfaceVariant.copy(alpha = .70f),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = .7.sp,
            )
        }
        Text(
            identity.name,
            color = scheme.onSurface,
            style = if (style == PlayerDesignStyle.V4)
                MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            fontWeight = if (style == PlayerDesignStyle.V3) FontWeight.Light else FontWeight.Black,
            letterSpacing = if (style == PlayerDesignStyle.V3) 2.8.sp else .3.sp,
        )
        Text(
            identity.tagline,
            color = scheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = .7.sp,
        )
    }
}

@Composable
private fun LegacyControlPanel(
    style: PlayerDesignStyle,
    identity: LegacyPlayerIdentity,
    accent: Color,
    controls: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val container = when (style) {
        PlayerDesignStyle.V1 -> scheme.surfaceContainer.copy(alpha = .79f)
        PlayerDesignStyle.V2 -> scheme.primaryContainer.copy(alpha = .31f)
        PlayerDesignStyle.V3 -> Color.Transparent
        PlayerDesignStyle.V4 -> Color.Black.copy(alpha = .67f)
        PlayerDesignStyle.V6 -> accent.copy(alpha = .15f)
        else -> scheme.surfaceContainer
    }
    Box(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = if (style == PlayerDesignStyle.V3) 17.dp else 9.dp)
            .clip(RoundedCornerShape(identity.panelDp))
            .background(container)
            .padding(top = 8.dp, bottom = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        controls()
    }
}

@Composable
private fun GalaxLegacyBackdrop(
    style: PlayerDesignStyle,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val base = scheme.background
    Box(
        modifier = modifier.drawWithCache {
            val gradient = when (style) {
                PlayerDesignStyle.V1 -> Brush.verticalGradient(
                    listOf(base, scheme.surfaceContainerLow, base),
                )
                PlayerDesignStyle.V2 -> Brush.linearGradient(
                    listOf(
                        lerp(accent, base, .85f),
                        scheme.surfaceContainerLow,
                        lerp(scheme.tertiary, base, .84f),
                    ),
                )
                PlayerDesignStyle.V3 -> Brush.verticalGradient(
                    listOf(base, base),
                )
                PlayerDesignStyle.V4 -> Brush.verticalGradient(
                    listOf(Color.Black, lerp(accent, Color.Black, .80f), Color.Black),
                )
                PlayerDesignStyle.V6 -> Brush.verticalGradient(
                    listOf(
                        lerp(accent, base, .74f),
                        lerp(scheme.tertiary, base, .78f),
                        base,
                    ),
                )
                else -> Brush.verticalGradient(listOf(base,base))
            }
            onDrawBehind {
                drawRect(gradient)
                when (style) {
                    PlayerDesignStyle.V1 -> {
                        for (i in 0..5) {
                            drawLine(
                                scheme.outlineVariant.copy(alpha = .22f),
                                Offset(size.width * .13f, size.height * (.08f + i * .16f)),
                                Offset(size.width * .87f, size.height * (.08f + i * .16f)),
                                1.dp.toPx(),
                            )
                        }
                    }
                    PlayerDesignStyle.V2 -> {
                        drawCircle(
                            Brush.radialGradient(
                                listOf(accent.copy(alpha = .14f), Color.Transparent),
                                center = Offset(size.width * .9f, size.height*.24f),
                                radius = size.width,
                            ),
                            radius = size.width,
                            center = Offset(size.width*.9f,size.height*.24f),
                        )
                    }
                    PlayerDesignStyle.V3 -> {
                        drawRect(
                            scheme.onBackground.copy(alpha = .15f),
                            topLeft = Offset(size.width * .08f, size.height*.12f),
                            size = Size(2.dp.toPx(),size.height*.46f),
                        )
                    }
                    PlayerDesignStyle.V4 -> {
                        drawRect(Color.Black.copy(alpha = .46f), Offset.Zero, Size(size.width,size.height*.06f))
                        drawRect(Color.Black.copy(alpha = .46f),
                            Offset(0f,size.height*.93f), Size(size.width,size.height*.07f))
                    }
                    PlayerDesignStyle.V6 -> {
                        for (i in 0..3) {
                            val radius = size.width * (.29f+i*.27f)
                            drawCircle(
                                accent.copy(alpha = .075f),
                                radius=radius,
                                center=Offset(size.width*.83f,size.height*.3f),
                                style=Stroke(width=(1+i).dp.toPx()),
                            )
                        }
                    }
                    else -> Unit
                }
            }
        },
    )
}
