/*
   Copyright 2025 Kyant

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.kyant.backdrop

import org.intellij.lang.annotations.Language

@Language("AGSL")
private const val RoundedRectSDF = """
float radiusAt(float2 coord, float4 radii) {
    if (coord.x >= 0.0) {
        if (coord.y <= 0.0) return radii.y;
        else return radii.z;
    } else {
        if (coord.y <= 0.0) return radii.x;
        else return radii.w;
    }
}

float sdRoundedRectangle(float2 coord, float2 halfSize, float4 radii) {
    float r = radiusAt(coord, radii);
    float2 innerHalfSize = halfSize - float2(r);
    float2 cornerCoord = abs(coord) - innerHalfSize;
    
    float outside = length(max(cornerCoord, 0.0)) - r;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;
}

float2 gradSdRoundedRectangle(float2 coord, float2 halfSize, float4 radii) {
    float r = radiusAt(coord, radii);
    float2 innerHalfSize = halfSize - float2(r);
    float2 cornerCoord = abs(coord) - innerHalfSize;
    
    float insideCorner = step(0.0, min(cornerCoord.x, cornerCoord.y)); // 1 if in corner
    float xMajor = step(cornerCoord.y, cornerCoord.x); // 1 if x is major
    float2 gradEdge = float2(xMajor, 1.0 - xMajor);
    float2 gradCorner = normalize(cornerCoord);
    return sign(coord) * mix(gradEdge, gradCorner, insideCorner);
}"""

@Language("AGSL")
internal const val RoundedRectRefractionShaderString = """
uniform shader content;

uniform float2 size;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;

$RoundedRectSDF

float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    float sd = sdRoundedRectangle(centeredCoord, halfSize, cornerRadii);
    if (-sd >= refractionHeight) {
        return content.eval(coord);
    }
    sd = min(sd, 0.0);
    
    float4 maxGradRadius = float4(min(halfSize.x, halfSize.y));
    float4 gradRadius = min(cornerRadii * 1.5, maxGradRadius);
    float2 normal = gradSdRoundedRectangle(centeredCoord, halfSize, gradRadius);
    
    float refractedDistance = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
    float2 refractedDirection = normalize(normal + depthEffect * normalize(centeredCoord));
    float2 refractedCoord = coord + refractedDistance * refractedDirection;
    
    return content.eval(refractedCoord);
}"""

@Language("AGSL")
internal const val RoundedRectDispersionShaderString = """
uniform shader content;

uniform float2 size;
uniform float4 cornerRadii;
uniform float dispersionHeight;
uniform float dispersionAmount;

$RoundedRectSDF

float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    float sd = sdRoundedRectangle(centeredCoord, halfSize, cornerRadii);
    if (-sd >= dispersionHeight) {
        return content.eval(coord);
    }
    sd = min(sd, 0.0);
    
    float dispersionDistance = circleMap(1.0 - -sd / dispersionHeight) * dispersionAmount;
    if (dispersionDistance < 2.0) {
        half4 color = content.eval(coord);
        return color;
    }
    
    float4 maxGradRadius = float4(min(halfSize.x, halfSize.y));
    float4 gradRadius = min(cornerRadii * 1.5, maxGradRadius);
    float2 normal = gradSdRoundedRectangle(centeredCoord, halfSize, gradRadius);
    float2 tangent = float2(normal.y, -normal.x);
    
    half4 dispersedColor = half4(0.0);
    half4 weight = half4(0.0);
    float maxI = ceil(min(dispersionDistance, 20.0));
    for (float i = 0.0; i < 20.0; i++) {
        float t = i / maxI;
        if (t > 1.0) break;
        half4 color = content.eval(coord + tangent * float2(t - 0.5) * dispersionDistance);
        half rMask = step(0.5, t);
        half gMask = step(0.25, t) * step(t, 0.75);
        half bMask = step(t, 0.5);
        half aMask = rMask + gMask + bMask;
        half4 mask = half4(rMask, gMask, bMask, aMask);
        dispersedColor += color * mask;
        weight += mask;
    }
    
    return dispersedColor / weight;
}"""

@Language("AGSL")
internal const val DefaultHighlightShaderString = """
uniform shader content;

uniform float2 size;
uniform float4 cornerRadii;
uniform float angle;
uniform float falloff;

$RoundedRectSDF

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    
    float4 maxGradRadius = float4(min(halfSize.x, halfSize.y));
    float4 gradRadius = min(cornerRadii * 1.5, maxGradRadius);
    float2 grad = gradSdRoundedRectangle(centeredCoord, halfSize, gradRadius);
    float2 normal = float2(-cos(angle), -sin(angle));
    float intensity = pow(abs(dot(normal, grad)), falloff);
    return content.eval(coord) * intensity;
}"""

@Language("AGSL")
internal const val AmbientHighlightShaderString = """
uniform shader content;

uniform float2 size;
uniform float4 cornerRadii;
uniform float angle;
uniform float falloff;

$RoundedRectSDF

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    
    float4 maxGradRadius = float4(min(halfSize.x, halfSize.y));
    float4 gradRadius = min(cornerRadii * 1.5, maxGradRadius);
    float2 grad = gradSdRoundedRectangle(centeredCoord, halfSize, gradRadius);
    float2 normal = float2(-cos(angle), -sin(angle));
    float d = dot(normal, grad);
    float alpha = content.eval(coord).a;
    float intensity = pow(abs(d), falloff);
    
    if (d > 0.0) {
        return half4(0.0, 0.0, 0.0, 1.0) * intensity * alpha;
    }
    if (d < 0.0) {
        return half4(1.0) * intensity * alpha;
    }
    return half4(0.0);
}"""

@Language("AGSL")
internal const val GammaAdjustmentShaderString = """
uniform shader content;

uniform float power;

half4 main(float2 coord) {
    half4 color = content.eval(coord);
    color.r = pow(color.r, power);
    color.g = pow(color.g, power);
    color.b = pow(color.b, power);
    return color;
}"""
