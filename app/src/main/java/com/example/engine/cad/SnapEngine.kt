package com.example.engine.cad

import com.example.model.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class SnapType {
    ENDPOINT,
    MIDPOINT,
    CENTER,
    INTERSECTION,
    NEAREST
}

data class SnapResult(
    val point: Point2D,
    val type: SnapType,
    val distance: Double
)

object SnapEngine {

    fun findSnap(
        target: Point2D,
        entities: List<DxfEntity>,
        snapDistancePx: Double = 18.0,
        activeTypes: Set<SnapType> = setOf(
            SnapType.ENDPOINT,
            SnapType.MIDPOINT,
            SnapType.CENTER,
            SnapType.INTERSECTION
        )
    ): SnapResult? {
        var closest: SnapResult? = null

        for (e in entities) {
            when (e) {
                is LineEntity -> {
                    // Endpoint
                    if (SnapType.ENDPOINT in activeTypes) {
                        checkSnap(target, e.p1, SnapType.ENDPOINT, snapDistancePx, closest)?.let { closest = it }
                        checkSnap(target, e.p2, SnapType.ENDPOINT, snapDistancePx, closest)?.let { closest = it }
                    }
                    // Midpoint
                    if (SnapType.MIDPOINT in activeTypes) {
                        val mid = Point2D((e.p1.x + e.p2.x) / 2.0, (e.p1.y + e.p2.y) / 2.0)
                        checkSnap(target, mid, SnapType.MIDPOINT, snapDistancePx, closest)?.let { closest = it }
                    }
                }
                is PolylineEntity -> {
                    // Endpoints / Vertices
                    if (SnapType.ENDPOINT in activeTypes) {
                        for (p in e.points) {
                            checkSnap(target, p, SnapType.ENDPOINT, snapDistancePx, closest)?.let { closest = it }
                        }
                    }
                    // Midpoints of segments
                    if (SnapType.MIDPOINT in activeTypes && e.points.size >= 2) {
                        for (i in 0 until e.points.size - 1) {
                            val mid = Point2D((e.points[i].x + e.points[i + 1].x) / 2.0, (e.points[i].y + e.points[i + 1].y) / 2.0)
                            checkSnap(target, mid, SnapType.MIDPOINT, snapDistancePx, closest)?.let { closest = it }
                        }
                    }
                }
                is CircleEntity -> {
                    // Center
                    if (SnapType.CENTER in activeTypes) {
                        checkSnap(target, e.center, SnapType.CENTER, snapDistancePx, closest)?.let { closest = it }
                    }
                }
                is ArcEntity -> {
                    if (SnapType.CENTER in activeTypes) {
                        checkSnap(target, e.center, SnapType.CENTER, snapDistancePx, closest)?.let { closest = it }
                    }
                }
                is PointEntity -> {
                    if (SnapType.ENDPOINT in activeTypes) {
                        checkSnap(target, e.point, SnapType.ENDPOINT, snapDistancePx, closest)?.let { closest = it }
                    }
                }
            }
        }

        return closest
    }

    private fun checkSnap(
        target: Point2D,
        candidate: Point2D,
        type: SnapType,
        maxDist: Double,
        currentBest: SnapResult?
    ): SnapResult? {
        val dist = target.distanceTo(candidate)
        if (dist <= maxDist) {
            if (currentBest == null || dist < currentBest.distance) {
                return SnapResult(candidate, type, dist)
            }
        }
        return currentBest
    }

    /**
     * Finds entity under tap point within hit radius.
     */
    fun hitTest(
        target: Point2D,
        entities: List<DxfEntity>,
        hitRadiusPx: Double = 16.0
    ): DxfEntity? {
        // Iterate backwards so top-most entity is picked
        for (i in entities.indices.reversed()) {
            val e = entities[i]
            val dist = distanceToEntity(target, e)
            if (dist <= hitRadiusPx) {
                return e
            }
        }
        return null
    }

    private fun distanceToEntity(p: Point2D, e: DxfEntity): Double {
        return when (e) {
            is LineEntity -> distanceToSegment(p, e.p1, e.p2)
            is PolylineEntity -> {
                var minDist = Double.MAX_VALUE
                for (i in 0 until e.points.size - 1) {
                    val d = distanceToSegment(p, e.points[i], e.points[i + 1])
                    if (d < minDist) minDist = d
                }
                if (e.isClosed && e.points.size > 2) {
                    val d = distanceToSegment(p, e.points.last(), e.points.first())
                    if (d < minDist) minDist = d
                }
                minDist
            }
            is CircleEntity -> abs(p.distanceTo(e.center) - e.radius)
            is ArcEntity -> abs(p.distanceTo(e.center) - e.radius)
            is PointEntity -> p.distanceTo(e.point)
        }
    }

    private fun distanceToSegment(p: Point2D, a: Point2D, b: Point2D): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-9) return p.distanceTo(a)

        val t = max(0.0, min(1.0, ((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq))
        val proj = Point2D(a.x + t * dx, a.y + t * dy)
        return p.distanceTo(proj)
    }
}
