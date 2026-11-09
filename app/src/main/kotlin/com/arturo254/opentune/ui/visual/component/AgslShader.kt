package com.arturo254.opentune.ui.visual.component

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi

@RequiresApi(33)
internal fun createAgslShader(source: String): Any = RuntimeShader(source)

@RequiresApi(33)
internal fun Any.setAgslUniform(name: String, value: Float) {
    (this as RuntimeShader).setFloatUniform(name, value)
}

@RequiresApi(33)
internal fun Any.setAgslUniform(name: String, first: Float, second: Float) {
    (this as RuntimeShader).setFloatUniform(name, first, second)
}

@RequiresApi(33)
internal fun Any.setAgslUniform(name: String, first: Float, second: Float, third: Float) {
    (this as RuntimeShader).setFloatUniform(name, first, second, third)
}

@RequiresApi(33)
internal fun Any.setAgslColorUniform(name: String, color: Int) {
    (this as RuntimeShader).setColorUniform(name, color)
}

@RequiresApi(33)
internal fun agslRenderEffect(shader: Any, inputName: String): RenderEffect =
    RenderEffect.createRuntimeShaderEffect(shader as RuntimeShader, inputName)

@RequiresApi(33)
internal fun Any.setAgslInputShader(name: String, input: android.graphics.Shader) {
    (this as RuntimeShader).setInputShader(name, input)
}

@RequiresApi(33)
internal fun Any.asAgslPaintShader(): android.graphics.Shader = this as RuntimeShader
