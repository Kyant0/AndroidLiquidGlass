package com.kyant.backdrop

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

actual sealed interface RuntimeShaderCache {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun obtainRuntimeShader(
        key: String,
        string: String
    ): RuntimeShader
}

internal class RuntimeShaderCacheImplAndroid : RuntimeShaderCache {

    private val runtimeShaders = mutableMapOf<String, RuntimeShader>()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun obtainRuntimeShader(key: String, string: String): RuntimeShader {
        return runtimeShaders.getOrPut(key) { RuntimeShader(string) }
    }

    fun clear() {
        runtimeShaders.clear()
    }
}

internal actual fun RuntimeShaderCacheImpl(): RuntimeShaderCache = RuntimeShaderCacheImplAndroid()

internal actual fun RuntimeShaderCache.clearCache() {
    (this as RuntimeShaderCacheImplAndroid).clear()
}
