package com.arturo254.opentune.ui.visual.utils

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.MotionDurationScale
import androidx.navigation.NavBackStackEntry

val AnimationSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

object AppMotion {
    
    var enabled by mutableStateOf(true)

    
    var speed by mutableFloatStateOf(1f)

    
    var elementBlur by mutableStateOf(true)

    private var systemScale by mutableFloatStateOf(1f)

    
    val durationScale: MotionDurationScale = object : MotionDurationScale {
        override val scaleFactor: Float
            get() = if (!enabled) 0f else systemScale / speed.coerceAtLeast(0.1f)
    }

    
    fun followSystemScale(context: Context): ContentObserver {
        val resolver = context.contentResolver
        fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        systemScale = read()
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                systemScale = read()
            }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        return observer
    }
}

fun animationSpeedLabel(speed: Float): String {
    val text = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0')
    return "${text}x"
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.motionEnter(
    transition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?,
): EnterTransition? = if (AppMotion.enabled) transition() else EnterTransition.None

fun AnimatedContentTransitionScope<NavBackStackEntry>.motionExit(
    transition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?,
): ExitTransition? = if (AppMotion.enabled) transition() else ExitTransition.None

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultMotionEnter(
    transition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition,
): EnterTransition = if (AppMotion.enabled) transition() else EnterTransition.None

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultMotionExit(
    transition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition,
): ExitTransition = if (AppMotion.enabled) transition() else ExitTransition.None
