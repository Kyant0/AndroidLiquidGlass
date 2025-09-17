package com.kyant.backdrop.highlight

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import com.kyant.backdrop.DynamicHighlightShaderString
import kotlin.math.PI

@Immutable
interface HighlightStyle {

    @RequiresApi(Build.VERSION_CODES.S)
    fun createRenderEffect(size: Size, width: Float): RenderEffect? {
        return null
    }

    @Immutable
    data object Solid : HighlightStyle

    @Immutable
    data object Soft : HighlightStyle

    @Immutable
    data class Dynamic(
        val angle: Float = 45f,
        @param:FloatRange(from = 0.0) val falloff: Float = 2f
    ) : HighlightStyle {

        @RequiresApi(Build.VERSION_CODES.S)
        override fun createRenderEffect(size: Size, width: Float): RenderEffect? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                RenderEffect.createRuntimeShaderEffect(
                    RuntimeShader(DynamicHighlightShaderString).apply {
                        setFloatUniform("size", size.width, size.height)
                        setFloatUniform("angle", angle * (PI / 180f).toFloat())
                        setFloatUniform("falloff", falloff)
                    },
                    "image"
                )
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
