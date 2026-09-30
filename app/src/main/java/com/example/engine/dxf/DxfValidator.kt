package com.example.engine.dxf

import com.example.model.*

data class DxfValidationReport(
    val isValid: Boolean,
    val totalEntities: Int,
    val lineCount: Int,
    val polylineCount: Int,
    val circleCount: Int,
    val pointCount: Int,
    val layerCount: Int,
    val boundingBox: BoundingBox,
    val warnings: List<String>,
    val errors: List<String>
)

object DxfValidator {

    fun validate(
        entities: List<DxfEntity>,
        layers: List<CadLayer>,
        calibration: CalibrationData,
        georef: GeoreferenceData
    ): DxfValidationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (entities.isEmpty()) {
            errors.add("Project contains 0 vector entities. Please vectorize an image or draw geometry.")
        }

        var lines = 0
        var polylines = 0
        var circles = 0
        var points = 0

        val layerNames = layers.map { it.name }.toSet()
        var overallBb = BoundingBox.EMPTY

        for (e in entities) {
            when (e) {
                is LineEntity -> {
                    lines++
                    if (e.p1.x.isNaN() || e.p1.y.isNaN() || e.p2.x.isNaN() || e.p2.y.isNaN()) {
                        errors.add("Line ${e.id} contains NaN coordinates.")
                    }
                    if (e.p1.x.isInfinite() || e.p1.y.isInfinite() || e.p2.x.isInfinite() || e.p2.y.isInfinite()) {
                        errors.add("Line ${e.id} contains Infinite coordinates.")
                    }
                    if (e.p1.distanceTo(e.p2) < 1e-4) {
                        warnings.add("Line ${e.id} has near-zero length.")
                    }
                }
                is PolylineEntity -> {
                    polylines++
                    if (e.points.size < 2) {
                        errors.add("Polyline ${e.id} has fewer than 2 vertices.")
                    }
                    for (p in e.points) {
                        if (p.x.isNaN() || p.y.isNaN() || p.x.isInfinite() || p.y.isInfinite()) {
                            errors.add("Polyline ${e.id} has invalid coordinates.")
                            break
                        }
                    }
                }
                is CircleEntity -> {
                    circles++
                    if (e.radius <= 0 || e.radius.isNaN()) {
                        errors.add("Circle ${e.id} has invalid radius: ${e.radius}")
                    }
                }
                is ArcEntity -> {
                    circles++
                    if (e.radius <= 0) errors.add("Arc ${e.id} has invalid radius.")
                }
                is PointEntity -> {
                    points++
                }
            }

            if (!layerNames.contains(e.layer)) {
                warnings.add("Entity ${e.id} references undefined layer '${e.layer}' (will be placed in Layer 0).")
            }

            val bb = e.boundingBox()
            overallBb = if (overallBb == BoundingBox.EMPTY) bb else overallBb.union(bb)
        }

        if (calibration.isCalibrated && calibration.realDistance <= 0) {
            warnings.add("Calibration is enabled but real distance is 0. Scale factor defaults to 1.0.")
        }

        if (georef.isEnabled && georef.gcps.size < 2) {
            warnings.add("Georeferencing requires at least 2 GCPs. Coordinates will remain in local units.")
        }

        val isValid = errors.isEmpty() && entities.isNotEmpty()

        return DxfValidationReport(
            isValid = isValid,
            totalEntities = entities.size,
            lineCount = lines,
            polylineCount = polylines,
            circleCount = circles,
            pointCount = points,
            layerCount = layers.size,
            boundingBox = overallBb,
            warnings = warnings,
            errors = errors
        )
    }
}
