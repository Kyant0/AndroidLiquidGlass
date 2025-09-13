package com.kyant.liquidglass

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import com.kyant.backdrop.LayerBackdrop
import com.kyant.backdrop.backdrop

@Deprecated(message = "[Backdrop API] Use backdrop instead of state, use onDrawBackdrop instead of transformBlock")
fun Modifier.liquidGlass(
    state: LiquidGlassProviderState,
    style: GlassStyle,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    transformBlock: (DrawTransform.() -> Unit)? = null
): Modifier =
    this.liquidGlass(
        backdrop = state.backdrop,
        style = style,
        compositingStrategy = compositingStrategy,
        onDrawBackdrop = draw@{
            if (transformBlock != null) {
                withTransform(transformBlock) {
                    this@draw.drawBackdrop()
                }
            } else {
                drawBackdrop()
            }
        }
    )

@Deprecated(message = "[Backdrop API] Use backdrop instead of state, use onDrawBackdrop instead of transformBlock")
fun Modifier.liquidGlass(
    state: LiquidGlassProviderState,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    transformBlock: (DrawTransform.() -> Unit)? = null,
    style: () -> GlassStyle
): Modifier =
    this.liquidGlass(
        backdrop = state.backdrop,
        compositingStrategy = compositingStrategy,
        onDrawBackdrop = draw@{
            if (transformBlock != null) {
                withTransform(transformBlock) {
                    this@draw.drawBackdrop()
                }
            } else {
                drawBackdrop()
            }
        },
        style = style
    )

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

@Deprecated("[Backdrop API] Use `rememberLayerBackdrop` instead")
@Composable
fun rememberLiquidGlassProviderState(
    backgroundColor: Color?
): LiquidGlassProviderState {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(backgroundColor, graphicsLayer) {
        LiquidGlassProviderState(
            backgroundColor = backgroundColor,
            graphicsLayer = graphicsLayer
        )
    }
}

@Deprecated("[Backdrop API] Use `LayerBackdrop` instead")
@Stable
class LiquidGlassProviderState internal constructor(
    val backgroundColor: Color?,
    internal val graphicsLayer: GraphicsLayer
) {

    val backdrop: LayerBackdrop = LayerBackdrop(graphicsLayer, backgroundColor)

    internal var position: Offset
        get() = backdrop.backdropPosition
        set(value) {
            backdrop.backdropPosition = value
        }
}
