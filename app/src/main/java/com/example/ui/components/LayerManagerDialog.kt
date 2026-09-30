package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CadLayer

@Composable
fun LayerManagerDialog(
    layers: List<CadLayer>,
    activeLayerName: String,
    onDismiss: () -> Unit,
    onLayerToggleVisibility: (String) -> Unit,
    onLayerToggleLock: (String) -> Unit,
    onSetActiveLayer: (String) -> Unit,
    onAddLayer: (name: String, colorHex: String, aci: Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newLayerName by remember { mutableStateOf("") }
    var newLayerColorHex by remember { mutableStateOf("#EF4444") }
    var newLayerAci by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إدارة الطبقات / CAD Layers", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Layer", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "تتحكم الطبقات في الألوان والرؤية وتنتقل مباشرة لملف DXF النهائي.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                    items(layers) { layer ->
                        val isActive = layer.name == activeLayerName
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSetActiveLayer(layer.name) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Color swatch
                                val color = try {
                                    Color(android.graphics.Color.parseColor(layer.colorHex))
                                } catch (_: Exception) { Color.White }

                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = layer.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.Bold else null
                                    )
                                    Text(
                                        text = "ACI ${layer.colorAci}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Visibility
                                IconButton(onClick = { onLayerToggleVisibility(layer.name) }) {
                                    Icon(
                                        imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Visibility",
                                        tint = if (layer.isVisible) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Lock
                                IconButton(onClick = { onLayerToggleLock(layer.name) }) {
                                    Icon(
                                        imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "Toggle Lock",
                                        tint = if (layer.isLocked) MaterialTheme.colorScheme.error else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق / Done")
            }
        }
    )

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إنشاء طبقة جديدة / Add Layer") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newLayerName,
                        onValueChange = { newLayerName = it.uppercase() },
                        label = { Text("اسم الطبقة / Layer Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("اختر لون الـ CAD:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val palette = listOf(
                            Pair("#EF4444", 1),
                            Pair("#F59E0B", 2),
                            Pair("#10B981", 3),
                            Pair("#06B6D4", 4),
                            Pair("#3B82F6", 5),
                            Pair("#EC4899", 6),
                            Pair("#F8FAFC", 7)
                        )
                        for ((hex, aci) in palette) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable {
                                        newLayerColorHex = hex
                                        newLayerAci = aci
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newLayerName.isNotBlank()) {
                            onAddLayer(newLayerName.trim(), newLayerColorHex, newLayerAci)
                            showAddDialog = false
                            newLayerName = ""
                        }
                    }
                ) {
                    Text("إضافة / Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء / Cancel")
                }
            }
        )
    }
}
