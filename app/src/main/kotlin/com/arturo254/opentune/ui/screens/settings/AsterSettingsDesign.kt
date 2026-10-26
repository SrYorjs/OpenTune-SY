package com.arturo254.opentune.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.screens.AsterBackdrop
import com.arturo254.opentune.ui.screens.AsterScene
import com.arturo254.opentune.ui.screens.asterPalette
import com.arturo254.opentune.ui.screens.rememberAsterPhase
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class AsterSettingsSection(val icon: Int, val description: Int, val geometry: Int, val title: Int) {
    HUB(R.drawable.settings, R.string.aster_settings_hub_description, 0, R.string.aster_settings_hub_title),
    APPEARANCE(R.drawable.palette, R.string.aster_settings_appearance_description, 0, R.string.aster_settings_appearance_title),
    PLAYER(R.drawable.play, R.string.aster_settings_player_description, 1, R.string.aster_settings_player_title),
    CONTENT(R.drawable.language, R.string.aster_settings_content_description, 2, R.string.aster_settings_content_title),
    STORAGE(R.drawable.storage, R.string.aster_settings_storage_description, 3, R.string.aster_settings_storage_title),
    PRIVACY(R.drawable.security, R.string.aster_settings_privacy_description, 4, R.string.aster_settings_privacy_title),
    INTEGRATIONS(R.drawable.integration, R.string.aster_settings_integrations_description, 5, R.string.aster_settings_integrations_title),
    STUDIO(R.drawable.palette, R.string.aster_settings_studio_description, 1, R.string.aster_settings_studio_title),
    GLASS(R.drawable.palette, R.string.aster_settings_glass_description, 0, R.string.aster_settings_glass_title),
    PALETTE(R.drawable.palette, R.string.aster_settings_palette_description, 2, R.string.aster_settings_palette_title),
    THEME(R.drawable.palette, R.string.aster_settings_theme_description, 2, R.string.aster_settings_theme_title),
    AOD(R.drawable.bedtime, R.string.aster_settings_aod_description, 4, R.string.aster_settings_aod_title),
    WIDGET(R.drawable.settings, R.string.aster_settings_widget_description, 3, R.string.aster_settings_widget_title),
    BACKUP(R.drawable.storage, R.string.aster_settings_backup_description, 3, R.string.aster_settings_backup_title),
    DISCORD(R.drawable.discord, R.string.aster_settings_discord_description, 5, R.string.aster_settings_discord_title),
    TOGETHER(R.drawable.fire, R.string.aster_settings_together_description, 5, R.string.aster_settings_together_title),
    LASTFM(R.drawable.integration, R.string.aster_settings_lastfm_description, 1, R.string.aster_settings_lastfm_title),
    EXPERIMENTAL(R.drawable.settings, R.string.aster_settings_experimental_description, 2, R.string.aster_settings_experimental_title),
    DEBUG(R.drawable.settings, R.string.aster_settings_debug_description, 1, R.string.aster_settings_debug_title),
    UPDATE(R.drawable.update, R.string.aster_settings_update_description, 0, R.string.aster_settings_update_title),
    CHANGELOG(R.drawable.history, R.string.aster_settings_changelog_description, 3, R.string.aster_settings_changelog_title),
    LOGIN(R.drawable.account, R.string.aster_settings_login_description, 5, R.string.aster_settings_login_title),
    AUTO(R.drawable.play, R.string.aster_settings_auto_description, 1, R.string.aster_settings_auto_title),
    ABOUT(R.drawable.opentune, R.string.aster_settings_about_description, 0, R.string.aster_settings_about_title),
    TOKEN(R.drawable.token, R.string.aster_settings_token_description, 4, R.string.aster_settings_token_title),
    BACKGROUND(R.drawable.palette, R.string.aster_settings_background_description, 2, R.string.aster_settings_background_title),
    ACCOUNT(R.drawable.account, R.string.aster_settings_account_description, 5, R.string.aster_settings_account_title),
}

val LocalAsterSettingsSection = compositionLocalOf<AsterSettingsSection?> { null }
internal val LocalAsterSettingsPhase = compositionLocalOf<State<Float>> { mutableStateOf(0f) }

