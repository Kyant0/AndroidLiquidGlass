package com.kyant.backdrop

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates

fun Modifier.backdrop(backdrop: Backdrop): Modifier {
    val element = backdrop.element
    return if (element != null) {
        this then element
    } else {
        this
    }
}

interface Backdrop {

    val element: Modifier.Element?

    fun DrawScope.drawBackdrop(coordinates: LayoutCoordinates)
}
