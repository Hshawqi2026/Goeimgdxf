package com.example.model

enum class ThresholdMode(val displayName: String) {
    ADAPTIVE_GAUSSIAN("Adaptive Local (Aerial/Sat)"),
    CANNY_EDGES("Canny Edges (CAD/Blueprint)"),
    OTSU_GLOBAL("Otsu Global (High Contrast)")
}

enum class CleanupPreset(val displayName: String) {
    LOW("Low Cleanup"),
    MEDIUM("Medium (Recommended)"),
    HIGH("High (CAD Minimal)"),
    CUSTOM("Custom Settings")
}

data class VectorizationSettings(
    val thresholdMode: ThresholdMode = ThresholdMode.ADAPTIVE_GAUSSIAN,
    val edgeSensitivity: Int = 35, // 10..100
    val contrastBoost: Float = 1.35f, // 1.0..2.5
    val morphologicalClosingRadius: Int = 2, // 1..5
    val douglasPeuckerEpsilon: Double = 2.5, // 0.5..12.0
    val minContourArea: Double = 60.0, // filter tiny pixel specks
    val minLineLength: Double = 8.0, // filter micro lines
    val detectRectangles: Boolean = true, // Orthogonal building snapping
    val detectCircles: Boolean = true, // Detect roundabouts/tanks
    val circleCircularityMin: Double = 0.82,
    val cleanupPreset: CleanupPreset = CleanupPreset.MEDIUM,
    val weldDistance: Double = 4.0, // Snap close endpoints
    val collinearAngleToleranceDeg: Double = 6.0, // Merge straight line segments
    val closeNearlyClosedPolygons: Boolean = true,
    val closeLoopDistance: Double = 12.0,
    val removeDuplicateLines: Boolean = true
) {
    companion object {
        fun forPreset(preset: CleanupPreset): VectorizationSettings {
            return when (preset) {
                CleanupPreset.LOW -> VectorizationSettings(
                    cleanupPreset = CleanupPreset.LOW,
                    douglasPeuckerEpsilon = 1.2,
                    weldDistance = 2.0,
                    collinearAngleToleranceDeg = 3.0,
                    minLineLength = 5.0,
                    closeLoopDistance = 6.0
                )
                CleanupPreset.MEDIUM -> VectorizationSettings(
                    cleanupPreset = CleanupPreset.MEDIUM,
                    douglasPeuckerEpsilon = 2.5,
                    weldDistance = 4.5,
                    collinearAngleToleranceDeg = 6.0,
                    minLineLength = 9.0,
                    closeLoopDistance = 12.0
                )
                CleanupPreset.HIGH -> VectorizationSettings(
                    cleanupPreset = CleanupPreset.HIGH,
                    douglasPeuckerEpsilon = 4.5,
                    weldDistance = 8.0,
                    collinearAngleToleranceDeg = 10.0,
                    minLineLength = 16.0,
                    closeLoopDistance = 20.0
                )
                CleanupPreset.CUSTOM -> VectorizationSettings(cleanupPreset = CleanupPreset.CUSTOM)
            }
        }
    }
}
