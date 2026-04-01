package com.kyant.backdrop.catalog.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.kyant.backdrop.catalog.R
import kotlin.math.min
import kotlin.math.roundToInt

object NotificationBranding {
    private const val TAG = "NotificationBranding"
    private const val LARGE_ICON_MAX_WIDTH_DP = 44
    private const val LARGE_ICON_MAX_HEIGHT_DP = 34

    fun getAppLogoBitmap(context: Context): Bitmap? {
        return try {
            val decoded = BitmapFactory.decodeResource(context.resources, R.drawable.vormex_logo)
                ?: return null
            val cropped = cropTransparentBounds(decoded)
            scalePreservingAspect(
                source = cropped,
                maxWidthPx = (context.resources.displayMetrics.density * LARGE_ICON_MAX_WIDTH_DP).roundToInt(),
                maxHeightPx = (context.resources.displayMetrics.density * LARGE_ICON_MAX_HEIGHT_DP).roundToInt()
            )
        } catch (e: Exception) {
            Log.w(TAG, "App logo bitmap failed: ${e.message}")
            null
        }
    }

    private fun cropTransparentBounds(source: Bitmap): Bitmap {
        var minX = source.width
        var minY = source.height
        var maxX = -1
        var maxY = -1

        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                if ((source.getPixel(x, y) ushr 24) != 0) {
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return source
        }

        return Bitmap.createBitmap(
            source,
            minX,
            minY,
            maxX - minX + 1,
            maxY - minY + 1
        )
    }

    private fun scalePreservingAspect(
        source: Bitmap,
        maxWidthPx: Int,
        maxHeightPx: Int
    ): Bitmap {
        val safeMaxWidth = maxWidthPx.coerceAtLeast(1)
        val safeMaxHeight = maxHeightPx.coerceAtLeast(1)
        val scale = min(
            safeMaxWidth / source.width.toFloat(),
            safeMaxHeight / source.height.toFloat()
        )

        if (scale >= 1f) {
            return source
        }

        val targetWidth = (source.width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (source.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }
}
