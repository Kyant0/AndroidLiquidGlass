package com.kyant.liquidglass

import androidx.compose.ui.graphics.drawscope.DrawScope

sealed interface BackdropDrawScope : DrawScope {

    fun drawBackdrop()
}

internal class BackdropDrawScopeImpl internal constructor(
    val drawScope: DrawScope,
    val onDrawBackdrop: DrawScope.() -> Unit
) : BackdropDrawScope, DrawScope by drawScope {

    override fun drawBackdrop() {
        onDrawBackdrop()
    }
}
