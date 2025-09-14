package com.kyant.backdrop.catalog.destinations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.catalog.CatalogDestination
import com.kyant.backdrop.catalog.components.Text
import com.kyant.backdrop.catalog.theme.TextStyles

@Composable
fun HomeContent(onNavigate: (CatalogDestination) -> Unit) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .displayCutoutPadding()
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16f.dp)
    ) {
        Text(
            "Backdrop Catalog",
            TextStyles.headline,
            Modifier.padding(16f.dp, 40f.dp, 16f.dp, 16f.dp)
        )
        Column {
            Text(
                "Control center",
                TextStyles.body,
                Modifier
                    .clickable { onNavigate(CatalogDestination.ControlCenter) }
                    .padding(16f.dp)
                    .fillMaxWidth()
            )
            Text(
                "Dialog",
                TextStyles.body,
                Modifier
                    .clickable { onNavigate(CatalogDestination.Dialog) }
                    .padding(16f.dp)
                    .fillMaxWidth()
            )
        }
    }
}
