package com.kyant.backdrop

import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

actual sealed interface RuntimeShaderCache {

    fun obtainRuntimeShaderBuilder(
        key: String,
        string: String
    ): RuntimeShaderBuilder
}

internal class RuntimeShaderCacheImplWasmJs : RuntimeShaderCache {

    private val effects = mutableMapOf<String, RuntimeEffect>()

    override fun obtainRuntimeShaderBuilder(key: String, string: String): RuntimeShaderBuilder {
        val effect = effects.getOrPut(key) { RuntimeEffect.makeForShader(string) }
        return RuntimeShaderBuilder(effect)
    }

    fun clear() {
        effects.values.forEach { it.close() }
        effects.clear()
    }
}

internal actual fun RuntimeShaderCacheImpl(): RuntimeShaderCache = RuntimeShaderCacheImplWasmJs()

internal actual fun RuntimeShaderCache.clearCache() {
    (this as RuntimeShaderCacheImplWasmJs).clear()
}
