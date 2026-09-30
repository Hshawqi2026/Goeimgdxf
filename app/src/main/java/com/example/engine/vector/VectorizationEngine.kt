package com.example.engine.vector

import android.graphics.Bitmap
import com.example.engine.image.ContourDetector
import com.example.engine.image.ImageProcessor
import com.example.model.*
import java.util.UUID

object VectorizationEngine {

    fun vectorize(
        bitmap: Bitmap,
        settings: VectorizationSettings,
        onProgress: (step: String, percent: Float) -> Unit = { _, _ -> }
    ): List<DxfEntity> {
        // Step 1: Image Preprocessing & Edge Segmentation
        val binary = ImageProcessor.processToBinary(bitmap, settings, onProgress)

        // Step 2: Contour Detection
        onProgress("Tracing geometric contours...", 0.70f)
        val rawContours = ContourDetector.findContours(
            binary = binary,
            minArea = settings.minContourArea,
            maxCount = 1500
        )

        // Step 3: Vector Entity Generation
        onProgress("Building vector CAD geometry...", 0.85f)
        val extractedEntities = mutableListOf<DxfEntity>()

        for (contour in rawContours) {
            val area = contour.calculateArea()
            val perimeter = contour.calculatePerimeter()

            // 1. Check for circular features
            if (settings.detectCircles && contour.isClosed && perimeter > 10) {
                val circle = ShapeDetector.detectCircle(
                    points = contour.points,
                    layer = CadLayer.LAYER_VEGETATION.name,
                    minCircularity = settings.circleCircularityMin
                )
                if (circle != null) {
                    extractedEntities.add(circle)
                    continue
                }
            }

            // 2. Simplify contour with Douglas-Peucker
            var simplified = DouglasPeucker.simplify(contour.points, settings.douglasPeuckerEpsilon)
            if (simplified.size < 2) continue

            // 3. Classify layer based on geometry
            val layerName = if (contour.isClosed) {
                if (area > 3500) {
                    CadLayer.LAYER_BOUNDARIES.name
                } else {
                    CadLayer.LAYER_BUILDINGS.name
                }
            } else {
                CadLayer.LAYER_ROADS.name
            }

            // 4. If building footprint, apply optional orthogonalization
            if (contour.isClosed && settings.detectRectangles && layerName == CadLayer.LAYER_BUILDINGS.name) {
                simplified = ShapeDetector.orthogonalizePolygon(simplified)
            }

            // 5. Construct entity
            if (simplified.size == 2) {
                extractedEntities.add(
                    LineEntity(
                        id = UUID.randomUUID().toString(),
                        layer = layerName,
                        p1 = simplified[0],
                        p2 = simplified[1]
                    )
                )
            } else {
                extractedEntities.add(
                    PolylineEntity(
                        id = UUID.randomUUID().toString(),
                        layer = layerName,
                        points = simplified,
                        isClosed = contour.isClosed
                    )
                )
            }
        }

        // Step 4: Geometry Cleaning & Welding
        onProgress("Cleaning geometry & welding endpoints...", 0.95f)
        val cleaned = GeometryCleaner.clean(extractedEntities, settings)

        onProgress("Vectorization complete!", 1.0f)
        return cleaned
    }
}
