package com.example.engine.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.model.ThresholdMode
import com.example.model.VectorizationSettings
import java.io.InputStream
import kotlin.math.*

object ImageProcessor {

    /**
     * Memory-safe bitmap loading with inSampleSize.
     * Keeps max dimension around 1200-1600 px for optimal speed & high-fidelity vectorization.
     */
    fun decodeSampledBitmap(
        inputStreamProvider: () -> InputStream?,
        maxDimension: Int = 1400
    ): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        var stream = inputStreamProvider() ?: return null
        BitmapFactory.decodeStream(stream, null, options)
        stream.close()

        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) return null

        var inSampleSize = 1
        val maxDim = max(width, height)
        while ((maxDim / inSampleSize) > maxDimension) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        stream = inputStreamProvider() ?: return null
        val bitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
        stream.close()
        return bitmap
    }

    /**
     * Complete preprocessing and edge binarization pipeline:
     * 1. Grayscale conversion
     * 2. Contrast enhancement (min-max / gamma stretch)
     * 3. Noise reduction (Gaussian blur)
     * 4. Edge detection / Adaptive thresholding
     * 5. Morphological Closing (dilation then erosion to connect building walls and road lines)
     */
    fun processToBinary(
        bitmap: Bitmap,
        settings: VectorizationSettings,
        onProgress: (step: String, percent: Float) -> Unit = { _, _ -> }
    ): BinaryImage {
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height

        onProgress("Analyzing image pixels...", 0.1f)
        val pixels = IntArray(totalPixels)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 1. Grayscale
        val gray = ByteArray(totalPixels)
        for (i in 0 until totalPixels) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
            gray[i] = lum.toByte()
        }

        onProgress("Enhancing contrast & reducing noise...", 0.25f)
        // 2. Contrast Enhancement
        enhanceContrast(gray, width, height, settings.contrastBoost)

        // 3. Gaussian Blur (5-tap separable: [1, 4, 6, 4, 1] / 16)
        val blurred = gaussianBlur5x5(gray, width, height)

        onProgress("Extracting edge boundaries...", 0.45f)
        // 4. Thresholding / Edge Detection
        val binary = when (settings.thresholdMode) {
            ThresholdMode.ADAPTIVE_GAUSSIAN -> adaptiveThreshold(blurred, width, height, settings.edgeSensitivity)
            ThresholdMode.CANNY_EDGES -> cannyEdgeDetector(blurred, width, height, settings.edgeSensitivity)
            ThresholdMode.OTSU_GLOBAL -> otsuThreshold(blurred, width, height)
        }

        onProgress("Connecting geometric segments...", 0.65f)
        // 5. Morphological Closing to bridge gaps in building walls and parcel lines
        val closed = if (settings.morphologicalClosingRadius > 0) {
            morphologicalClose(binary, width, height, settings.morphologicalClosingRadius)
        } else {
            binary
        }

        return BinaryImage(closed, width, height)
    }

    private fun enhanceContrast(gray: ByteArray, width: Int, height: Int, contrastFactor: Float) {
        val n = width * height
        // Find 2nd and 98th percentile for robust min-max stretch
        val hist = IntArray(256)
        for (i in 0 until n) {
            hist[gray[i].toInt() and 0xFF]++
        }
        val p2Count = (n * 0.02).toInt()
        val p98Count = (n * 0.98).toInt()
        var cum = 0
        var minVal = 0
        var maxVal = 255
        for (v in 0..255) {
            cum += hist[v]
            if (cum >= p2Count && minVal == 0) minVal = v
            if (cum >= p98Count) {
                maxVal = v
                break
            }
        }
        val range = max(1, maxVal - minVal)
        for (i in 0 until n) {
            val v = (gray[i].toInt() and 0xFF)
            val stretched = ((v - minVal) * 255.0f / range).coerceIn(0f, 255f)
            // Apply subtle contrast curve
            val centered = (stretched - 128f) * contrastFactor + 128f
            gray[i] = centered.toInt().coerceIn(0, 255).toByte()
        }
    }

    private fun gaussianBlur5x5(src: ByteArray, width: Int, height: Int): ByteArray {
        val temp = ByteArray(width * height)
        val dst = ByteArray(width * height)

        // Horizontal pass [1, 4, 6, 4, 1] / 16
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val p_2 = (src[rowOffset + (x - 2).coerceIn(0, width - 1)].toInt() and 0xFF)
                val p_1 = (src[rowOffset + (x - 1).coerceIn(0, width - 1)].toInt() and 0xFF)
                val p0  = (src[rowOffset + x].toInt() and 0xFF)
                val p1  = (src[rowOffset + (x + 1).coerceIn(0, width - 1)].toInt() and 0xFF)
                val p2  = (src[rowOffset + (x + 2).coerceIn(0, width - 1)].toInt() and 0xFF)
                val sum = (p_2 + 4 * p_1 + 6 * p0 + 4 * p1 + p2) shr 4
                temp[rowOffset + x] = sum.toByte()
            }
        }

        // Vertical pass
        for (x in 0 until width) {
            for (y in 0 until height) {
                val p_2 = (temp[(y - 2).coerceIn(0, height - 1) * width + x].toInt() and 0xFF)
                val p_1 = (temp[(y - 1).coerceIn(0, height - 1) * width + x].toInt() and 0xFF)
                val p0  = (temp[y * width + x].toInt() and 0xFF)
                val p1  = (temp[(y + 1).coerceIn(0, height - 1) * width + x].toInt() and 0xFF)
                val p2  = (temp[(y + 2).coerceIn(0, height - 1) * width + x].toInt() and 0xFF)
                val sum = (p_2 + 4 * p_1 + 6 * p0 + 4 * p1 + p2) shr 4
                dst[y * width + x] = sum.toByte()
            }
        }
        return dst
    }

    /**
     * Fast O(1) integral-image based adaptive thresholding (Bradley-Roth).
     * Ideal for aerial and cadastral images with uneven sunlight and shadows.
     */
    private fun adaptiveThreshold(gray: ByteArray, width: Int, height: Int, sensitivity: Int): ByteArray {
        val out = ByteArray(width * height)
        val s = max(8, width / 32) // Window radius
        val s2 = s / 2

        // Compute integral image
        val integral = LongArray((width + 1) * (height + 1))
        for (y in 0 until height) {
            var sum = 0L
            val rowOffset = y * width
            for (x in 0 until width) {
                sum += (gray[rowOffset + x].toInt() and 0xFF)
                val idx = (y + 1) * (width + 1) + (x + 1)
                val aboveIdx = y * (width + 1) + (x + 1)
                integral[idx] = integral[aboveIdx] + sum
            }
        }

        // Threshold factor: percentage below local mean
        val t = 1.0 - (sensitivity * 0.003) // e.g. 0.85 to 0.95

        for (y in 0 until height) {
            val y1 = max(0, y - s2)
            val y2 = min(height - 1, y + s2)
            val rowOffset = y * width

            for (x in 0 until width) {
                val x1 = max(0, x - s2)
                val x2 = min(width - 1, x + s2)
                val count = (x2 - x1 + 1) * (y2 - y1 + 1)

                val idxA = y1 * (width + 1) + x1
                val idxB = y1 * (width + 1) + (x2 + 1)
                val idxC = (y2 + 1) * (width + 1) + x1
                val idxD = (y2 + 1) * (width + 1) + (x2 + 1)

                val sum = integral[idxD] - integral[idxB] - integral[idxC] + integral[idxA]
                val localMean = sum.toDouble() / count
                val pixelVal = (gray[rowOffset + x].toInt() and 0xFF).toDouble()

                // Mark edge/boundary as 1 (foreground)
                if (pixelVal <= localMean * t || pixelVal >= localMean * (2.0 - t)) {
                    out[rowOffset + x] = 1
                } else {
                    out[rowOffset + x] = 0
                }
            }
        }
        return out
    }

    /**
     * Canny Edge Detector: Sobel gradients, Non-Maximum Suppression, Hysteresis
     */
    private fun cannyEdgeDetector(gray: ByteArray, width: Int, height: Int, sensitivity: Int): ByteArray {
        val out = ByteArray(width * height)
        val mag = FloatArray(width * height)
        val angle = ByteArray(width * height) // 0: 0 deg, 1: 45 deg, 2: 90 deg, 3: 135 deg

        val highThreshold = max(20f, (110 - sensitivity).toFloat())
        val lowThreshold = highThreshold * 0.4f

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val p00 = gray[(y - 1) * width + (x - 1)].toInt() and 0xFF
                val p01 = gray[(y - 1) * width + x].toInt() and 0xFF
                val p02 = gray[(y - 1) * width + (x + 1)].toInt() and 0xFF
                val p10 = gray[y * width + (x - 1)].toInt() and 0xFF
                val p12 = gray[y * width + (x + 1)].toInt() and 0xFF
                val p20 = gray[(y + 1) * width + (x - 1)].toInt() and 0xFF
                val p21 = gray[(y + 1) * width + x].toInt() and 0xFF
                val p22 = gray[(y + 1) * width + (x + 1)].toInt() and 0xFF

                val gx = (p02 + 2 * p12 + p22) - (p00 + 2 * p10 + p20)
                val gy = (p20 + 2 * p21 + p22) - (p00 + 2 * p01 + p02)
                val m = sqrt((gx * gx + gy * gy).toFloat())
                val idx = y * width + x
                mag[idx] = m

                var a = atan2(gy.toDouble(), gx.toDouble()) * 180.0 / PI
                if (a < 0) a += 180.0
                angle[idx] = when {
                    a < 22.5 || a >= 157.5 -> 0 // Horizontal
                    a in 22.5..67.5 -> 1       // 45
                    a in 67.5..112.5 -> 2      // Vertical
                    else -> 3                  // 135
                }.toByte()
            }
        }

        // Non-Maximum Suppression (NMS)
        val nms = FloatArray(width * height)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val m = mag[idx]
                if (m < lowThreshold) continue

                val isMax = when (angle[idx].toInt()) {
                    0 -> m >= mag[idx - 1] && m >= mag[idx + 1]
                    1 -> m >= mag[(y - 1) * width + (x + 1)] && m >= mag[(y + 1) * width + (x - 1)]
                    2 -> m >= mag[(y - 1) * width + x] && m >= mag[(y + 1) * width + x]
                    3 -> m >= mag[(y - 1) * width + (x - 1)] && m >= mag[(y + 1) * width + (x + 1)]
                    else -> false
                }
                if (isMax) nms[idx] = m
            }
        }

        // Hysteresis thresholding
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                if (nms[idx] >= highThreshold) {
                    out[idx] = 1
                } else if (nms[idx] >= lowThreshold) {
                    // Check if connected to high threshold neighbor
                    var hasHighNeighbor = false
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (nms[(y + dy) * width + (x + dx)] >= highThreshold) {
                                hasHighNeighbor = true
                                break
                            }
                        }
                        if (hasHighNeighbor) break
                    }
                    if (hasHighNeighbor) out[idx] = 1
                }
            }
        }
        return out
    }

    private fun otsuThreshold(gray: ByteArray, width: Int, height: Int): ByteArray {
        val total = width * height
        val hist = IntArray(256)
        for (i in 0 until total) {
            hist[gray[i].toInt() and 0xFF]++
        }

        var sum = 0.0
        for (t in 0..255) sum += t * hist[t]

        var sumB = 0.0
        var wB = 0
        var varMax = 0.0
        var threshold = 128

        for (t in 0..255) {
            wB += hist[t]
            if (wB == 0) continue
            val wF = total - wB
            if (wF == 0) break

            sumB += t * hist[t]
            val mB = sumB / wB
            val mF = (sum - sumB) / wF
            val varBetween = wB.toDouble() * wF.toDouble() * (mB - mF) * (mB - mF)

            if (varBetween > varMax) {
                varMax = varBetween
                threshold = t
            }
        }

        val out = ByteArray(total)
        for (i in 0 until total) {
            out[i] = if ((gray[i].toInt() and 0xFF) < threshold) 1 else 0
        }
        return out
    }

    private fun morphologicalClose(binary: ByteArray, width: Int, height: Int, radius: Int): ByteArray {
        val dilated = dilate(binary, width, height, radius)
        return erode(dilated, width, height, radius)
    }

    private fun dilate(src: ByteArray, width: Int, height: Int, radius: Int): ByteArray {
        val dst = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (src[y * width + x].toInt() == 1) {
                    for (dy in -radius..radius) {
                        val ny = y + dy
                        if (ny !in 0 until height) continue
                        for (dx in -radius..radius) {
                            val nx = x + dx
                            if (nx in 0 until width) {
                                dst[ny * width + nx] = 1
                            }
                        }
                    }
                }
            }
        }
        return dst
    }

    private fun erode(src: ByteArray, width: Int, height: Int, radius: Int): ByteArray {
        val dst = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (src[y * width + x].toInt() == 1) {
                    var allForeground = true
                    for (dy in -radius..radius) {
                        val ny = y + dy
                        if (ny !in 0 until height) {
                            allForeground = false
                            break
                        }
                        for (dx in -radius..radius) {
                            val nx = x + dx
                            if (nx !in 0 until width || src[ny * width + nx].toInt() != 1) {
                                allForeground = false
                                break
                            }
                        }
                        if (!allForeground) break
                    }
                    if (allForeground) {
                        dst[y * width + x] = 1
                    }
                }
            }
        }
        return dst
    }
}

data class BinaryImage(
    val data: ByteArray,
    val width: Int,
    val height: Int
) {
    fun get(x: Int, y: Int): Int {
        if (x !in 0 until width || y !in 0 until height) return 0
        return data[y * width + x].toInt() and 0xFF
    }
}
