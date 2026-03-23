package com.kyant.backdrop.catalog.linkedin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom SVG-style icons for the glass theme
 */

// Three vertical dots menu icon
@Composable
fun MenuDotsIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val dotRadius = size.toPx() / 10f
        val spacing = size.toPx() / 3f
        val centerX = size.toPx() / 2f
        
        // Three dots vertically
        drawCircle(
            color = color,
            radius = dotRadius,
            center = Offset(centerX, spacing)
        )
        drawCircle(
            color = color,
            radius = dotRadius,
            center = Offset(centerX, spacing * 2f)
        )
        drawCircle(
            color = color,
            radius = dotRadius,
            center = Offset(centerX, spacing * 3f - dotRadius)
        )
    }
}

// Like/Thumb up icon
@Composable
fun LikeIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    filled: Boolean = false
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        val path = Path().apply {
            // Thumb shape
            moveTo(s * 0.25f, s * 0.45f)
            lineTo(s * 0.25f, s * 0.9f)
            lineTo(s * 0.4f, s * 0.9f)
            lineTo(s * 0.4f, s * 0.45f)
            close()
            
            // Hand part
            moveTo(s * 0.4f, s * 0.5f)
            lineTo(s * 0.75f, s * 0.5f)
            quadraticTo(s * 0.9f, s * 0.5f, s * 0.9f, s * 0.4f)
            quadraticTo(s * 0.9f, s * 0.3f, s * 0.75f, s * 0.3f)
            lineTo(s * 0.6f, s * 0.3f)
            quadraticTo(s * 0.7f, s * 0.15f, s * 0.55f, s * 0.1f)
            quadraticTo(s * 0.4f, s * 0.12f, s * 0.4f, s * 0.4f)
        }
        
        if (filled) {
            drawPath(path, color)
        } else {
            drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

// Heart icon (for loved reactions)
@Composable
fun HeartIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    filled: Boolean = false
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        val path = Path().apply {
            moveTo(s * 0.5f, s * 0.85f)
            // Left curve
            cubicTo(
                s * 0.1f, s * 0.6f,
                s * 0.1f, s * 0.25f,
                s * 0.5f, s * 0.25f
            )
            // Right curve
            cubicTo(
                s * 0.9f, s * 0.25f,
                s * 0.9f, s * 0.6f,
                s * 0.5f, s * 0.85f
            )
            close()
        }
        
        if (filled) {
            drawPath(path, color)
        } else {
            drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

// Comment/Chat bubble icon
@Composable
fun CommentIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        val path = Path().apply {
            // Rounded rectangle bubble
            moveTo(s * 0.15f, s * 0.2f)
            lineTo(s * 0.85f, s * 0.2f)
            quadraticTo(s * 0.95f, s * 0.2f, s * 0.95f, s * 0.3f)
            lineTo(s * 0.95f, s * 0.55f)
            quadraticTo(s * 0.95f, s * 0.65f, s * 0.85f, s * 0.65f)
            lineTo(s * 0.4f, s * 0.65f)
            lineTo(s * 0.2f, s * 0.85f)
            lineTo(s * 0.25f, s * 0.65f)
            lineTo(s * 0.15f, s * 0.65f)
            quadraticTo(s * 0.05f, s * 0.65f, s * 0.05f, s * 0.55f)
            lineTo(s * 0.05f, s * 0.3f)
            quadraticTo(s * 0.05f, s * 0.2f, s * 0.15f, s * 0.2f)
            close()
        }
        
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Three dots inside
        val dotRadius = s / 24f
        val dotY = s * 0.42f
        drawCircle(color, dotRadius, Offset(s * 0.35f, dotY))
        drawCircle(color, dotRadius, Offset(s * 0.5f, dotY))
        drawCircle(color, dotRadius, Offset(s * 0.65f, dotY))
    }
}

