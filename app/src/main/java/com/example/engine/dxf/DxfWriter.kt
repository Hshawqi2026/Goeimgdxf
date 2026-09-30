package com.example.engine.dxf

import com.example.model.*
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.Locale

object DxfWriter {

    /**
     * Exports vector entities to a valid AutoCAD DXF file (Release 2000 AC1015).
     */
    fun exportToDxf(
        destinationFile: File,
        entities: List<DxfEntity>,
        layers: List<CadLayer>,
        calibration: CalibrationData,
        georef: GeoreferenceData,
        imageHeight: Double = 1000.0
    ) {
        val writer = PrintWriter(FileWriter(destinationFile))
        try {
            writeDxf(writer, entities, layers, calibration, georef, imageHeight)
        } finally {
            writer.flush()
            writer.close()
        }
    }

    fun generateDxfString(
        entities: List<DxfEntity>,
        layers: List<CadLayer>,
        calibration: CalibrationData,
        georef: GeoreferenceData,
        imageHeight: Double = 1000.0
    ): String {
        val sb = StringBuilder()
        writeDxf(sb, entities, layers, calibration, georef, imageHeight)
        return sb.toString()
    }

    private fun writeDxf(
        out: Appendable,
        entities: List<DxfEntity>,
        layers: List<CadLayer>,
        calibration: CalibrationData,
        georef: GeoreferenceData,
        imageHeight: Double
    ) {
        // Map pixel coordinates to CAD coordinates
        fun mapPoint(p: Point2D): Point2D {
            return if (georef.isEnabled && georef.gcps.size >= 2) {
                georef.transform(p)
            } else if (calibration.isCalibrated) {
                val scale = calibration.unitsPerPixel
                // Invert Y so up in image is +Y in CAD
                val cadY = (imageHeight - p.y) * scale
                val cadX = p.x * scale
                Point2D(cadX, cadY)
            } else {
                Point2D(p.x, imageHeight - p.y)
            }
        }

        fun writeTag(code: Int, value: Any) {
            out.append(code.toString()).append("\n")
            out.append(value.toString()).append("\n")
        }

        fun fmt(d: Double): String = String.format(Locale.US, "%.4f", d)

        // Calculate Bounding Box
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE

        for (e in entities) {
            val bb = e.boundingBox()
            val p1 = mapPoint(Point2D(bb.minX, bb.minY))
            val p2 = mapPoint(Point2D(bb.maxX, bb.maxY))
            minX = minOf(minX, p1.x, p2.x)
            minY = minOf(minY, p1.y, p2.y)
            maxX = maxOf(maxX, p1.x, p2.x)
            maxY = maxOf(maxY, p1.y, p2.y)
        }
        if (minX > maxX) {
            minX = 0.0; minY = 0.0; maxX = 100.0; maxY = 100.0
        }

        // ================= HEADER =================
        writeTag(0, "SECTION")
        writeTag(2, "HEADER")

        writeTag(9, "\$ACADVER")
        writeTag(1, "AC1015") // AutoCAD 2000

        writeTag(9, "\$INSUNITS")
        writeTag(70, calibration.unit.dxfCode) // 6=Meters, 4=MM, 2=Feet

        writeTag(9, "\$MEASUREMENT")
        writeTag(70, if (calibration.unit == CadUnit.FT || calibration.unit == CadUnit.INCH) 0 else 1) // 1=Metric

        writeTag(9, "\$EXTMIN")
        writeTag(10, fmt(minX))
        writeTag(20, fmt(minY))
        writeTag(30, "0.0")

        writeTag(9, "\$EXTMAX")
        writeTag(10, fmt(maxX))
        writeTag(20, fmt(maxY))
        writeTag(30, "0.0")

        writeTag(0, "ENDSEC")

        // ================= TABLES =================
        writeTag(0, "SECTION")
        writeTag(2, "TABLES")

        // VPORT Table
        writeTag(0, "TABLE")
        writeTag(2, "VPORT")
        writeTag(70, 0)
        writeTag(0, "ENDTAB")

        // LTYPE Table
        writeTag(0, "TABLE")
        writeTag(2, "LTYPE")
        writeTag(70, 1)
        writeTag(0, "LTYPE")
        writeTag(2, "CONTINUOUS")
        writeTag(70, 64)
        writeTag(3, "Solid line")
        writeTag(72, 65)
        writeTag(73, 0)
        writeTag(40, "0.0")
        writeTag(0, "ENDTAB")

        // LAYER Table
        writeTag(0, "TABLE")
        writeTag(2, "LAYER")
        writeTag(70, layers.size)
        for (layer in layers) {
            writeTag(0, "LAYER")
            writeTag(2, layer.name)
            writeTag(70, if (layer.isLocked) 4 else 0)
            writeTag(62, if (layer.isVisible) layer.colorAci else -layer.colorAci)
            writeTag(6, "CONTINUOUS")
        }
        writeTag(0, "ENDTAB")

        writeTag(0, "ENDSEC")

        // ================= BLOCKS =================
        writeTag(0, "SECTION")
        writeTag(2, "BLOCKS")
        writeTag(0, "ENDSEC")

        // ================= ENTITIES =================
        writeTag(0, "SECTION")
        writeTag(2, "ENTITIES")

        for (entity in entities) {
            when (entity) {
                is LineEntity -> {
                    val p1 = mapPoint(entity.p1)
                    val p2 = mapPoint(entity.p2)
                    writeTag(0, "LINE")
                    writeTag(8, entity.layer)
                    writeTag(10, fmt(p1.x))
                    writeTag(20, fmt(p1.y))
                    writeTag(30, "0.0")
                    writeTag(11, fmt(p2.x))
                    writeTag(21, fmt(p2.y))
                    writeTag(31, "0.0")
                }
                is PolylineEntity -> {
                    if (entity.points.size >= 2) {
                        writeTag(0, "LWPOLYLINE")
                        writeTag(8, entity.layer)
                        writeTag(90, entity.points.size)
                        writeTag(70, if (entity.isClosed) 1 else 0) // 1 = closed, 0 = open
                        for (p in entity.points) {
                            val mp = mapPoint(p)
                            writeTag(10, fmt(mp.x))
                            writeTag(20, fmt(mp.y))
                        }
                    }
                }
                is CircleEntity -> {
                    val center = mapPoint(entity.center)
                    val scale = if (calibration.isCalibrated) calibration.unitsPerPixel else 1.0
                    val r = entity.radius * scale
                    writeTag(0, "CIRCLE")
                    writeTag(8, entity.layer)
                    writeTag(10, fmt(center.x))
                    writeTag(20, fmt(center.y))
                    writeTag(30, "0.0")
                    writeTag(40, fmt(r))
                }
                is ArcEntity -> {
                    val center = mapPoint(entity.center)
                    val scale = if (calibration.isCalibrated) calibration.unitsPerPixel else 1.0
                    val r = entity.radius * scale
                    writeTag(0, "ARC")
                    writeTag(8, entity.layer)
                    writeTag(10, fmt(center.x))
                    writeTag(20, fmt(center.y))
                    writeTag(30, "0.0")
                    writeTag(40, fmt(r))
                    writeTag(50, fmt(entity.startAngleDeg))
                    writeTag(51, fmt(entity.endAngleDeg))
                }
                is PointEntity -> {
                    val pt = mapPoint(entity.point)
                    writeTag(0, "POINT")
                    writeTag(8, entity.layer)
                    writeTag(10, fmt(pt.x))
                    writeTag(20, fmt(pt.y))
                    writeTag(30, "0.0")
                }
            }
        }

        writeTag(0, "ENDSEC")
        writeTag(0, "EOF")
    }
}
