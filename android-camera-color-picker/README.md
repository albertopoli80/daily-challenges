# Efficient Circular Color Extraction (Android Camera)

This module provides a high-performance way to sample the average color from a circular region in an Android `android.media.Image` (YUV_420_888).

## The Performance Problem
When building a real-time color picker, many developers make the mistake of:
1. Converting the entire 12MP camera frame from YUV to RGB.
2. Creating a Bitmap from the whole frame.
3. Cropping the Bitmap to a circle.

This causes massive memory allocations and high CPU/GPU usage, leading to thermal throttling and low FPS.

## Our Optimized Solution: "Direct Sampling"
Our `CircularColorExtractor` is designed for speed:
1. **Geometric Pruning**: We don't iterate over the entire image. We only process pixels within the bounding box of the circle.
2. **Pythagorean Skip**: For every row, we calculate the exact `x` range using `sqrt(r^2 - dy^2)`. This ensures we only touch pixels that are actually inside the circle.
3. **Direct YUV-to-RGB**: We read the raw Y, U, and V bytes directly from the hardware buffers and perform the math on-the-fly for only the sampled pixels.
4. **Zero Allocations**: No Bitmaps or temporary objects are created during the extraction process, making it perfectly safe for 60FPS loops.

## How to use
Pass the `android.media.Image` from your `ImageAnalysis.Analyzer` (CameraX) or `onImageAvailable` (Camera2) callback to the `extractAverageColor` function.

```kotlin
val avgColor = CircularColorExtractor.extractAverageColor(
    image = cameraImage,
    centerX = screenWidth / 2,
    centerY = screenHeight / 2,
    radius = 50
)
```

## Result
A lightweight, zero-latency color detection engine that allows your app to stay cool and responsive even during long camera sessions.