// Header notification bell icon
@Composable
fun NotificationBellIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 13f

        val bellPath = Path().apply {
            moveTo(s * 0.3f, s * 0.7f)
            cubicTo(
                s * 0.3f, s * 0.5f,
                s * 0.34f, s * 0.36f,
                s * 0.43f, s * 0.28f
            )
            cubicTo(
                s * 0.46f, s * 0.22f,
                s * 0.54f, s * 0.22f,
                s * 0.57f, s * 0.28f
            )
            cubicTo(
                s * 0.66f, s * 0.36f,
                s * 0.7f, s * 0.5f,
                s * 0.7f, s * 0.7f
            )
            lineTo(s * 0.78f, s * 0.7f)
            quadraticTo(s * 0.83f, s * 0.7f, s * 0.83f, s * 0.76f)
            quadraticTo(s * 0.83f, s * 0.82f, s * 0.78f, s * 0.82f)
            lineTo(s * 0.22f, s * 0.82f)
            quadraticTo(s * 0.17f, s * 0.82f, s * 0.17f, s * 0.76f)
            quadraticTo(s * 0.17f, s * 0.7f, s * 0.22f, s * 0.7f)
            close()
        }

        drawPath(
            path = bellPath,
            color = color,
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawLine(
            color = color,
            start = Offset(s * 0.5f, s * 0.14f),
            end = Offset(s * 0.5f, s * 0.22f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = color,
            radius = s * 0.04f,
            center = Offset(s * 0.5f, s * 0.68f)
        )

        drawLine(
            color = color,
            start = Offset(s * 0.4f, s * 0.89f),
            end = Offset(s * 0.6f, s * 0.89f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

// Header messages icon with layered chat bubbles
@Composable
fun HeaderMessageIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 13f

        val backBubble = Path().apply {
            moveTo(s * 0.42f, s * 0.2f)
            lineTo(s * 0.72f, s * 0.2f)
            quadraticTo(s * 0.82f, s * 0.2f, s * 0.82f, s * 0.3f)
            lineTo(s * 0.82f, s * 0.44f)
            quadraticTo(s * 0.82f, s * 0.54f, s * 0.72f, s * 0.54f)
            lineTo(s * 0.62f, s * 0.54f)
        }

        drawPath(
            path = backBubble,
            color = color.copy(alpha = 0.55f),
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val frontBubble = Path().apply {
            moveTo(s * 0.16f, s * 0.3f)
            lineTo(s * 0.66f, s * 0.3f)
            quadraticTo(s * 0.78f, s * 0.3f, s * 0.78f, s * 0.42f)
            lineTo(s * 0.78f, s * 0.58f)
            quadraticTo(s * 0.78f, s * 0.7f, s * 0.66f, s * 0.7f)
            lineTo(s * 0.36f, s * 0.7f)
            lineTo(s * 0.24f, s * 0.82f)
            lineTo(s * 0.27f, s * 0.68f)
            lineTo(s * 0.16f, s * 0.68f)
            quadraticTo(s * 0.06f, s * 0.68f, s * 0.06f, s * 0.58f)
            lineTo(s * 0.06f, s * 0.42f)
            quadraticTo(s * 0.06f, s * 0.3f, s * 0.16f, s * 0.3f)
            close()
        }

        drawPath(
            path = frontBubble,
            color = color,
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawLine(
            color = color,
            start = Offset(s * 0.23f, s * 0.44f),
            end = Offset(s * 0.6f, s * 0.44f),
            strokeWidth = strokeWidth * 0.9f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color.copy(alpha = 0.8f),
            start = Offset(s * 0.23f, s * 0.56f),
            end = Offset(s * 0.49f, s * 0.56f),
            strokeWidth = strokeWidth * 0.9f,
            cap = StrokeCap.Round
        )
    }
}

// Footer home icon
@Composable
fun FooterHomeIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 13f

        val roofPath = Path().apply {
            moveTo(s * 0.18f, s * 0.48f)
            lineTo(s * 0.5f, s * 0.18f)
            lineTo(s * 0.82f, s * 0.48f)
        }
        drawPath(
            path = roofPath,
            color = color,
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.24f, s * 0.46f),
            size = Size(s * 0.52f, s * 0.32f),
            cornerRadius = CornerRadius(s * 0.08f),
            style = Stroke(strokeWidth)
        )

        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.44f, s * 0.58f),
            size = Size(s * 0.12f, s * 0.2f),
            cornerRadius = CornerRadius(s * 0.04f),
            style = Stroke(strokeWidth)
        )
    }
}

