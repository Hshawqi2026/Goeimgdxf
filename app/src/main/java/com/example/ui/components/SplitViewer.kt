package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import kotlin.math.roundToInt

@Composable
fun SplitViewer(
    modifier: Modifier = Modifier,
    rasterBitmap: Bitmap?,
    entities: List<DxfEntity>,
    layers: List<CadLayer>,
    scale: Float,
    panOffset: Offset,
    onScaleChange: (Float) -> Unit,
    onPanOffsetChange: (Offset) -> Unit
) {
    var splitFraction by remember { mutableFloatStateOf(0.5f) }
    val layerMap = remember(layers) { layers.associateBy { it.name } }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(0.1f, 30.0f)
        onScaleChange(newScale)
        onPanOffsetChange(panOffset + offsetChange)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformState)
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()
        val splitX = totalWidth * splitFraction

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Right Side: Clean CAD Vector (White CAD technical background)
            drawRect(Color(0xFF0F172A))

            // Draw Raster Image clipped to left half (0 .. splitX)
            if (rasterBitmap != null) {
                clipRect(left = 0f, top = 0f, right = splitX, bottom = totalHeight) {
                    val imgW = (rasterBitmap.width * scale).roundToInt()
                    val imgH = (rasterBitmap.height * scale).roundToInt()
                    drawImage(
                        image = rasterBitmap.asImageBitmap(),
                        dstOffset = IntOffset(panOffset.x.roundToInt(), panOffset.y.roundToInt()),
                        dstSize = IntSize(imgW, imgH)
                    )
                }
            }

            // Draw Vector CAD Lines clipped to right half (splitX .. totalWidth)
            clipRect(left = splitX, top = 0f, right = totalWidth, bottom = totalHeight) {
                // Background grid on CAD side
                var step = 40f * scale
                while (step < 20f) step *= 2f
                while (step > 80f) step /= 2f
                val startX = (panOffset.x % step)
                val startY = (panOffset.y % step)
                var gx = startX
                while (gx < totalWidth) {
                    if (gx >= splitX) {
                        drawLine(Color(0x1F64748B), Offset(gx, 0f), Offset(gx, totalHeight), 1f)
                    }
                    gx += step
                }
                var gy = startY
                while (gy < totalHeight) {
                    drawLine(Color(0x1F64748B), Offset(splitX, gy), Offset(totalWidth, gy), 1f)
                    gy += step
                }

                // Render vector entities
                for (entity in entities) {
                    val layer = layerMap[entity.layer]
                    if (layer != null && !layer.isVisible) continue

                    val entityColor = if (layer != null) {
                        Color(android.graphics.Color.parseColor(layer.colorHex))
                    } else Color.White

                    val strokeWidth = 2.0f * scale.coerceAtMost(2f)

                    fun modelToScreen(p: Point2D) = Offset(
                        (p.x * scale + panOffset.x).toFloat(),
                        (p.y * scale + panOffset.y).toFloat()
                    )

                    when (entity) {
                        is LineEntity -> {
                            drawLine(
                                color = entityColor,
                                start = modelToScreen(entity.p1),
                                end = modelToScreen(entity.p2),
                                strokeWidth = strokeWidth.coerceAtLeast(1.5f),
                                cap = StrokeCap.Round
                            )
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
                                if (entity.isClosed) path.close()
                                drawPath(
                                    path = path,
                                    color = entityColor,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = strokeWidth.coerceAtLeast(1.5f),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                        is CircleEntity -> {
                            drawCircle(
                                color = entityColor,
                                center = modelToScreen(entity.center),
                                radius = (entity.radius * scale).toFloat().coerceAtLeast(2f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth.coerceAtLeast(1.5f))
                            )
                        }
                        is ArcEntity -> {
                            val c = modelToScreen(entity.center)
                            val r = (entity.radius * scale).toFloat()
                            var sweep = (entity.endAngleDeg - entity.startAngleDeg).toFloat()
                            if (sweep < 0) sweep += 360f
                            drawArc(
                                color = entityColor,
                                startAngle = entity.startAngleDeg.toFloat(),
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(c.x - r, c.y - r),
                                size = Size(r * 2, r * 2),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth.coerceAtLeast(1.5f))
                            )
                        }
                        is PointEntity -> {
                            drawCircle(entityColor, 3f, modelToScreen(entity.point))
                        }
                    }
                }
            }

            // Divider Line
            drawLine(
                color = Color(0xFF38BDF8),
                start = Offset(splitX, 0f),
                end = Offset(splitX, totalHeight),
                strokeWidth = 3f
            )
        }

        // Draggable Handle
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (splitX - 22.dp.toPx()).roundToInt(),
                        (totalHeight / 2f - 22.dp.toPx()).roundToInt()
                    )
                }
                .size(44.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF0284C7))
                .pointerInput(totalWidth) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = (splitX + dragAmount.x).coerceIn(40f, totalWidth - 40f)
                        splitFraction = newX / totalWidth
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CompareArrows,
                contentDescription = "Slide to compare",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Label badges: [1] Satellite Photo (Left) vs [2] CAD Vector Map (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = CircleShape,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = "1 • الصورة الأصلية (Raster)",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Surface(
                color = Color(0xFF0369A1).copy(alpha = 0.85f),
                shape = CircleShape,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = "2 • المخطط الهندسي (Vector CAD)",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
