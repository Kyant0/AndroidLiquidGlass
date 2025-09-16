package com.kyant.backdrop

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

interface BackdropEffectScope : Density {

    val size: Size

    val layoutDirection: LayoutDirection

    val shape: Shape

    var renderEffect: RenderEffect?

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun obtainRuntimeShader(key: String, string: String): RuntimeShader
}

internal abstract class BackdropEffectScopeImpl : BackdropEffectScope {

    private val runtimeShaders = mutableMapOf<String, RuntimeShader>()

    override var density: Float by mutableFloatStateOf(1f)
    override var fontScale: Float by mutableFloatStateOf(1f)
    override var size: Size by mutableStateOf(Size.Unspecified)
    override var layoutDirection: LayoutDirection by mutableStateOf(LayoutDirection.Ltr)
    override var renderEffect: RenderEffect? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun obtainRuntimeShader(key: String, string: String): RuntimeShader {
        return runtimeShaders.getOrPut(key) { RuntimeShader(string) }
    }

    fun applyDrawScope(scope: CacheDrawScope) {
        density = scope.density
        fontScale = scope.fontScale
        size = scope.size
        layoutDirection = scope.layoutDirection
        renderEffect = null
    }
}
