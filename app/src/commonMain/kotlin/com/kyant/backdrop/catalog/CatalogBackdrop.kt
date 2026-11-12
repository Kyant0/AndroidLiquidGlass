package com.kyant.backdrop.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.backdrop.drawBackdrop as libraryDrawBackdrop
import com.kyant.backdrop.drawPlainBackdrop as libraryDrawPlainBackdrop

// Catalog comparison shader: clamp the final output back into SDR.
private const val CatalogSdrOutput = """
uniform shader content;
float4 main(float2 p) {
    float4 c = content.eval(p);
    return float4(clamp(c.rgb, float3(0.0), float3(c.a)), c.a);
}
"""

@Composable
fun Modifier.catalogDrawBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    highlight: (() -> Highlight?)? = { Highlight.Default },
    shadow: (() -> Shadow?)? = { Shadow.Default },
    innerShadow: (() -> InnerShadow?)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    exportedBackdrop: LayerBackdrop? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = { it() },
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null
): Modifier {
    val hdr = LocalCatalogComponentHdr.current
    return libraryDrawBackdrop(
        backdrop = backdrop,
        shape = shape,
        effects = {
            effects()
            if (!hdr && isRuntimeShaderSupported()) {
                runtimeShaderEffect("CatalogSdrOutput", CatalogSdrOutput, "content") {}
            }
        },
        highlight = highlight,
        shadow = shadow,
        innerShadow = innerShadow,
        layerBlock = layerBlock,
        exportedBackdrop = exportedBackdrop,
        onDrawBehind = onDrawBehind,
        onDrawBackdrop = onDrawBackdrop,
        onDrawSurface = onDrawSurface,
        onDrawFront = onDrawFront
    )
}

@Composable
fun Modifier.catalogDrawPlainBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    exportedBackdrop: LayerBackdrop? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = { it() },
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null
): Modifier {
    val hdr = LocalCatalogComponentHdr.current
    return libraryDrawPlainBackdrop(
        backdrop = backdrop,
        shape = shape,
        effects = {
            effects()
            if (!hdr && isRuntimeShaderSupported()) {
                runtimeShaderEffect("CatalogSdrOutput", CatalogSdrOutput, "content") {}
            }
        },
        layerBlock = layerBlock,
        exportedBackdrop = exportedBackdrop,
        onDrawBehind = onDrawBehind,
        onDrawBackdrop = onDrawBackdrop,
        onDrawSurface = onDrawSurface,
        onDrawFront = onDrawFront
    )
}
