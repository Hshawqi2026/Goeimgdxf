package com.example.engine.vector

import com.example.model.*
import java.util.UUID
import kotlin.math.abs
import kotlin.math.min

object GeometryCleaner {

    fun clean(
        entities: List<DxfEntity>,
        settings: VectorizationSettings
    ): List<DxfEntity> {
        val result = mutableListOf<DxfEntity>()

        for (entity in entities) {
            when (entity) {
                is LineEntity -> {
                    if (entity.calculateLength() >= settings.minLineLength) {
                        result.add(entity)
                    }
                }
                is PolylineEntity -> {
                    val simplified = DouglasPeucker.simplify(entity.points, settings.douglasPeuckerEpsilon)
                    if (simplified.size >= 2) {
                        var isClosed = entity.isClosed
                        val first = simplified.first()
                        val last = simplified.last()

                        // Check if ends can be welded/closed
                        val endDist = first.distanceTo(last)
                        if (!isClosed && settings.closeNearlyClosedPolygons && endDist <= settings.closeLoopDistance && simplified.size >= 3) {
                            isClosed = true
                        }

                        // Filter out degenerate zero area polygons
                        val poly = entity.copy(points = simplified, isClosed = isClosed)
                        if (isClosed && poly.calculateArea() < settings.minContourArea) {
                            continue
                        }
                        if (poly.calculateLength() >= settings.minLineLength) {
                            result.add(poly)
                        }
                    }
                }
                is CircleEntity -> {
                    if (entity.radius >= 3.0) {
                        result.add(entity)
                    }
                }
                else -> result.add(entity)
            }
        }

        // Weld close endpoints across entities
        val welded = weldEndpoints(result, settings.weldDistance)

        // Remove duplicates if enabled
        val deduplicated = if (settings.removeDuplicateLines) {
            removeDuplicates(welded)
        } else {
            welded
        }

        return deduplicated
    }

    private fun weldEndpoints(entities: List<DxfEntity>, weldDist: Double): List<DxfEntity> {
        if (weldDist <= 0.1) return entities

        // Collect all distinct endpoints
        val keypoints = mutableListOf<Point2D>()
        for (e in entities) {
            when (e) {
                is LineEntity -> {
                    keypoints.add(e.p1)
                    keypoints.add(e.p2)
                }
                is PolylineEntity -> {
                    if (e.points.isNotEmpty()) {
                        keypoints.add(e.points.first())
                        keypoints.add(e.points.last())
                    }
                }
                else -> {}
            }
        }

        // Cluster keypoints
        fun findClusterRep(p: Point2D): Point2D {
            for (kp in keypoints) {
                if (p.distanceTo(kp) <= weldDist) {
                    return kp
                }
            }
            return p
        }

        return entities.map { e ->
            when (e) {
                is LineEntity -> {
                    val p1Weld = findClusterRep(e.p1)
                    val p2Weld = findClusterRep(e.p2)
                    if (p1Weld.distanceTo(p2Weld) > 1e-4) {
                        e.copy(p1 = p1Weld, p2 = p2Weld)
                    } else e
                }
                is PolylineEntity -> {
                    if (e.points.size >= 2) {
                        val pts = e.points.toMutableList()
                        pts[0] = findClusterRep(pts[0])
                        pts[pts.size - 1] = findClusterRep(pts[pts.size - 1])
                        e.copy(points = pts)
                    } else e
                }
                else -> e
            }
        }
    }

    private fun removeDuplicates(entities: List<DxfEntity>): List<DxfEntity> {
        val unique = mutableListOf<DxfEntity>()
        val seenLineKeys = mutableSetOf<String>()

        for (e in entities) {
            if (e is LineEntity) {
                val k1 = "${Math.round(e.p1.x)},${Math.round(e.p1.y)}-${Math.round(e.p2.x)},${Math.round(e.p2.y)}"
                val k2 = "${Math.round(e.p2.x)},${Math.round(e.p2.y)}-${Math.round(e.p1.x)},${Math.round(e.p1.y)}"
                if (!seenLineKeys.contains(k1) && !seenLineKeys.contains(k2)) {
                    seenLineKeys.add(k1)
                    seenLineKeys.add(k2)
                    unique.add(e)
                }
            } else {
                unique.add(e)
            }
        }
        return unique
    }
}
