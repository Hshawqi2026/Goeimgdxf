package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.dxf.DxfValidationReport
import java.util.Locale

@Composable
fun DxfExportDialog(
    report: DxfValidationReport,
    projectName: String,
    onDismiss: () -> Unit,
    onExportSaveFile: (fileName: String) -> Unit,
    onShareDxf: () -> Unit
) {
    var fileName by remember { mutableStateOf("${projectName.replace(" ", "_")}.dxf") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (report.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (report.isValid) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("تصدير ملف DXF / Export", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Compatibility badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F766E).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "متوافق مع: AutoCAD • Civil 3D • QGIS • LibreCAD • Global Mapper",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF0D9488),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("اسم الملف / File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // DXF Validation Summary Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "تقرير الفحص الهندسي / Validation Report",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ReportItem("إجمالي العناصر الهندسية", "${report.totalEntities} كيان (Entities)")
                        ReportItem("الخطوط المستقيمة (LINE)", "${report.lineCount}")
                        ReportItem("المضلعات والمسارات (LWPOLYLINE)", "${report.polylineCount}")
                        ReportItem("الدوائر والأقواس (CIRCLE/ARC)", "${report.circleCount}")
                        ReportItem("عدد الطبقات المصدّرة", "${report.layerCount} طبقة (Layers)")
                        val bb = report.boundingBox
                        ReportItem(
                            "حدود الرسم (Bounding Box)",
                            String.format(Locale.US, "W: %.1f x H: %.1f", bb.width, bb.height)
                        )
                    }
                }

                if (report.warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("تنبيهات (Warnings):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFD97706))
                    for (w in report.warnings) {
                        Text("• $w", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (report.errors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("أخطاء (Errors):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    for (e in report.errors) {
                        Text("• $e", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        onShareDxf()
                        onDismiss()
                    },
                    enabled = report.isValid
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة / Share")
                }

                Button(
                    onClick = {
                        val validName = if (fileName.endsWith(".dxf", ignoreCase = true)) fileName else "$fileName.dxf"
                        onExportSaveFile(validName)
                        onDismiss()
                    },
                    enabled = report.isValid
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حفظ DXF")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء / Cancel")
            }
        }
    )
}

@Composable
private fun ReportItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
