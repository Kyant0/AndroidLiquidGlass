package com.kyant.backdrop

import androidx.compose.ui.unit.Density

internal class MutableDensity : Density {

    override var density: Float = 1f

    override var fontScale: Float = 1f

    fun update(density: Density) {
        this.density = density.density
        fontScale = density.fontScale
    }
}
