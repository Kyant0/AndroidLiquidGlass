package com.kyant.blur

import android.graphics.RenderEffect
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.toAndroidTileMode
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateLayer
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropDrawScope
import com.kyant.backdrop.SimpleBackdropDrawScope

private val DefaultOnDrawBackdrop: BackdropDrawScope.() -> Unit = { drawBackdrop() }

fun Modifier.blur(
    backdrop: Backdrop,
    style: BlurStyle,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    onDrawBackdrop: BackdropDrawScope.() -> Unit = DefaultOnDrawBackdrop
): Modifier =
    this
        .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SimpleBlurElement(
                    backdrop = backdrop,
                    style = style,
                    compositingStrategy = compositingStrategy,
                    onDrawBackdrop = onDrawBackdrop
                )
            } else {
                Modifier
            }
        )

fun Modifier.blur(
    backdrop: Backdrop,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    onDrawBackdrop: BackdropDrawScope.() -> Unit = DefaultOnDrawBackdrop,
    style: () -> BlurStyle
): Modifier =
    this
        .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                BlurElement(
                    backdrop = backdrop,
                    style = style,
                    compositingStrategy = compositingStrategy,
                    onDrawBackdrop = onDrawBackdrop
                )
            } else {
                Modifier
            }
        )

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class SimpleBlurElement(
    val backdrop: Backdrop,
    val style: BlurStyle,
    val compositingStrategy: CompositingStrategy,
    val onDrawBackdrop: BackdropDrawScope.() -> Unit
) : ModifierNodeElement<SimpleBlurNode>() {

    override fun create(): SimpleBlurNode {
        return SimpleBlurNode(
            backdrop = backdrop,
            style = style,
            compositingStrategy = compositingStrategy,
            onDrawBackdrop = onDrawBackdrop
        )
    }

    override fun update(node: SimpleBlurNode) {
        node.update(
            backdrop = backdrop,
            style = style,
            compositingStrategy = compositingStrategy,
            onDrawBackdrop = onDrawBackdrop
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "blur"
        properties["backdrop"] = backdrop
        properties["style"] = style
        properties["compositingStrategy"] = compositingStrategy
        properties["onDrawBackdrop"] = onDrawBackdrop
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimpleBlurElement) return false

        if (backdrop != other.backdrop) return false
        if (style != other.style) return false
        if (compositingStrategy != other.compositingStrategy) return false
        if (onDrawBackdrop != other.onDrawBackdrop) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backdrop.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + compositingStrategy.hashCode()
        result = 31 * result + onDrawBackdrop.hashCode()
        return result
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class BlurElement(
    val backdrop: Backdrop,
    val style: () -> BlurStyle,
    val compositingStrategy: CompositingStrategy,
    val onDrawBackdrop: BackdropDrawScope.() -> Unit
) : ModifierNodeElement<BlurNode>() {

    override fun create(): BlurNode {
        return BlurNode(
            backdrop = backdrop,
            style = style,
            compositingStrategy = compositingStrategy,
            onDrawBackdrop = onDrawBackdrop
        )
    }

    override fun update(node: BlurNode) {
        node.update(
            backdrop = backdrop,
            style = style,
            compositingStrategy = compositingStrategy,
            onDrawBackdrop = onDrawBackdrop
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "blur"
        properties["backdrop"] = backdrop
        properties["style"] = style
        properties["compositingStrategy"] = compositingStrategy
        properties["onDrawBackdrop"] = onDrawBackdrop
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlurElement) return false

        if (backdrop != other.backdrop) return false
        if (style != other.style) return false
        if (compositingStrategy != other.compositingStrategy) return false
        if (onDrawBackdrop != other.onDrawBackdrop) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backdrop.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + compositingStrategy.hashCode()
        result = 31 * result + onDrawBackdrop.hashCode()
        return result
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class SimpleBlurNode(
    var backdrop: Backdrop,
    var style: BlurStyle,
    var compositingStrategy: CompositingStrategy,
    var onDrawBackdrop: BackdropDrawScope.() -> Unit
) : LayoutModifierNode, GlobalPositionAwareModifierNode, DelegatingNode() {

    override val shouldAutoInvalidate: Boolean = false

    private var graphicsLayer: GraphicsLayer? = null

    private var drawBackdropBlock: DrawScope.() -> Unit = {}

    private val drawNode = delegate(CacheDrawModifierNode {
        val style = style

        val colorFilter = style.material.colorFilter
        val colorFilterEffect =
            if (colorFilter != null) {
                RenderEffect.createColorFilterEffect(colorFilter.asAndroidColorFilter())
            } else {
                null
            }

        val blurRadiusPx = style.blurRadius.toPx()
        val blurRenderEffect =
            if (blurRadiusPx > 0f) {
                val tileMode = style.tileMode.toAndroidTileMode()
                if (colorFilterEffect != null) {
                    RenderEffect.createBlurEffect(
                        blurRadiusPx,
                        blurRadiusPx,
                        colorFilterEffect,
                        tileMode
                    )
                } else {
                    RenderEffect.createBlurEffect(
                        blurRadiusPx,
                        blurRadiusPx,
                        tileMode
                    )
                }
            } else {
                colorFilterEffect
            }

        val renderEffect = blurRenderEffect?.asComposeRenderEffect()

        val graphicsLayer = graphicsLayer
        graphicsLayer?.renderEffect = renderEffect

        onDrawWithContent {
            if (graphicsLayer != null) {
                graphicsLayer.record {
                    val backdropDrawScope = SimpleBackdropDrawScope(this, drawBackdropBlock)
                    onDrawBackdrop(backdropDrawScope)
                }

                drawLayer(graphicsLayer)
            }

            style.material.brush?.let { brush ->
                drawRect(
                    brush = brush,
                    alpha = style.material.alpha,
                    blendMode = style.material.blendMode
                )
            }

            drawContent()
        }
    })

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        clip = true
        shape = style.shape
        compositingStrategy = this@SimpleBlurNode.compositingStrategy
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
            drawBackdropBlock = { with(backdrop) { drawChild(coordinates) } }
            drawNode.invalidateDraw()
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
    }

    fun update(
        backdrop: Backdrop,
        style: BlurStyle,
        compositingStrategy: CompositingStrategy,
        onDrawBackdrop: BackdropDrawScope.() -> Unit
    ) {
        if (this.style != style ||
            this.compositingStrategy != compositingStrategy
        ) {
            this.style = style
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

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class BlurNode(
    var backdrop: Backdrop,
    var style: () -> BlurStyle,
    var compositingStrategy: CompositingStrategy,
    var onDrawBackdrop: BackdropDrawScope.() -> Unit
) : ObserverModifierNode, DelegatingNode() {

    override val shouldAutoInvalidate: Boolean = false

    private val glassNode = delegate(
        SimpleBlurNode(
            backdrop = backdrop,
            style = style(),
            compositingStrategy = compositingStrategy,
            onDrawBackdrop = onDrawBackdrop
        )
    )

    override fun onObservedReadsChanged() {
        updateStyle()
    }

    fun update(
        backdrop: Backdrop,
        style: () -> BlurStyle,
        compositingStrategy: CompositingStrategy,
        onDrawBackdrop: BackdropDrawScope.() -> Unit
    ) {
        if (this.backdrop != backdrop ||
            this.style != style ||
            this.compositingStrategy != compositingStrategy ||
            this.onDrawBackdrop != onDrawBackdrop
        ) {
            this.backdrop = backdrop
            this.style = style
            this.compositingStrategy = compositingStrategy
            this.onDrawBackdrop = onDrawBackdrop
            updateStyle()
        }
    }

    private fun updateStyle() {
        observeReads {
            glassNode.update(
                backdrop = backdrop,
                style = style(),
                compositingStrategy = compositingStrategy,
                onDrawBackdrop = onDrawBackdrop
            )
        }
    }
}
