package com.kyant.backdrop.highlight

import android.graphics.Shader
import android.os.Build
import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import com.kyant.backdrop.DynamicHighlightShaderString
import com.kyant.backdrop.RuntimeShaderCacheScope
import kotlin.math.PI

@Immutable
interface HighlightStyle {

    fun RuntimeShaderCacheScope.createShader(size: Size): Shader?

    @Immutable
    data object Solid : HighlightStyle {

        override fun RuntimeShaderCacheScope.createShader(size: Size): Shader? = null
    }

    @Immutable
    data class Dynamic(
        val angle: Float = 45f,
        @param:FloatRange(from = 0.0) val falloff: Float = 1f
    ) : HighlightStyle {

        override fun RuntimeShaderCacheScope.createShader(size: Size): Shader? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                obtainRuntimeShader("Dynamic", DynamicHighlightShaderString).apply {
                    setFloatUniform("size", size.width, size.height)
                    setFloatUniform("angle", angle * (PI / 180f).toFloat())
                    setFloatUniform("falloff", falloff)
                }
            } else {
                null
            }
        }
    }

    companion object {

        @Stable
        val Default: Dynamic = Dynamic()
    }
}
