package com.arturo254.opentune

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.arturo254.opentune.constants.AppFontKey
import com.arturo254.opentune.constants.AppFontStyle
import com.arturo254.opentune.constants.CustomThemeColorKey
import com.arturo254.opentune.constants.DarkModeKey
import com.arturo254.opentune.constants.DynamicThemeKey
import com.arturo254.opentune.constants.LyricsSyncOffsetKey
import com.arturo254.opentune.constants.PureBlackKey
import com.arturo254.opentune.constants.ThumbnailCornerRadiusKey
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.ui.screens.settings.DarkMode
import com.arturo254.opentune.ui.screens.settings.ThemePalettes
import com.arturo254.opentune.ui.theme.ColorSaver
import com.arturo254.opentune.ui.theme.DefaultThemeColor
import com.arturo254.opentune.ui.theme.OpenTuneTheme
import com.arturo254.opentune.ui.theme.ThemeSeedPalette
import com.arturo254.opentune.ui.theme.ThemeSeedPaletteCodec
import com.arturo254.opentune.ui.theme.extractThemeColor
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

/**
 * Owns app theme preferences and dynamic artwork color extraction.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MainActivityThemeHost(
    activity: ComponentActivity,
    playerConnection: PlayerConnection?,
    systemUiController: MainActivitySystemUiController,
    content: @Composable (
        pureBlack: Boolean,
        lyricsSyncOffset: Int,
    ) -> Unit,
) {
    val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
    val customThemeColorValue by rememberPreference(CustomThemeColorKey, defaultValue = "default")
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val appFontStyle by rememberEnumPreference(AppFontKey, defaultValue = AppFontStyle.OPEN_TUNE)
    val lyricsSyncOffset by rememberPreference(LyricsSyncOffsetKey, defaultValue = 0)
    val artworkCornerRadius by rememberPreference(ThumbnailCornerRadiusKey, defaultValue = 16f)

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme =
        remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) {
                isSystemInDarkTheme
            } else {
                darkTheme == DarkMode.ON
            }
        }

    LaunchedEffect(useDarkTheme) {
        systemUiController.setSystemBarAppearance(useDarkTheme)
    }

    val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
    val pureBlack = pureBlackEnabled && useDarkTheme

    val customThemeSeedPalette =
        remember(customThemeColorValue) {
            if (customThemeColorValue.startsWith("#")) {
                null
            } else if (customThemeColorValue.startsWith("seedPalette:")) {
                ThemeSeedPaletteCodec.decodeFromPreference(customThemeColorValue)
            } else {
                ThemePalettes
                    .findById(customThemeColorValue)
                    ?.let {
                        ThemeSeedPalette(
                            primary = it.primary,
                            secondary = it.secondary,
                            tertiary = it.tertiary,
                            neutral = it.neutral,
                        )
                    }
            }
        }

    val customThemeColor =
        remember(customThemeColorValue, customThemeSeedPalette) {
            if (customThemeColorValue.startsWith("#")) {
                try {
                    val colorString = customThemeColorValue.removePrefix("#")
                    Color("#$colorString".toColorInt())
                } catch (_: Exception) {
                    DefaultThemeColor
                }
            } else {
                customThemeSeedPalette?.primary ?: DefaultThemeColor
            }
        }

    var themeColor by rememberSaveable(stateSaver = ColorSaver) {
        mutableStateOf(DefaultThemeColor)
    }

    LaunchedEffect(
        playerConnection,
        enableDynamicTheme,
        isSystemInDarkTheme,
        customThemeColor,
    ) {
        val connection = playerConnection
        if (!enableDynamicTheme || connection == null) {
            themeColor =
                if (!enableDynamicTheme) {
                    customThemeColor
                } else {
                    DefaultThemeColor
                }
            return@LaunchedEffect
        }

        connection.service.currentMediaMetadata.collectLatest { song ->
            if (song != null) {
                withContext(Dispatchers.Default) {
                    try {
                        val result =
                            activity.imageLoader.execute(
                                ImageRequest
                                    .Builder(activity)
                                    .data(song.thumbnailUrl)
                                    .allowHardware(false)
                                    .build(),
                            )
                        val extractedColor = result.image?.toBitmap()?.extractThemeColor()

                        withContext(Dispatchers.Main) {
                            themeColor = extractedColor ?: DefaultThemeColor
                        }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) {
                            themeColor = DefaultThemeColor
                        }
                    }
                }
            } else {
                themeColor =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        DefaultThemeColor
                    } else {
                        customThemeColor
                    }
            }
        }
    }

    OpenTuneTheme(
        darkTheme = useDarkTheme,
        pureBlack = pureBlack,
        motionScheme = MotionScheme.expressive(),
        themeColor = themeColor,
        seedPalette = if (!enableDynamicTheme) customThemeSeedPalette else null,
        appFontStyle = appFontStyle,
        artworkCornerRadiusDp = artworkCornerRadius,
    ) {
        content(
            pureBlack,
            lyricsSyncOffset,
        )
    }
}
