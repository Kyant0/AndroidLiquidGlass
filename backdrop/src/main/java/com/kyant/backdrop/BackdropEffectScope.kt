package com.kyant.backdrop

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

interface BackdropEffectScope : Density {

    val size: Size

    val layoutDirection: LayoutDirection

    val shape: Shape

    var renderEffect: RenderEffect?

    val cornerRadii: FloatArray
        get() {
            val shape = shape
            require(shape is CornerBasedShape) { "Only CornerBasedShape is supported." }
            val isLtr = layoutDirection == LayoutDirection.Ltr
            return floatArrayOf(
                if (isLtr) shape.topStart.toPx(size, this)
                else shape.topEnd.toPx(size, this),
                if (isLtr) shape.topEnd.toPx(size, this)
                else shape.topStart.toPx(size, this),
                if (isLtr) shape.bottomEnd.toPx(size, this)
                else shape.bottomStart.toPx(size, this),
                if (isLtr) shape.bottomStart.toPx(size, this)
                else shape.bottomEnd.toPx(size, this)
            )
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun obtainRuntimeShader(key: String, string: String): RuntimeShader
}
