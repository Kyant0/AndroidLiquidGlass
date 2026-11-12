# Liquid Glass (Backdrop)

![frontPhoto](artworks/banner.jpg)

A customizable Liquid Glass effect library for Compose Multiplatform.

## Docs

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kyant0/backdrop)](https://central.sonatype.com/artifact/io.github.kyant0/backdrop)

[Documentation](https://kyant.gitbook.io/backdrop)

## HDR

Backdrop keeps extended-range colors on the paths it owns: shader color uniforms, paint colors, and
the tint and lighting filters. Color matrices stay on the platform filter, so how they treat
extended-range values follows the platform and OS release. On Android 14 and later Backdrop can
also refresh its own rendering resources when the display HDR/SDR ratio changes, so the effects
follow the display's current headroom.

HDR output is opt-in and belongs to the app: Backdrop never enables HDR on a window by itself.

```kotlin
CompositionLocalProvider(LocalBackdropHdrRefreshInterval provides 200.milliseconds) {
  BackdropHdrScope {
    // rememberLayerBackdrop / drawBackdrop / drawPlainBackdrop
  }
}
```

Enable HDR on the Android window yourself, for example with
`window.colorMode = ActivityInfo.COLOR_MODE_HDR`. `BackdropHdrScope` then:

- publishes the sampled display ratio through `LocalBackdropHdrHeadroom`, which is 1 while the
  display reports no headroom,
- throttles refreshes to `LocalBackdropHdrRefreshInterval`, 200 milliseconds by default; the value
  must be finite and non-negative,
- refreshes only the rendering resources Backdrop owns, so the composition, animations and
  selection state stay intact, and
- leaves `GraphicsLayer`s you supply yourself, plus ancestor and third-party effect layers, under
  your control.

Platforms without the display callback keep their usual rendering behavior, with
`LocalBackdropHdrHeadroom` staying at 1. Effects that run through platform intermediates, such as
the system blur, follow the platform's own color handling.

## Components

The library does not include any high-level components; you will need to create your own.
Below are some example components:

- [LiquidButton](/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidButton.kt)
- [LiquidToggle](/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidToggle.kt)
- [LiquidSlider](/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidSlider.kt)
- [LiquidBottomTabs](/app/src/commonMain/kotlin/com/kyant/backdrop/catalog/components/LiquidBottomTabs.kt)

## Demo

- [Backdrop Catalog](./androidApp/release/androidApp-release.apk)

![Screenshots of Backdrop Catalog](artworks/catalog_app.jpg)
