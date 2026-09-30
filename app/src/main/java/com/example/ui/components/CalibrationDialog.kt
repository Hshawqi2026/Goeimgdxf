package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.CadUnit
import com.example.model.CalibrationData
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationDialog(
    calibration: CalibrationData,
    onDismiss: () -> Unit,
    onStartPickPoints: () -> Unit,
    onApplyCalibration: (realDist: Double, unit: CadUnit) -> Unit,
    onResetCalibration: () -> Unit
) {
    var distanceInput by remember { mutableStateOf(if (calibration.realDistance > 0) calibration.realDistance.toString() else "50.0") }
    var selectedUnit by remember { mutableStateOf(calibration.unit) }
    var unitMenuExpanded by remember { mutableStateOf(false) }

    val hasPoints = calibration.point1 != null && calibration.point2 != null
    val pixelDist = calibration.pixelDistance

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("معايرة القياس / Scale Calibration") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "حدد نقطتين معروفتي المسافة على الصورة، وأدخل البعد الحقيقي لحساب مقياس الرسم الهندسي.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Point selection status
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("النقاط المحددة / Picked Points:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = onStartPickPoints,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("تحديد على الرسم")
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (calibration.point1 != null) {
                                "P1: (${calibration.point1.x.toInt()}, ${calibration.point1.y.toInt()})"
                            } else "P1: غير محدد",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = if (calibration.point2 != null) {
                                "P2: (${calibration.point2.x.toInt()}, ${calibration.point2.y.toInt()})"
                            } else "P2: غير محدد",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (hasPoints) {
                            Text(
                                text = String.format(Locale.US, "المسافة بالبكسل: %.1f px", pixelDist),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Real distance input & Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = distanceInput,
                        onValueChange = { distanceInput = it },
                        label = { Text("المسافة الحقيقية") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = unitMenuExpanded,
                        onExpandedChange = { unitMenuExpanded = !unitMenuExpanded },
                        modifier = Modifier.width(100.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedUnit.symbol,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("الوحدة") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = unitMenuExpanded,
                            onDismissRequest = { unitMenuExpanded = false }
                        ) {
                            for (u in CadUnit.entries) {
                                DropdownMenuItem(
                                    text = { Text("${u.symbol} (${u.displayName})") },
                                    onClick = {
                                        selectedUnit = u
                                        unitMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (hasPoints) {
                    val realDistVal = distanceInput.toDoubleOrNull() ?: 0.0
                    if (realDistVal > 0 && pixelDist > 0) {
                        val factor = realDistVal / pixelDist
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = String.format(Locale.US, "المقياس الناتج: 1 بكسل = %.4f %s", factor, selectedUnit.symbol),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dist = distanceInput.toDoubleOrNull() ?: 0.0
                    if (dist > 0) {
                        onApplyCalibration(dist, selectedUnit)
                        onDismiss()
                    }
                },
                enabled = hasPoints && (distanceInput.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("تطبيق المعايرة / Apply")
            }
        },
        dismissButton = {
            Row {
                if (calibration.isCalibrated) {
                    TextButton(onClick = {
                        onResetCalibration()
                        onDismiss()
                    }) {
                        Text("إعادة ضبط / Reset", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("إلغاء / Cancel")
                }
            }
        }
    )
}
