package com.kyant.backdrop.catalog.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.BackdropEffectScope
import org.intellij.lang.annotations.Language
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource

@Composable
fun rememberSdfShader(drawableResource: DrawableResource): SdfShader {
    val imageResource = imageResource(drawableResource)
    return remember(imageResource) { SdfShader(imageResource) }
}

expect fun SdfShader(imageBitmap: ImageBitmap): SdfShader

@Immutable
interface SdfShader {

    val width: Int

    val height: Int

    fun BackdropEffectScope.apply(
        refractionHeight: Float = 48f.dp.toPx(),
        lightAngle: Float = 45f
    )
}

@Language("AGSL")
internal const val SdfShaderString =
    """
uniform shader content;
uniform shader sdfTex;

uniform float2 size;
uniform float2 sdfTexSize;
uniform float refractionHeight;
uniform float lightAngle;

float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    half2 p = coord / size * sdfTexSize;
    if (p.x < 0.0 || p.y < 0.0 || p.x >= sdfTexSize.x || p.y >= sdfTexSize.y) {
        return half4(0.0);
    }
    half4 v = sdfTex.eval(p);
    float sd = v.r * 2.0 - 1.0;
    v.a = smoothstep(0.5, 1.0, v.a);
    if (v.a <= 0.0) {
        return half4(0.0);
    }
    if (v.a < 1.0) {
        sd = 0.0;
    }
    float2 normal = normalize(v.gb * 2.0 - 1.0);
    
    float intensity = circleMap(1.0 - min(1.0, -sd * 1.5));
    float2 refractedCoord = coord - intensity * refractionHeight * normal;

    half4 color = content.eval(refractedCoord) * v.a;
    float2 lightDir = float2(cos(lightAngle * 3.1415926 / 180.0), sin(lightAngle * 3.1415926 / 180.0));
    float bevelIntensity = clamp(dot(normal, lightDir), 0.0, 1.0);
    color.rgb *= 1.0 + 0.5 * intensity * bevelIntensity;
    bevelIntensity = clamp(dot(normal, -lightDir), 0.0, 1.0);
    color.rgb *= 1.0 + 0.5 * bevelIntensity * min(1.0, smoothstep(1.0, 0.0, abs(intensity - 0.25) * 6.0));
    return color;
}"""