@Composable
fun AsterSettingsEnvironment(
    section: AsterSettingsSection,
    modifier: Modifier = Modifier.fillMaxSize(),
    content: @Composable () -> Unit,
) {
    val palette = asterPalette()
    val phase = rememberAsterPhase(active = true)
    val original = MaterialTheme.colorScheme
    val colors = original.copy(
        primary = palette.violet,
        onPrimary = palette.base,
        primaryContainer = lerp(palette.panel, palette.violet, .16f),
        onPrimaryContainer = palette.ink,
        secondary = palette.ice,
        onSecondary = palette.base,
        tertiary = palette.rose,
        onTertiary = palette.base,
        background = palette.base,
        onBackground = palette.ink,
        surface = palette.base,
        onSurface = palette.ink,
        surfaceVariant = palette.panel,
        onSurfaceVariant = palette.muted,
        surfaceContainerLowest = palette.base,
        surfaceContainerLow = lerp(palette.base, palette.panel, .65f),
        surfaceContainer = palette.panel,
        surfaceContainerHigh = lerp(palette.panel, palette.violet, .05f),
        surfaceContainerHighest = lerp(palette.panel, palette.violet, .09f),
        outline = palette.muted.copy(alpha = .65f),
        outlineVariant = palette.line,
    )
    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(26.dp),
            extraLarge = RoundedCornerShape(32.dp),
        ),
    ) {
        CompositionLocalProvider(
            LocalAsterSettingsSection provides section,
            LocalAsterSettingsPhase provides phase,
        ) {
            Box(modifier) {
                AsterBackdrop(AsterScene.SEARCH, phase, Modifier.matchParentSize())
                content()
            }
        }
    }
}

@Composable
fun AsterSettingsScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = Color.Transparent,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsterSettingsTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().background(scheme.background.copy(alpha = .96f))) {
        TopAppBar(
            title = {
                ProvideTextStyle(MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)) {
                    title()
                }
            },
            navigationIcon = navigationIcon,
            actions = actions,
            windowInsets = windowInsets,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = scheme.onSurface,
                navigationIconContentColor = scheme.onSurface,
                actionIconContentColor = scheme.primary,
            ),
            scrollBehavior = scrollBehavior,
        )
        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = .55f), thickness = .5.dp)
    }
}

@Composable
fun AsterSettingsHero(
    title: String,
    modifier: Modifier = Modifier,
    description: String = stringResource((LocalAsterSettingsSection.current ?: AsterSettingsSection.HUB).description),
    section: AsterSettingsSection = LocalAsterSettingsSection.current ?: AsterSettingsSection.HUB,
) {
    val colors = MaterialTheme.colorScheme
    val phase = LocalAsterSettingsPhase.current
    val accent = when (section.geometry % 3) {
        1 -> colors.secondary
        2 -> colors.tertiary
        else -> colors.primary
    }
    Column(
        modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.aster_settings_collection),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text((section.ordinal + 1).toString().padStart(2, '0'), color = accent,
                style = MaterialTheme.typography.labelMedium)
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Light,
            letterSpacing = (-1.3).sp,
            color = colors.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(description, style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant, lineHeight = 21.sp)
            }
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.matchParentSize()) {
                    val t = phase.value * 2f * PI.toFloat()
                    val r = size.minDimension * .43f
                    when (section.geometry) {
                        0 -> rotate(-25f, center) {
                            repeat(4) { index ->
                                drawOval(accent.copy(alpha = .16f + index * .1f),
                                    topLeft = center - Offset(r, r * (.2f + index * .12f)),
                                    size = androidx.compose.ui.geometry.Size(r * 2, r * (.4f + index * .24f)),
                                    style = Stroke(1.dp.toPx()))
                            }
                        }
                        1 -> repeat(13) { index ->
                            val x = size.width * .12f + index * size.width * .064f
                            val h = r * (.18f + .65f * kotlin.math.abs(sin(t + index * .5f)))
                            drawLine(accent.copy(alpha = .25f + index % 3 * .18f),
                                Offset(x, center.y - h), Offset(x, center.y + h), 2.dp.toPx())
                        }
                        2 -> rotate(35f + phase.value * 20f, center) {
                            repeat(4) { index ->
                                val edge = r * (.45f + index * .18f)
                                drawRect(accent.copy(alpha = .2f + index * .1f), center - Offset(edge, edge),
                                    androidx.compose.ui.geometry.Size(edge * 2, edge * 2), style = Stroke(1.dp.toPx()))
                            }
                        }
                        3 -> repeat(5) { index ->
                            val y = size.height * (.18f + index * .16f)
                            drawLine(accent.copy(alpha = .18f + index * .12f), Offset(size.width * .12f, y),
                                Offset(size.width * (.85f - index * .08f), y), 3.dp.toPx())
                        }
                        4 -> repeat(4) { index ->
                            drawArc(accent.copy(alpha = .2f + index * .15f), 45f + index * 30f, 235f, false,
                                center - Offset(r, r) * (1f - index * .2f),
                                androidx.compose.ui.geometry.Size(r * 2, r * 2) * (1f - index * .2f),
                                style = Stroke(1.dp.toPx()))
                        }
                        else -> repeat(6) { index ->
                            val angle = index * PI.toFloat() / 3 + t * .15f
                            val point = center + Offset(cos(angle), sin(angle)) * r
                            drawLine(accent.copy(alpha = .22f), center, point, 1.dp.toPx())
                            drawCircle(accent.copy(alpha = .7f), 3.dp.toPx(), point)
                        }
                    }
                }
            }
        }
        Box(Modifier.width(40.dp).height(2.dp).background(accent, CircleShape))
    }
}

