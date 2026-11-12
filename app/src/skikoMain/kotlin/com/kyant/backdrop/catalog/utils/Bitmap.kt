package com.kyant.backdrop.catalog.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.SamplingMode

actual fun ImageBitmap.scale(width: Int, height: Int): ImageBitmap {
    val bitmap = Bitmap()
    val source = this.asSkiaBitmap()
    bitmap.allocPixels(source.imageInfo.withWidthHeight(width, height))
    val image = Image.makeFromBitmap(source)
    image.scalePixels(bitmap.peekPixels()!!, SamplingMode.LINEAR, false)
    return bitmap.asComposeImageBitmap()
}
