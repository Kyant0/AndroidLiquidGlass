# Liquid Glass

Apple's Liquid Glass effect for Android Jetpack Compose.

## Demos

- [Playground app](./app/release/app-release.apk), Android 13 and above is required.

![](./artworks/features.jpg)

- [Music player demo](./glassmusic/release/glassmusic-release.apk) that integrates **liquid bottom tabs** and **adaptive
  luminance**.

<img alt="Luminance sampler demo" height="400" src="./artworks/luminance_sampler_demo.png"/>

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
    backgroundColor = Color.White
)

// content behind the glass
Box(Modifier.liquidGlassProvider(providerState))

// glass
Box(
    Modifier.liquidGlass(
        providerState,
        GlassStyle(
            shape = RoundedCornerShape(16.dp),
            innerRefraction = InnerRefraction(
                height = RefractionHeight(8.dp),
                amount = RefractionAmount((-16).dp)
            ),
            material = GlassMaterial(
                blurRadius = 8.dp,
                brush = SolidColor(Color.White),
                alpha = 0.3f
            )
        )
    )
)
```

#### [Experimental] Dynamically adjusted tint by luminance behind the glass

⚠️ The API is likely to change in the future.

```kotlin
val luminanceSampler = remember { ContinuousLuminanceSampler() }

liquidGlass(
    providerState,
    luminanceSampler = luminanceSampler
) {
    val luminance = luminanceSampler.luminance

    GlassStyle(
        // ...
        material = GlassMaterial(
            brush = SolidColor(Color.White), // or Color.Black
            alpha = luminance // write down your own logic here
        )
    )
}
```

See [here](./glassmusic/src/main/java/com/kyant/glassmusic/BottomTabs.kt#L123) for more details.

### Tips

- The following case is not supported:

```kotlin
LiquidGlassProvider(providerState) {
    LiquidGlass(providerState) {}
}
```

Instead, you should rewrite it like this:

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
        // ...
        transformBlock = { scale(1f / scaleX, 1f / scaleY, Offset.Zero) }
    )
```

## Comparisons

Android device: Google Pixel 4 XL (the smallest width is adjusted to 440 dp to match the density of the iOS device)

iOS device: iPhone 16 Pro Max (simulator)

Test glass area size: 300 x 300, corner radius: 30

|                   iOS                    |                   Android                    |
|:----------------------------------------:|:--------------------------------------------:|
| ![](./artworks/ios_inner_refraction.png) | ![](./artworks/android_inner_refraction.png) |

Complete comparisons:

- [Inner refraction](https://github.com/Kyant0/AndroidLiquidGlass/blob/530bed05f8342bf607463a775dea93a531f73f42/docs/Inner%20refraction%20comparisons.md)
- [Bleed](https://github.com/Kyant0/AndroidLiquidGlass/blob/530bed05f8342bf607463a775dea93a531f73f42/docs/Bleed%20comparisons.md)

## Special thanks

- [GlassExplorer](https://github.com/ktiays/GlassExplorer)

## Star history

[![Star history chart](https://api.star-history.com/svg?repos=Kyant0/AndroidLiquidGlass&type=Date)](https://www.star-history.com/#Kyant0/AndroidLiquidGlass&Date)
