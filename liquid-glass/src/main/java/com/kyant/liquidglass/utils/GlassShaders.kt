package com.kyant.liquidglass.utils

import org.intellij.lang.annotations.Language

internal object GlassShaders {

    @Language("AGSL")
    internal val sdRectangleShaderUtils = """
float sdRectangle(float2 coord, float2 halfSize) {
    float2 d = abs(coord) - halfSize;
    float outside = length(max(d, 0.0));
    float inside = min(max(d.x, d.y), 0.0);
    return outside + inside;
}

float sdRoundedRectangle(float2 coord, float2 halfSize, float cornerRadius) {
    float2 innerHalfSize = halfSize - float2(cornerRadius);
    return sdRectangle(coord, innerHalfSize) - cornerRadius;
}

float2 gradSdRoundedRectangle(float2 coord, float2 halfSize, float cornerRadius) {
    float2 innerHalfSize = halfSize - float2(cornerRadius);
    float2 cornerCoord = abs(coord) - innerHalfSize;
    
    float insideCorner = step(0.0, min(cornerCoord.x, cornerCoord.y)); // 1 if in corner
    float xMajor = step(cornerCoord.y, cornerCoord.x); // 1 if x is major
    float2 gradEdge = float2(xMajor, 1.0 - xMajor);
    float2 gradCorner = normalize(cornerCoord);
    return sign(coord) * mix(gradEdge, gradCorner, insideCorner);
}"""

    @Language("AGSL")
    val refractionShaderString = """
uniform shader image;

uniform float2 size;
uniform float cornerRadius;

uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;

$sdRectangleShaderUtils

float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    float sd = sdRoundedRectangle(centeredCoord, halfSize, cornerRadius);
    
    if (-sd >= refractionHeight) {
        return image.eval(coord);
    }
    
    sd = min(sd, 0.0);
    float maxGradRadius = max(min(halfSize.x, halfSize.y), cornerRadius);
    float gradRadius = min(cornerRadius * 1.5, maxGradRadius);
    float2 normal = gradSdRoundedRectangle(centeredCoord, halfSize, gradRadius);
    
    float refractedDistance = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
    float2 refractedDirection = normalize(normal + depthEffect * normalize(centeredCoord));
    float2 refractedCoord = coord + refractedDistance * refractedDirection;
    
    return image.eval(refractedCoord);
}"""

    @Language("AGSL")
    val dispersionShaderString = """
uniform shader image;

uniform float2 size;
uniform float cornerRadius;

uniform float dispersionHeight;
uniform float dispersionAmount;

$sdRectangleShaderUtils

float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

float2 normalToTangent(float2 normal) {
    return float2(normal.y, -normal.x);
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    float sd = sdRoundedRectangle(centeredCoord, halfSize, cornerRadius);
    
    if (-sd >= dispersionHeight) {
        return image.eval(coord);
    }
    
    sd = min(sd, 0.0);
    float maxGradRadius = max(min(halfSize.x, halfSize.y), cornerRadius);
    float gradRadius = min(cornerRadius * 1.5, maxGradRadius);
    float2 normal = gradSdRoundedRectangle(centeredCoord, halfSize, gradRadius);
    float2 tangent = normalToTangent(normal);
    
    float dispersionDistance = circleMap(1.0 - -sd / dispersionHeight) * dispersionAmount;
    if (dispersionDistance < 2.0) {
        half4 color = image.eval(coord);
        return color;
    }
    
    half4 dispersedColor = half4(0.0);
    half4 weight = half4(0.0);
    float maxI = min(dispersionDistance, 20.0);
    for (float i = 0.0; i < 20.0; i++) {
        float t = i / maxI;
        if (t > 1.0) break;
        half4 color = image.eval(coord + tangent * float2(t - 0.5) * dispersionDistance);
        half rMask = step(0.5, t);
        half gMask = step(0.25, t) * step(t, 0.75);
        half bMask = step(t, 0.5);
        half4 mask = half4(rMask, gMask, bMask, 0.0);
        dispersedColor += color * mask;
        weight += mask;
    }
    dispersedColor /= weight;
    dispersedColor.a = image.eval(coord).a;
    return dispersedColor;
}"""

    @Language("AGSL")
    val highlightShaderString = """
uniform shader image;

uniform float2 size;
uniform float cornerRadius;
uniform float angle;
uniform float decay;

$sdRectangleShaderUtils

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    
    float2 grad = gradSdRoundedRectangle(centeredCoord, halfSize, cornerRadius);
    float2 topLightNormal = float2(-cos(angle), -sin(angle));
    float topLightFraction = dot(topLightNormal, grad);
    float bottomLightFraction = -topLightFraction;
    float fraction = pow(max(topLightFraction, bottomLightFraction), decay);
    
    return image.eval(coord) * fraction;
}"""
}
