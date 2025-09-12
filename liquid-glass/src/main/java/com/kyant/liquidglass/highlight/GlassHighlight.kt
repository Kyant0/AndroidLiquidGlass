package com.kyant.liquidglass.highlight

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.liquidglass.utils.GlassShaders
import kotlin.math.PI

/**
 * The highlight effect applied to the liquid glass.
 */
@Immutable
sealed interface GlassHighlight {

    val width: Dp

    val color: Color

    val blendMode: BlendMode

    @Stable
    fun createRenderEffect(size: Size, density: Density, cornerRadius: Float): RenderEffect? {
        return null
    }

    /**
     * No highlight effect.
     */
    @Immutable
    data object None : GlassHighlight {

        override val width: Dp = Dp.Unspecified

        override val color: Color = Color.Unspecified

        override val blendMode: BlendMode = DrawScope.DefaultBlendMode
    }

    /**
     * Solid highlight effect.
     *
     * @param width
     * The width of the highlight.
     *
     * @param color
     * The color of the highlight.
     *
     * @param blendMode
     * The blend mode of the highlight.
     */
    @Immutable
    data class Solid(
        override val width: Dp = 1f.dp,
        override val color: Color = Color.White.copy(alpha = 0.4f),
        override val blendMode: BlendMode = BlendMode.Plus
    ) : GlassHighlight

    /**
     * Soft highlight effect with blur.
     *
     * @param width
     * The width of the highlight.
     *
     * @param color
     * The color of the highlight.
     *
     * @param blendMode
     * The blend mode of the highlight.
     */
    @Immutable
    data class Soft(
        override val width: Dp = 1f.dp,
        override val color: Color = Color.White.copy(alpha = 0.4f),
        override val blendMode: BlendMode = BlendMode.Plus
    ) : GlassHighlight {

        override fun createRenderEffect(size: Size, density: Density, cornerRadius: Float): RenderEffect? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val blurRadiusPx = with(density) { (width / 2f).toPx() }
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
     * @param width
     * The width of the highlight.
     *
     * @param color
     * The color of the highlight.
     *
     * @param blendMode
     * The blend mode of the highlight.
     *
     * @param angle
     * The angle of the highlight in degrees.
     *
     * @param falloff
     * The falloff of the highlight. Higher value results in a sharper highlight.
     */
    @Immutable
    data class Dynamic(
        override val width: Dp = 1f.dp,
        override val color: Color = Color.White.copy(alpha = 0.4f),
        override val blendMode: BlendMode = BlendMode.Plus,
        val angle: Float = 45f,
        @param:FloatRange(from = 0.0) val falloff: Float = 1f
    ) : GlassHighlight {

        private var highlightShader: RuntimeShader? = null

        override fun createRenderEffect(size: Size, density: Density, cornerRadius: Float): RenderEffect? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val blurRadiusPx = with(density) { (width / 2f).toPx() }
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
                    ?: RuntimeShader(GlassShaders.highlightShaderString)
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

        @Stable
        val Solid: Solid = Solid()

        @Stable
        val Soft: Soft = Soft()
    }
}
