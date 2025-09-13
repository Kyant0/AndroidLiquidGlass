package com.kyant.backdrop

import android.graphics.RenderEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateLayer
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection

fun Modifier.drawBackdrop(
    backdrop: Backdrop,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    onDrawBackdrop: BackdropDrawScope.() -> Unit
): Modifier =
    this then DrawBackdropElement(
        backdrop = backdrop,
        compositingStrategy = compositingStrategy,
        onDrawBackdrop = onDrawBackdrop
    )

private class DrawBackdropElement(
    val backdrop: Backdrop,
    var compositingStrategy: CompositingStrategy,
    val onDrawBackdrop: BackdropDrawScope.() -> Unit
) : ModifierNodeElement<DrawBackdropNode>() {

    override fun create(): DrawBackdropNode {
        return DrawBackdropNode(
            backdrop = backdrop,
            compositingStrategy = compositingStrategy,
            onDrawBackdrop = onDrawBackdrop
        )
    }

    override fun update(node: DrawBackdropNode) {
        node.update(
            backdrop = backdrop,
            compositingStrategy = compositingStrategy,
            onDrawBackdrop = onDrawBackdrop
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawBackdrop"
        properties["backdrop"] = backdrop
        properties["compositingStrategy"] = compositingStrategy
        properties["onDrawBackdrop"] = onDrawBackdrop
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawBackdropElement) return false

        if (backdrop != other.backdrop) return false
        if (compositingStrategy != other.compositingStrategy) return false
        if (onDrawBackdrop != other.onDrawBackdrop) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backdrop.hashCode()
        result = 31 * result + compositingStrategy.hashCode()
        result = 31 * result + onDrawBackdrop.hashCode()
        return result
    }
}

private class DrawBackdropNode(
    var backdrop: Backdrop,
    var compositingStrategy: CompositingStrategy,
    var onDrawBackdrop: BackdropDrawScope.() -> Unit
) : LayoutModifierNode, GlobalPositionAwareModifierNode, DelegatingNode() {

    override val shouldAutoInvalidate: Boolean = false

    private var graphicsLayer: GraphicsLayer? = null
    private val graphicsLayers: MutableMap<String, GraphicsLayer> = mutableMapOf()
    private var clipShape: Shape? by mutableStateOf(null)
    private var shouldClip by mutableStateOf(true)
    private var drawBackdropBlock: DrawScope.() -> Unit by mutableStateOf({})
    private var onDrawBackdropBlock: DrawScope.(DrawScope.() -> Unit) -> Unit = DefaultOnDrawBackdropBlock
    private var onDrawSurfaceBlock: (DrawScope.() -> Unit)? = null

    private val drawNode = delegate(CacheDrawModifierNode {
        val scope = object : BackdropDrawScope {

            override val size: Size = this@CacheDrawModifierNode.size
            override val layoutDirection: LayoutDirection = this@CacheDrawModifierNode.layoutDirection
            override val density: Float = this@CacheDrawModifierNode.density
            override val fontScale: Float = this@CacheDrawModifierNode.fontScale

            override var shape: Shape? by mutableStateOf(null)
            override var clip: Boolean by mutableStateOf(true)
            override var renderEffect: RenderEffect? = null

            override fun obtainGraphicsLayer(key: String): GraphicsLayer {
                return graphicsLayers.getOrPut(key) {
                    this@CacheDrawModifierNode.obtainGraphicsLayer()
                }
            }

            override fun onDrawBackdrop(block: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit) {
                onDrawBackdropBlock = block
            }

            override fun onDrawSurface(block: DrawScope.() -> Unit) {
                onDrawSurfaceBlock = block
            }
        }

        onDrawBackdrop(scope)
        clipShape = scope.shape
        shouldClip = scope.clip

        graphicsLayer?.renderEffect = scope.renderEffect?.asComposeRenderEffect()

        onDrawWithContent {
            val graphicsLayer = graphicsLayer
            if (graphicsLayer != null) {
                graphicsLayer.record { onDrawBackdropBlock(drawBackdropBlock) }
                drawLayer(graphicsLayer)
            }
            onDrawSurfaceBlock?.invoke(this)
            drawContent()
        }
    })

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val clipShape = clipShape
        if (clipShape != null) {
            clip = shouldClip
            shape = clipShape
        }
        compositingStrategy = this@DrawBackdropNode.compositingStrategy
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

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            drawBackdropBlock = { with(backdrop) { drawBackdrop(coordinates) } }
        }
    }

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        graphicsLayer =
            graphicsContext.createGraphicsLayer().apply {
                compositingStrategy = androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
            }
    }

    override fun onDetach() {
        val graphicsContext = requireGraphicsContext()
        graphicsLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            graphicsLayer = null
        }
        graphicsLayers.values.forEach { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
        }
        graphicsLayers.clear()
    }

    fun update(
        backdrop: Backdrop,
        compositingStrategy: CompositingStrategy,
        onDrawBackdrop: BackdropDrawScope.() -> Unit
    ) {
        if (this.compositingStrategy != compositingStrategy) {
            this.compositingStrategy = compositingStrategy
            invalidateLayer()
        }
        if (this.backdrop != backdrop ||
            this.onDrawBackdrop != onDrawBackdrop
        ) {
            this.backdrop = backdrop
            this.onDrawBackdrop = onDrawBackdrop
            drawNode.invalidateDrawCache()
        }
    }
}

private val DefaultOnDrawBackdropBlock: DrawScope.(DrawScope.() -> Unit) -> Unit = { it() }
