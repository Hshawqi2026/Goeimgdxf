package com.example.model

import kotlin.math.max
import kotlin.math.min

data class BoundingBox(
    val minX: Double,
    val minY: Double,
    val maxX: Double,
    val maxY: Double
) {
    val width: Double get() = max(0.0, maxX - minX)
    val height: Double get() = max(0.0, maxY - minY)
    val centerX: Double get() = (minX + maxX) / 2.0
    val centerY: Double get() = (minY + maxY) / 2.0

    fun contains(point: Point2D): Boolean {
        return point.x in minX..maxX && point.y in minY..maxY
    }

    fun expand(margin: Double): BoundingBox {
        return BoundingBox(minX - margin, minY - margin, maxX + margin, maxY + margin)
    }

    fun union(other: BoundingBox): BoundingBox {
        return BoundingBox(
            min(minX, other.minX),
            min(minY, other.minY),
            max(maxX, other.maxX),
            max(maxY, other.maxY)
        )
    }

    companion object {
        val EMPTY = BoundingBox(0.0, 0.0, 0.0, 0.0)

        fun fromPoints(points: List<Point2D>): BoundingBox {
            if (points.isEmpty()) return EMPTY
            var minX = points[0].x
            var minY = points[0].y
            var maxX = points[0].x
            var maxY = points[0].y
            for (p in points) {
                if (p.x < minX) minX = p.x
                if (p.y < minY) minY = p.y
                if (p.x > maxX) maxX = p.x
                if (p.y > maxY) maxY = p.y
            }
            return BoundingBox(minX, minY, maxX, maxY)
        }
    }
}
