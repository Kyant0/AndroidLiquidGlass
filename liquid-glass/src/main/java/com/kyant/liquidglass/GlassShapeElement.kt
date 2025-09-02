package com.kyant.liquidglass

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateLayer
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset

internal class GlassShapeElement(
    val style: () -> GlassStyle,
    val compositingStrategy: CompositingStrategy
) : ModifierNodeElement<GlassShapeNode>() {

    override fun create(): GlassShapeNode {
        return GlassShapeNode(
            style = style,
            compositingStrategy = compositingStrategy
        )
    }

    override fun update(node: GlassShapeNode) {
        node.update(
            style = style,
            compositingStrategy = compositingStrategy
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassShape"
        properties["style"] = style
        properties["compositingStrategy"] = compositingStrategy
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GlassShapeElement) return false

        if (style != other.style) return false
        if (compositingStrategy != other.compositingStrategy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = style.hashCode()
        result = 31 * result + compositingStrategy.hashCode()
        return result
    }
}

internal class GlassShapeNode(
    var style: () -> GlassStyle,
    var compositingStrategy: CompositingStrategy
) : LayoutModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        clip = true
        shape = style().shape
        compositingStrategy = this@GlassShapeNode.compositingStrategy
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)

        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(IntOffset.Zero, layerBlock = layerBlock)
        }
    }

    fun update(
        style: () -> GlassStyle,
        compositingStrategy: CompositingStrategy
    ) {
        if (this.style != style || this.compositingStrategy != compositingStrategy) {
            this.style = style
            this.compositingStrategy = compositingStrategy
            invalidateLayer()
        }
    }
}
