package com.kyant.backdrop

import androidx.compose.ui.graphics.drawscope.DrawScope

sealed interface BackdropDrawScope : DrawScope {

    fun drawBackdrop()
}

class SimpleBackdropDrawScope(
    val drawScope: DrawScope,
    val onDrawBackdrop: DrawScope.() -> Unit
) : BackdropDrawScope, DrawScope by drawScope {

    override fun drawBackdrop() {
        onDrawBackdrop()
    }
}
