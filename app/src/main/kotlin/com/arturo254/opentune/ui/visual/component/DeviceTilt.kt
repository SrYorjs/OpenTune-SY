package com.arturo254.opentune.ui.visual.component

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val Follow = 0.9f

private const val Recentre = 0.012f

private const val Smooth = 0.22f

@Stable
class DeviceTilt internal constructor() {
    
    var rotationX by mutableFloatStateOf(0f)
        internal set

    
    var rotationY by mutableFloatStateOf(0f)
        internal set
}

@Composable
fun rememberDeviceTilt(maxDegrees: Float = 14f): DeviceTilt {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tilt = remember { DeviceTilt() }

    DisposableEffect(context, lifecycleOwner) {
        val sensors = context.getSystemService(SensorManager::class.java)
        val sensor = sensors?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensors?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensors == null || sensor == null) return@DisposableEffect onDispose { }

        val matrix = FloatArray(9)
        val angles = FloatArray(3)
        var restPitch = Float.NaN
        var restRoll = Float.NaN

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                SensorManager.getOrientation(matrix, angles)
                val pitch = Math.toDegrees(angles[1].toDouble()).toFloat()
                val roll = Math.toDegrees(angles[2].toDouble()).toFloat()
                if (restPitch.isNaN()) {
                    restPitch = pitch
                    restRoll = roll
                }
                val dPitch = pitch - restPitch
                val dRoll = wrapDegrees(roll - restRoll)
                restPitch += dPitch * Recentre
                restRoll = wrapDegrees(restRoll + dRoll * Recentre)

                val targetX = (-dPitch * Follow).coerceIn(-maxDegrees, maxDegrees)
                val targetY = (dRoll * Follow).coerceIn(-maxDegrees, maxDegrees)
                tilt.rotationX += (targetX - tilt.rotationX) * Smooth
                tilt.rotationY += (targetY - tilt.rotationY) * Smooth
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        var listening = false
        fun listen(on: Boolean) {
            if (on == listening) return
            listening = on
            if (on) {
                
                restPitch = Float.NaN
                sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            } else {
                sensors.unregisterListener(listener)
                tilt.rotationX = 0f
                tilt.rotationY = 0f
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> listen(true)
                Lifecycle.Event.ON_PAUSE -> listen(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) listen(true)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            listen(false)
        }
    }
    return tilt
}

private fun wrapDegrees(d: Float): Float {
    var x = d % 360f
    if (x > 180f) x -= 360f
    if (x < -180f) x += 360f
    return x
}
