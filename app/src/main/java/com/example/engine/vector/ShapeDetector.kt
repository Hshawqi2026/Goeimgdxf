package com.example.engine.vector

import com.example.model.*
import java.util.UUID
import kotlin.math.*

object ShapeDetector {

    /**
     * Checks if a closed contour is circular and returns CircleEntity if matched.
     */
    fun detectCircle(points: List<Point2D>, layer: String, minCircularity: Double = 0.82): CircleEntity? {
        if (points.size < 6) return null

        // Calculate centroid
        var cx = 0.0
        var cy = 0.0
        for (p in points) {
            cx += p.x
            cy += p.y
        }
        cx /= points.size
        cy /= points.size
        val center = Point2D(cx, cy)

        // Calculate average radius and variance
        var avgR = 0.0
        for (p in points) {
            avgR += center.distanceTo(p)
        }
        avgR /= points.size

        if (avgR < 5.0) return null // too small for circle

        var variance = 0.0
        for (p in points) {
            val d = center.distanceTo(p) - avgR
            variance += d * d
        }
        variance /= points.size
        val stdDev = sqrt(variance)

        // Standard deviation of radius must be small relative to radius (< 15%)
        if (stdDev / avgR < 0.18) {
            return CircleEntity(
                id = UUID.randomUUID().toString(),
                layer = layer,
                center = center,
                radius = avgR
            )
        }
        return null
    }

    /**
     * Snaps near-orthogonal corners of building polygons to exact 90-degree CAD corners.
     */
    fun orthogonalizePolygon(points: List<Point2D>, angleToleranceDeg: Double = 16.0): List<Point2D> {
        if (points.size < 4) return points

        val pts = points.toMutableList()
        val n = pts.size

        for (i in 0 until n) {
            val prev = pts[(i - 1 + n) % n]
            val curr = pts[i]
            val next = pts[(i + 1) % n]

            val ang1 = prev.angleDegreesTo(curr)
            val ang2 = curr.angleDegreesTo(next)
            var delta = abs(ang2 - ang1)
            while (delta > 180.0) delta = abs(delta - 360.0)

            // If angle delta is near 90 or 270 degrees, snap it
            val diffFrom90 = abs(delta - 90.0)
            if (diffFrom90 <= angleToleranceDeg) {
                // Determine dominant orientation (horizontal or vertical)
                val targetAngle = if (abs(ang1) < 45 || abs(ang1) > 135) {
                    // Previous segment was horizontal-ish -> next segment should be vertical
                    if (ang2 > 0) 90.0 else -90.0
                } else {
                    // Previous segment was vertical-ish -> next segment should be horizontal
                    if (abs(ang2) < 90) 0.0 else 180.0
                }
                val segLen = curr.distanceTo(next)
                val rad = Math.toRadians(targetAngle)
                pts[(i + 1) % n] = Point2D(curr.x + segLen * cos(rad), curr.y + segLen * sin(rad))
            }
        }
        return pts
    }
}
