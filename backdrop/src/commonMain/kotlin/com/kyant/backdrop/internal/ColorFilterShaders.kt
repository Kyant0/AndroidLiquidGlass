package com.kyant.backdrop.internal

internal const val TintShaderString = """
uniform shader content;
layout(color) uniform float4 color;
uniform int mode;
float4 main(float2 p) {
    float4 dst = content.eval(p);
    float4 src = float4(color.rgb * color.a, color.a);
    float4 result;
    if (mode == 0) result = float4(0.0);
    else if (mode == 1) result = src;
    else if (mode == 2) result = dst;
    else if (mode == 3) result = src + dst * (1.0 - src.a);
    else if (mode == 4) result = dst + src * (1.0 - dst.a);
    else if (mode == 5) result = src * dst.a;
    else if (mode == 6) result = dst * src.a;
    else if (mode == 7) result = src * (1.0 - dst.a);
    else if (mode == 8) result = dst * (1.0 - src.a);
    else if (mode == 9) result = src * dst.a + dst * (1.0 - src.a);
    else if (mode == 10) result = dst * src.a + src * (1.0 - dst.a);
    else if (mode == 11) result = src * (1.0 - dst.a) + dst * (1.0 - src.a);
    else if (mode == 12) result = src + dst;
    else if (mode == 13) result = src * dst;
    else if (mode == 14) result = src * dst + src * (1.0 - dst.a) + dst * (1.0 - src.a);
    else result = src + dst - src * dst;
    result.a = clamp(result.a, 0.0, 1.0);
    return result;
}
"""

internal const val LightingShaderString = """
uniform shader content;
layout(color) uniform float4 multiplyColor;
layout(color) uniform float4 addColor;
float4 main(float2 p) {
    float4 color = content.eval(p);
    float3 rgb = color.a > 0.0 ? color.rgb / color.a : float3(0.0);
    return float4((rgb * multiplyColor.rgb + addColor.rgb) * color.a, color.a);
}
"""
