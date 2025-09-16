# Legacy Documentation

## 1.0.0-alpha09

```kotlin
val backdrop = rememberLayerBackdrop(backgroundColor = Color.White)

Box {
    // backdrop content used to fill the backdrop layer
    Box(Modifier.backdrop(backdrop))

    // icon button with glass effect
    Box(
        Modifier
            // [optional] drop shadow effect
            .backdropShadow(CircleShape)
            // draw the backdrop with glass effect
            .drawBackdrop(backdrop) {
                // set the shape
                shape = CircleShape

                // apply visual effects
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    saturation()
                    blur(2f.dp)
                    // here is the glass effect
                    refraction(height = 8f.dp.toPx(), amount = size.minDimension)
                }

                // [optional] draw a brilliant highlight
                drawHighlight()
            }
            .clickable {}
            .size(48.dp),
        contentAlignment = Alignment.Center
    ) {}
}
```

## Before 1.0.0-alpha09 (exclusive)

```kotlin
val providerState = rememberLiquidGlassProviderState(
    // if the providing content has any transparent area and there is a background behind the content, set the
    // background color here, or set it to null
    backgroundColor = Color.White
)

// the content behind the glass
Box(Modifier.liquidGlassProvider(providerState))

// glass
Box(
    Modifier.liquidGlass(
        providerState,
        GlassStyle(
            shape = RoundedCornerShape(16f.dp),
            // you can customize more properties
            innerRefraction = InnerRefraction(
                height = RefractionHeight(8f.dp),
                amount = RefractionAmount((-16f).dp),
                depthEffect = 0f // or `1f` to have more 3D effect
            ),
            dispersion = Dispersion.None, // or `Dispersion.Automatic` to enable dispersion effect
            material = GlassMaterial(
                blurRadius = 2f.dp,
                brush = SolidColor(Color.White),
                alpha = 0.3f
            ),
            highlight = GlassHighlight.Default,
            shadow = GlassShadow.Default // or `null` to disable shadow
        )
    )
)
```

Use the block modifier variant to improve performance when the style changes frequently:

```kotlin
val progress by animateFloatAsState(if (isPressed) 1f else 0f)

Box(
    Modifier.liquidGlass(providerState) {
        GlassStyle(
            shape = RoundedCornerShape(16f.dp),
            innerRefraction = InnerRefraction(
                height = RefractionHeight(8f.dp * progress),
                amount = RefractionAmount((-16f).dp * progress)
            ),
            material = GlassMaterial(
                blurRadius = 2f.dp * progress,
                brush = SolidColor(Color.White),
                alpha = 0.3f * progress
            )
        )
    }
)
```
