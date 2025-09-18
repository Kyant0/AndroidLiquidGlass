package com.kyant.backdrop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope

internal class SimpleGraphicsLayerScope : GraphicsLayerScope {

    val matrix = Matrix()

    override var size: Size = Size.Unspecified
    override var density: Float = 1f
    override var fontScale: Float = 1f

    override var scaleX: Float = 1f
    override var scaleY: Float = 1f
    override var alpha: Float = 0f
    override var translationX: Float = 0f
    override var translationY: Float = 0f
    override var shadowElevation: Float = 0f
    override var rotationX: Float = 0f
    override var rotationY: Float = 0f
    override var rotationZ: Float = 0f
    override var cameraDistance: Float = 8f
    override var transformOrigin: TransformOrigin by mutableStateOf(TransformOrigin.Center)
    override var shape: Shape = RectangleShape
    override var clip: Boolean = false
    override var renderEffect: RenderEffect? = null
    override var ambientShadowColor: Color = Color.Unspecified
    override var spotShadowColor: Color = Color.Unspecified
    override var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto
    override var blendMode: BlendMode = BlendMode.SrcOver
    override var colorFilter: ColorFilter? = null

    fun apply(scope: DrawScope, layerBlock: GraphicsLayerScope.() -> Unit) {
        size = scope.size
        density = scope.density
        fontScale = scope.fontScale

        layerBlock()

        matrix.resetToPivotedTransform(
            rotationX = rotationX,
            rotationY = rotationY,
            rotationZ = rotationZ,
            scaleX = scaleX,
            scaleY = scaleY
        )
        matrix.invert()
    }
}
