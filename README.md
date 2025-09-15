# Liquid Glass

Apple's Liquid Glass effect for Android Jetpack Compose.

## Demos

- [Playground app](./app/release/app-release.apk), Android 13 and above is required.

![](artworks/playground_app.jpg)

- [Catalog](./catalog/release/catalog-release.apk)

|                                                                                   |                                                                             |
|:---------------------------------------------------------------------------------:|:---------------------------------------------------------------------------:|
| <img alt="Control center" width="200" src="artworks/catalog_control_center.jpg"/> | <img alt="Bottom tabs" width="200" src="artworks/catalog_bottom_tabs.jpg"/> |

## Library

⚠️ The library is in alpha stage, every API may be changed, use it on your own risk.

### Add to project

[![JitPack Release](https://jitpack.io/v/Kyant0/AndroidLiquidGlass.svg)](https://jitpack.io/#Kyant0/AndroidLiquidGlass)

```kotlin
// settings.gradle.kts in root project
dependencyResolutionManagement {
    repositories {
        maven("https://jitpack.io")
    }
}

// build.gradle.kts in module
implementation("com.github.Kyant0:AndroidLiquidGlass:<version>")
```

### Examples

### After 1.0.0-alpha10

#### Basic example

```kotlin
val backdrop = rememberBackdrop()

Box {
    // backdrop content used to fill the backdrop layer
    Box(Modifier.backdrop(backdrop))

    // icon button with glass effect
    Box(
        Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shapeProvider = { CircleShape },
                // draw a scrim to increase readability
                onDrawSurface = { drawRect(background.copy(alpha = 0.5f)) }
            ) {
                // saturation boost
                saturation()
                // blur
                blur(2f.dp.toPx())
                // glass effect
                refraction(height = 8f.dp.toPx(), amount = size.minDimension)
            }
            .clickable {}
            .size(48.dp),
        contentAlignment = Alignment.Center
    ) {}
}
```

#### Advanced example

Here is the full definition of the `drawBackdrop` modifier:

```kotlin
fun Modifier.drawBackdrop(
    backdrop: Backdrop,
    shapeProvider: () -> Shape,
    highlight: (() -> Highlight?)? = DefaultHighlight,
    shadow: (() -> Shadow?)? = DefaultShadow,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = DefaultOnDrawBackdrop,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    effects: BackdropEffectScope.() -> Unit
): Modifier
```

To apply effects for the Composable's self content, use the `contentBackdrop` modifier:

```kotlin
fun Modifier.contentBackdrop(
    shapeProvider: () -> Shape,
    highlight: (() -> Highlight?)? = DefaultHighlight,
    shadow: (() -> Shadow?)? = DefaultShadow,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    effects: BackdropEffectScope.() -> Unit
): Modifier
```

The following example shows how to draw a dynamic highlight and apply scaling to the backdrop content:

```kotlin
val backdrop = rememberBackdrop()

Box {
    // backdrop content used to fill the backdrop layer
    Box(Modifier.backdrop(backdrop))

    // [optional] scale if needed
    val originalScale = 1.2f

    // the highlight angle in degrees produced by sensors
    val highlightAngle = 45f

    // icon button with glass effect
    Box(
        Modifier
            // [optional] scale if needed
            .graphicsLayer {
                scaleX = originalScale
                scaleY = originalScale
            }
            .drawBackdrop(
                backdrop = backdrop,
                shapeProvider = { CircleShape },
                // the dynamic highlight
                highlight = { Highlight { HighlightStyle.Dynamic(angle = highlightAngle) } },
                // the backdrop content to draw with effects
                onDrawBackdrop = { drawBackdrop ->
                    // apply the inverse scale to the backdrop content
                    scale(1f / originalScale, 1f / originalScale, Offset.Zero) {
                        drawBackdrop()
                    }
                    // [optional] call `drawImage()` here or similar to draw additional content to the front of backdrop layer
                },
                // the surface content to draw above the backdrop layer and below the highlight layer
                onDrawSurface = { drawRect(background.copy(alpha = 0.5f)) }
            )
            .clickable {}
            .size(48.dp),
        contentAlignment = Alignment.Center
    ) {}
}
```

### 1.0.0-alpha09

#### Basic example

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

#### Advanced example

```kotlin
val backdrop = rememberLayerBackdrop(backgroundColor = Color.White)

Box {
    // backdrop content used to fill the backdrop layer
    Box(Modifier.backdrop(backdrop))

    // [optional] scale if needed
    val originalScale = 1f

    // icon button with glass effect
    Box(
        Modifier
            // [optional] scale if needed
            .graphicsLayer {
                scaleX = originalScale
                scaleY = originalScale
            }
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
                    // here is the glass effect with dispersion
                    refractionWithDispersion(height = 8f.dp.toPx(), amount = size.minDimension)
                }

                // [optional] draw custom content on the backdrop layer
                onDrawBackdrop { drawBackdrop ->
                    // apply the inverse scale to the backdrop content
                    scale(1f / originalScale, 1f / originalScale, Offset.Zero) {
                        drawBackdrop()
                    }
                }

                // [optional] draw a scrim with a brilliant highlight
                onDrawSurfaceWithHighlight { drawRect(Color.White.copy(alpha = 0.3f)) }
            }
            .clickable {}
            .size(48.dp),
        contentAlignment = Alignment.Center
    ) {}
}
```

### Before 1.0.0-alpha09 (exclusive)

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

## Comparing with iOS

iOS device: iPhone 16 Pro Max (emulator), using [GlassExplorer](https://github.com/ktiays/GlassExplorer)

Android device: Google Pixel 4 XL (the smallest width is adjusted to 440 dp to match the density of the iOS device)

Glass size: 300 x 300, corner radius: 30

|                   iOS                    |                   Android                    |
|:----------------------------------------:|:--------------------------------------------:|
| ![](./artworks/ios_inner_refraction.png) | ![](./artworks/android_inner_refraction.png) |

Complete comparisons:

- [Inner refraction](https://github.com/Kyant0/AndroidLiquidGlass/blob/530bed05f8342bf607463a775dea93a531f73f42/docs/Inner%20refraction%20comparisons.md)
- [Bleed](https://github.com/Kyant0/AndroidLiquidGlass/blob/530bed05f8342bf607463a775dea93a531f73f42/docs/Bleed%20comparisons.md)

## Star history

[![Star history chart](https://api.star-history.com/svg?repos=Kyant0/AndroidLiquidGlass&type=Date)](https://www.star-history.com/#Kyant0/AndroidLiquidGlass&Date)
