package com.kyant.backdrop

/**
 * Platform-abstracted runtime shader cache.
 *
 * On Android: caches [android.graphics.RuntimeShader] instances.
 * On wasmJs: caches [org.jetbrains.skia.RuntimeShaderBuilder] instances.
 */
expect sealed interface RuntimeShaderCache

internal expect fun RuntimeShaderCacheImpl(): RuntimeShaderCache

internal expect fun RuntimeShaderCache.clearCache()
