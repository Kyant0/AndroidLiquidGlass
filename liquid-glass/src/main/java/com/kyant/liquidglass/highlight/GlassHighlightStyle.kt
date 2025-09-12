package com.kyant.liquidglass.highlight

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import com.kyant.liquidglass.utils.GlassShaders
import kotlin.math.PI

@Immutable
interface GlassHighlightStyle {

    @Stable
    fun createRenderEffect(size: Size, width: Float): RenderEffect? {
        return null
    }

    /**
     * Solid highlight effect.
     */
    @Immutable
    data object Solid : GlassHighlightStyle

    /**
     * Soft highlight effect with blur.
     */
    @Immutable
    data object Soft : GlassHighlightStyle {

        override fun createRenderEffect(size: Size, width: Float): RenderEffect? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val blurRadiusPx = width / 2f
                if (blurRadiusPx > 0f) {
                    RenderEffect.createBlurEffect(
                        blurRadiusPx,
                        blurRadiusPx,
                        Shader.TileMode.DECAL
                    )
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    /**
     * Dynamic highlight effect with shimmering effect.
     *
     * @param angle
     * The angle of the highlight in degrees.
     *
     * @param falloff
     * The falloff of the highlight. Higher value results in a sharper highlight.
     */
    @Immutable
    data class Dynamic(
        val angle: Float = 45f,
        @param:FloatRange(from = 0.0) val falloff: Float = 1f
    ) : GlassHighlightStyle {

        private var highlightShader: RuntimeShader? = null

        override fun createRenderEffect(size: Size, width: Float): RenderEffect? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val blurRadiusPx = width / 2f
                val blurRenderEffect =
                    if (blurRadiusPx > 0f) {
                        RenderEffect.createBlurEffect(
                            blurRadiusPx,
                            blurRadiusPx,
                            Shader.TileMode.DECAL
                        )
                    } else {
                        null
                    }

                val shader = highlightShader
                    ?: RuntimeShader(GlassShaders.dynamicHighlightStyleShaderString)
                        .also { highlightShader = it }

                val highlightRenderEffect =
                    RenderEffect.createRuntimeShaderEffect(
                        shader.apply {
                            setFloatUniform("size", size.width, size.height)

                            setFloatUniform("angle", angle * (PI / 180f).toFloat())
                            setFloatUniform("falloff", falloff)
                        },
                        "image"
                    )

                if (blurRenderEffect != null) {
                    RenderEffect.createChainEffect(
                        highlightRenderEffect,
                        blurRenderEffect
                    )
                } else {
                    highlightRenderEffect
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
