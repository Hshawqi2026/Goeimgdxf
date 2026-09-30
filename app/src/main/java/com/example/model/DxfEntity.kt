package com.example.model

import kotlin.math.*

sealed class DxfEntity {
    abstract val id: String
    abstract val layer: String
    abstract val color: Long
    abstract val isSelected: Boolean

    abstract fun calculateLength(): Double
    abstract fun calculateArea(): Double
    abstract fun calculateCenter(): Point2D
    abstract fun boundingBox(): BoundingBox
    abstract fun translate(dx: Double, dy: Double): DxfEntity
    abstract fun scale(factor: Double, origin: Point2D): DxfEntity
    abstract fun rotate(angleRad: Double, origin: Point2D): DxfEntity
    abstract fun withSelected(selected: Boolean): DxfEntity
    abstract fun withLayer(newLayer: String): DxfEntity
}

data class LineEntity(
    override val id: String,
    override val layer: String = "0",
    val p1: Point2D,
    val p2: Point2D,
    override val color: Long = 0xFFFFFFFFL,
    override val isSelected: Boolean = false
) : DxfEntity() {
    override fun calculateLength(): Double = p1.distanceTo(p2)
    override fun calculateArea(): Double = 0.0
    override fun calculateCenter(): Point2D = Point2D((p1.x + p2.x) / 2.0, (p1.y + p2.y) / 2.0)
    override fun boundingBox(): BoundingBox = BoundingBox.fromPoints(listOf(p1, p2))

    fun angleDegrees(): Double = p1.angleDegreesTo(p2)

    override fun translate(dx: Double, dy: Double): LineEntity =
        copy(p1 = p1.translate(dx, dy), p2 = p2.translate(dx, dy))

    override fun scale(factor: Double, origin: Point2D): LineEntity =
        copy(p1 = p1.scale(factor, origin), p2 = p2.scale(factor, origin))

    override fun rotate(angleRad: Double, origin: Point2D): LineEntity =
        copy(p1 = p1.rotate(angleRad, origin), p2 = p2.rotate(angleRad, origin))

    override fun withSelected(selected: Boolean): LineEntity = copy(isSelected = selected)
    override fun withLayer(newLayer: String): LineEntity = copy(layer = newLayer)
}

data class PolylineEntity(
    override val id: String,
    override val layer: String = "0",
    val points: List<Point2D>,
    val isClosed: Boolean = false,
    override val color: Long = 0xFFFFFFFFL,
    override val isSelected: Boolean = false
) : DxfEntity() {
    override fun calculateLength(): Double {
        if (points.size < 2) return 0.0
        var len = 0.0
        for (i in 0 until points.size - 1) {
            len += points[i].distanceTo(points[i + 1])
        }
        if (isClosed && points.size > 2) {
            len += points.last().distanceTo(points.first())
        }
        return len
    }

    override fun calculateArea(): Double {
        if (!isClosed || points.size < 3) return 0.0
        // Shoelace formula
        var sum = 0.0
        val n = points.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            sum += points[i].x * points[j].y - points[j].x * points[i].y
        }
        return abs(sum) / 2.0
    }

    override fun calculateCenter(): Point2D {
        if (points.isEmpty()) return Point2D(0.0, 0.0)
        var sumX = 0.0
        var sumY = 0.0
        for (p in points) {
            sumX += p.x
            sumY += p.y
        }
        return Point2D(sumX / points.size, sumY / points.size)
    }

    override fun boundingBox(): BoundingBox = BoundingBox.fromPoints(points)

    override fun translate(dx: Double, dy: Double): PolylineEntity =
        copy(points = points.map { it.translate(dx, dy) })

    override fun scale(factor: Double, origin: Point2D): PolylineEntity =
        copy(points = points.map { it.scale(factor, origin) })

    override fun rotate(angleRad: Double, origin: Point2D): PolylineEntity =
        copy(points = points.map { it.rotate(angleRad, origin) })

    override fun withSelected(selected: Boolean): PolylineEntity = copy(isSelected = selected)
    override fun withLayer(newLayer: String): PolylineEntity = copy(layer = newLayer)
}

