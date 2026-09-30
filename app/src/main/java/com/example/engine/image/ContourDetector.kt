package com.example.engine.image

import com.example.model.Point2D
import kotlin.math.abs

data class RawContour(
    val points: List<Point2D>,
    val isClosed: Boolean,
    val isHole: Boolean = false
) {
    fun calculateArea(): Double {
        if (points.size < 3) return 0.0
        var sum = 0.0
        val n = points.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            sum += points[i].x * points[j].y - points[j].x * points[i].y
        }
        return abs(sum) / 2.0
    }

    fun calculatePerimeter(): Double {
        if (points.size < 2) return 0.0
        var p = 0.0
        for (i in 0 until points.size - 1) {
            p += points[i].distanceTo(points[i + 1])
        }
        if (isClosed && points.size > 2) {
            p += points.last().distanceTo(points.first())
        }
        return p
    }
}

object ContourDetector {

    // 8-neighborhood offsets (Clockwise: E, SE, S, SW, W, NW, N, NE)
    private val DX = intArrayOf(1, 1, 0, -1, -1, -1, 0, 1)
    private val DY = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)

    /**
     * Extracts contours from binary image using topological border following.
     */
    fun findContours(
        binary: BinaryImage,
        minArea: Double = 30.0,
        maxCount: Int = 1200
    ): List<RawContour> {
        val width = binary.width
        val height = binary.height
        val visited = BooleanArray(width * height)
        val contours = mutableListOf<RawContour>()

        // Scan line by line
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                if (binary.data[idx].toInt() == 1 && !visited[idx]) {
                    // Check if border pixel (has at least one 0 neighbor)
                    var isBorder = false
                    for (k in 0 until 8) {
                        val nx = x + DX[k]
                        val ny = y + DY[k]
                        if (binary.get(nx, ny) == 0) {
                            isBorder = true
                            break
                        }
                    }

                    if (isBorder) {
                        val chain = traceBorder(binary, visited, x, y, width, height)
                        if (chain.size >= 4) {
                            val isClosed = chain.first().distanceTo(chain.last()) <= 4.0
                            val contour = RawContour(chain, isClosed = isClosed)
                            if (contour.calculateArea() >= minArea || chain.size >= 8) {
                                contours.add(contour)
                                if (contours.size >= maxCount) return contours
                            }
                        }
                    }
                }
            }
        }
        return contours
    }

    /**
     * Traces boundary of connected component using Moore neighbor algorithm.
     */
    private fun traceBorder(
        binary: BinaryImage,
        visited: BooleanArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int
    ): List<Point2D> {
        val points = mutableListOf<Point2D>()
        var currX = startX
        var currY = startY
        var dir = 0 // initial direction
        val maxSteps = 4000
        var steps = 0

        points.add(Point2D(currX.toDouble(), currY.toDouble()))
        visited[currY * width + currX] = true

        while (steps < maxSteps) {
            steps++
            var foundNext = false
            // Scan 8 neighbors starting from (dir + 5) % 8 (backtrack direction)
            val searchStart = (dir + 5) % 8

            for (i in 0 until 8) {
                val checkDir = (searchStart + i) % 8
                val nx = currX + DX[checkDir]
                val ny = currY + DY[checkDir]

                if (nx in 0 until width && ny in 0 until height && binary.get(nx, ny) == 1) {
                    currX = nx
                    currY = ny
                    dir = checkDir
                    foundNext = true
                    break
                }
            }

            if (!foundNext) break

            // Check if looped back to start
            if (currX == startX && currY == startY && points.size > 2) {
                break
            }

            visited[currY * width + currX] = true
            points.add(Point2D(currX.toDouble(), currY.toDouble()))
        }

        return points
    }
}
