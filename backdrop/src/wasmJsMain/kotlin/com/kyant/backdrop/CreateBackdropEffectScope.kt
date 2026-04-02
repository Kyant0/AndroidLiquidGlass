package com.kyant.backdrop

import org.jetbrains.skia.RuntimeShaderBuilder
import androidx.compose.ui.graphics.Shape

internal actual fun createBackdropEffectScope(shapeProvider: () -> Shape): BackdropEffectScopeImpl {
    return object : BackdropEffectScopeImpl() {
        override val shape: Shape get() = shapeProvider()

        private val _cache = RuntimeShaderCacheImpl() as RuntimeShaderCacheImplWasmJs

        override val runtimeShaderCache: RuntimeShaderCache get() = _cache

        override fun obtainRuntimeShaderBuilder(key: String, string: String): RuntimeShaderBuilder {
            return _cache.obtainRuntimeShaderBuilder(key, string)
        }
    }
}