@Composable
fun AsterSettingsHeading(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(3.dp).height(14.dp).background(colors.primary, CircleShape))
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
            color = colors.onSurfaceVariant, modifier = Modifier.weight(1f).semantics { heading() })
        if (trailing != null) Text(trailing, color = colors.primary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun AsterSettingsPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(.75.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
fun AsterPreferenceRow(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supporting: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    grouped: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val body: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp)
                .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (icon != null) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                    .background(scheme.primary.copy(alpha = if (enabled) .1f else .04f)),
                    contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(LocalContentColor provides scheme.primary.copy(alpha = if (enabled) 1f else .38f)) {
                        Box(Modifier.size(21.dp), contentAlignment = Alignment.Center) { icon() }
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                CompositionLocalProvider(LocalContentColor provides scheme.onSurface.copy(alpha = if (enabled) 1f else .38f)) {
                    ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)) { title() }
                }
                if (supporting != null) CompositionLocalProvider(
                    LocalContentColor provides scheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else .38f),
                ) { ProvideTextStyle(MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp)) { supporting() } }
            }
            if (trailing != null) trailing()
            else if (onClick != null) Icon(painterResource(R.drawable.navigate_next), null,
                Modifier.size(18.dp), tint = scheme.onSurfaceVariant.copy(alpha = if (enabled) .6f else .25f))
        }
    }
    if (grouped) Box(modifier) { body() }
    else AsterSettingsPanel(modifier.padding(horizontal = 18.dp, vertical = 4.dp)) { body() }
}

@Composable
fun AsterSettingsSwitch(
    title: @Composable () -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    grouped: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    AsterPreferenceRow(
        title = title,
        modifier = modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        supporting = description?.let { { Text(it) } },
        icon = icon,
        enabled = enabled,
        grouped = grouped,
        trailing = {
            Switch(checked = checked, onCheckedChange = null, enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = scheme.primary,
                    checkedThumbColor = scheme.onPrimary,
                    checkedBorderColor = Color.Transparent,
                    uncheckedTrackColor = scheme.surfaceContainerHighest,
                    uncheckedThumbColor = scheme.onSurfaceVariant,
                    uncheckedBorderColor = scheme.outline,
                ))
        },
    )
}

@Composable
fun AsterSettingsPageIntro(modifier: Modifier = Modifier) {
    val section = LocalAsterSettingsSection.current ?: AsterSettingsSection.HUB
    AsterSettingsHero(title = stringResource(section.title), modifier = modifier)
}

@androidx.compose.ui.tooling.preview.Preview(name = "Settings · Light", widthDp = 360)
@androidx.compose.ui.tooling.preview.Preview(name = "Settings · Dark", widthDp = 360,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@androidx.compose.ui.tooling.preview.Preview(name = "Settings · Large text", widthDp = 320, fontScale = 1.6f)
@Composable
private fun AsterSettingsControlsPreview() {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    var checked by remember { mutableStateOf(true) }
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        AsterSettingsEnvironment(AsterSettingsSection.PRIVACY) {
            Column {
                AsterSettingsPageIntro()
                AsterSettingsSwitch(
                    title = { Text(stringResource(R.string.pause_listen_history)) },
                    description = stringResource(R.string.aster_settings_privacy_description),
                    icon = { Icon(painterResource(R.drawable.history), null) },
                    checked = checked,
                    onCheckedChange = { checked = it },
                )
                AsterSettingsSwitch(
                    title = { Text(stringResource(R.string.pause_search_history)) },
                    icon = { Icon(painterResource(R.drawable.search), null) },
                    checked = false,
                    onCheckedChange = {},
                    enabled = false,
                )
            }
        }
    }
}
