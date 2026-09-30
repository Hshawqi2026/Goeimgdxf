package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadTopBar(
    projectName: String,
    viewMode: ViewMode,
    canUndo: Boolean,
    canRedo: Boolean,
    onBack: () -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFitScreen: () -> Unit,
    onSaveProject: () -> Unit,
    onExportDxf: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "GeoImage2DXF Pro",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            // Undo / Redo
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.4f)
                )
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.4f)
                )
            }

            // Fit to screen
            IconButton(onClick = onFitScreen) {
                Icon(Icons.Default.FitScreen, contentDescription = "Fit View")
            }

            // Save
            IconButton(onClick = onSaveProject) {
                Icon(Icons.Default.Save, contentDescription = "Save Project")
            }

            // Export DXF
            Button(
                onClick = onExportDxf,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.padding(end = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = "Export DXF", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("DXF", fontWeight = FontWeight.Bold)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun ViewModeSelectorBar(
    viewMode: ViewMode,
    overlayOpacity: Float,
    onViewModeChange: (ViewMode) -> Unit,
    onOverlayOpacityChange: (Float) -> Unit,
    cursorCoordText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = viewMode == ViewMode.ORIGINAL,
                    onClick = { onViewModeChange(ViewMode.ORIGINAL) },
                    label = { Text("الصورة / Raster", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = viewMode == ViewMode.VECTOR,
                    onClick = { onViewModeChange(ViewMode.VECTOR) },
                    label = { Text("المخطط / Vector", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = viewMode == ViewMode.OVERLAY,
                    onClick = { onViewModeChange(ViewMode.OVERLAY) },
                    label = { Text("تطابق / Overlay", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = viewMode == ViewMode.SPLIT,
                    onClick = { onViewModeChange(ViewMode.SPLIT) },
                    label = { Text("مقارنة / Split", fontSize = 11.sp) }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Cursor coordinates readout
                Text(
                    text = cursorCoordText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Opacity slider when in overlay mode
            if (viewMode == ViewMode.OVERLAY) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("شفافية الصورة:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = overlayOpacity,
                        onValueChange = onOverlayOpacityChange,
                        valueRange = 0.0f..1.0f,
                        modifier = Modifier.weight(1f).height(24.dp)
                    )
                }
            }
        }
    }
}
