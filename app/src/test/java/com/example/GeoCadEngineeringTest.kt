package com.example

import com.example.engine.cad.SnapEngine
import com.example.engine.dxf.DxfValidator
import com.example.engine.dxf.DxfWriter
import com.example.engine.vector.DouglasPeucker
import com.example.engine.vector.GeometryCleaner
import com.example.engine.vector.ShapeDetector
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.PI

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeoCadEngineeringTest {

    @Test
    fun testPoint2DMath() {
        val p1 = Point2D(0.0, 0.0)
        val p2 = Point2D(3.0, 4.0)

        assertEquals(5.0, p1.distanceTo(p2), 1e-6)
        assertEquals(25.0, p2.dot(p2), 1e-6)

        val translated = p2.translate(2.0, -1.0)
        assertEquals(5.0, translated.x, 1e-6)
        assertEquals(3.0, translated.y, 1e-6)

        val rotated = Point2D(10.0, 0.0).rotate(PI / 2.0)
        assertEquals(0.0, rotated.x, 1e-6)
        assertEquals(10.0, rotated.y, 1e-6)
    }

    @Test
    fun testPolygonAreaAndLength() {
        // Rectangle 20m x 10m
        val pts = listOf(
            Point2D(0.0, 0.0),
            Point2D(20.0, 0.0),
            Point2D(20.0, 10.0),
            Point2D(0.0, 10.0)
        )
        val poly = PolylineEntity(
            id = "test_poly",
            layer = "BUILDINGS",
            points = pts,
            isClosed = true
        )

        assertEquals(200.0, poly.calculateArea(), 1e-4)
        assertEquals(60.0, poly.calculateLength(), 1e-4)
    }

    @Test
    fun testDouglasPeucker() {
        // Line with intermediate points along straight path
        val pts = listOf(
            Point2D(0.0, 0.0),
            Point2D(10.0, 0.1),
            Point2D(20.0, -0.1),
            Point2D(30.0, 0.05),
            Point2D(40.0, 0.0)
        )
        val simplified = DouglasPeucker.simplify(pts, epsilon = 1.0)
        // With epsilon 1.0, intermediate micro-deviations (<0.1) should be flattened
        assertEquals(2, simplified.size)
        assertEquals(0.0, simplified.first().x, 1e-4)
        assertEquals(40.0, simplified.last().x, 1e-4)
    }

    @Test
    fun testCircleDetection() {
        // Generate points along circle radius 50 at center (100, 100)
        val circlePts = mutableListOf<Point2D>()
        val numSteps = 24
        for (i in 0 until numSteps) {
            val theta = 2 * PI * i / numSteps
            circlePts.add(Point2D(100.0 + 50.0 * kotlin.math.cos(theta), 100.0 + 50.0 * kotlin.math.sin(theta)))
        }

        val circle = ShapeDetector.detectCircle(circlePts, layer = "VEGETATION")
        assertNotNull(circle)
        assertEquals(100.0, circle!!.center.x, 1.0)
        assertEquals(100.0, circle.center.y, 1.0)
        assertEquals(50.0, circle.radius, 1.0)
    }

    @Test
    fun testGeometryCleanerDeduplication() {
        val l1 = LineEntity("l1", "ROADS", Point2D(0.0, 0.0), Point2D(10.0, 10.0))
        val l2 = LineEntity("l2", "ROADS", Point2D(10.0, 10.0), Point2D(0.0, 0.0)) // duplicate reverse
        val l3 = LineEntity("l3", "ROADS", Point2D(0.0, 0.0), Point2D(1.0, 1.0))   // micro line (< minLength)

        val settings = VectorizationSettings(minLineLength = 5.0, removeDuplicateLines = true)
        val cleaned = GeometryCleaner.clean(listOf(l1, l2, l3), settings)

        assertEquals(1, cleaned.size)
        assertEquals("l1", cleaned[0].id)
    }

    @Test
    fun testCalibrationAndScale() {
        val cal = CalibrationData(
            point1 = Point2D(0.0, 0.0),
            point2 = Point2D(100.0, 0.0),
            realDistance = 50.0, // 100 pixels = 50 meters
            unit = CadUnit.M,
            isCalibrated = true
        )

        assertEquals(0.5, cal.unitsPerPixel, 1e-6)
        assertEquals("25.00 m", cal.formatDistance(50.0))
        assertEquals("25.00 m²", cal.formatArea(100.0)) // 100 px² * 0.5 * 0.5 = 25 m²
    }

    @Test
    fun testGeoreferenceHelmert() {
        val gcps = listOf(
            GcpPoint("1", Point2D(0.0, 0.0), 300000.0, 4000000.0),
            GcpPoint("2", Point2D(100.0, 0.0), 300100.0, 4000000.0)
        )
        val georef = GeoreferenceData(
            gcps = gcps,
            isEnabled = true
        )

        val worldPt = georef.transform(Point2D(50.0, 0.0))
        assertEquals(300050.0, worldPt.x, 1e-3)
        assertEquals(4000000.0, worldPt.y, 1e-3)
    }

    @Test
    fun testDxfGenerationAndValidation() {
        val line = LineEntity("1", "ROADS", Point2D(10.0, 10.0), Point2D(100.0, 100.0))
        val poly = PolylineEntity(
            "2",
            "BUILDINGS",
            listOf(Point2D(20.0, 20.0), Point2D(50.0, 20.0), Point2D(50.0, 50.0), Point2D(20.0, 50.0)),
            isClosed = true
        )
        val circle = CircleEntity("3", "VEGETATION", Point2D(80.0, 80.0), 15.0)

        val entities = listOf(line, poly, circle)
        val layers = CadLayer.defaultLayers()
        val cal = CalibrationData(realDistance = 10.0, isCalibrated = true)
        val georef = GeoreferenceData()

        // 1. Validate
        val report = DxfValidator.validate(entities, layers, cal, georef)
        assertTrue(report.isValid)
        assertEquals(3, report.totalEntities)
        assertEquals(1, report.lineCount)
        assertEquals(1, report.polylineCount)
        assertEquals(1, report.circleCount)
        assertTrue(report.errors.isEmpty())

        // 2. Generate DXF String
        val dxf = DxfWriter.generateDxfString(entities, layers, cal, georef, 1000.0)

        // Check key DXF elements
        assertTrue(dxf.contains("SECTION"))
        assertTrue(dxf.contains("HEADER"))
        assertTrue(dxf.contains("AC1015"))
        assertTrue(dxf.contains("TABLES"))
        assertTrue(dxf.contains("LAYER"))
        assertTrue(dxf.contains("BUILDINGS"))
        assertTrue(dxf.contains("ROADS"))
        assertTrue(dxf.contains("ENTITIES"))
        assertTrue(dxf.contains("LINE"))
        assertTrue(dxf.contains("LWPOLYLINE"))
        assertTrue(dxf.contains("CIRCLE"))
        assertTrue(dxf.contains("EOF"))
    }

    @Test
    fun testProjectSerialization() {
        val entities = listOf(
            LineEntity("line_1", "ROADS", Point2D(10.5, 20.5), Point2D(30.5, 40.5)),
            CircleEntity("circ_1", "VEGETATION", Point2D(50.0, 60.0), 12.0)
        )
        val project = ProjectData(
            id = "proj_test_123",
            name = "Test Cadastral Project",
            entities = entities
        )

        val json = project.toJson()
        val restored = ProjectData.fromJson(json)

        assertEquals(project.id, restored.id)
        assertEquals(project.name, restored.name)
        assertEquals(2, restored.entities.size)
        assertTrue(restored.entities[0] is LineEntity)
        assertTrue(restored.entities[1] is CircleEntity)
    }
}
