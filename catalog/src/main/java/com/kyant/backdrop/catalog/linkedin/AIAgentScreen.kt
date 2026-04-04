package com.kyant.backdrop.catalog.linkedin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AIAgentScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: AgentViewModel = viewModel(factory = AgentViewModel.Factory(context))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F7F2))
    ) {
        AgentSheetContent(
            viewModel = viewModel,
            surface = "global",
            surfaceContext = mapOf("surface" to "global", "entry" to "ai_agent_screen"),
            contentColor = Color.Black,
            accentColor = Color(0xFF2F80ED),
            onDismiss = onNavigateBack,
            isFullScreen = true
        )
    }
}
