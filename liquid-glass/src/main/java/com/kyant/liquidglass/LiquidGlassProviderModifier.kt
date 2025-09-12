package com.kyant.liquidglass

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

@Deprecated(message = "[Backdrop API] Use backdrop modifier instead")
fun Modifier.liquidGlassProvider(state: LiquidGlassProviderState): Modifier =
    this.backdrop(state.backdrop)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LiquidGlassProviderElement(
    val state: LiquidGlassProviderState
) : ModifierNodeElement<LiquidGlassProviderNode>() {

    override fun create(): LiquidGlassProviderNode {
        return LiquidGlassProviderNode(state = state)
    }

    override fun update(node: LiquidGlassProviderNode) {
        node.update(state = state)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "liquidGlassProvider"
        properties["state"] = state
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LiquidGlassProviderElement) return false

        if (state != other.state) return false

        return true
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LiquidGlassProviderNode(
    var state: LiquidGlassProviderState
) : DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    override fun ContentDrawScope.draw() {
        drawContent()

        state.graphicsLayer.record {
            state.backgroundColor?.let { drawRect(it) }
            this@draw.drawContent()
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            state.position = coordinates.positionOnScreen()
        }
    }

    fun update(
        state: LiquidGlassProviderState
    ) {
        this.state = state
    }
}
