package com.example.presentation

import android.graphics.Rect
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import com.example.domain.model.OcrBlock

@Composable
fun OcrCanvasOverlay(
    detectedBlocks: List<OcrBlock>,
    modifier: Modifier = Modifier,
    isScanning: Boolean = true
) {
    // Elegant pulsing loop for the scanning corner guides
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_brackets")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bracket_alpha"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            // CRITICAL: Force offscreen buffer layer so BlendMode.Clear doesn't draw black
            .graphicsLayer(alpha = 0.99f)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Central scanning inspect focus window
        val rectWidth = canvasWidth * 0.82f
        val rectHeight = canvasHeight * 0.38f
        val left = (canvasWidth - rectWidth) / 2f
        val top = (canvasHeight - rectHeight) / 2.3f
        val right = left + rectWidth
        val bottom = top + rectHeight

        val scanWindow = ComposeRect(left, top, right, bottom)

        // 1. Draw diminished dark translucent scrim everywhere outside the window
        drawIntoCanvas { canvas ->
            val scrimPaint = Paint().apply {
                color = Color.Black.copy(alpha = 0.65f)
                style = PaintingStyle.Fill
            }
            canvas.drawRect(ComposeRect(0f, 0f, canvasWidth, canvasHeight), scrimPaint)

            // Clear the rounded scan window area dynamically
            val clearPaint = Paint().apply {
                blendMode = BlendMode.Clear
            }
            val windowPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = scanWindow,
                        cornerRadius = CornerRadius(24f, 24f)
                    )
                )
            }
            canvas.drawPath(windowPath, clearPaint)
        }

        // 2. Add an outer elegant bounding stroke
        drawRoundRect(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = Offset(left, top),
            size = Size(rectWidth, rectHeight),
            cornerRadius = CornerRadius(24f, 24f),
            style = Stroke(width = 2f)
        )

        // 3. Highlighted Pulsing Corner Brackets
        val cornerLen = 42f
        val strokeW = 8f
        val cornerColor = Color(0xFF4ADE80).copy(alpha = pulseAlpha)

        // Top-Left corner
        drawPath(
            path = Path().apply {
                moveTo(left, top + cornerLen)
                lineTo(left, top)
                lineTo(left + cornerLen, top)
            },
            color = cornerColor,
            style = Stroke(width = strokeW)
        )

        // Top-Right corner
        drawPath(
            path = Path().apply {
                moveTo(right - cornerLen, top)
                lineTo(right, top)
                lineTo(right, top + cornerLen)
            },
            color = cornerColor,
            style = Stroke(width = strokeW)
        )

        // Bottom-Left corner
        drawPath(
            path = Path().apply {
                moveTo(left, bottom - cornerLen)
                lineTo(left, bottom)
                lineTo(left + cornerLen, bottom)
            },
            color = cornerColor,
            style = Stroke(width = strokeW)
        )

        // Bottom-Right corner
        drawPath(
            path = Path().apply {
                moveTo(right - cornerLen, bottom)
                lineTo(right, bottom)
                lineTo(right, bottom - cornerLen)
            },
            color = cornerColor,
            style = Stroke(width = strokeW)
        )

        // 4. Transform ML Kit bounding boxes over standard preview viewfinder
        // ML Kit usually outputs coordinates relative to its image analyzer size (e.g., 480x640 or 720x1280).
        // Let's map coordinates with a relative scale ratio safely.
        detectedBlocks.forEach { block ->
            block.boundingBox?.let { box ->
                // Basic scaling ratio (assume common 720x1280 source for safe rendering)
                val sourceW = 720f
                val sourceH = 1280f

                val scaleX = canvasWidth / sourceW
                val scaleY = canvasHeight / sourceH

                val mappedLeft = box.left * scaleX
                val mappedTop = box.top * scaleY
                val mappedW = box.width() * scaleX
                val mappedH = box.height() * scaleY

                // Draw bounding box card
                drawRoundRect(
                    color = Color(0xFF4ADE80).copy(alpha = 0.12f),
                    topLeft = Offset(mappedLeft, mappedTop),
                    size = Size(mappedW, mappedH),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                drawRoundRect(
                    color = Color(0xFF4ADE80).copy(alpha = 0.85f),
                    topLeft = Offset(mappedLeft, mappedTop),
                    size = Size(mappedW, mappedH),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}
