package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.engine.cad.SnapEngine
import com.example.engine.cad.SnapResult
import com.example.engine.cad.SnapType
import com.example.model.*
import kotlin.math.PI
import kotlin.math.roundToInt

enum class ViewMode {
    ORIGINAL,
    VECTOR,
    OVERLAY,
    SPLIT
}

enum class CadTool {
    PAN_ZOOM,
    SELECT,
    DRAW_LINE,
    DRAW_POLYLINE,
    DRAW_RECT,
    DRAW_CIRCLE,
    CALIBRATE_PICK,
    GEOREF_PICK
}

@Composable
fun CadCanvas(
    modifier: Modifier = Modifier,
    rasterBitmap: Bitmap?,
    entities: List<DxfEntity>,
    layers: List<CadLayer>,
    viewMode: ViewMode,
    overlayOpacity: Float,
    activeTool: CadTool,
    selectedEntityId: String?,
    scale: Float,
    panOffset: Offset,
    onScaleChange: (Float) -> Unit,
    onPanOffsetChange: (Offset) -> Unit,
    onEntitySelected: (DxfEntity?) -> Unit,
    onPointPicked: (Point2D) -> Unit = {},
    onGeometryAdded: (DxfEntity) -> Unit = {},
    onEntityMoved: (entityId: String, dx: Double, dy: Double) -> Unit = { _, _, _ -> },
    onCursorMoved: (Point2D) -> Unit = {}
) {
    val layerMap = remember(layers) { layers.associateBy { it.name } }
    var snapCandidate by remember { mutableStateOf<SnapResult?>(null) }
    var inProgressPoints by remember { mutableStateOf<List<Point2D>>(emptyList()) }
    var dragStartPoint by remember { mutableStateOf<Point2D?>(null) }
    var currentTouchPoint by remember { mutableStateOf<Point2D?>(null) }

    // Screen to Model coordinate conversion
    fun screenToModel(screenOffset: Offset): Point2D {
        val mx = (screenOffset.x - panOffset.x) / scale
        val my = (screenOffset.y - panOffset.y) / scale
        return Point2D(mx.toDouble(), my.toDouble())
    }

    // Model to Screen coordinate conversion
    fun modelToScreen(modelPoint: Point2D): Offset {
        val sx = (modelPoint.x * scale + panOffset.x).toFloat()
        val sy = (modelPoint.y * scale + panOffset.y).toFloat()
        return Offset(sx, sy)
    }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        if (activeTool == CadTool.PAN_ZOOM) {
            val newScale = (scale * zoomChange).coerceIn(0.1f, 30.0f)
            onScaleChange(newScale)
            onPanOffsetChange(panOffset + offsetChange)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformState)
            .pointerInput(activeTool, scale, panOffset, entities) {
                detectTapGestures { offset ->
                    val modelPt = screenToModel(offset)
                    onCursorMoved(modelPt)

                    when (activeTool) {
                        CadTool.SELECT -> {
                            val hit = SnapEngine.hitTest(modelPt, entities, hitRadiusPx = (16.0 / scale))
                            onEntitySelected(hit)
                        }
                        CadTool.CALIBRATE_PICK, CadTool.GEOREF_PICK -> {
                            val snap = SnapEngine.findSnap(modelPt, entities, snapDistancePx = (20.0 / scale))
                            val picked = snap?.point ?: modelPt
                            onPointPicked(picked)
                        }
                        CadTool.DRAW_LINE -> {
                            val snap = SnapEngine.findSnap(modelPt, entities, snapDistancePx = (18.0 / scale))
                            val pt = snap?.point ?: modelPt
                            if (inProgressPoints.isEmpty()) {
                                inProgressPoints = listOf(pt)
                            } else {
                                val line = LineEntity(
                                    id = java.util.UUID.randomUUID().toString(),
                                    layer = CadLayer.LAYER_BUILDINGS.name,
                                    p1 = inProgressPoints[0],
                                    p2 = pt
                                )
                                onGeometryAdded(line)
                                inProgressPoints = emptyList()
                            }
                        }
                        CadTool.DRAW_POLYLINE -> {
                            val snap = SnapEngine.findSnap(modelPt, entities, snapDistancePx = (18.0 / scale))
                            val pt = snap?.point ?: modelPt
                            // If tapped close to start point, close polyline
                            if (inProgressPoints.size >= 2 && pt.distanceTo(inProgressPoints.first()) <= (18.0 / scale)) {
                                val poly = PolylineEntity(
                                    id = java.util.UUID.randomUUID().toString(),
                                    layer = CadLayer.LAYER_BUILDINGS.name,
                                    points = inProgressPoints,
                                    isClosed = true
                                )
                                onGeometryAdded(poly)
                                inProgressPoints = emptyList()
                            } else {
                                inProgressPoints = inProgressPoints + pt
                            }
                        }
                        CadTool.DRAW_RECT -> {
                            val snap = SnapEngine.findSnap(modelPt, entities, snapDistancePx = (18.0 / scale))
                            val pt = snap?.point ?: modelPt
                            if (inProgressPoints.isEmpty()) {
                                inProgressPoints = listOf(pt)
                            } else {
                                val p1 = inProgressPoints[0]
                                val p2 = Point2D(pt.x, p1.y)
                                val p3 = pt
                                val p4 = Point2D(p1.x, pt.y)
                                val poly = PolylineEntity(
                                    id = java.util.UUID.randomUUID().toString(),
                                    layer = CadLayer.LAYER_BUILDINGS.name,
                                    points = listOf(p1, p2, p3, p4),
                                    isClosed = true
                                )
                                onGeometryAdded(poly)
                                inProgressPoints = emptyList()
                            }
                        }
                        CadTool.DRAW_CIRCLE -> {
                            val snap = SnapEngine.findSnap(modelPt, entities, snapDistancePx = (18.0 / scale))
                            val pt = snap?.point ?: modelPt
                            if (inProgressPoints.isEmpty()) {
                                inProgressPoints = listOf(pt)
                            } else {
                                val center = inProgressPoints[0]
                                val radius = center.distanceTo(pt)
                                if (radius > 1.0) {
                                    val circle = CircleEntity(
                                        id = java.util.UUID.randomUUID().toString(),
                                        layer = CadLayer.LAYER_VEGETATION.name,
                                        center = center,
                                        radius = radius
                                    )
                                    onGeometryAdded(circle)
                                }
                                inProgressPoints = emptyList()
                            }
                        }
                        CadTool.PAN_ZOOM -> {
                            // Tap to deselect
                            onEntitySelected(null)
                        }
                    }
                }
            }
            .pointerInput(activeTool, scale, panOffset, selectedEntityId) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val pt = screenToModel(offset)
                        dragStartPoint = pt
                        currentTouchPoint = pt
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (activeTool == CadTool.PAN_ZOOM) {
                            onPanOffsetChange(panOffset + dragAmount)
                        } else if (activeTool == CadTool.SELECT && selectedEntityId != null) {
                            val dx = dragAmount.x / scale
                            val dy = dragAmount.y / scale
                            onEntityMoved(selectedEntityId, dx.toDouble(), dy.toDouble())
                        }
                        val pt = screenToModel(change.position)
                        currentTouchPoint = pt
                        onCursorMoved(pt)
                        snapCandidate = SnapEngine.findSnap(pt, entities, snapDistancePx = (18.0 / scale))
                    },
                    onDragEnd = {
                        dragStartPoint = null
                        currentTouchPoint = null
                        snapCandidate = null
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 1. Draw CAD background (Navy slate grid in Vector mode, transparent/dark otherwise)
            if (viewMode == ViewMode.VECTOR) {
                drawRect(Color(0xFF0F172A))
                drawCadGrid(panOffset, scale, canvasWidth, canvasHeight)
            }

            // 2. Draw Raster Image (Original or Overlay)
            if (rasterBitmap != null && (viewMode == ViewMode.ORIGINAL || viewMode == ViewMode.OVERLAY)) {
                val alpha = if (viewMode == ViewMode.OVERLAY) (1.0f - overlayOpacity).coerceIn(0.1f, 1.0f) else 1.0f
                val imgW = (rasterBitmap.width * scale).roundToInt()
                val imgH = (rasterBitmap.height * scale).roundToInt()

                drawImage(
                    image = rasterBitmap.asImageBitmap(),
                    dstOffset = IntOffset(panOffset.x.roundToInt(), panOffset.y.roundToInt()),
                    dstSize = IntSize(imgW, imgH),
                    alpha = alpha
                )
            }

            // 3. Draw Vector Entities (Vector and Overlay modes)
            if (viewMode == ViewMode.VECTOR || viewMode == ViewMode.OVERLAY) {
                for (entity in entities) {
                    val layer = layerMap[entity.layer]
                    if (layer != null && !layer.isVisible) continue

                    val isSelected = entity.id == selectedEntityId
                    val entityColor = if (isSelected) {
                        Color(0xFF38BDF8) // Bright Cyan for selected
                    } else if (layer != null) {
                        Color(android.graphics.Color.parseColor(layer.colorHex))
                    } else {
                        Color.White
                    }

                    val strokeWidth = if (isSelected) 3.5f * scale.coerceAtMost(2f) else 1.8f * scale.coerceAtMost(2f)

                    when (entity) {
                        is LineEntity -> {
                            val p1Screen = modelToScreen(entity.p1)
                            val p2Screen = modelToScreen(entity.p2)
                            drawLine(
                                color = entityColor,
                                start = p1Screen,
                                end = p2Screen,
                                strokeWidth = strokeWidth.coerceAtLeast(1.5f),
                                cap = StrokeCap.Round
                            )
                            if (isSelected) {
                                drawHandle(p1Screen)
                                drawHandle(p2Screen)
                            }
                        }
                        is PolylineEntity -> {
                            if (entity.points.size >= 2) {
                                val path = Path()
                                val first = modelToScreen(entity.points.first())
                                path.moveTo(first.x, first.y)
                                for (i in 1 until entity.points.size) {
                                    val sp = modelToScreen(entity.points[i])
                                    path.lineTo(sp.x, sp.y)
                                }
                                if (entity.isClosed) {
                                    path.close()
                                }
                                drawPath(
                                    path = path,
                                    color = entityColor,
                                    style = Stroke(
                                        width = strokeWidth.coerceAtLeast(1.5f),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                                if (isSelected) {
                                    for (p in entity.points) {
                                        drawHandle(modelToScreen(p))
                                    }
                                }
                            }
                        }
                        is CircleEntity -> {
                            val centerScreen = modelToScreen(entity.center)
                            val radiusScreen = (entity.radius * scale).toFloat()
                            drawCircle(
                                color = entityColor,
                                center = centerScreen,
                                radius = radiusScreen.coerceAtLeast(2f),
                                style = Stroke(width = strokeWidth.coerceAtLeast(1.5f))
                            )
                            if (isSelected) {
                                drawHandle(centerScreen)
                                drawHandle(Offset(centerScreen.x + radiusScreen, centerScreen.y))
                            }
                        }
                        is ArcEntity -> {
                            val centerScreen = modelToScreen(entity.center)
                            val radiusScreen = (entity.radius * scale).toFloat()
                            var sweep = (entity.endAngleDeg - entity.startAngleDeg).toFloat()
                            if (sweep < 0) sweep += 360f
                            drawArc(
                                color = entityColor,
                                startAngle = entity.startAngleDeg.toFloat(),
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(centerScreen.x - radiusScreen, centerScreen.y - radiusScreen),
                                size = Size(radiusScreen * 2, radiusScreen * 2),
                                style = Stroke(width = strokeWidth.coerceAtLeast(1.5f))
                            )
                        }
                        is PointEntity -> {
                            val ptScreen = modelToScreen(entity.point)
                            drawCircle(
                                color = entityColor,
                                center = ptScreen,
                                radius = 4f
                            )
                        }
                    }
                }
            }

            // 4. Draw In-Progress Drawing geometry
            if (inProgressPoints.isNotEmpty() && currentTouchPoint != null) {
                val previewColor = Color(0xFFFBBF24) // Yellow preview
                when (activeTool) {
                    CadTool.DRAW_LINE -> {
                        val p1Screen = modelToScreen(inProgressPoints[0])
                        val p2Screen = modelToScreen(currentTouchPoint!!)
                        drawLine(
                            color = previewColor,
                            start = p1Screen,
                            end = p2Screen,
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    }
                    CadTool.DRAW_POLYLINE -> {
                        val path = Path()
                        val p1 = modelToScreen(inProgressPoints[0])
                        path.moveTo(p1.x, p1.y)
                        for (i in 1 until inProgressPoints.size) {
                            val sp = modelToScreen(inProgressPoints[i])
                            path.lineTo(sp.x, sp.y)
                        }
                        val curr = modelToScreen(currentTouchPoint!!)
                        path.lineTo(curr.x, curr.y)
                        drawPath(
                            path = path,
                            color = previewColor,
                            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                        )
                    }
                    CadTool.DRAW_RECT -> {
                        val p1 = inProgressPoints[0]
                        val curr = currentTouchPoint!!
                        val sp1 = modelToScreen(p1)
                        val sp2 = modelToScreen(Point2D(curr.x, p1.y))
                        val sp3 = modelToScreen(curr)
                        val sp4 = modelToScreen(Point2D(p1.x, curr.y))
                        val path = Path().apply {
                            moveTo(sp1.x, sp1.y)
                            lineTo(sp2.x, sp2.y)
                            lineTo(sp3.x, sp3.y)
                            lineTo(sp4.x, sp4.y)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = previewColor,
                            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                        )
                    }
                    CadTool.DRAW_CIRCLE -> {
                        val center = modelToScreen(inProgressPoints[0])
                        val curr = modelToScreen(currentTouchPoint!!)
                        val r = (inProgressPoints[0].distanceTo(currentTouchPoint!!) * scale).toFloat()
                        drawCircle(
                            color = previewColor,
                            center = center,
                            radius = r.coerceAtLeast(2f),
                            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                        )
                    }
                    else -> {}
                }
            }

            // 5. Draw Snap Indicator
            snapCandidate?.let { snap ->
                val snapScreen = modelToScreen(snap.point)
                val snapColor = Color(0xFF10B981) // Emerald Snap
                when (snap.type) {
                    SnapType.ENDPOINT -> {
                        // Square marker
                        drawRect(
                            color = snapColor,
                            topLeft = Offset(snapScreen.x - 7f, snapScreen.y - 7f),
                            size = Size(14f, 14f),
                            style = Stroke(width = 2.5f)
                        )
                    }
                    SnapType.MIDPOINT -> {
                        // Triangle marker
                        val triPath = Path().apply {
                            moveTo(snapScreen.x, snapScreen.y - 8f)
                            lineTo(snapScreen.x - 8f, snapScreen.y + 6f)
                            lineTo(snapScreen.x + 8f, snapScreen.y + 6f)
                            close()
                        }
                        drawPath(triPath, snapColor, style = Stroke(width = 2.5f))
                    }
                    SnapType.CENTER -> {
                        // Circle marker
                        drawCircle(snapColor, 8f, snapScreen, style = Stroke(width = 2.5f))
                    }
                    else -> {
                        // Crosshair marker
                        drawLine(snapColor, Offset(snapScreen.x - 8f, snapScreen.y), Offset(snapScreen.x + 8f, snapScreen.y), 2f)
                        drawLine(snapColor, Offset(snapScreen.x, snapScreen.y - 8f), Offset(snapScreen.x, snapScreen.y + 8f), 2f)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawHandle(center: Offset) {
    drawCircle(
        color = Color(0xFF0284C7),
        radius = 5.5f,
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = 5.5f,
        center = center,
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawCadGrid(pan: Offset, scale: Float, width: Float, height: Float) {
    var step = 50f * scale
    while (step < 25f) step *= 2f
    while (step > 100f) step /= 2f

    val startX = (pan.x % step)
    val startY = (pan.y % step)

    val gridColor = Color(0x1A64748B)

    var x = startX
    while (x < width) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, height), 1f)
        x += step
    }

    var y = startY
    while (y < height) {
        drawLine(gridColor, Offset(0f, y), Offset(width, y), 1f)
        y += step
    }
}
