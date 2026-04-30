package com.example.camera.color

import android.graphics.Color
import android.media.Image
import androidx.annotation.ColorInt
import kotlin.math.sqrt

/**
 * Efficiently extracts the average color from a circular region of an Android Camera Image.
 * 
 * SCENARIO: A real-time color picker UI where only a small circular area 
 * in the center of the screen is used to detect color.
 */
object CircularColorExtractor {

    /**
     * Extracts the average ARGB color.
     * @param image The raw Image from Camera2 or CameraX (usually YUV_420_888)
     * @param centerX Center X of the circle in image coordinates
     * @param centerY Center Y of the circle in image coordinates
     * @param radius Radius of the sampling circle
     */
    @ColorInt
    fun extractAverageColor(image: Image, centerX: Int, centerY: Int, radius: Int): Int {
        // Plane 0 is Y (Luminosity), Plane 1 is U, Plane 2 is V
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var pixelCount = 0

        // MATHEMATICAL OPTIMIZATION:
        // We only iterate within the bounding box of the circle.
        // For each row 'y', we calculate the 'x' range using: x = sqrt(r^2 - dy^2)
        for (y in (centerY - radius)..(centerY + radius)) {
            if (y < 0 || y >= image.height) continue

            val dy = y - centerY
            val xHalfWidth = sqrt((radius * radius - dy * dy).toDouble()).toInt() //Circle in a Square box

            for (x in (centerX - xHalfWidth)..(centerX + xHalfWidth)) {
                if (x < 0 || x >= image.width) continue

                // 1. Extract YUV values
                val yIndex = y * yRowStride + x
                val uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride

                // We use 'and 0xFF' to convert signed byte to unsigned int
                val yVal = yBuffer.get(yIndex).toInt() and 0xFF
                val uVal = (uBuffer.get(uvIndex).toInt() and 0xFF) - 128
                val vVal = (vBuffer.get(uvIndex).toInt() and 0xFF) - 128

                // 2. Fast YUV to RGB conversion
                // R = Y + 1.402 * V
                // G = Y - 0.344136 * U - 0.714136 * V
                // B = Y + 1.772 * U
                val r = (yVal + 1.370705 * vVal).toInt().coerceIn(0, 255)
                val g = (yVal - 0.337633 * uVal - 0.698001 * vVal).toInt().coerceIn(0, 255)
                val b = (yVal + 1.732446 * uVal).toInt().coerceIn(0, 255)

                sumR += r
                sumG += g
                sumB += b
                pixelCount++
            }
        }

        return if (pixelCount > 0) {
            Color.argb(255, (sumR / pixelCount).toInt(), (sumG / pixelCount).toInt(), (sumB / pixelCount).toInt())
        } else {
            Color.TRANSPARENT
        }
    }
}