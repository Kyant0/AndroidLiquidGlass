package com.kyant.backdrop

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

internal interface BackdropShapeProvider {

    val innerShape: Shape

    val shape: Shape
}

@Immutable
internal class CachedBackdropShapeProvider(
    val shapeProvider: () -> Shape
) : BackdropShapeProvider {

    private var _outline: Outline? = null
    private var _size: Size = Size.Unspecified
    private var _layoutDirection: LayoutDirection? = null
    private var _density: Density? = null

    override val innerShape: Shape
        get() = shapeProvider()

    override val shape: Shape = object : Shape {

        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ): Outline {
            if (_outline == null || _size != size || _layoutDirection != layoutDirection || _density != density) {
                _size = size
                _layoutDirection = layoutDirection
                _density = density
                _outline = innerShape.createOutline(size, layoutDirection, density)
            }

            return _outline!!
        }
    }
}
