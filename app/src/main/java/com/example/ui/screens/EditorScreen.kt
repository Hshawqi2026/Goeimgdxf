package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CadLayer
import com.example.ui.components.*
import com.example.ui.viewmodel.EditorViewModel

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val project by viewModel.project.collectAsStateWithLifecycle()
    val rasterBitmap by viewModel.rasterBitmap.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val overlayOpacity by viewModel.overlayOpacity.collectAsStateWithLifecycle()
    val activeTool by viewModel.activeTool.collectAsStateWithLifecycle()
    val selectedEntity by viewModel.selectedEntity.collectAsStateWithLifecycle()
    val zoomScale by viewModel.zoomScale.collectAsStateWithLifecycle()
    val panOffset by viewModel.panOffset.collectAsStateWithLifecycle()
    val cursorCoordText by viewModel.cursorCoordText.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val processStep by viewModel.processStep.collectAsStateWithLifecycle()
    val processProgress by viewModel.processProgress.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val validationReport by viewModel.validationReport.collectAsStateWithLifecycle()

    val showLayersDialog by viewModel.showLayersDialog.collectAsStateWithLifecycle()
    val showCalibrationDialog by viewModel.showCalibrationDialog.collectAsStateWithLifecycle()
    val showGeorefDialog by viewModel.showGeorefDialog.collectAsStateWithLifecycle()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()
    val showExportDialog by viewModel.showExportDialog.collectAsStateWithLifecycle()
    val showPropertiesSheet by viewModel.showPropertiesSheet.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            Column {
                CadTopBar(
                    projectName = project.name,
                    viewMode = viewMode,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onBack = onNavigateBack,
                    onViewModeChange = { viewModel.setViewMode(it) },
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onFitScreen = {
                        // Fit canvas bounds
                        viewModel.fitToScreen(1080f, 1600f)
                    },
                    onSaveProject = {
                        viewModel.saveProject(context) { path ->
                            Toast.makeText(context, "تم حفظ المشروع بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onExportDxf = {
                        viewModel.prepareDxfExport()
                    }
                )
                ViewModeSelectorBar(
                    viewMode = viewMode,
                    overlayOpacity = overlayOpacity,
                    onViewModeChange = { viewModel.setViewMode(it) },
                    onOverlayOpacityChange = { viewModel.setOverlayOpacity(it) },
                    cursorCoordText = cursorCoordText
                )
            }
        },
        bottomBar = {
            CadBottomBar(
                activeTool = activeTool,
                onToolSelected = { viewModel.setActiveTool(it) },
                onAutoVectorize = { viewModel.autoVectorize() },
                onCleanGeometry = { viewModel.cleanGeometry() },
                onOpenLayers = { viewModel.showLayersDialog.value = true },
                onOpenCalibrate = { viewModel.showCalibrationDialog.value = true },
                onOpenGeoref = { viewModel.showGeorefDialog.value = true },
                onOpenSettings = { viewModel.showSettingsDialog.value = true },
                isCalibrated = project.calibration.isCalibrated,
                isGeoreferenced = project.georef.isEnabled,
                isProcessing = isProcessing
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Main Canvas Area
            if (viewMode == ViewMode.SPLIT) {
                SplitViewer(
                    rasterBitmap = rasterBitmap,
                    entities = project.entities,
                    layers = project.layers,
                    scale = zoomScale,
                    panOffset = panOffset,
                    onScaleChange = { viewModel.setZoomScale(it) },
                    onPanOffsetChange = { viewModel.setPanOffset(it) }
                )
            } else {
                CadCanvas(
                    rasterBitmap = rasterBitmap,
                    entities = project.entities,
                    layers = project.layers,
                    viewMode = viewMode,
                    overlayOpacity = overlayOpacity,
                    activeTool = activeTool,
                    selectedEntityId = selectedEntity?.id,
                    scale = zoomScale,
                    panOffset = panOffset,
                    onScaleChange = { viewModel.setZoomScale(it) },
                    onPanOffsetChange = { viewModel.setPanOffset(it) },
                    onEntitySelected = { viewModel.selectEntity(it) },
                    onPointPicked = { viewModel.onPointPicked(it) },
                    onGeometryAdded = { viewModel.addGeometry(it) },
                    onEntityMoved = { id, dx, dy -> viewModel.moveEntity(id, dx, dy) },
                    onCursorMoved = { viewModel.updateCursor(it) }
                )
            }

            // Properties Bottom Sheet
            if (showPropertiesSheet && selectedEntity != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                ) {
                    PropertiesSheet(
                        entity = selectedEntity!!,
                        layers = project.layers,
                        calibration = project.calibration,
                        onDismiss = { viewModel.selectEntity(null) },
                        onChangeLayer = { id, layer -> viewModel.changeEntityLayer(id, layer) },
                        onDeleteEntity = { id -> viewModel.deleteEntity(id) },
                        onDuplicateEntity = { e -> viewModel.duplicateEntity(e) },
                        onToggleClosed = { p -> viewModel.togglePolylineClosed(p) }
                    )
                }
            }

            // Processing Progress Overlay
            ProcessingOverlay(
                isProcessing = isProcessing,
                currentStep = processStep,
                progress = processProgress
            )
        }
    }

    // Dialogs
    if (showLayersDialog) {
        LayerManagerDialog(
            layers = project.layers,
            activeLayerName = CadLayer.LAYER_BUILDINGS.name,
            onDismiss = { viewModel.showLayersDialog.value = false },
            onLayerToggleVisibility = { viewModel.toggleLayerVisibility(it) },
            onLayerToggleLock = { viewModel.toggleLayerLock(it) },
            onSetActiveLayer = {},
            onAddLayer = { name, hex, aci -> viewModel.addCustomLayer(name, hex, aci) }
        )
    }

    if (showCalibrationDialog) {
        CalibrationDialog(
            calibration = project.calibration,
            onDismiss = { viewModel.showCalibrationDialog.value = false },
            onStartPickPoints = {
                viewModel.showCalibrationDialog.value = false
                viewModel.setActiveTool(CadTool.CALIBRATE_PICK)
                Toast.makeText(context, "المس نقطتين على الرسم لتحديد خط المعايرة", Toast.LENGTH_LONG).show()
            },
            onApplyCalibration = { dist, unit -> viewModel.applyCalibration(dist, unit) },
            onResetCalibration = { viewModel.resetCalibration() }
        )
    }

    if (showGeorefDialog) {
        GeorefDialog(
            georef = project.georef,
            onDismiss = { viewModel.showGeorefDialog.value = false },
            onUpdateGeoref = { viewModel.updateGeoref(it) },
            onStartPickGcp = {
                viewModel.showGeorefDialog.value = false
                viewModel.setActiveTool(CadTool.GEOREF_PICK)
                Toast.makeText(context, "المس نقطة تحكم أرضية على الرسم (GCP)", Toast.LENGTH_LONG).show()
            }
        )
    }

    if (showSettingsDialog) {
        VectorSettingsDialog(
            settings = project.settings,
            onDismiss = { viewModel.showSettingsDialog.value = false },
            onApplySettings = { viewModel.applySettings(it) }
        )
    }

    if (showExportDialog && validationReport != null) {
        DxfExportDialog(
            report = validationReport!!,
            projectName = project.name,
            onDismiss = { viewModel.showExportDialog.value = false },
            onExportSaveFile = { fileName ->
                viewModel.exportDxfFile(context, fileName) { file ->
                    Toast.makeText(context, "تم حفظ الملف في:\n${file.name}", Toast.LENGTH_LONG).show()
                }
            },
            onShareDxf = {
                viewModel.shareDxfFile(context)
            }
        )
    }
}
