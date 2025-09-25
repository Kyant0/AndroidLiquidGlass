package com.kyant.backdrop

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

sealed interface RuntimeShaderCacheScope {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun obtainRuntimeShader(key: String, string: String): RuntimeShader
}

internal class RuntimeShaderCacheScopeImpl : RuntimeShaderCacheScope {

    private val runtimeShaders = mutableMapOf<String, RuntimeShader>()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun obtainRuntimeShader(key: String, string: String): RuntimeShader {
        return runtimeShaders.getOrPut(key) { RuntimeShader(string) }
    }
}
