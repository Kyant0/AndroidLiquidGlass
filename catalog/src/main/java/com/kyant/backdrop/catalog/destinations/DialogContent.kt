package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.utils.LoremIpsum
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun DialogContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f)
        else Color(0xFF121212).copy(0.4f)
    val dimColor =
        if (isLightTheme) Color(0xFF29293A).copy(0.23f)
        else Color(0xFF121212).copy(0.56f)

    BackdropDemoScaffold(
        Modifier.drawWithContent {
            drawContent()
            drawRect(dimColor)
        },
        initialPainterResId = R.drawable.system_home_screen_light
    ) { backdrop ->
        Column(
            Modifier
                .padding(40f.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(48f.dp) },
                    highlight = { Highlight.Plain },
                    effects = {
                        colorControls(
                            brightness = if (isLightTheme) 0.2f else 0f,
                            saturation = 1.5f
                        )
                        blur(if (isLightTheme) 16f.dp.toPx() else 8f.dp.toPx())
                        refraction(24f.dp.toPx(), 48f.dp.toPx(), true)
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .fillMaxWidth()
        ) {
            BasicText(
                "Dialog Title",
                Modifier.padding(28f.dp, 24f.dp, 28f.dp, 12f.dp),
                style = TextStyle(contentColor, 24f.sp, FontWeight.Medium)
            )

            BasicText(
                LoremIpsum,
                Modifier
                    .then(
                        if (isLightTheme) {
                            // plus darker
                            Modifier
                        } else {
                            // plus lighter
                            Modifier.graphicsLayer(blendMode = BlendMode.Plus)
                        }
                    )
                    .padding(24f.dp, 12f.dp, 24f.dp, 12f.dp),
                style = TextStyle(contentColor.copy(0.68f), 15f.sp),
                maxLines = 5
            )

            Row(
                Modifier
                    .padding(24f.dp, 12f.dp, 24f.dp, 24f.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16f.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier
                        .clip(ContinuousCapsule)
                        .background(containerColor.copy(0.2f))
                        .clickable {}
                        .height(48f.dp)
                        .weight(1f)
                        .padding(horizontal = 16f.dp),
                    horizontalArrangement = Arrangement.spacedBy(4f.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        "Cancel",
                        style = TextStyle(contentColor, 16f.sp)
                    )
                }
                Row(
                    Modifier
                        .clip(ContinuousCapsule)
                        .background(accentColor)
                        .clickable {}
                        .height(48f.dp)
                        .weight(1f)
                        .padding(horizontal = 16f.dp),
                    horizontalArrangement = Arrangement.spacedBy(4f.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        "Okay",
                        style = TextStyle(Color.White, 16f.sp)
                    )
                }
            }
        }
    }
}
