package com.kyant.glassmusic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kyant.blur.BlurStyle
import com.kyant.blur.Highlight
import com.kyant.blur.blur
import com.kyant.blur.highlight
import com.kyant.liquidglass.GlassStyle
import com.kyant.liquidglass.highlight.GlassHighlight
import com.kyant.liquidglass.liquidGlass
import com.kyant.liquidglass.liquidGlassProvider
import com.kyant.liquidglass.material.GlassMaterial
import com.kyant.liquidglass.refraction.InnerRefraction
import com.kyant.liquidglass.refraction.RefractionAmount
import com.kyant.liquidglass.refraction.RefractionHeight
import com.kyant.liquidglass.rememberLiquidGlassProviderState

@Composable
fun MainContent() {
    val background = Color.White
    val liquidGlassProviderState = rememberLiquidGlassProviderState(background)

    val iconButtonLiquidGlassStyle =
        GlassStyle(
            CircleShape,
            innerRefraction = InnerRefraction(
                height = RefractionHeight(8.dp),
                amount = RefractionAmount.Full
            ),
            material = GlassMaterial(
                brush = SolidColor(background.copy(alpha = 0.5f))
            )
        )

    Box(
        Modifier.fillMaxSize()
    ) {
        // content
        Box(
            Modifier
                .liquidGlassProvider(liquidGlassProviderState)
                .fillMaxSize()
        ) {
            SongsContent()
        }

        // top bar
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .safeDrawingPadding()
                .height(56.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .liquidGlass(liquidGlassProviderState, iconButtonLiquidGlassStyle)
                    .clickable {}
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.paint(
                        painterResource(R.drawable.arrow_back_24px),
                        colorFilter = ColorFilter.tint(Color.Black)
                    )
                )
            }

            Box(
                Modifier
                    .liquidGlass(liquidGlassProviderState, iconButtonLiquidGlassStyle)
                    .clickable {}
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.paint(
                        painterResource(R.drawable.more_vert_24px),
                        colorFilter = ColorFilter.tint(Color.Black)
                    )
                )
            }
        }

        // bottom bars
        Column(
            Modifier
                .padding(32.dp, 8.dp)
                .safeDrawingPadding()
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .highlight(Highlight(CircleShape))
                    .blur(
                        liquidGlassProviderState.backdrop,
                        BlurStyle(
                            CircleShape,
                            24.dp
                        )
                    )
                    .height(56.dp)
                    .fillMaxWidth()
            )

            Box(
                Modifier
                    .liquidGlass(
                        liquidGlassProviderState,
                        GlassStyle(
                            CircleShape,
                            innerRefraction = InnerRefraction(
                                height = RefractionHeight(8.dp),
                                amount = RefractionAmount.Half
                            )
                        )
                    )
                    .height(56.dp)
                    .fillMaxWidth()
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val selectedTab = remember { mutableStateOf(MainNavTab.Songs) }
                BottomTabs(
                    tabs = MainNavTab.entries,
                    selectedTabState = selectedTab,
                    liquidGlassProviderState = liquidGlassProviderState,
                    background = background,
                    modifier = Modifier.weight(1f)
                ) { tab ->
                    when (tab) {
                        MainNavTab.Songs -> BottomTab(
                            icon = { color ->
                                Box(
                                    Modifier.paint(
                                        painterResource(R.drawable.home_24px),
                                        colorFilter = ColorFilter.tint(color())
                                    )
                                )
                            },
                            label = { color ->
                                BasicText("Songs", color = color)
                            }
                        )

                        MainNavTab.Library -> BottomTab(
                            icon = { color ->
                                Box(
                                    Modifier.paint(
                                        painterResource(R.drawable.library_music_24px),
                                        colorFilter = ColorFilter.tint(color())
                                    )
                                )
                            },
                            label = { color ->
                                BasicText("Library", color = color)
                            }
                        )

                        MainNavTab.Settings -> BottomTab(
                            icon = { color ->
                                Box(
                                    Modifier.paint(
                                        painterResource(R.drawable.settings_24px),
                                        colorFilter = ColorFilter.tint(color())
                                    )
                                )
                            },
                            label = { color ->
                                BasicText("Settings", color = color)
                            }
                        )
                    }
                }

                Box(
                    Modifier
                        .liquidGlass(
                            liquidGlassProviderState,
                            GlassStyle(
                                CircleShape,
                                innerRefraction = InnerRefraction(
                                    height = RefractionHeight(8.dp),
                                    amount = RefractionAmount.Full
                                ),
                                material = GlassMaterial(
                                    brush = SolidColor(Color(0xFFFDD835).copy(alpha = 0.8f))
                                ),
                                highlight = GlassHighlight.Default.copy(
                                    width = 2.dp,
                                    color = Color(0xFFFFF59D),
                                    blendMode = BlendMode.Overlay
                                )
                            )
                        )
                        .clickable {}
                        .size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.paint(
                            painterResource(R.drawable.search_24px),
                            colorFilter = ColorFilter.tint(Color.Black)
                        )
                    )
                }
            }
        }
    }
}
