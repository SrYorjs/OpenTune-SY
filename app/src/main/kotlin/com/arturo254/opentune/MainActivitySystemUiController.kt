package com.arturo254.opentune

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.arturo254.opentune.constants.AppLanguageKey
import com.arturo254.opentune.constants.DisableScreenshotKey
import com.arturo254.opentune.constants.SYSTEM_DEFAULT
import com.arturo254.opentune.utils.PreferenceStore
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.setAppLocale
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns MainActivity window/system configuration so the activity can remain a
 * thin lifecycle + Compose host.
 */
internal class MainActivitySystemUiController(
    private val activity: ComponentActivity,
    private val scope: CoroutineScope,
) {
    fun configureInitialWindow() {
        activity.window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    }

    fun initializeLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return

        val initialLocale =
            PreferenceStore.get(AppLanguageKey)
                ?.takeUnless { it == SYSTEM_DEFAULT }
                ?.let(Locale::forLanguageTag)
                ?: Locale.getDefault()

        setAppLocale(activity, initialLocale)

        scope.launch(Dispatchers.IO) {
            runCatching {
                activity.dataStore.data.first()[AppLanguageKey]
            }.onSuccess { language ->
                val targetLocale =
                    language
                        ?.takeUnless { it == SYSTEM_DEFAULT }
                        ?.let(Locale::forLanguageTag)
                        ?: Locale.getDefault()

                if (targetLocale != initialLocale) {
                    withContext(Dispatchers.Main) {
                        setAppLocale(activity, targetLocale)
                        activity.recreate()
                    }
                }
            }
        }
    }

    fun observeScreenshotProtection() {
        scope.launch(Dispatchers.IO) {
            activity.dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest { disableScreenshots ->
                    withContext(Dispatchers.Main) {
                        if (disableScreenshots) {
                            activity.window.setFlags(
                                WindowManager.LayoutParams.FLAG_SECURE,
                                WindowManager.LayoutParams.FLAG_SECURE,
                            )
                        } else {
                            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }
                }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(
            activity.window,
            activity.window.decorView.rootView,
        ).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            activity.window.statusBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            activity.window.navigationBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }

    fun updateSystemBarsVisibility(
        isYearInMusicScreen: Boolean,
        isAlwaysOnDisplayScreen: Boolean,
        isPlayerExpanded: Boolean,
        playerFullscreen: Boolean,
    ) {
        val controller =
            WindowCompat.getInsetsController(
                activity.window,
                activity.window.decorView,
            )

        when {
            isAlwaysOnDisplayScreen -> {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }

            isPlayerExpanded && playerFullscreen -> {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }

            isYearInMusicScreen -> {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }

            else -> {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
