package com.kyant.blur

import org.intellij.lang.annotations.Language

@Suppress("ConstPropertyName")
internal object BlurShaders {

    @Language("AGSL")
    const val dynamicHighlightStyleShaderString = """
uniform shader image;

uniform float2 size;

uniform float angle;
uniform float falloff;

float2 gradSdRoundedRectangle(float2 coord, float2 halfSize, float cornerRadius) {
    float2 innerHalfSize = halfSize - float2(cornerRadius);
    float2 cornerCoord = abs(coord) - innerHalfSize;
    
    float insideCorner = step(0.0, min(cornerCoord.x, cornerCoord.y)); // 1 if in corner
    float xMajor = step(cornerCoord.y, cornerCoord.x); // 1 if x is major
    float2 gradEdge = float2(xMajor, 1.0 - xMajor);
    float2 gradCorner = normalize(cornerCoord);
    return sign(coord) * mix(gradEdge, gradCorner, insideCorner);
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    
    float2 grad = gradSdRoundedRectangle(centeredCoord, halfSize, min(halfSize.x, halfSize.y));
    float2 normal = float2(-cos(angle), -sin(angle));
    float intensity = pow(abs(dot(normal, grad)), falloff);
    
    return image.eval(coord) * intensity;
}"""
}
