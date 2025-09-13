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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.LayerBackdrop
import com.kyant.backdrop.backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorFilter
import com.kyant.backdrop.effects.dispersion
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.highlight.highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.backdrop.shadow.backdropShadow
import com.kyant.liquidglass.dispersion.Dispersion
import com.kyant.liquidglass.highlight.GlassHighlightStyle

private val DefaultOnDrawBackdrop: DrawScope.(DrawScope.() -> Unit) -> Unit = { it() }

@Deprecated(message = "Use the new Backdrop API")
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    style: GlassStyle,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = DefaultOnDrawBackdrop
): Modifier =
    this.liquidGlass(
        backdrop = backdrop,
        compositingStrategy = compositingStrategy,
        onDrawBackdrop = onDrawBackdrop,
        style = { style }
    )

@Deprecated(message = "Use the new Backdrop API")
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = DefaultOnDrawBackdrop,
    style: () -> GlassStyle
): Modifier =
    this
        .backdropShadow {
            val style = style()

            style.shadow?.run {
                Shadow(
                    shape = style.shape,
                    elevation = elevation,
                    color = color,
                    offset = offset,
                    blendMode = blendMode
                )
            }
        }
        .drawBackdrop(backdrop, compositingStrategy) {
            val style = style()

            shape = style.shape

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                style.material.colorFilter?.let { colorFilter(it) }
                blur(style.material.blurRadius)

                val refractionHeight = style.innerRefraction.height.toPx(size, this)
                val refractionAmount = style.innerRefraction.amount.toPx(size, this)

                refraction(
                    height = refractionHeight,
                    amount = -refractionAmount,
                    hasDepthEffect = style.innerRefraction.depthEffect > 0f
                )

                val dispersion = style.dispersion
                val dispersionHeight: Float
                val dispersionAmount: Float
                when (dispersion) {
                    Dispersion.None -> {
                        dispersionHeight = 0f
                        dispersionAmount = 0f
                    }

                    Dispersion.Automatic -> {
                        dispersionHeight = refractionHeight
                        dispersionAmount = -refractionAmount
                    }

                    is Dispersion.Fractional -> {
                        dispersionHeight = refractionHeight * dispersion.fraction
                        dispersionAmount = -refractionAmount * dispersion.fraction
                    }
                }
                dispersion(
                    height = dispersionHeight,
                    amount = dispersionAmount
                )
            }

            onDrawBackdrop(onDrawBackdrop)

            val highlight = style.highlight?.let {
                highlight(
                    width = it.width.toPx(),
                    color = it.color,
                    blendMode = it.blendMode,
                    style = when (it.style) {
                        is GlassHighlightStyle.Solid -> HighlightStyle.Solid
                        is GlassHighlightStyle.Soft -> HighlightStyle.Soft
                        is GlassHighlightStyle.Dynamic -> HighlightStyle.Dynamic(
                            angle = it.style.angle,
                            falloff = it.style.falloff
                        )

                        else -> HighlightStyle.Default
                    }
                )
            }

            onDrawSurface {
                style.material.brush?.let {
                    drawRect(
                        it,
                        alpha = style.material.alpha,
                        blendMode = style.material.blendMode
                    )
                }
                highlight?.run { draw() }
            }
        }

@Deprecated(message = "Use the new Backdrop API")
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
        onDrawBackdrop = { drawBackdrop ->
            if (transformBlock != null) {
                withTransform(transformBlock) {
                    drawBackdrop()
                }
            } else {
                drawBackdrop()
            }
        }
    )

@Deprecated(message = "Use the new Backdrop API")
fun Modifier.liquidGlass(
    state: LiquidGlassProviderState,
    compositingStrategy: CompositingStrategy = CompositingStrategy.Offscreen,
    transformBlock: (DrawTransform.() -> Unit)? = null,
    style: () -> GlassStyle
): Modifier =
    this.liquidGlass(
        backdrop = state.backdrop,
        compositingStrategy = compositingStrategy,
        onDrawBackdrop = { drawBackdrop ->
            if (transformBlock != null) {
                withTransform(transformBlock) {
                    drawBackdrop()
                }
            } else {
                drawBackdrop()
            }
        },
        style = style
    )

@Deprecated(message = "Use the new Backdrop API")
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
