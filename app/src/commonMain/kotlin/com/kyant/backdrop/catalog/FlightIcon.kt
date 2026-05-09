package com.kyant.backdrop.catalog

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val FlightIcon: ImageVector
    get() {
        if (_FlightIcon != null) return _FlightIcon!!

        _FlightIcon = ImageVector.Builder(
            name = "Flight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF1f1f1f))
            ) {
                moveTo(400f, 552f)
                lineTo(147f, 653f)
                quadToRelative(-24f, 10f, -45.5f, -4.5f)
                reflectiveQuadTo(80f, 608f)
                verticalLineToRelative(-22f)
                quadToRelative(0f, -12f, 5.5f, -23f)
                reflectiveQuadToRelative(15.5f, -18f)
                lineToRelative(299f, -209f)
                verticalLineToRelative(-176f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(480f, 80f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(560f, 160f)
                verticalLineToRelative(176f)
                lineToRelative(299f, 209f)
                quadToRelative(10f, 7f, 15.5f, 18f)
                reflectiveQuadToRelative(5.5f, 23f)
                verticalLineToRelative(22f)
                quadToRelative(0f, 26f, -21.5f, 40.5f)
                reflectiveQuadTo(813f, 653f)
                lineTo(560f, 552f)
                verticalLineToRelative(144f)
                lineToRelative(103f, 72f)
                quadToRelative(8f, 6f, 12.5f, 14.5f)
                reflectiveQuadTo(680f, 801f)
                verticalLineToRelative(24f)
                quadToRelative(0f, 20f, -16.5f, 32.5f)
                reflectiveQuadTo(627f, 864f)
                lineToRelative(-147f, -44f)
                lineToRelative(-147f, 44f)
                quadToRelative(-20f, 6f, -36.5f, -6.5f)
                reflectiveQuadTo(280f, 825f)
                verticalLineToRelative(-24f)
                quadToRelative(0f, -10f, 4.5f, -18.5f)
                reflectiveQuadTo(297f, 768f)
                lineToRelative(103f, -72f)
                verticalLineToRelative(-144f)
                close()
            }
        }.build()

        return _FlightIcon!!
    }

private var _FlightIcon: ImageVector? = null

