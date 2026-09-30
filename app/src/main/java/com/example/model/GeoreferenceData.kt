package com.example.model

import java.util.Locale
import kotlin.math.*

data class GcpPoint(
    val id: String,
    val pixelPoint: Point2D,
    val worldX: Double,
    val worldY: Double,
    val label: String = ""
)

enum class GeorefMethod(val displayName: String) {
    CONFORMAL_2_POINT("2-Point Conformal (Helmert)"),
    AFFINE_3_POINT("3-Point Affine (General 6-param)")
}

data class GeoreferenceData(
    val crsName: String = "WGS 84 / UTM Zone 36N",
    val gcps: List<GcpPoint> = emptyList(),
    val isEnabled: Boolean = false,
    val method: GeorefMethod = GeorefMethod.CONFORMAL_2_POINT
) {
    /**
     * Compute transformation matrix parameters.
     * For 2-point Helmert:
     *   worldX = a * px - b * py + c
     *   worldY = b * px + a * py + d
     */
    fun transform(point: Point2D): Point2D {
        if (!isEnabled || gcps.size < 2) return point

        if (gcps.size == 2 || method == GeorefMethod.CONFORMAL_2_POINT) {
            val p1 = gcps[0]
            val p2 = gcps[1]

            val dPx = p2.pixelPoint.x - p1.pixelPoint.x
            val dPy = p2.pixelPoint.y - p1.pixelPoint.y
            val dWx = p2.worldX - p1.worldX
            val dWy = p2.worldY - p1.worldY

            val denom = dPx * dPx + dPy * dPy
            if (denom < 1e-9) return point

            val a = (dPx * dWx + dPy * dWy) / denom
            val b = (dPx * dWy - dPy * dWx) / denom
            val c = p1.worldX - (a * p1.pixelPoint.x - b * p1.pixelPoint.y)
            val d = p1.worldY - (b * p1.pixelPoint.x + a * p1.pixelPoint.y)

            val outX = a * point.x - b * point.y + c
            val outY = b * point.x + a * point.y + d
            return Point2D(outX, outY)
        } else {
            // 3-point Affine
            val p1 = gcps[0]
            val p2 = gcps[1]
            val p3 = gcps[2]

            // Matrix inversion for [px py 1] -> [wx wy]
            val det = p1.pixelPoint.x * (p2.pixelPoint.y - p3.pixelPoint.y) -
                      p1.pixelPoint.y * (p2.pixelPoint.x - p3.pixelPoint.x) +
                      (p2.pixelPoint.x * p3.pixelPoint.y - p3.pixelPoint.x * p2.pixelPoint.y)

            if (abs(det) < 1e-9) return point

            val a1 = ((p2.pixelPoint.y - p3.pixelPoint.y) * p1.worldX +
                      (p3.pixelPoint.y - p1.pixelPoint.y) * p2.worldX +
                      (p1.pixelPoint.y - p2.pixelPoint.y) * p3.worldX) / det
            val b1 = ((p3.pixelPoint.x - p2.pixelPoint.x) * p1.worldX +
                      (p1.pixelPoint.x - p3.pixelPoint.x) * p2.worldX +
                      (p2.pixelPoint.x - p1.pixelPoint.x) * p3.worldX) / det
            val c1 = ((p2.pixelPoint.x * p3.pixelPoint.y - p3.pixelPoint.x * p2.pixelPoint.y) * p1.worldX +
                      (p3.pixelPoint.x * p1.pixelPoint.y - p1.pixelPoint.x * p3.pixelPoint.y) * p2.worldX +
                      (p1.pixelPoint.x * p2.pixelPoint.y - p2.pixelPoint.x * p1.pixelPoint.y) * p3.worldX) / det

            val a2 = ((p2.pixelPoint.y - p3.pixelPoint.y) * p1.worldY +
                      (p3.pixelPoint.y - p1.pixelPoint.y) * p2.worldY +
                      (p1.pixelPoint.y - p2.pixelPoint.y) * p3.worldY) / det
            val b2 = ((p3.pixelPoint.x - p2.pixelPoint.x) * p1.worldY +
                      (p1.pixelPoint.x - p3.pixelPoint.x) * p2.worldY +
                      (p2.pixelPoint.x - p1.pixelPoint.x) * p3.worldY) / det
            val c2 = ((p2.pixelPoint.x * p3.pixelPoint.y - p3.pixelPoint.x * p2.pixelPoint.y) * p1.worldY +
                      (p3.pixelPoint.x * p1.pixelPoint.y - p1.pixelPoint.x * p3.pixelPoint.y) * p2.worldY +
                      (p1.pixelPoint.x * p2.pixelPoint.y - p2.pixelPoint.x * p1.pixelPoint.y) * p3.worldY) / det

            val outX = a1 * point.x + b1 * point.y + c1
            val outY = a2 * point.x + b2 * point.y + c2
            return Point2D(outX, outY)
        }
    }

    fun formatWorldCoordinate(px: Double, py: Double): String {
        val wp = transform(Point2D(px, py))
        return String.format(Locale.US, "E: %.2f | N: %.2f", wp.x, wp.y)
    }
}
