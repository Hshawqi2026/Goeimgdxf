package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CadBottomBar(
    activeTool: CadTool,
    onToolSelected: (CadTool) -> Unit,
    onAutoVectorize: () -> Unit,
    onCleanGeometry: () -> Unit,
    onOpenLayers: () -> Unit,
    onOpenCalibrate: () -> Unit,
    onOpenGeoref: () -> Unit,
    onOpenSettings: () -> Unit,
    isCalibrated: Boolean,
    isGeoreferenced: Boolean,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 8.dp)
        ) {
            // Primary Action Row: AUTO VECTORIZE & CLEAN GEOMETRY
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAutoVectorize,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)), // Emerald CAD Green
                    modifier = Modifier.weight(1.5f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جاري التحليل...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Auto Vectorize", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AUTO VECTORIZE", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }

                OutlinedButton(
                    onClick = onCleanGeometry,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = "Clean Geometry", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تنظيف CAD", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // CAD Tools & Dialog Triggers Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CadToolButton(
                    icon = Icons.Default.PanTool,
                    label = "تحريك",
                    isSelected = activeTool == CadTool.PAN_ZOOM,
                    onClick = { onToolSelected(CadTool.PAN_ZOOM) }
                )
                CadToolButton(
                    icon = Icons.Default.NearMe,
                    label = "تحديد",
                    isSelected = activeTool == CadTool.SELECT,
                    onClick = { onToolSelected(CadTool.SELECT) }
                )
                CadToolButton(
                    icon = Icons.Default.Timeline,
                    label = "خط",
                    isSelected = activeTool == CadTool.DRAW_LINE,
                    onClick = { onToolSelected(CadTool.DRAW_LINE) }
                )
                CadToolButton(
                    icon = Icons.Default.Polyline,
                    label = "مسار",
                    isSelected = activeTool == CadTool.DRAW_POLYLINE,
                    onClick = { onToolSelected(CadTool.DRAW_POLYLINE) }
                )
                CadToolButton(
                    icon = Icons.Default.CropSquare,
                    label = "مستطيل",
                    isSelected = activeTool == CadTool.DRAW_RECT,
                    onClick = { onToolSelected(CadTool.DRAW_RECT) }
                )
                CadToolButton(
                    icon = Icons.Default.Circle,
                    label = "دائرة",
                    isSelected = activeTool == CadTool.DRAW_CIRCLE,
                    onClick = { onToolSelected(CadTool.DRAW_CIRCLE) }
                )

                VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 4.dp))

                // Layers button
                CadUtilityButton(
                    icon = Icons.Default.Layers,
                    label = "الطبقات",
                    onClick = onOpenLayers
                )

                // Calibrate button (with indicator)
                CadUtilityButton(
                    icon = Icons.Default.Straighten,
                    label = if (isCalibrated) "معاير ✓" else "معايرة",
                    badgeColor = if (isCalibrated) Color(0xFF10B981) else null,
                    onClick = onOpenCalibrate
                )

                // Georef button
                CadUtilityButton(
                    icon = Icons.Default.Public,
                    label = if (isGeoreferenced) "إسناد ✓" else "إسناد",
                    badgeColor = if (isGeoreferenced) Color(0xFF06B6D4) else null,
                    onClick = onOpenGeoref
                )

                // Settings button
                CadUtilityButton(
                    icon = Icons.Default.Tune,
                    label = "إعدادات",
                    onClick = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun CadToolButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CadUtilityButton(
    icon: ImageVector,
    label: String,
    badgeColor: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = badgeColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                if (badgeColor != null) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .align(Alignment.TopEnd)
                    )
                }
            }
            Text(
                text = label,
                fontSize = 10.sp,
                color = badgeColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (badgeColor != null) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
