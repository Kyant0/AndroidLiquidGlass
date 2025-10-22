package com.kyant.backdrop.catalog.destinations

import android.graphics.RenderEffect
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect

@Composable
fun ProgressiveBlurContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val tintColor = if (isLightTheme) Color.White else Color(0xFF808080)

    BackdropDemoScaffold { backdrop ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16f.dp)
        ) {
            Box(
                Modifier
                    .drawPlainBackdrop(
                        backdrop = backdrop,
                        shape = { RectangleShape },
                        effects = {
                            blur(4f.dp.toPx())
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                effect(
                                    RenderEffect.createRuntimeShaderEffect(
                                        obtainRuntimeShader(
                                            "AlphaMask",
                                            """
uniform shader content;

uniform float2 size;
layout(color) uniform half4 tint;
uniform float tintIntensity;

half4 main(float2 coord) {
    float blurAlpha = smoothstep(size.y, size.y * 0.5, coord.y);
    float tintAlpha = smoothstep(size.y, size.y * 0.5, coord.y);
    return mix(content.eval(coord) * blurAlpha, tint * tintAlpha, tintIntensity);
}"""
                                        ).apply {
                                            setFloatUniform("size", size.width, size.height)
                                            setColorUniform("tint", tintColor.toArgb())
                                            setFloatUniform("tintIntensity", 0.8f)
                                        },
                                        "content"
                                    )
                                )
                            }
                        }
                    )
                    .height(128f.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                BasicText("alpha-masked progressive blur", style = TextStyle(contentColor, 16f.sp))
            }
        }
    }
}