// Footer find / discover icon
@Composable
fun FooterFindIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 13f
        val center = Offset(s * 0.5f, s * 0.5f)

        drawCircle(
            color = color,
            radius = s * 0.28f,
            center = center,
            style = Stroke(strokeWidth)
        )

        val needlePath = Path().apply {
            moveTo(s * 0.58f, s * 0.26f)
            lineTo(s * 0.66f, s * 0.62f)
            lineTo(s * 0.44f, s * 0.54f)
            close()
        }
        drawPath(
            path = needlePath,
            color = color,
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawCircle(
            color = color,
            radius = s * 0.04f,
            center = center
        )
    }
}

// Footer create / post icon
@Composable
fun FooterCreateIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 13f

        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.18f, s * 0.18f),
            size = Size(s * 0.64f, s * 0.64f),
            cornerRadius = CornerRadius(s * 0.18f),
            style = Stroke(strokeWidth)
        )

        drawLine(
            color = color,
            start = Offset(s * 0.5f, s * 0.32f),
            end = Offset(s * 0.5f, s * 0.68f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(s * 0.32f, s * 0.5f),
            end = Offset(s * 0.68f, s * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

// Footer more icon
@Composable
fun FooterMoreIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 13f
        val cellSize = s * 0.2f
        val radius = CornerRadius(s * 0.06f)
        val positions = listOf(
            Offset(s * 0.18f, s * 0.18f),
            Offset(s * 0.62f, s * 0.18f),
            Offset(s * 0.18f, s * 0.62f),
            Offset(s * 0.62f, s * 0.62f)
        )

        positions.forEach { topLeft ->
            drawRoundRect(
                color = color,
                topLeft = topLeft,
                size = Size(cellSize, cellSize),
                cornerRadius = radius,
                style = Stroke(strokeWidth)
            )
        }
    }
}

// Footer profile icon
@Composable
fun FooterProfileIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 13f

        drawCircle(
            color = color,
            radius = s * 0.14f,
            center = Offset(s * 0.5f, s * 0.34f),
            style = Stroke(strokeWidth)
        )

        val bodyPath = Path().apply {
            moveTo(s * 0.24f, s * 0.78f)
            cubicTo(
                s * 0.26f, s * 0.58f,
                s * 0.74f, s * 0.58f,
                s * 0.76f, s * 0.78f
            )
        }
        drawPath(
            path = bodyPath,
            color = color,
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// Share/Send icon (arrow pointing up-right from box)
@Composable
fun ShareIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        // Arrow pointing up-right
        val arrowPath = Path().apply {
            moveTo(s * 0.5f, s * 0.1f)
            lineTo(s * 0.85f, s * 0.1f)
            lineTo(s * 0.85f, s * 0.45f)
            moveTo(s * 0.85f, s * 0.1f)
            lineTo(s * 0.35f, s * 0.6f)
        }
        drawPath(arrowPath, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Box/base
        val boxPath = Path().apply {
            moveTo(s * 0.15f, s * 0.35f)
            lineTo(s * 0.15f, s * 0.85f)
            lineTo(s * 0.7f, s * 0.85f)
            lineTo(s * 0.7f, s * 0.55f)
        }
        drawPath(boxPath, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// Bookmark/Save icon
@Composable
fun BookmarkIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    filled: Boolean = false
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        val path = Path().apply {
            moveTo(s * 0.2f, s * 0.1f)
            lineTo(s * 0.8f, s * 0.1f)
            lineTo(s * 0.8f, s * 0.9f)
            lineTo(s * 0.5f, s * 0.65f)
            lineTo(s * 0.2f, s * 0.9f)
            close()
        }
        
        if (filled) {
            drawPath(path, color)
        } else {
            drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

// Link icon
@Composable
fun LinkIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        // Two chain links
        rotate(-45f, pivot = Offset(s / 2, s / 2)) {
            // First link
            drawRoundRect(
                color = color,
                topLeft = Offset(s * 0.05f, s * 0.35f),
                size = Size(s * 0.45f, s * 0.3f),
                cornerRadius = CornerRadius(s * 0.15f),
                style = Stroke(strokeWidth)
            )
            // Second link
            drawRoundRect(
                color = color,
                topLeft = Offset(s * 0.5f, s * 0.35f),
                size = Size(s * 0.45f, s * 0.3f),
                cornerRadius = CornerRadius(s * 0.15f),
                style = Stroke(strokeWidth)
            )
        }
    }
}

// Edit/Pencil icon
@Composable
fun EditIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        rotate(-45f, pivot = Offset(s / 2, s / 2)) {
            // Pencil body
            val path = Path().apply {
                moveTo(s * 0.3f, s * 0.15f)
                lineTo(s * 0.7f, s * 0.15f)
                lineTo(s * 0.7f, s * 0.7f)
                lineTo(s * 0.5f, s * 0.85f)
                lineTo(s * 0.3f, s * 0.7f)
                close()
            }
            drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            
            // Tip line
            drawLine(
                color = color,
                start = Offset(s * 0.35f, s * 0.65f),
                end = Offset(s * 0.65f, s * 0.65f),
                strokeWidth = strokeWidth
            )
        }
    }
}

// Delete/Trash icon
@Composable
fun TrashIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        // Lid
        drawLine(
            color = color,
            start = Offset(s * 0.15f, s * 0.2f),
            end = Offset(s * 0.85f, s * 0.2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        
        // Handle
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.35f, s * 0.1f),
            size = Size(s * 0.3f, s * 0.12f),
            cornerRadius = CornerRadius(s * 0.05f),
            style = Stroke(strokeWidth)
        )
        
        // Body
        val bodyPath = Path().apply {
            moveTo(s * 0.2f, s * 0.25f)
            lineTo(s * 0.25f, s * 0.85f)
            lineTo(s * 0.75f, s * 0.85f)
            lineTo(s * 0.8f, s * 0.25f)
        }
        drawPath(bodyPath, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Lines inside
        drawLine(color, Offset(s * 0.4f, s * 0.35f), Offset(s * 0.4f, s * 0.75f), strokeWidth)
        drawLine(color, Offset(s * 0.6f, s * 0.35f), Offset(s * 0.6f, s * 0.75f), strokeWidth)
    }
}

