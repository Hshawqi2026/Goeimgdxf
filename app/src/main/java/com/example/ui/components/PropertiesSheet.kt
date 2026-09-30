package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesSheet(
    entity: DxfEntity,
    layers: List<CadLayer>,
    calibration: CalibrationData,
    onDismiss: () -> Unit,
    onChangeLayer: (entityId: String, newLayer: String) -> Unit,
    onDeleteEntity: (entityId: String) -> Unit,
    onDuplicateEntity: (DxfEntity) -> Unit,
    onToggleClosed: (PolylineEntity) -> Unit
) {
    var layerMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val typeName = when (entity) {
                        is LineEntity -> "خط / LINE"
                        is PolylineEntity -> if (entity.isClosed) "مضلع مغلق / POLYGON" else "خط متعدد / POLYLINE"
                        is CircleEntity -> "دائرة / CIRCLE"
                        is ArcEntity -> "قوس / ARC"
                        is PointEntity -> "نقطة / POINT"
                    }
                    Text(
                        text = typeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Layer Selector Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الطبقة / Layer:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                ExposedDropdownMenuBox(
                    expanded = layerMenuExpanded,
                    onExpandedChange = { layerMenuExpanded = !layerMenuExpanded },
                    modifier = Modifier.width(160.dp)
                ) {
                    OutlinedTextField(
                        value = entity.layer,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = layerMenuExpanded) },
                        modifier = Modifier.menuAnchor(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(
                        expanded = layerMenuExpanded,
                        onDismissRequest = { layerMenuExpanded = false }
                    ) {
                        for (l in layers) {
                            DropdownMenuItem(
                                text = { Text(l.name) },
                                onClick = {
                                    onChangeLayer(entity.id, l.name)
                                    layerMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Geometric Metrics
            when (entity) {
                is LineEntity -> {
                    MetricRow("الطول / Length", calibration.formatDistance(entity.calculateLength()))
                    MetricRow("زاوية الميل / Angle", String.format(Locale.US, "%.1f°", entity.angleDegrees()))
                    MetricRow("نقطة البداية (P1)", String.format(Locale.US, "(%.1f, %.1f)", entity.p1.x, entity.p1.y))
                    MetricRow("نقطة النهاية (P2)", String.format(Locale.US, "(%.1f, %.1f)", entity.p2.x, entity.p2.y))
                }
                is PolylineEntity -> {
                    MetricRow("عدد النقاط / Vertices", entity.points.size.toString())
                    MetricRow("الحالة / State", if (entity.isClosed) "مغلق (Closed)" else "مفتوح (Open)")
                    MetricRow("المحيط / Perimeter", calibration.formatDistance(entity.calculateLength()))
                    if (entity.isClosed) {
                        MetricRow("المساحة / Area", calibration.formatArea(entity.calculateArea()))
                    }
                    val center = entity.calculateCenter()
                    MetricRow("المركز / Centroid", String.format(Locale.US, "(%.1f, %.1f)", center.x, center.y))
                }
                is CircleEntity -> {
                    val rCal = entity.radius * calibration.unitsPerPixel
                    MetricRow("نصف القطر / Radius", String.format(Locale.US, "%.2f %s", rCal, calibration.unit.symbol))
                    MetricRow("القطر / Diameter", String.format(Locale.US, "%.2f %s", rCal * 2.0, calibration.unit.symbol))
                    MetricRow("المحيط / Circumference", calibration.formatDistance(entity.calculateLength()))
                    MetricRow("المساحة / Area", calibration.formatArea(entity.calculateArea()))
                    MetricRow("المركز / Center", String.format(Locale.US, "(%.1f, %.1f)", entity.center.x, entity.center.y))
                }
                is ArcEntity -> {
                    MetricRow("نصف القطر / Radius", calibration.formatDistance(entity.radius))
                    MetricRow("زاوية البداية", String.format(Locale.US, "%.1f°", entity.startAngleDeg))
                    MetricRow("زاوية النهاية", String.format(Locale.US, "%.1f°", entity.endAngleDeg))
                    MetricRow("طول القوس", calibration.formatDistance(entity.calculateLength()))
                }
                is PointEntity -> {
                    MetricRow("الإحداثيات / Coords", String.format(Locale.US, "(%.1f, %.1f)", entity.point.x, entity.point.y))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (entity is PolylineEntity) {
                    OutlinedButton(
                        onClick = { onToggleClosed(entity) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (entity.isClosed) "فتح المضلع" else "إغلاق المضلع", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = { onDuplicateEntity(entity) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نسخ", fontSize = 12.sp)
                }

                Button(
                    onClick = { onDeleteEntity(entity.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
