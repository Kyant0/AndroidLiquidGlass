package com.kyant.backdrop

import androidx.compose.ui.graphics.drawscope.DrawScope

fun interface BackdropSurfaceDrawScope {

    fun DrawScope.draw()
}

object NoOpBackdropSurfaceDrawScope : BackdropSurfaceDrawScope {

    override fun DrawScope.draw() {}
}