// Warning/Report icon
@Composable
fun WarningIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        // Triangle
        val path = Path().apply {
            moveTo(s * 0.5f, s * 0.1f)
            lineTo(s * 0.9f, s * 0.85f)
            lineTo(s * 0.1f, s * 0.85f)
            close()
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Exclamation mark
        drawLine(
            color = color,
            start = Offset(s * 0.5f, s * 0.35f),
            end = Offset(s * 0.5f, s * 0.55f),
            strokeWidth = strokeWidth * 1.5f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = color,
            radius = strokeWidth,
            center = Offset(s * 0.5f, s * 0.7f)
        )
    }
}

// Block/Not interested icon
@Composable
fun BlockIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        val radius = s * 0.38f
        val center = Offset(s / 2, s / 2)
        
        // Circle
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(strokeWidth)
        )
        
        // Diagonal line
        val offset = radius * 0.7f
        drawLine(
            color = color,
            start = Offset(center.x - offset, center.y - offset),
            end = Offset(center.x + offset, center.y + offset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

// Celebrate icon (party popper style)
@Composable
fun CelebrateIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        // Cone
        val conePath = Path().apply {
            moveTo(s * 0.2f, s * 0.85f)
            lineTo(s * 0.55f, s * 0.3f)
            lineTo(s * 0.7f, s * 0.5f)
            close()
        }
        drawPath(conePath, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Confetti
        drawCircle(color, s / 20f, Offset(s * 0.75f, s * 0.15f))
        drawCircle(color, s / 25f, Offset(s * 0.85f, s * 0.3f))
        drawCircle(color, s / 20f, Offset(s * 0.6f, s * 0.15f))
        
        // Star
        drawLine(color, Offset(s * 0.4f, s * 0.1f), Offset(s * 0.4f, s * 0.2f), strokeWidth * 0.8f, StrokeCap.Round)
        drawLine(color, Offset(s * 0.35f, s * 0.15f), Offset(s * 0.45f, s * 0.15f), strokeWidth * 0.8f, StrokeCap.Round)
    }
}

// Lightbulb/Insightful icon
@Composable
fun InsightfulIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        // Bulb
        val bulbPath = Path().apply {
            moveTo(s * 0.5f, s * 0.1f)
            cubicTo(s * 0.2f, s * 0.1f, s * 0.15f, s * 0.45f, s * 0.3f, s * 0.6f)
            lineTo(s * 0.3f, s * 0.7f)
            lineTo(s * 0.7f, s * 0.7f)
            lineTo(s * 0.7f, s * 0.6f)
            cubicTo(s * 0.85f, s * 0.45f, s * 0.8f, s * 0.1f, s * 0.5f, s * 0.1f)
            close()
        }
        drawPath(bulbPath, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Base lines
        drawLine(color, Offset(s * 0.35f, s * 0.78f), Offset(s * 0.65f, s * 0.78f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(s * 0.38f, s * 0.86f), Offset(s * 0.62f, s * 0.86f), strokeWidth, StrokeCap.Round)
    }
}

// Question mark/Curious icon
@Composable
fun CuriousIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 10f
        
        // Question mark curve
        val path = Path().apply {
            moveTo(s * 0.3f, s * 0.3f)
            cubicTo(s * 0.3f, s * 0.1f, s * 0.7f, s * 0.1f, s * 0.7f, s * 0.3f)
            cubicTo(s * 0.7f, s * 0.45f, s * 0.5f, s * 0.45f, s * 0.5f, s * 0.6f)
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Dot
        drawCircle(
            color = color,
            radius = strokeWidth * 0.8f,
            center = Offset(s * 0.5f, s * 0.8f)
        )
    }
}

