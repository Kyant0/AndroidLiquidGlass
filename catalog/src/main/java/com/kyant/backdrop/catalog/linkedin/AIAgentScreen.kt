package com.kyant.backdrop.catalog.linkedin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.data.SettingsPreferences

@Composable
fun AIAgentScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: AgentViewModel = viewModel(factory = AgentViewModel.Factory(context))
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = DefaultThemeModeKey)
    val glassBackgroundKey by SettingsPreferences.glassBackgroundPreset(context)
        .collectAsState(initial = DefaultGlassBackgroundPresetKey)
    val accentPaletteKey by SettingsPreferences.accentPalette(context)
        .collectAsState(initial = DefaultAccentPaletteKey)
    val glassMotionStyleKey by SettingsPreferences.glassMotionStyle(context)
        .collectAsState(initial = DefaultGlassMotionStyleKey)
    val reduceAnimations by SettingsPreferences.reduceAnimations(context).collectAsState(initial = false)
    val isGlassTheme = themeMode == "glass"
    val isDarkTheme = themeMode == "dark"
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    val accentColor = glassAccentPalette(accentPaletteKey).color
    val backdrop = rememberLayerBackdrop()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isGlassTheme) {
            GlassBackgroundLayer(
                modifier = Modifier
                    .layerBackdrop(backdrop)
                    .fillMaxSize(),
                backgroundKey = glassBackgroundKey,
                accentColor = accentColor,
                motionStyleKey = glassMotionStyleKey,
                reduceAnimations = reduceAnimations
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDarkTheme) Color.Black else Color(0xFFF9F7F2))
            )
        }

        AgentSheetContent(
            viewModel = viewModel,
            surface = "global",
            surfaceContext = mapOf("surface" to "global", "entry" to "ai_agent_screen"),
            userDisplayName = null,
            contentColor = contentColor,
            accentColor = accentColor,
            backdrop = backdrop,
            reduceAnimations = reduceAnimations,
            isDarkTheme = isDarkTheme,
            enableInlineNavigationActions = false,
            onDismiss = onNavigateBack,
            isFullScreen = true
        )
    }
}
