package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.compose.ui.geometry.Offset
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.ProjectRepository
import com.example.engine.dxf.DxfValidationReport
import com.example.engine.dxf.DxfValidator
import com.example.engine.dxf.DxfWriter
import com.example.engine.image.ImageProcessor
import com.example.engine.vector.GeometryCleaner
import com.example.engine.vector.VectorizationEngine
import com.example.model.*
import com.example.ui.components.CadTool
import com.example.ui.components.ViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class EditorViewModel(
    private val repository: ProjectRepository,
    private val initialProjectId: String? = null
) : ViewModel() {

    private val _project = MutableStateFlow(
        ProjectData(
            id = initialProjectId ?: UUID.randomUUID().toString(),
            name = "مشروع هندسي جديد",
            createdAt = System.currentTimeMillis()
        )
    )
    val project: StateFlow<ProjectData> = _project.asStateFlow()

    private val _rasterBitmap = MutableStateFlow<Bitmap?>(null)
    val rasterBitmap: StateFlow<Bitmap?> = _rasterBitmap.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.VECTOR)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _overlayOpacity = MutableStateFlow(0.35f)
    val overlayOpacity: StateFlow<Float> = _overlayOpacity.asStateFlow()

    private val _activeTool = MutableStateFlow(CadTool.PAN_ZOOM)
    val activeTool: StateFlow<CadTool> = _activeTool.asStateFlow()

    private val _selectedEntity = MutableStateFlow<DxfEntity?>(null)
    val selectedEntity: StateFlow<DxfEntity?> = _selectedEntity.asStateFlow()

    private val _zoomScale = MutableStateFlow(1.0f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _panOffset = MutableStateFlow(Offset.Zero)
    val panOffset: StateFlow<Offset> = _panOffset.asStateFlow()

    private val _cursorCoordText = MutableStateFlow("X: 0 | Y: 0")
    val cursorCoordText: StateFlow<String> = _cursorCoordText.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processStep = MutableStateFlow("جاهز")
    val processStep: StateFlow<String> = _processStep.asStateFlow()

    private val _processProgress = MutableStateFlow(0f)
    val processProgress: StateFlow<Float> = _processProgress.asStateFlow()

    private val _validationReport = MutableStateFlow<DxfValidationReport?>(null)
    val validationReport: StateFlow<DxfValidationReport?> = _validationReport.asStateFlow()

    // Dialog state
    var showLayersDialog = MutableStateFlow(false)
    var showCalibrationDialog = MutableStateFlow(false)
    var showGeorefDialog = MutableStateFlow(false)
    var showSettingsDialog = MutableStateFlow(false)
    var showExportDialog = MutableStateFlow(false)
    var showPropertiesSheet = MutableStateFlow(false)

    // Undo / Redo stacks
    private val undoStack = mutableListOf<List<DxfEntity>>()
    private val redoStack = mutableListOf<List<DxfEntity>>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    init {
        if (initialProjectId != null) {
            viewModelScope.launch {
                val loaded = repository.loadProject(initialProjectId)
                if (loaded != null) {
                    _project.value = loaded
                    if (loaded.imagePath != null) {
                        loadBitmapFromFile(File(loaded.imagePath))
                    }
                }
            }
        }
    }

    fun setViewMode(mode: ViewMode) { _viewMode.value = mode }
    fun setOverlayOpacity(opacity: Float) { _overlayOpacity.value = opacity }
    fun setActiveTool(tool: CadTool) { _activeTool.value = tool }
    fun setZoomScale(scale: Float) { _zoomScale.value = scale }
    fun setPanOffset(offset: Offset) { _panOffset.value = offset }

    fun updateCursor(pt: Point2D) {
        val p = _project.value
        _cursorCoordText.value = if (p.georef.isEnabled && p.georef.gcps.size >= 2) {
            p.georef.formatWorldCoordinate(pt.x, pt.y)
        } else {
            p.calibration.formatCoordinate(pt.x, pt.y)
        }
    }

    fun selectEntity(entity: DxfEntity?) {
        _selectedEntity.value = entity
        showPropertiesSheet.value = entity != null
    }

    private fun pushHistory() {
        undoStack.add(_project.value.entities)
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(_project.value.entities)
            _project.value = _project.value.copy(entities = prev)
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = true
            _selectedEntity.value = null
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(_project.value.entities)
            _project.value = _project.value.copy(entities = next)
            _canUndo.value = true
            _canRedo.value = redoStack.isNotEmpty()
            _selectedEntity.value = null
        }
    }

    fun addGeometry(entity: DxfEntity) {
        pushHistory()
        _project.value = _project.value.copy(entities = _project.value.entities + entity)
    }

    fun moveEntity(entityId: String, dx: Double, dy: Double) {
        val updated = _project.value.entities.map { e ->
            if (e.id == entityId) e.translate(dx, dy) else e
        }
        _project.value = _project.value.copy(entities = updated)
        _selectedEntity.value = updated.find { it.id == entityId }
    }

    fun deleteEntity(entityId: String) {
        pushHistory()
        val updated = _project.value.entities.filter { it.id != entityId }
        _project.value = _project.value.copy(entities = updated)
        _selectedEntity.value = null
        showPropertiesSheet.value = false
    }

    fun duplicateEntity(entity: DxfEntity) {
        pushHistory()
        val cloned = entity.translate(20.0, 20.0)
        _project.value = _project.value.copy(entities = _project.value.entities + cloned)
        selectEntity(cloned)
    }

    fun changeEntityLayer(entityId: String, newLayer: String) {
        pushHistory()
        val updated = _project.value.entities.map { e ->
            if (e.id == entityId) e.withLayer(newLayer) else e
        }
        _project.value = _project.value.copy(entities = updated)
        _selectedEntity.value = updated.find { it.id == entityId }
    }

    fun togglePolylineClosed(polyline: PolylineEntity) {
        pushHistory()
        val toggled = polyline.copy(isClosed = !polyline.isClosed)
        val updated = _project.value.entities.map { if (it.id == polyline.id) toggled else it }
        _project.value = _project.value.copy(entities = updated)
        _selectedEntity.value = toggled
    }

    // Load user image from Uri safely
    fun importImageFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _processStep.value = "جاري تحميل الصورة بأمان..."
            _processProgress.value = 0.1f

            val bmp = ImageProcessor.decodeSampledBitmap(
                inputStreamProvider = { context.contentResolver.openInputStream(uri) },
                maxDimension = 1400
            )

            if (bmp != null) {
                // Save locally to cache/images
                val imgFile = File(context.filesDir, "image_${System.currentTimeMillis()}.jpg")
                val fos = FileOutputStream(imgFile)
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush()
                fos.close()

                withContext(Dispatchers.Main) {
                    _rasterBitmap.value = bmp
                    _project.value = _project.value.copy(
                        imageWidth = bmp.width,
                        imageHeight = bmp.height,
                        imagePath = imgFile.absolutePath,
                        name = "مخطط ${imgFile.nameWithoutExtension}"
                    )
                    _viewMode.value = ViewMode.SPLIT // show split comparison!
                    _isProcessing.value = false
                }
            } else {
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                }
            }
        }
    }

    // Load built-in sample cadastral aerial imagery
    fun loadSampleAerial(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            _processStep.value = "تحميل النموذج الجوي المساحي..."
            _processProgress.value = 0.2f

            val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.sample_cadastral)
            if (bmp != null) {
                withContext(Dispatchers.Main) {
                    _rasterBitmap.value = bmp
                    _project.value = _project.value.copy(
                        imageWidth = bmp.width,
                        imageHeight = bmp.height,
                        name = "نموذج مساحي جوي (Cadastral Sample)"
                    )
                    _viewMode.value = ViewMode.SPLIT
                    _isProcessing.value = false
                }
            } else {
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                }
            }
        }
    }

    private fun loadBitmapFromFile(file: File) {
        if (!file.exists()) return
        val bmp = BitmapFactory.decodeFile(file.absolutePath)
        if (bmp != null) {
            _rasterBitmap.value = bmp
        }
    }

    // AUTO VECTORIZE
    fun autoVectorize() {
        val bmp = _rasterBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _isProcessing.value = true
            _processProgress.value = 0.05f

            val extracted = VectorizationEngine.vectorize(
                bitmap = bmp,
                settings = _project.value.settings,
                onProgress = { step, pct ->
                    _processStep.value = step
                    _processProgress.value = pct
                }
            )

            withContext(Dispatchers.Main) {
                pushHistory()
                _project.value = _project.value.copy(entities = extracted)
                _viewMode.value = ViewMode.SPLIT // display split view or vector view
                _isProcessing.value = false
                _processStep.value = "جاهز"
            }
        }
    }

    // CLEAN GEOMETRY
    fun cleanGeometry() {
        val currentEntities = _project.value.entities
        if (currentEntities.isEmpty()) return

        viewModelScope.launch(Dispatchers.Default) {
            _isProcessing.value = true
            _processStep.value = "تنظيف العناصر وتوصيل النهايات..."
            _processProgress.value = 0.5f

            val cleaned = GeometryCleaner.clean(currentEntities, _project.value.settings)

            withContext(Dispatchers.Main) {
                pushHistory()
                _project.value = _project.value.copy(entities = cleaned)
                _isProcessing.value = false
            }
        }
    }

    // Layers
    fun toggleLayerVisibility(layerName: String) {
        val updated = _project.value.layers.map {
            if (it.name == layerName) it.copy(isVisible = !it.isVisible) else it
        }
        _project.value = _project.value.copy(layers = updated)
    }

    fun toggleLayerLock(layerName: String) {
        val updated = _project.value.layers.map {
            if (it.name == layerName) it.copy(isLocked = !it.isLocked) else it
        }
        _project.value = _project.value.copy(layers = updated)
    }

    fun addCustomLayer(name: String, colorHex: String, aci: Int) {
        val newLayer = CadLayer(
            id = "layer_${System.currentTimeMillis()}",
            name = name,
            colorHex = colorHex,
            colorAci = aci
        )
        _project.value = _project.value.copy(layers = _project.value.layers + newLayer)
    }

    // Calibration
    fun onPointPicked(point: Point2D) {
        if (_activeTool.value == CadTool.CALIBRATE_PICK) {
            val cal = _project.value.calibration
            val updated = if (cal.point1 == null) {
                cal.copy(point1 = point)
            } else {
                cal.copy(point2 = point)
            }
            _project.value = _project.value.copy(calibration = updated)
            showCalibrationDialog.value = true
            _activeTool.value = CadTool.PAN_ZOOM
        } else if (_activeTool.value == CadTool.GEOREF_PICK) {
            val gcp = GcpPoint(
                id = UUID.randomUUID().toString(),
                pixelPoint = point,
                worldX = point.x * 10.0,
                worldY = point.y * 10.0,
                label = "GCP ${_project.value.georef.gcps.size + 1}"
            )
            _project.value = _project.value.copy(
                georef = _project.value.georef.copy(
                    gcps = _project.value.georef.gcps + gcp
                )
            )
            showGeorefDialog.value = true
            _activeTool.value = CadTool.PAN_ZOOM
        }
    }

    fun applyCalibration(realDist: Double, unit: CadUnit) {
        val cal = _project.value.calibration.copy(
            realDistance = realDist,
            unit = unit,
            isCalibrated = true
        )
        _project.value = _project.value.copy(calibration = cal)
    }

    fun resetCalibration() {
        _project.value = _project.value.copy(calibration = CalibrationData())
    }

    fun updateGeoref(georef: GeoreferenceData) {
        _project.value = _project.value.copy(georef = georef)
    }

    fun applySettings(settings: VectorizationSettings) {
        _project.value = _project.value.copy(settings = settings)
    }

    fun prepareDxfExport() {
        val report = DxfValidator.validate(
            entities = _project.value.entities,
            layers = _project.value.layers,
            calibration = _project.value.calibration,
            georef = _project.value.georef
        )
        _validationReport.value = report
        showExportDialog.value = true
    }

    fun saveProject(context: Context, onSaved: (String) -> Unit) {
        viewModelScope.launch {
            val path = repository.saveProject(_project.value)
            withContext(Dispatchers.Main) {
                onSaved(path)
            }
        }
    }

    fun exportDxfFile(context: Context, fileName: String, onComplete: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "DXF_Exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val targetFile = File(exportDir, fileName)
            DxfWriter.exportToDxf(
                destinationFile = targetFile,
                entities = _project.value.entities,
                layers = _project.value.layers,
                calibration = _project.value.calibration,
                georef = _project.value.georef,
                imageHeight = _project.value.imageHeight.toDouble()
            )

            withContext(Dispatchers.Main) {
                onComplete(targetFile)
            }
        }
    }

    fun shareDxfFile(context: Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val exportDir = File(context.cacheDir, "shared_dxf")
            if (!exportDir.exists()) exportDir.mkdirs()

            val safeName = "${_project.value.name.replace(" ", "_")}.dxf"
            val targetFile = File(exportDir, safeName)
            DxfWriter.exportToDxf(
                destinationFile = targetFile,
                entities = _project.value.entities,
                layers = _project.value.layers,
                calibration = _project.value.calibration,
                georef = _project.value.georef,
                imageHeight = _project.value.imageHeight.toDouble()
            )

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/dxf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "DXF CAD Drawing - ${_project.value.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(shareIntent, "مشاركة مخطط DXF"))
                onComplete()
            }
        }
    }

    fun fitToScreen(screenWidth: Float, screenHeight: Float) {
        val p = _project.value
        val imgW = if (p.imageWidth > 0) p.imageWidth.toFloat() else 1000f
        val imgH = if (p.imageHeight > 0) p.imageHeight.toFloat() else 1000f

        val scaleX = screenWidth / imgW
        val scaleY = screenHeight / imgH
        val optimalScale = minOf(scaleX, scaleY) * 0.95f

        _zoomScale.value = optimalScale.coerceIn(0.2f, 5.0f)
        val cx = (screenWidth - imgW * optimalScale) / 2f
        val cy = (screenHeight - imgH * optimalScale) / 2f
        _panOffset.value = Offset(cx, cy)
    }
}
