package com.kyant.backdrop

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Shape

internal actual fun createBackdropEffectScope(shapeProvider: () -> Shape): BackdropEffectScopeImpl {
    return object : BackdropEffectScopeImpl() {
        override val shape: Shape get() = shapeProvider()

        private val _cache = RuntimeShaderCacheImpl() as RuntimeShaderCacheImplAndroid

        override val runtimeShaderCache: RuntimeShaderCache get() = _cache

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun obtainRuntimeShader(key: String, string: String): RuntimeShader {
            return _cache.obtainRuntimeShader(key, string)
        }
    }
}
