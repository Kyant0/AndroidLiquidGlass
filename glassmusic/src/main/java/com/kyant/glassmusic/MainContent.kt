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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kyant.expressa.graphics.Hct.Companion.toHct
import com.kyant.expressa.m3.LocalColorScheme
import com.kyant.expressa.m3.LocalStaticColors
import com.kyant.expressa.m3.shape.CornerShape
import com.kyant.expressa.prelude.*
import com.kyant.expressa.ui.Icon
import com.kyant.expressa.ui.LocalContentColor
import com.kyant.expressa.ui.Text
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
    val background = surfaceContainer
    val liquidGlassProviderState = rememberLiquidGlassProviderState(background)

    val iconButtonLiquidGlassStyle =
        GlassStyle(
            CornerShape.full,
            innerRefraction = InnerRefraction(
                height = RefractionHeight(8.dp),
                amount = RefractionAmount.Full
            ),
            material = GlassMaterial(
                brush = SolidColor(surfaceBright.copy(alpha = 0.5f))
            )
        )

    Box(
        Modifier.fillMaxSize()
    ) {
        Box(
            Modifier
                .liquidGlassProvider(liquidGlassProviderState)
                .fillMaxSize()
        ) {
            SongsContent()
        }

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
                    .shadow(
                        elevation = 8.dp,
                        shape = CornerShape.full,
                        ambientColor = shadow.copy(alpha = 0.5f),
                        spotColor = shadow.copy(alpha = 0.5f)
                    )
                    .liquidGlass(liquidGlassProviderState, iconButtonLiquidGlassStyle)
                    .clickable {}
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back_24px),
                    "Navigate back",
                    Modifier.size(22.dp)
                )
            }

            Box(
                Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = CornerShape.full,
                        ambientColor = shadow.copy(alpha = 0.5f),
                        spotColor = shadow.copy(alpha = 0.5f)
                    )
                    .liquidGlass(liquidGlassProviderState, iconButtonLiquidGlassStyle)
                    .clickable {}
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.more_vert_24px),
                    "More options",
                    Modifier.size(22.dp)
                )
            }
        }

        Column(
            Modifier
                .padding(32.dp, 8.dp)
                .safeDrawingPadding()
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier
                    .liquidGlass(
                        liquidGlassProviderState,
                        GlassStyle(
                            CornerShape.full,
                            innerRefraction = InnerRefraction(
                                height = RefractionHeight(8.dp),
                                amount = RefractionAmount((-28).dp)
                            ),
                            material = GlassMaterial(
                                blurRadius = 8.dp,
                                brush = SolidColor(Color.White),
                                alpha = 0.5f
                            )
                        )
                    )
                    .height(56.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {}

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                            icon = { Icon(painterResource(R.drawable.home_24px)) },
                            label = { Text("Songs") }
                        )

                        MainNavTab.Library -> BottomTab(
                            icon = { Icon(painterResource(R.drawable.library_music_24px)) },
                            label = { Text("Library") }
                        )

                        MainNavTab.Settings -> BottomTab(
                            icon = { Icon(painterResource(R.drawable.settings_24px)) },
                            label = { Text("Settings") }
                        )
                    }
                }

                CompositionLocalProvider(LocalStaticColors provides yellowStaticColors) {
                    CompositionLocalProvider(LocalContentColor provides onAccentContainer) {
                        Box(
                            Modifier
                                .liquidGlass(
                                    liquidGlassProviderState,
                                    GlassStyle(
                                        CornerShape.full,
                                        innerRefraction = InnerRefraction(
                                            height = RefractionHeight(8.dp),
                                            amount = RefractionAmount.Full
                                        ),
                                        material = GlassMaterial(
                                            brush = SolidColor(accentContainer.copy(alpha = 0.8f))
                                        ),
                                        highlight = GlassHighlight.Default.copy(
                                            width = 2.dp,
                                            color = accentContainer.toHct().copy(
                                                chroma = 200.0,
                                                tone = if (LocalColorScheme.current.isDark) 90.0 else 85.0
                                            ).toColor(),
                                            blendMode = BlendMode.Overlay
                                        )
                                    )
                                )
                                .clickable {}
                                .size(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(R.drawable.search_24px),
                                "Search",
                                Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
