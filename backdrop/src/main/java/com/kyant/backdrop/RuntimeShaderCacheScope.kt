package com.kyant.backdrop

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

interface RuntimeShaderCacheScope {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun obtainRuntimeShader(key: String, string: String): RuntimeShader
}
