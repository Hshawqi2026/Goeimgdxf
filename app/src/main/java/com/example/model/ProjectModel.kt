package com.example.model

import org.json.JSONArray
import org.json.JSONObject

data class ProjectData(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val layers: List<CadLayer> = CadLayer.defaultLayers(),
    val entities: List<DxfEntity> = emptyList(),
    val calibration: CalibrationData = CalibrationData(),
    val georef: GeoreferenceData = GeoreferenceData(),
    val settings: VectorizationSettings = VectorizationSettings(),
    val imageWidth: Int = 1000,
    val imageHeight: Int = 1000,
    val imagePath: String? = null
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("format", "GeoImage2DXF_Pro")
        root.put("version", 1)
        root.put("id", id)
        root.put("name", name)
        root.put("createdAt", createdAt)
        root.put("updatedAt", updatedAt)
        root.put("imageWidth", imageWidth)
        root.put("imageHeight", imageHeight)
        root.put("imagePath", imagePath ?: "")

        // Layers
        val layersArray = JSONArray()
        for (l in layers) {
            val lObj = JSONObject()
            lObj.put("id", l.id)
            lObj.put("name", l.name)
            lObj.put("colorHex", l.colorHex)
            lObj.put("colorAci", l.colorAci)
            lObj.put("isVisible", l.isVisible)
            lObj.put("isLocked", l.isLocked)
            layersArray.put(lObj)
        }
        root.put("layers", layersArray)

        // Calibration
        val calObj = JSONObject()
        calObj.put("isCalibrated", calibration.isCalibrated)
        calObj.put("realDistance", calibration.realDistance)
        calObj.put("unit", calibration.unit.symbol)
        calibration.point1?.let {
            calObj.put("p1x", it.x)
            calObj.put("p1y", it.y)
        }
        calibration.point2?.let {
            calObj.put("p2x", it.x)
            calObj.put("p2y", it.y)
        }
        root.put("calibration", calObj)

        // Georef
        val geoObj = JSONObject()
        geoObj.put("isEnabled", georef.isEnabled)
        geoObj.put("crsName", georef.crsName)
        geoObj.put("method", georef.method.name)
        val gcpsArray = JSONArray()
        for (gcp in georef.gcps) {
            val gObj = JSONObject()
            gObj.put("id", gcp.id)
            gObj.put("px", gcp.pixelPoint.x)
            gObj.put("py", gcp.pixelPoint.y)
            gObj.put("wx", gcp.worldX)
            gObj.put("wy", gcp.worldY)
            gObj.put("label", gcp.label)
            gcpsArray.put(gObj)
        }
        geoObj.put("gcps", gcpsArray)
        root.put("georef", geoObj)

        // Entities
        val entArray = JSONArray()
        for (e in entities) {
            val eObj = JSONObject()
            eObj.put("id", e.id)
            eObj.put("layer", e.layer)
            eObj.put("color", e.color)
            when (e) {
                is LineEntity -> {
                    eObj.put("type", "LINE")
                    eObj.put("x1", e.p1.x)
                    eObj.put("y1", e.p1.y)
                    eObj.put("x2", e.p2.x)
                    eObj.put("y2", e.p2.y)
                }
                is PolylineEntity -> {
                    eObj.put("type", "POLYLINE")
                    eObj.put("isClosed", e.isClosed)
                    val pts = JSONArray()
                    for (p in e.points) {
                        val pObj = JSONObject()
                        pObj.put("x", p.x)
                        pObj.put("y", p.y)
                        pts.put(pObj)
                    }
                    eObj.put("points", pts)
                }
                is CircleEntity -> {
                    eObj.put("type", "CIRCLE")
                    eObj.put("cx", e.center.x)
                    eObj.put("cy", e.center.y)
                    eObj.put("r", e.radius)
                }
                is ArcEntity -> {
                    eObj.put("type", "ARC")
                    eObj.put("cx", e.center.x)
                    eObj.put("cy", e.center.y)
                    eObj.put("r", e.radius)
                    eObj.put("startAngle", e.startAngleDeg)
                    eObj.put("endAngle", e.endAngleDeg)
                }
                is PointEntity -> {
                    eObj.put("type", "POINT")
                    eObj.put("x", e.point.x)
                    eObj.put("y", e.point.y)
                }
            }
            entArray.put(eObj)
        }
        root.put("entities", entArray)

        return root.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): ProjectData {
            val root = JSONObject(jsonStr)
            val id = root.optString("id", System.currentTimeMillis().toString())
            val name = root.optString("name", "Untitled")
            val createdAt = root.optLong("createdAt", System.currentTimeMillis())
            val updatedAt = root.optLong("updatedAt", System.currentTimeMillis())
            val imgW = root.optInt("imageWidth", 1000)
            val imgH = root.optInt("imageHeight", 1000)
            val imgPath = root.optString("imagePath", null)

            // Layers
            val layersList = mutableListOf<CadLayer>()
            val lArr = root.optJSONArray("layers")
            if (lArr != null) {
                for (i in 0 until lArr.length()) {
                    val lo = lArr.getJSONObject(i)
                    layersList.add(
                        CadLayer(
                            id = lo.getString("id"),
                            name = lo.getString("name"),
                            colorHex = lo.getString("colorHex"),
                            colorAci = lo.getInt("colorAci"),
                            isVisible = lo.optBoolean("isVisible", true),
                            isLocked = lo.optBoolean("isLocked", false)
                        )
                    )
                }
            } else {
                layersList.addAll(CadLayer.defaultLayers())
            }

            // Calibration
            val calObj = root.optJSONObject("calibration")
            val cal = if (calObj != null) {
                val isCal = calObj.optBoolean("isCalibrated", false)
                val dist = calObj.optDouble("realDistance", 0.0)
                val u = CadUnit.fromSymbol(calObj.optString("unit", "m"))
                val p1 = if (calObj.has("p1x")) Point2D(calObj.getDouble("p1x"), calObj.getDouble("p1y")) else null
                val p2 = if (calObj.has("p2x")) Point2D(calObj.getDouble("p2x"), calObj.getDouble("p2y")) else null
                CalibrationData(p1, p2, dist, u, isCal)
            } else CalibrationData()

            // Georef
            val geoObj = root.optJSONObject("georef")
            val georef = if (geoObj != null) {
                val isEn = geoObj.optBoolean("isEnabled", false)
                val crs = geoObj.optString("crsName", "WGS 84 / UTM Zone 36N")
                val methStr = geoObj.optString("method", GeorefMethod.CONFORMAL_2_POINT.name)
                val meth = try { GeorefMethod.valueOf(methStr) } catch (_: Exception) { GeorefMethod.CONFORMAL_2_POINT }
                val gcps = mutableListOf<GcpPoint>()
                val gArr = geoObj.optJSONArray("gcps")
                if (gArr != null) {
                    for (i in 0 until gArr.length()) {
                        val go = gArr.getJSONObject(i)
                        gcps.add(
                            GcpPoint(
                                id = go.getString("id"),
                                pixelPoint = Point2D(go.getDouble("px"), go.getDouble("py")),
                                worldX = go.getDouble("wx"),
                                worldY = go.getDouble("wy"),
                                label = go.optString("label", "")
                            )
                        )
                    }
                }
                GeoreferenceData(crs, gcps, isEn, meth)
            } else GeoreferenceData()

            // Entities
            val entList = mutableListOf<DxfEntity>()
            val entArr = root.optJSONArray("entities")
            if (entArr != null) {
                for (i in 0 until entArr.length()) {
                    val eo = entArr.getJSONObject(i)
                    val eid = eo.getString("id")
                    val elayer = eo.getString("layer")
                    val ecolor = eo.optLong("color", 0xFFFFFFFFL)
                    when (eo.getString("type")) {
                        "LINE" -> {
                            entList.add(
                                LineEntity(
                                    id = eid,
                                    layer = elayer,
                                    p1 = Point2D(eo.getDouble("x1"), eo.getDouble("y1")),
                                    p2 = Point2D(eo.getDouble("x2"), eo.getDouble("y2")),
                                    color = ecolor
                                )
                            )
                        }
                        "POLYLINE" -> {
                            val closed = eo.optBoolean("isClosed", false)
                            val pts = mutableListOf<Point2D>()
                            val ptsArr = eo.getJSONArray("points")
                            for (j in 0 until ptsArr.length()) {
                                val po = ptsArr.getJSONObject(j)
                                pts.add(Point2D(po.getDouble("x"), po.getDouble("y")))
                            }
                            entList.add(
                                PolylineEntity(
                                    id = eid,
                                    layer = elayer,
                                    points = pts,
                                    isClosed = closed,
                                    color = ecolor
                                )
                            )
                        }
                        "CIRCLE" -> {
                            entList.add(
                                CircleEntity(
                                    id = eid,
                                    layer = elayer,
                                    center = Point2D(eo.getDouble("cx"), eo.getDouble("cy")),
                                    radius = eo.getDouble("r"),
                                    color = ecolor
                                )
                            )
                        }
                        "ARC" -> {
                            entList.add(
                                ArcEntity(
                                    id = eid,
                                    layer = elayer,
                                    center = Point2D(eo.getDouble("cx"), eo.getDouble("cy")),
                                    radius = eo.getDouble("r"),
                                    startAngleDeg = eo.getDouble("startAngle"),
                                    endAngleDeg = eo.getDouble("endAngle"),
                                    color = ecolor
                                )
                            )
                        }
                        "POINT" -> {
                            entList.add(
                                PointEntity(
                                    id = eid,
                                    layer = elayer,
                                    point = Point2D(eo.getDouble("x"), eo.getDouble("y")),
                                    color = ecolor
                                )
                            )
                        }
                    }
                }
            }

            return ProjectData(
                id = id,
                name = name,
                createdAt = createdAt,
                updatedAt = updatedAt,
                layers = layersList,
                entities = entList,
                calibration = cal,
                georef = georef,
                imageWidth = imgW,
                imageHeight = imgH,
                imagePath = if (imgPath.isNullOrEmpty()) null else imgPath
            )
        }
    }
}