data class CircleEntity(
    override val id: String,
    override val layer: String = "0",
    val center: Point2D,
    val radius: Double,
    override val color: Long = 0xFFFFFFFFL,
    override val isSelected: Boolean = false
) : DxfEntity() {
    override fun calculateLength(): Double = 2 * PI * radius
    override fun calculateArea(): Double = PI * radius * radius
    override fun calculateCenter(): Point2D = center
    override fun boundingBox(): BoundingBox = BoundingBox(
        center.x - radius, center.y - radius,
        center.x + radius, center.y + radius
    )

    override fun translate(dx: Double, dy: Double): CircleEntity =
        copy(center = center.translate(dx, dy))

    override fun scale(factor: Double, origin: Point2D): CircleEntity =
        copy(center = center.scale(factor, origin), radius = radius * factor)

    override fun rotate(angleRad: Double, origin: Point2D): CircleEntity =
        copy(center = center.rotate(angleRad, origin))

    override fun withSelected(selected: Boolean): CircleEntity = copy(isSelected = selected)
    override fun withLayer(newLayer: String): CircleEntity = copy(layer = newLayer)
}

data class ArcEntity(
    override val id: String,
    override val layer: String = "0",
    val center: Point2D,
    val radius: Double,
    val startAngleDeg: Double,
    val endAngleDeg: Double,
    override val color: Long = 0xFFFFFFFFL,
    override val isSelected: Boolean = false
) : DxfEntity() {
    override fun calculateLength(): Double {
        var sweep = endAngleDeg - startAngleDeg
        if (sweep < 0) sweep += 360.0
        return Math.toRadians(sweep) * radius
    }
    override fun calculateArea(): Double = 0.0
    override fun calculateCenter(): Point2D = center
    override fun boundingBox(): BoundingBox = BoundingBox(
        center.x - radius, center.y - radius,
        center.x + radius, center.y + radius
    )

    override fun translate(dx: Double, dy: Double): ArcEntity =
        copy(center = center.translate(dx, dy))

    override fun scale(factor: Double, origin: Point2D): ArcEntity =
        copy(center = center.scale(factor, origin), radius = radius * factor)

    override fun rotate(angleRad: Double, origin: Point2D): ArcEntity {
        val deltaDeg = Math.toDegrees(angleRad)
        return copy(
            center = center.rotate(angleRad, origin),
            startAngleDeg = (startAngleDeg + deltaDeg) % 360.0,
            endAngleDeg = (endAngleDeg + deltaDeg) % 360.0
        )
    }

    override fun withSelected(selected: Boolean): ArcEntity = copy(isSelected = selected)
    override fun withLayer(newLayer: String): ArcEntity = copy(layer = newLayer)
}

data class PointEntity(
    override val id: String,
    override val layer: String = "0",
    val point: Point2D,
    override val color: Long = 0xFFFFFFFFL,
    override val isSelected: Boolean = false
) : DxfEntity() {
    override fun calculateLength(): Double = 0.0
    override fun calculateArea(): Double = 0.0
    override fun calculateCenter(): Point2D = point
    override fun boundingBox(): BoundingBox = BoundingBox(point.x - 1, point.y - 1, point.x + 1, point.y + 1)

    override fun translate(dx: Double, dy: Double): PointEntity = copy(point = point.translate(dx, dy))
    override fun scale(factor: Double, origin: Point2D): PointEntity = copy(point = point.scale(factor, origin))
    override fun rotate(angleRad: Double, origin: Point2D): PointEntity = copy(point = point.rotate(angleRad, origin))
    override fun withSelected(selected: Boolean): PointEntity = copy(isSelected = selected)
    override fun withLayer(newLayer: String): PointEntity = copy(layer = newLayer)
}
