package com.kyant.backdrop.highlight

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.asComposeShader
import com.kyant.backdrop.AmbientHighlightShaderString
import com.kyant.backdrop.DefaultHighlightShaderString
import com.kyant.backdrop.RuntimeShaderCache
import kotlin.math.PI

@Immutable
data class DefaultHighlightStyle(
    override val color: Color = Color.White.copy(alpha = 0.5f),
    override val blendMode: BlendMode = BlendMode.Plus,
    val angle: Float = 45f,
    val falloff: Float = 1f
) : HighlightStyle {

    override fun DrawScope.createShader(
        shape: Shape,
        runtimeShaderCache: RuntimeShaderCache
    ): Shader? {
        val builder = runtimeShaderCache.obtainRuntimeShaderBuilder(
            "Default",
            DefaultHighlightShaderString
        )
        builder.uniform("size", size.width, size.height)
        val radii = getCornerRadii(shape)
        builder.uniform("cornerRadii", radii[0], radii[1], radii[2], radii[3])
        val c = color.copy(alpha = 1f)
        builder.uniform("color", c.red, c.green, c.blue, c.alpha)
        builder.uniform("angle", angle * (PI / 180f).toFloat())
        builder.uniform("falloff", falloff)
        return builder.makeShader().asComposeShader()
    }
}

@Immutable
data class AmbientHighlightStyle(
    val intensity: Float = 0.38f
) : HighlightStyle {

    override val color: Color = Color.White.copy(alpha = intensity)

    override val blendMode: BlendMode = DrawScope.DefaultBlendMode

    override fun DrawScope.createShader(
        shape: Shape,
        runtimeShaderCache: RuntimeShaderCache
    ): Shader? {
        val builder = runtimeShaderCache.obtainRuntimeShaderBuilder(
            "Ambient",
            AmbientHighlightShaderString
        )
        builder.uniform("size", size.width, size.height)
        val radii = getCornerRadii(shape)
        builder.uniform("cornerRadii", radii[0], radii[1], radii[2], radii[3])
        builder.uniform("angle", 45f * (PI / 180f).toFloat())
        builder.uniform("falloff", 1f)
        return builder.makeShader().asComposeShader()
    }
}

internal actual fun expect_DefaultHighlightStyle(): HighlightStyle = DefaultHighlightStyle()
internal actual fun expect_AmbientHighlightStyle(): HighlightStyle = AmbientHighlightStyle()
