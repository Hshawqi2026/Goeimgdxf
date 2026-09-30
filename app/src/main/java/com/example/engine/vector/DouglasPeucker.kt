package com.example.engine.vector

import com.example.model.Point2D
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

object DouglasPeucker {

    /**
     * Ramer-Douglas-Peucker simplification algorithm.
     */
    fun simplify(points: List<Point2D>, epsilon: Double): List<Point2D> {
        if (points.size <= 2) return points

        val isClosed = points.first().distanceTo(points.last()) < 1e-4

        return if (isClosed && points.size > 3) {
            // Find point furthest from start
            var maxDist = -1.0
            var farIdx = 0
            val p0 = points[0]
            for (i in 1 until points.size) {
                val d = p0.distanceTo(points[i])
                if (d > maxDist) {
                    maxDist = d
                    farIdx = i
                }
            }
            // Split into two halves and simplify both
            val half1 = simplifyOpen(points.subList(0, farIdx + 1), epsilon)
            val half2 = simplifyOpen(points.subList(farIdx, points.size), epsilon)
            val combined = half1.dropLast(1) + half2
            combined
        } else {
            simplifyOpen(points, epsilon)
        }
    }

    private fun simplifyOpen(points: List<Point2D>, epsilon: Double): List<Point2D> {
        if (points.size <= 2) return points

        var maxDistance = 0.0
        var index = 0
        val start = points.first()
        val end = points.last()

        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], start, end)
            if (dist > maxDistance) {
                maxDistance = dist
                index = i
            }
        }

        return if (maxDistance > epsilon) {
            val rec1 = simplifyOpen(points.subList(0, index + 1), epsilon)
            val rec2 = simplifyOpen(points.subList(index, points.size), epsilon)
            rec1.dropLast(1) + rec2
        } else {
            listOf(start, end)
        }
    }

    private fun perpendicularDistance(p: Point2D, lineStart: Point2D, lineEnd: Point2D): Double {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val lineLenSq = dx * dx + dy * dy
        if (lineLenSq < 1e-9) return p.distanceTo(lineStart)

        // Area of triangle * 2 / base length
        val numerator = abs(dy * p.x - dx * p.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x)
        return numerator / sqrt(lineLenSq)
    }
}
