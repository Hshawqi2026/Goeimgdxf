package com.example.model

import kotlin.math.*

data class Point2D(val x: Double, val y: Double) {
    fun distanceTo(other: Point2D): Double {
        val dx = other.x - x
        val dy = other.y - y
        return sqrt(dx * dx + dy * dy)
    }

    fun angleTo(other: Point2D): Double {
        return atan2(other.y - y, other.x - x)
    }

    fun angleDegreesTo(other: Point2D): Double {
        var deg = Math.toDegrees(angleTo(other))
        if (deg < 0) deg += 360.0
        return deg
    }

    fun translate(dx: Double, dy: Double): Point2D = Point2D(x + dx, y + dy)

    fun scale(factor: Double, origin: Point2D = Point2D(0.0, 0.0)): Point2D {
        return Point2D(
            origin.x + (x - origin.x) * factor,
            origin.y + (y - origin.y) * factor
        )
    }

    fun rotate(angleRad: Double, origin: Point2D = Point2D(0.0, 0.0)): Point2D {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        val dx = x - origin.x
        val dy = y - origin.y
        return Point2D(
            origin.x + dx * cosA - dy * sinA,
            origin.y + dx * sinA + dy * cosA
        )
    }

    operator fun plus(other: Point2D): Point2D = Point2D(x + other.x, y + other.y)
    operator fun minus(other: Point2D): Point2D = Point2D(x - other.x, y - other.y)
    operator fun times(scalar: Double): Point2D = Point2D(x * scalar, y * scalar)
    operator fun div(scalar: Double): Point2D = Point2D(x / scalar, y / scalar)

    fun dot(other: Point2D): Double = x * other.x + y * other.y
    fun cross(other: Point2D): Double = x * other.y - y * other.x
    fun length(): Double = sqrt(x * x + y * y)

    fun normalized(): Point2D {
        val len = length()
        return if (len > 1e-9) Point2D(x / len, y / len) else Point2D(0.0, 0.0)
    }
}