// Repost icon (circular arrows)
@Composable
fun RepostIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        // Top arrow
        val topArrowPath = Path().apply {
            moveTo(s * 0.7f, s * 0.25f)
            lineTo(s * 0.85f, s * 0.35f)
            lineTo(s * 0.7f, s * 0.45f)
        }
        drawPath(topArrowPath, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Top line
        drawLine(color, Offset(s * 0.2f, s * 0.35f), Offset(s * 0.8f, s * 0.35f), strokeWidth, StrokeCap.Round)
        
        // Bottom arrow
        val bottomArrowPath = Path().apply {
            moveTo(s * 0.3f, s * 0.55f)
            lineTo(s * 0.15f, s * 0.65f)
            lineTo(s * 0.3f, s * 0.75f)
        }
        drawPath(bottomArrowPath, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Bottom line
        drawLine(color, Offset(s * 0.2f, s * 0.65f), Offset(s * 0.8f, s * 0.65f), strokeWidth, StrokeCap.Round)
    }
}

// Lightning bolt / Zap icon (for Smart Matches)
@Composable
fun ZapIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        val path = Path().apply {
            moveTo(s * 0.55f, s * 0.1f)
            lineTo(s * 0.25f, s * 0.5f)
            lineTo(s * 0.45f, s * 0.5f)
            lineTo(s * 0.4f, s * 0.9f)
            lineTo(s * 0.75f, s * 0.45f)
            lineTo(s * 0.55f, s * 0.45f)
            close()
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// Users / People icon (for All People)
@Composable
fun UsersIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        // Front person head
        drawCircle(
            color = color,
            radius = s * 0.12f,
            center = Offset(s * 0.35f, s * 0.3f),
            style = Stroke(strokeWidth)
        )
        
        // Front person body
        val frontBodyPath = Path().apply {
            moveTo(s * 0.1f, s * 0.85f)
            quadraticTo(s * 0.1f, s * 0.5f, s * 0.35f, s * 0.5f)
            quadraticTo(s * 0.6f, s * 0.5f, s * 0.6f, s * 0.85f)
        }
        drawPath(frontBodyPath, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Back person head
        drawCircle(
            color = color,
            radius = s * 0.1f,
            center = Offset(s * 0.65f, s * 0.25f),
            style = Stroke(strokeWidth)
        )
        
        // Back person body
        val backBodyPath = Path().apply {
            moveTo(s * 0.5f, s * 0.7f)
            quadraticTo(s * 0.5f, s * 0.42f, s * 0.65f, s * 0.42f)
            quadraticTo(s * 0.9f, s * 0.42f, s * 0.9f, s * 0.7f)
        }
        drawPath(backBodyPath, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// Sparkle / Star icon (for For You)
@Composable
fun SparkleIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        // Main star
        val starPath = Path().apply {
            moveTo(s * 0.5f, s * 0.1f)
            lineTo(s * 0.58f, s * 0.35f)
            lineTo(s * 0.85f, s * 0.35f)
            lineTo(s * 0.65f, s * 0.52f)
            lineTo(s * 0.72f, s * 0.8f)
            lineTo(s * 0.5f, s * 0.65f)
            lineTo(s * 0.28f, s * 0.8f)
            lineTo(s * 0.35f, s * 0.52f)
            lineTo(s * 0.15f, s * 0.35f)
            lineTo(s * 0.42f, s * 0.35f)
            close()
        }
        drawPath(starPath, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// Graduation cap icon (for Campus)
@Composable
fun GraduationCapIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        // Cap top (diamond shape)
        val capPath = Path().apply {
            moveTo(s * 0.5f, s * 0.15f)
            lineTo(s * 0.9f, s * 0.35f)
            lineTo(s * 0.5f, s * 0.55f)
            lineTo(s * 0.1f, s * 0.35f)
            close()
        }
        drawPath(capPath, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Cap base curve
        val basePath = Path().apply {
            moveTo(s * 0.2f, s * 0.45f)
            lineTo(s * 0.2f, s * 0.65f)
            quadraticTo(s * 0.5f, s * 0.85f, s * 0.8f, s * 0.65f)
            lineTo(s * 0.8f, s * 0.45f)
        }
        drawPath(basePath, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Tassel
        drawLine(color, Offset(s * 0.9f, s * 0.35f), Offset(s * 0.9f, s * 0.7f), strokeWidth, StrokeCap.Round)
        drawCircle(color, s * 0.04f, Offset(s * 0.9f, s * 0.72f))
    }
}

// Location pin icon (for Nearby)
@Composable
fun LocationPinIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokeWidth = s / 12f
        
        // Pin shape
        val pinPath = Path().apply {
            moveTo(s * 0.5f, s * 0.9f)
            quadraticTo(s * 0.15f, s * 0.55f, s * 0.15f, s * 0.35f)
            cubicTo(s * 0.15f, s * 0.1f, s * 0.85f, s * 0.1f, s * 0.85f, s * 0.35f)
            quadraticTo(s * 0.85f, s * 0.55f, s * 0.5f, s * 0.9f)
            close()
        }
        drawPath(pinPath, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Inner circle
        drawCircle(
            color = color,
            radius = s * 0.12f,
            center = Offset(s * 0.5f, s * 0.35f),
            style = Stroke(strokeWidth)
        )
    }
}
