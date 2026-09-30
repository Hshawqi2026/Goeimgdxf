package com.example.model

import java.util.Locale

enum class CadUnit(
    val symbol: String,
    val displayName: String,
    val toMeters: Double,
    val dxfCode: Int
) {
    MM("mm", "Millimeters", 0.001, 4),
    CM("cm", "Centimeters", 0.01, 5),
    M("m", "Meters", 1.0, 6),
    KM("km", "Kilometers", 1000.0, 7),
    FT("ft", "Feet", 0.3048, 2),
    INCH("in", "Inches", 0.0254, 1);

    companion object {
        fun fromSymbol(sym: String): CadUnit =
            entries.find { it.symbol.equals(sym, ignoreCase = true) } ?: M
    }
}

data class CalibrationData(
    val point1: Point2D? = null,
    val point2: Point2D? = null,
    val realDistance: Double = 0.0,
    val unit: CadUnit = CadUnit.M,
    val isCalibrated: Boolean = false
) {
    val pixelDistance: Double
        get() = if (point1 != null && point2 != null) point1.distanceTo(point2) else 0.0

    /**
     * Scale factor: real units per pixel.
     * e.g., 0.05 meters per pixel.
     */
    val unitsPerPixel: Double
        get() = if (isCalibrated && pixelDistance > 1e-6 && realDistance > 0.0) {
            realDistance / pixelDistance
        } else {
            1.0 // 1 unit per pixel if uncalibrated
        }

    fun formatDistance(distPixels: Double): String {
        val real = distPixels * unitsPerPixel
        return if (isCalibrated) {
            String.format(Locale.US, "%.2f %s", real, unit.symbol)
        } else {
            String.format(Locale.US, "%.1f px", distPixels)
        }
    }

    fun formatArea(areaPixels2: Double): String {
        val real = areaPixels2 * unitsPerPixel * unitsPerPixel
        return if (isCalibrated) {
            String.format(Locale.US, "%.2f %s²", real, unit.symbol)
        } else {
            String.format(Locale.US, "%.0f px²", areaPixels2)
        }
    }

    fun formatCoordinate(px: Double, py: Double): String {
        val rx = px * unitsPerPixel
        val ry = py * unitsPerPixel
        return if (isCalibrated) {
            String.format(Locale.US, "X: %.2f %s  Y: %.2f %s", rx, unit.symbol, ry, unit.symbol)
        } else {
            String.format(Locale.US, "X: %.0f px  Y: %.0f px", px, py)
        }
    }
}
