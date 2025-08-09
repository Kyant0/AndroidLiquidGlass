package com.kyant.glassmusic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.unit.dp

class BottomTabsScope {

    inner class BottomTab(
        val icon: @Composable (color: ColorProducer) -> Unit,
        val label: @Composable (color: ColorProducer) -> Unit,
        val modifier: Modifier = Modifier
    ) {

        @Composable
        internal fun Content(
            contentColor: ColorProducer,
            modifier: Modifier = Modifier
        ) {
            Column(
                modifier
                    .then(this.modifier)
                    .height(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
            ) {
                icon(contentColor)
                label(contentColor)
            }
        }
    }
}
