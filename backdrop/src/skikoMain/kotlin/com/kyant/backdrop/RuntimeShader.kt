package com.kyant.backdrop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.asComposeShader
import com.kyant.backdrop.internal.colorUniformComponents
import org.intellij.lang.annotations.Language
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

actual fun RuntimeShader(@Language("AGSL") shaderString: String): RuntimeShader {
    val shader = RuntimeShaderBuilder(RuntimeEffect.makeForShader(shaderString))
    return SkikoRuntimeShader(shader)
}

actual fun RuntimeShader.asComposeShader(): Shader {
    return this.asSkikoRuntimeShader().makeShader().asComposeShader()
}

fun RuntimeShader.asSkikoRuntimeShader(): RuntimeShaderBuilder {
    return (this as SkikoRuntimeShader).shader
}

internal class SkikoRuntimeShader(val shader: RuntimeShaderBuilder) : RuntimeShader {

    override fun setFloatUniform(name: String, value: Float) {
        shader.uniform(name, value)
    }

    override fun setFloatUniform(name: String, value1: Float, value2: Float) {
        shader.uniform(name, value1, value2)
    }

    override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float) {
        shader.uniform(name, value1, value2, value3)
    }

    override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float, value4: Float) {
        shader.uniform(name, value1, value2, value3, value4)
    }

    override fun setFloatUniform(name: String, values: FloatArray) {
        shader.uniform(name, values)
    }

    override fun setIntUniform(name: String, value: Int) {
        shader.uniform(name, value)
    }

    override fun setIntUniform(name: String, value1: Int, value2: Int) {
        shader.uniform(name, value1, value2)
    }

    override fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int) {
        shader.uniform(name, value1, value2, value3)
    }

    override fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int, value4: Int) {
        shader.uniform(name, value1, value2, value3, value4)
    }

    override fun setIntUniform(name: String, values: IntArray) {
        throw UnsupportedOperationException("Setting int array uniforms is not supported on Skia RuntimeShader.")
    }

    override fun setColorUniform(name: String, color: Color) {
        val components = colorUniformComponents(color)
        // Skia transforms layout(color) uniforms from unpremultiplied sRGB to the
        // destination color space. Premultiplication belongs in the shader.
        shader.uniform(name, components[0], components[1], components[2], color.alpha)
    }
}
