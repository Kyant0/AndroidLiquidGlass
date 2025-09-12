# Liquid Glass

Apple's Liquid Glass effect for Android Jetpack Compose.

## Demos

- [Playground app](./app/release/app-release.apk), Android 13 and above is required.

![](artworks/playground_app.jpg)

- [Music player demo](./glassmusic/release/glassmusic-release.apk)

<img alt="Screenshots of a music player demo" height="400" src="artworks/music_player_demo.png"/>

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

### Tips

- You should not nest `LiquidGlassProvider` and `LiquidGlass`:

```kotlin
LiquidGlassProvider(providerState) {
    LiquidGlass(providerState) {}
}
```

Instead, use a parent layout:

```kotlin
Box {
    LiquidGlassProvider(providerState) {}
    LiquidGlass(providerState) {}
}
```

- Apply the scale correctly:

```kotlin
Modifier
    .scale(scaleX, scaleY)
    .liquidGlass(
        // `pivot = Offset.Zero` is required
        transformBlock = { scale(1f / scaleX, 1f / scaleY, Offset.Zero) }
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
