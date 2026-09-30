package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CleanupPreset
import com.example.model.ThresholdMode
import com.example.model.VectorizationSettings
import java.util.Locale

@Composable
fun VectorSettingsDialog(
    settings: VectorizationSettings,
    onDismiss: () -> Unit,
    onApplySettings: (VectorizationSettings) -> Unit
) {
    var thresholdMode by remember { mutableStateOf(settings.thresholdMode) }
    var edgeSensitivity by remember { mutableFloatStateOf(settings.edgeSensitivity.toFloat()) }
    var douglasPeuckerEpsilon by remember { mutableFloatStateOf(settings.douglasPeuckerEpsilon.toFloat()) }
    var minContourArea by remember { mutableFloatStateOf(settings.minContourArea.toFloat()) }
    var detectRectangles by remember { mutableStateOf(settings.detectRectangles) }
    var detectCircles by remember { mutableStateOf(settings.detectCircles) }
    var cleanupPreset by remember { mutableStateOf(settings.cleanupPreset) }
    var weldDistance by remember { mutableFloatStateOf(settings.weldDistance.toFloat()) }
    var collinearAngle by remember { mutableFloatStateOf(settings.collinearAngleToleranceDeg.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إعدادات المعالجة والـ Vectorize") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "اضبط خوارزميات الاستخراج لتناسب نوع الصورة (أقمار صناعية، صور درون، أو مخططات كاد).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Threshold mode
                Text("خوارزمية كشف الحدود / Edge Mode:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                for (mode in ThresholdMode.entries) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = thresholdMode == mode,
                            onClick = { thresholdMode = mode }
                        )
                        Text(mode.displayName, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sensitivity slider
                Text(
                    text = String.format(Locale.US, "حساسية استخراج الحواف: %d", edgeSensitivity.toInt()),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = edgeSensitivity,
                    onValueChange = { edgeSensitivity = it },
                    valueRange = 10f..90f
                )

                // Douglas-Peucker Epsilon
                Text(
                    text = String.format(Locale.US, "تبسيط المنحنيات (Douglas-Peucker): %.1f px", douglasPeuckerEpsilon),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = douglasPeuckerEpsilon,
                    onValueChange = { douglasPeuckerEpsilon = it },
                    valueRange = 0.5f..8.0f
                )

                // Cleanup Preset
                Text("مستوى تنظيف الـ Geometry / Cleanup Preset:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (preset in CleanupPreset.entries) {
                        FilterChip(
                            selected = cleanupPreset == preset,
                            onClick = {
                                cleanupPreset = preset
                                val pSettings = VectorizationSettings.forPreset(preset)
                                douglasPeuckerEpsilon = pSettings.douglasPeuckerEpsilon.toFloat()
                                weldDistance = pSettings.weldDistance.toFloat()
                                collinearAngle = pSettings.collinearAngleToleranceDeg.toFloat()
                            },
                            label = { Text(preset.name, fontSize = 10.sp) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Endpoint weld distance
                Text(
                    text = String.format(Locale.US, "مسافة دمج نقاط النهاية (Weld): %.1f px", weldDistance),
                    fontSize = 12.sp
                )
                Slider(
                    value = weldDistance,
                    onValueChange = { weldDistance = it },
                    valueRange = 1.0f..15.0f
                )

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تعامد جدران المباني (90° Snap)", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = detectRectangles, onCheckedChange = { detectRectangles = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("اكتشاف الدوائر والمستديرات", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = detectCircles, onCheckedChange = { detectCircles = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onApplySettings(
                    settings.copy(
                        thresholdMode = thresholdMode,
                        edgeSensitivity = edgeSensitivity.toInt(),
                        douglasPeuckerEpsilon = douglasPeuckerEpsilon.toDouble(),
                        minContourArea = minContourArea.toDouble(),
                        detectRectangles = detectRectangles,
                        detectCircles = detectCircles,
                        cleanupPreset = cleanupPreset,
                        weldDistance = weldDistance.toDouble(),
                        collinearAngleToleranceDeg = collinearAngle.toDouble()
                    )
                )
                onDismiss()
            }) {
                Text("تطبيق الإعدادات / Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء / Cancel")
            }
        }
    )
}
