package com.kyant.backdrop

import android.graphics.RenderEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

interface BackdropDrawScope : Density {

    val size: Size

    val layoutDirection: LayoutDirection

    var shape: Shape?

    var clip: Boolean

    var renderEffect: RenderEffect?

    fun obtainGraphicsLayer(key: String): GraphicsLayer

    fun onDrawBackdrop(block: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit)

    fun onDrawSurface(block: DrawScope.() -> Unit)
}
