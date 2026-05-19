# Project Patches and Improvements

This document summarizes the technical fixes and features implemented to improve the stability, performance, and visual fidelity of the Android Liquid Glass library.

## Bug Fixes and Performance Patches

### Memory Leak in Adaptive Luminance Calculation
The AdaptiveLuminanceGlassContent.kt file was creating multiple Bitmap instances including snapshots, scaled thumbnails, and ARGB copies inside a high-frequency loop. These bitmaps were not being recycled, which caused significant memory pressure and potential Out-Of-Memory crashes. The fix involved implementing explicit recycle calls for all intermediate bitmaps and optimizing the IO dispatcher block to ensure resources are freed immediately after the average luminance calculation is complete.

### LayoutCoordinates Reference Leaks
In the LayerBackdropModifier.kt and DrawBackdropModifier.kt files, the modifier node implementations were failing to clear layoutCoordinates or the layerCoordinates of exported backdrops when instances were updated. This resulted in the retention of references to detached layout nodes. Logic was added to the update blocks of these modifiers to properly reset coordinate references whenever backdrops are swapped.

### Sensor Hardware Safety
The UISensor.kt implementation previously assumed that an accelerometer would always be present on the device. Running the application on hardware or emulators without this sensor would trigger a crash. The implementation has been updated with null safety checks for both the accelerometer and rotation vector sensors to ensure listeners are only registered when the hardware is available.

### SDF Shader Resource Safety
The rememberSdfShader function performed an unsafe cast to BitmapDrawable, which caused crashes if a user provided a drawable resource that was not a bitmap, such as a VectorDrawable. This has been updated to use safe casting and a nullable return type. The LockScreenContent.kt file was also updated to handle null shaders gracefully.

### Inverse Transformation Matrix Math
InverseLayerScope.kt was previously ignoring translation and transform origin values when calculating the inverse transform for backdrops. This caused backdrops to misalign or drift when their parent layers were moved or scaled. The matrix math has been re-implemented to correctly account for the pivot point and undo translations, ensuring consistent alignment regardless of layer transformations.

## New Feature Dynamic Light Effect

A motion-aware lighting system was added to the library to simulate dynamic glass reflections similar to those found in iOS.

The technical implementation involved integrating the rotation vector sensor into the core UISensor logic. Unlike a standard accelerometer, this provides stable 3D orientation data including roll and pitch for smooth light movement. A new AGSL light shader was created to simulate a light source reflecting off the curved borders of the glass based on a 2D offset.

This feature was integrated into the core library as HighlightStyle.Light. The UISensor logic was moved from the catalog to the library module to make it available to all users. High-level components such as LiquidButton, LiquidSlider, LiquidToggle, and LiquidBottomTabs were updated to use this dynamic effect by default. As a result, the borders of all glass elements now feature a shimmer that reacts in real-time as the physical device is tilted.
