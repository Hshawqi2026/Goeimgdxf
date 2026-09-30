package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GcpPoint
import com.example.model.GeorefMethod
import com.example.model.GeoreferenceData
import com.example.model.Point2D
import java.util.UUID

@Composable
fun GeorefDialog(
    georef: GeoreferenceData,
    onDismiss: () -> Unit,
    onUpdateGeoref: (GeoreferenceData) -> Unit,
    onStartPickGcp: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(georef.isEnabled) }
    var crsName by remember { mutableStateOf(georef.crsName) }
    var selectedMethod by remember { mutableStateOf(georef.method) }
    var gcps by remember { mutableStateOf(georef.gcps) }

    // Dialog for adding/editing a GCP
    var showAddGcpDialog by remember { mutableStateOf(false) }
    var newGcpPx by remember { mutableStateOf("") }
    var newGcpPy by remember { mutableStateOf("") }
    var newGcpWx by remember { mutableStateOf("") }
    var newGcpWy by remember { mutableStateOf("") }
    var newGcpLabel by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الإسناد الجغرافي / Georeferencing") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل الإسناد الجغرافي:", fontWeight = FontWeight.Bold)
                    Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = crsName,
                    onValueChange = { crsName = it },
                    label = { Text("نظام الإحداثيات / CRS (مثال: UTM Zone 36N)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("طريقة التحويل / Transformation:", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = selectedMethod == GeorefMethod.CONFORMAL_2_POINT,
                        onClick = { selectedMethod = GeorefMethod.CONFORMAL_2_POINT },
                        label = { Text("2-Point Helmert", fontSize = 11.sp) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    FilterChip(
                        selected = selectedMethod == GeorefMethod.AFFINE_3_POINT,
                        onClick = { selectedMethod = GeorefMethod.AFFINE_3_POINT },
                        label = { Text("3-Point Affine", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("نقاط التحكم الأرضية (GCPs):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick = onStartPickGcp) {
                            Text("تحديد على الصورة", fontSize = 11.sp)
                        }
                        IconButton(onClick = { showAddGcpDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add GCP")
                        }
                    }
                }

                if (gcps.isEmpty()) {
                    Text(
                        "لم تتم إضافة نقاط تحكم. أضف نقطتين على الأقل لتحويل الإحداثيات إلى UTM أو الإحداثيات الحقيقية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                        itemsIndexed(gcps) { index, gcp ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (gcp.label.isNotBlank()) gcp.label else "GCP ${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "صورة: (${gcp.pixelPoint.x.toInt()}, ${gcp.pixelPoint.y.toInt()}) -> عالمي: (${gcp.worldX}, ${gcp.worldY})",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { gcps = gcps.filter { it.id != gcp.id } },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onUpdateGeoref(
                    georef.copy(
                        isEnabled = isEnabled,
                        crsName = crsName,
                        method = selectedMethod,
                        gcps = gcps
                    )
                )
                onDismiss()
            }) {
                Text("حفظ الإعدادات / Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء / Cancel")
            }
        }
    )

    if (showAddGcpDialog) {
        AlertDialog(
            onDismissRequest = { showAddGcpDialog = false },
            title = { Text("إضافة نقطة تحكم / Add GCP") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newGcpLabel,
                        onValueChange = { newGcpLabel = it },
                        label = { Text("اسم النقطة (مثال: Corner A)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = newGcpPx,
                            onValueChange = { newGcpPx = it },
                            label = { Text("Pixel X") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newGcpPy,
                            onValueChange = { newGcpPy = it },
                            label = { Text("Pixel Y") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = newGcpWx,
                            onValueChange = { newGcpWx = it },
                            label = { Text("World Easting") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newGcpWy,
                            onValueChange = { newGcpWy = it },
                            label = { Text("World Northing") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val px = newGcpPx.toDoubleOrNull() ?: 0.0
                    val py = newGcpPy.toDoubleOrNull() ?: 0.0
                    val wx = newGcpWx.toDoubleOrNull() ?: 0.0
                    val wy = newGcpWy.toDoubleOrNull() ?: 0.0
                    val gcp = GcpPoint(
                        id = UUID.randomUUID().toString(),
                        pixelPoint = Point2D(px, py),
                        worldX = wx,
                        worldY = wy,
                        label = newGcpLabel.ifBlank { "GCP ${gcps.size + 1}" }
                    )
                    gcps = gcps + gcp
                    showAddGcpDialog = false
                    newGcpPx = ""; newGcpPy = ""; newGcpWx = ""; newGcpWy = ""; newGcpLabel = ""
                }) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGcpDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
