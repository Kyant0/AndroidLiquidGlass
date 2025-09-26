package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.catalog.BackdropDemoScaffold
import com.kyant.backdrop.catalog.Block
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs

@Composable
fun BottomTabsContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White

    val airplaneModeIcon = painterResource(R.drawable.flight_40px)
    val iconColorFilter = ColorFilter.tint(contentColor)

    BackdropDemoScaffold(
        initialPainterResId = R.drawable.system_home_screen_light
    ) { backdrop ->
        Column(verticalArrangement = Arrangement.spacedBy(32f.dp)) {
            Block {
                var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

                LiquidBottomTabs(
                    selectedTabIndex = { selectedTabIndex },
                    onTabSelected = { selectedTabIndex = it },
                    backdrop = backdrop,
                    tabsCount = 3,
                    modifier = Modifier.padding(horizontal = 36f.dp)
                ) {
                    repeat(3) { index ->
                        LiquidBottomTab({ selectedTabIndex = index }) {
                            Box(
                                Modifier
                                    .size(28f.dp)
                                    .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                            )
                            BasicText(
                                "Tab ${index + 1}",
                                style = TextStyle(contentColor, 12f.sp)
                            )
                        }
                    }
                }
            }
            Block {
                var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

                LiquidBottomTabs(
                    selectedTabIndex = { selectedTabIndex },
                    onTabSelected = { selectedTabIndex = it },
                    backdrop = backdrop,
                    tabsCount = 4,
                    modifier = Modifier.padding(horizontal = 36f.dp)
                ) {
                    repeat(4) { index ->
                        LiquidBottomTab({ selectedTabIndex = index }) {
                            Box(
                                Modifier
                                    .size(28f.dp)
                                    .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                            )
                            BasicText(
                                "Tab ${index + 1}",
                                style = TextStyle(contentColor, 12f.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}
