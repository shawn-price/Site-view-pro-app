package com.example.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CaptureMode
import com.example.model.HudFilterMode
import com.example.model.HudTargetPoint
import com.example.model.JobMode
import com.example.model.ProcessingState
import com.example.model.ProjectWorkspace
import com.example.model.SpatialFace
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class HudUiState(
    val activeJobMode: JobMode = JobMode.PAINTING,
    val filterMode: HudFilterMode = HudFilterMode.TACTICAL_OPTIC,
    val captureMode: CaptureMode = CaptureMode.VIDEO,
    val processingState: ProcessingState = ProcessingState.IDLE,
    val overlaysVisible: Boolean = true,
    val isModelLocked: Boolean = false,
    val projectName: String = "SURVEY_SITE_001",
    val spatialFaces: List<SpatialFace> = emptyList(),
    val selectedFaceId: String? = null,
    val workspaceExportJson: String? = null,
    val showSaveWorkspaceDialog: Boolean = false,
    val showNewProjectDialog: Boolean = false,
    val showFacePropertySheet: Boolean = false,
    val targetPoints: List<HudTargetPoint> = emptyList(),
    val selectedPointId: String? = null,
    val zoomLevel: Float = 1.0f,
    val laserRangerActive: Boolean = true,
    val currentRangeMeters: Float = 4.25f,
    val isTorchOn: Boolean = false,
    val isHudGridVisible: Boolean = true,
    val isHorizonVisible: Boolean = true,
    val isMilScaleVisible: Boolean = true,
    val simulatedPitchDeg: Float = 1.2f,
    val simulatedRollDeg: Float = -0.6f,
    val simulatedAzimuthDeg: Float = 248.5f,
    val isUsingManualTilt: Boolean = false,
    val showJobSpecEditor: Boolean = false,
    // Job-specific custom parameters
    val paintingCoverageRate: Float = 10.5f, // m²/L
    val paintingCoats: Float = 2.0f,
    val paintingWetMil: Float = 120.0f, // µm
    val plasteringDepthMm: Float = 12.0f, // mm
    val plasteringMixRatio: Float = 3.0f, // 1:X parts sand
    val plasteringPlumbTol: Float = 1.5f, // mm/m
    val screedingDepthMm: Float = 65.0f, // mm
    val screedingFallGradient: Float = 1.67f, // %
    val screedingToleranceMm: Float = 3.0f,
    val snapshotFlashTrigger: Long = 0L,
    val batteryPercent: Int = 88,
    val isBatteryCharging: Boolean = false,
    val batteryVoltage: Float = 4.12f,
    val lastActionNotification: String? = "TACTICAL HUD ONLINE",
    val isCrosshairAlignedWithEdge: Boolean = false,
    val alignedEdgeName: String? = null,
    val edgeAlignmentTrigger: Long = 0L,
    val jobModeSwitchTrigger: Long = 0L,
    val pinAcquiredTrigger: Long = 0L
) {
    // Calculated polygon area in m² based on normalized coordinates calibrated to current range
    val measuredAreaSqM: Float
        get() {
            if (targetPoints.size < 3) return 0f
            // Calibration factor: at currentRangeMeters, the camera sensor FOV maps to physical dimensions
            val fovWidthMeters = currentRangeMeters * 0.95f
            val fovHeightMeters = currentRangeMeters * 1.45f

            var sum = 0.0
            val n = targetPoints.size
            for (i in 0 until n) {
                val p1 = targetPoints[i]
                val p2 = targetPoints[(i + 1) % n]
                val x1 = p1.normX * fovWidthMeters
                val y1 = p1.normY * fovHeightMeters
                val x2 = p2.normX * fovWidthMeters
                val y2 = p2.normY * fovHeightMeters
                sum += (x1 * y2 - x2 * y1)
            }
            return abs(sum.toFloat()) * 0.5f
        }

    // Total perimeter / point-to-point line length
    val measuredPerimeterM: Float
        get() {
            if (targetPoints.size < 2) return 0f
            val fovWidthMeters = currentRangeMeters * 0.95f
            val fovHeightMeters = currentRangeMeters * 1.45f
            var total = 0f
            for (i in 0 until targetPoints.size - 1) {
                val p1 = targetPoints[i]
                val p2 = targetPoints[i + 1]
                val dx = (p2.normX - p1.normX) * fovWidthMeters
                val dy = (p2.normY - p1.normY) * fovHeightMeters
                total += sqrt(dx * dx + dy * dy)
            }
            return total
        }

    // Dynamic job estimation based on active mode
    val jobCalculationSummary: String
        get() {
            val area = measuredAreaSqM
            return when (activeJobMode) {
                JobMode.PAINTING -> {
                    if (area > 0.05f) {
                        val liters = (area / paintingCoverageRate) * paintingCoats
                        String.format("%.2f m² Area • %.1f L Paint Req. (%.0f coats)", area, liters, paintingCoats)
                    } else if (measuredPerimeterM > 0.1f) {
                        String.format("%.2f m Cut-in Edge Line", measuredPerimeterM)
                    } else {
                        "Tap points to measure wall/ceiling surface area"
                    }
                }
                JobMode.PLASTERING -> {
                    if (area > 0.05f) {
                        val volM3 = area * (plasteringDepthMm / 1000f)
                        val bags = volM3 * 1350f / 25f // ~25kg bags at 1350kg/m³ density
                        String.format("%.2f m² • %.3f m³ Mix (≈%.1f bags @ 25kg)", area, volM3, bags)
                    } else if (measuredPerimeterM > 0.1f) {
                        String.format("%.2f m Stop-Bead Lineal Length", measuredPerimeterM)
                    } else {
                        "Tap points to calculate render volume & mix ratio"
                    }
                }
                JobMode.SCREEDING -> {
                    if (area > 0.05f) {
                        val volM3 = area * (screedingDepthMm / 1000f)
                        val fallDropMm = measuredPerimeterM * (screedingFallGradient / 100f) * 1000f
                        String.format("%.2f m² • %.2f m³ Screed Bed • Fall: %.0f mm", area, volM3, fallDropMm)
                    } else if (measuredPerimeterM > 0.1f) {
                        String.format("%.2f m Datum Run Length", measuredPerimeterM)
                    } else {
                        "Tap points to map floor datum & fall gradient"
                    }
                }
            }
        }
}

class CameraHudViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HudUiState())
    val uiState: StateFlow<HudUiState> = _uiState.asStateFlow()

    init {
        // Pre-populate with 3 sample points demonstrating tactical surveying layout
        val defaultPoints = listOf(
            HudTargetPoint(
                id = UUID.randomUUID().toString(),
                normX = 0.32f,
                normY = 0.36f,
                label = "PT-01 [DATUM-TOP-L]",
                estimatedDistanceM = 4.20f,
                pitchAngleDeg = 2.4f,
                azimuthDeg = 246.1f
            ),
            HudTargetPoint(
                id = UUID.randomUUID().toString(),
                normX = 0.68f,
                normY = 0.38f,
                label = "PT-02 [DATUM-TOP-R]",
                estimatedDistanceM = 4.35f,
                pitchAngleDeg = 2.1f,
                azimuthDeg = 250.8f
            ),
            HudTargetPoint(
                id = UUID.randomUUID().toString(),
                normX = 0.65f,
                normY = 0.66f,
                label = "PT-03 [CORNER-BASE]",
                estimatedDistanceM = 4.15f,
                pitchAngleDeg = -3.8f,
                azimuthDeg = 250.2f
            ),
            HudTargetPoint(
                id = UUID.randomUUID().toString(),
                normX = 0.30f,
                normY = 0.64f,
                label = "PT-04 [FLOOR-DATUM]",
                estimatedDistanceM = 4.08f,
                pitchAngleDeg = -4.1f,
                azimuthDeg = 245.9f
            )
        )
        _uiState.update { it.copy(targetPoints = defaultPoints) }
    }

    fun setJobMode(mode: JobMode) {
        _uiState.update {
            it.copy(
                activeJobMode = mode,
                lastActionNotification = "JOB MODE: ${mode.title} ENGAGED",
                jobModeSwitchTrigger = System.currentTimeMillis()
            )
        }
    }

    fun setFilterMode(filter: HudFilterMode) {
        _uiState.update {
            it.copy(
                filterMode = filter,
                lastActionNotification = "OPTICAL FILTER: ${filter.label.uppercase()}"
            )
        }
    }

    fun addTargetPoint(normX: Float, normY: Float) {
        val count = _uiState.value.targetPoints.size + 1
        val estimatedDist = _uiState.value.currentRangeMeters + ((normY - 0.5f) * 0.4f)
        val pitch = (_uiState.value.simulatedPitchDeg + (0.5f - normY) * 20f)
        val azimuth = (_uiState.value.simulatedAzimuthDeg + (normX - 0.5f) * 30f)

        val newPoint = HudTargetPoint(
            id = UUID.randomUUID().toString(),
            normX = normX.coerceIn(0.05f, 0.95f),
            normY = normY.coerceIn(0.05f, 0.95f),
            label = "PT-%02d".format(count),
            estimatedDistanceM = String.format("%.2f", estimatedDist).toFloat(),
            pitchAngleDeg = String.format("%.1f", pitch).toFloat(),
            azimuthDeg = String.format("%.1f", azimuth).toFloat()
        )

        _uiState.update { state ->
            val updatedPoints = state.targetPoints + newPoint
            state.copy(
                targetPoints = updatedPoints,
                selectedPointId = newPoint.id,
                lastActionNotification = "PIN ACQUIRED: ${newPoint.label} (%.2fm)".format(newPoint.estimatedDistanceM),
                pinAcquiredTrigger = System.currentTimeMillis()
            )
        }
        checkEdgeAlignment()
    }

    fun selectPoint(pointId: String?) {
        _uiState.update { it.copy(selectedPointId = pointId) }
    }

    fun deletePoint(pointId: String) {
        _uiState.update { state ->
            val updated = state.targetPoints.filterNot { it.id == pointId }
            state.copy(
                targetPoints = updated,
                selectedPointId = null,
                lastActionNotification = "PIN REMOVED"
            )
        }
    }

    fun clearAllPoints() {
        _uiState.update {
            it.copy(
                targetPoints = emptyList(),
                selectedPointId = null,
                lastActionNotification = "GRID CLEARED"
            )
        }
    }

    fun undoLastPoint() {
        _uiState.update { state ->
            if (state.targetPoints.isNotEmpty()) {
                val updated = state.targetPoints.dropLast(1)
                state.copy(
                    targetPoints = updated,
                    selectedPointId = updated.lastOrNull()?.id,
                    lastActionNotification = "UNDO LAST PIN"
                )
            } else {
                state
            }
        }
    }

    companion object {
        val MILITARY_BINOCULAR_MAGNIFICATIONS = listOf(1.0f, 2.0f, 4.0f, 8.0f)
    }

    fun setZoomLevel(zoom: Float) {
        val clamped = zoom.coerceIn(1.0f, 8.0f)
        val rounded = (Math.round(clamped * 10f) / 10f)
        val description = when {
            rounded >= 7.8f -> "8.0X [8X MILITARY HIGH-POWER]"
            rounded in 3.8f..4.2f -> "4.0X [4X RECON MAGNIFICATION]"
            rounded in 1.8f..2.2f -> "2.0X [2X OPTICAL ENHANCE]"
            rounded <= 1.1f -> "1.0X [1X WIDE TACTICAL FOV]"
            else -> "%.1fX [BINOCULAR MAG]".format(rounded)
        }
        _uiState.update {
            it.copy(
                zoomLevel = rounded,
                lastActionNotification = "MAGNIFICATION: $description"
            )
        }
    }

    fun onPinchZoom(deltaScale: Float) {
        val current = _uiState.value.zoomLevel
        val target = (current * deltaScale).coerceIn(1.0f, 8.0f)
        val rounded = (Math.round(target * 10f) / 10f)
        if (abs(rounded - current) >= 0.1f) {
            val description = when {
                rounded >= 7.8f -> "8.0X [8X MIL HIGH-POWER]"
                rounded in 3.8f..4.2f -> "4.0X [4X RECON MAG]"
                rounded in 1.8f..2.2f -> "2.0X [2X OPTICAL]"
                rounded <= 1.1f -> "1.0X [1X WIDE FOV]"
                else -> "%.1fX [DETAIL INSPECT]".format(rounded)
            }
            _uiState.update {
                it.copy(
                    zoomLevel = rounded,
                    lastActionNotification = "PINCH ZOOM: $description"
                )
            }
        }
    }

    fun cycleBinocularZoom() {
        val current = _uiState.value.zoomLevel
        val next = when {
            current < 1.8f -> 2.0f
            current < 3.8f -> 4.0f
            current < 7.8f -> 8.0f
            else -> 1.0f
        }
        setZoomLevel(next)
    }

    fun toggleTorch() {
        _uiState.update {
            val nextState = !it.isTorchOn
            it.copy(
                isTorchOn = nextState,
                lastActionNotification = if (nextState) "TACTICAL ILLUMINATOR ON" else "TACTICAL ILLUMINATOR OFF"
            )
        }
    }

    fun toggleLaserRanger() {
        _uiState.update {
            val next = !it.laserRangerActive
            it.copy(
                laserRangerActive = next,
                lastActionNotification = if (next) "LASER RANGEFINDER: ARMED" else "LASER RANGEFINDER: STANDBY"
            )
        }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(isHudGridVisible = !it.isHudGridVisible) }
    }

    fun toggleHorizon() {
        _uiState.update { it.copy(isHorizonVisible = !it.isHorizonVisible) }
    }

    fun toggleMilScale() {
        _uiState.update { it.copy(isMilScaleVisible = !it.isMilScaleVisible) }
    }

    fun setJobSpecEditorVisible(show: Boolean) {
        _uiState.update { it.copy(showJobSpecEditor = show) }
    }

    fun updatePaintingParams(coverage: Float, coats: Float, wetMil: Float) {
        _uiState.update {
            it.copy(
                paintingCoverageRate = coverage,
                paintingCoats = coats,
                paintingWetMil = wetMil,
                lastActionNotification = "PAINT SPECS UPDATED"
            )
        }
    }

    fun updatePlasteringParams(depthMm: Float, mixRatio: Float, plumbTol: Float) {
        _uiState.update {
            it.copy(
                plasteringDepthMm = depthMm,
                plasteringMixRatio = mixRatio,
                plasteringPlumbTol = plumbTol,
                lastActionNotification = "PLASTER SPECS UPDATED"
            )
        }
    }

    fun updateScreedingParams(depthMm: Float, fallGradient: Float, toleranceMm: Float) {
        _uiState.update {
            it.copy(
                screedingDepthMm = depthMm,
                screedingFallGradient = fallGradient,
                screedingToleranceMm = toleranceMm,
                lastActionNotification = "SCREED SPECS UPDATED"
            )
        }
    }

    fun updateOrientationFromSensor(pitch: Float, roll: Float, azimuth: Float) {
        if (!_uiState.value.isUsingManualTilt) {
            _uiState.update {
                it.copy(
                    simulatedPitchDeg = pitch,
                    simulatedRollDeg = roll,
                    simulatedAzimuthDeg = azimuth
                )
            }
            checkEdgeAlignment()
        }
    }

    fun setManualOrientation(pitch: Float, roll: Float) {
        _uiState.update {
            it.copy(
                simulatedPitchDeg = pitch,
                simulatedRollDeg = roll,
                isUsingManualTilt = true
            )
        }
        checkEdgeAlignment()
    }

    fun resetSensorMode() {
        _uiState.update { it.copy(isUsingManualTilt = false) }
    }

    fun triggerSnapshotFlash() {
        _uiState.update {
            it.copy(
                snapshotFlashTrigger = System.currentTimeMillis(),
                lastActionNotification = "SURVEY SNAPSHOT CAPTURED"
            )
        }
    }

    /**
     * Checks if the center viewfinder crosshairs (normalized (0.5, 0.5)) align with:
     * 1. Any edge segment between surveyed target points (within distance threshold)
     * 2. Plumb/level horizon datum (|roll| < 0.4° and |pitch| < 0.4°)
     */
    private fun checkEdgeAlignment() {
        val state = _uiState.value
        val cx = 0.5f
        val cy = 0.5f

        // Check level horizon alignment
        val isLevelAligned = abs(state.simulatedRollDeg) < 0.45f && abs(state.simulatedPitchDeg) < 0.45f
        var alignedName: String? = if (isLevelAligned) "LEVEL HORIZON DATUM [0°]" else null

        // Check proximity to polygon edges
        if (alignedName == null && state.targetPoints.size >= 2) {
            val points = state.targetPoints
            val n = points.size
            for (i in 0 until n) {
                if (i < n - 1 || n >= 3) {
                    val p1 = points[i]
                    val p2 = points[(i + 1) % n]
                    val dist = distanceToSegment(cx, cy, p1.normX, p1.normY, p2.normX, p2.normY)
                    if (dist < 0.038f) { // ~3.8% of screen threshold
                        alignedName = "EDGE: ${p1.label} ➔ ${p2.label}"
                        break
                    }
                }
            }
        }

        val isAligned = alignedName != null
        val wasAligned = state.isCrosshairAlignedWithEdge

        if (isAligned && !wasAligned) {
            // New alignment lock event!
            _uiState.update {
                it.copy(
                    isCrosshairAlignedWithEdge = true,
                    alignedEdgeName = alignedName,
                    edgeAlignmentTrigger = System.currentTimeMillis(),
                    lastActionNotification = "RETICLE SNAP: $alignedName"
                )
            }
        } else if (!isAligned && wasAligned) {
            // Out of alignment
            _uiState.update {
                it.copy(
                    isCrosshairAlignedWithEdge = false,
                    alignedEdgeName = null
                )
            }
        }
    }

    private fun distanceToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val lengthSq = dx * dx + dy * dy
        if (lengthSq < 1e-6f) {
            val dpx = px - x1
            val dpy = py - y1
            return sqrt(dpx * dpx + dpy * dpy)
        }
        val t = (((px - x1) * dx + (py - y1) * dy) / lengthSq).coerceIn(0f, 1f)
        val projX = x1 + t * dx
        val projY = y1 + t * dy
        val ex = px - projX
        val ey = py - projY
        return sqrt(ex * ex + ey * ey)
    }

    // ==================== PROJECT CONTROL ACTIONS ====================

    fun setCaptureMode(mode: CaptureMode) {
        _uiState.update {
            it.copy(
                captureMode = mode,
                lastActionNotification = "CAPTURE MODE: ${mode.label}"
            )
        }
    }

    fun openNewProjectDialog() {
        _uiState.update { it.copy(showNewProjectDialog = true) }
    }

    fun dismissNewProjectDialog() {
        _uiState.update { it.copy(showNewProjectDialog = false) }
    }

    /**
     * [New Button]: Resets the session state entirely, clears current spatial models,
     * and prompts for a fresh project workspace initialization.
     */
    fun createNewProject(name: String) {
        _uiState.update {
            it.copy(
                projectName = name.ifBlank { "SURVEY_SITE_${System.currentTimeMillis() % 10000}" },
                targetPoints = emptyList(),
                spatialFaces = emptyList(),
                selectedPointId = null,
                selectedFaceId = null,
                isModelLocked = false,
                processingState = ProcessingState.IDLE,
                overlaysVisible = true,
                showNewProjectDialog = false,
                showFacePropertySheet = false,
                lastActionNotification = "NEW PROJECT INITIALIZED: $name"
            )
        }
    }

    /**
     * [Restart Capture Button]: Clears the current video/photo frame tracking buffer
     * and restarts the ongoing edge/corner detection session from scratch.
     */
    fun restartCaptureBuffer() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    processingState = ProcessingState.SCANNING,
                    selectedPointId = null,
                    selectedFaceId = null,
                    isModelLocked = false,
                    lastActionNotification = "FRAME BUFFER FLUSHED // RESTARTING SCAN"
                )
            }
            delay(400)
            // Re-acquire fresh datum pins
            val freshPoints = listOf(
                HudTargetPoint(UUID.randomUUID().toString(), 0.24f, 0.28f, "DATUM-NW", 4.20f, 2.1f, 246.0f),
                HudTargetPoint(UUID.randomUUID().toString(), 0.76f, 0.28f, "DATUM-NE", 4.25f, 2.1f, 250.0f),
                HudTargetPoint(UUID.randomUUID().toString(), 0.76f, 0.72f, "DATUM-SE", 4.15f, -3.2f, 250.0f),
                HudTargetPoint(UUID.randomUUID().toString(), 0.24f, 0.72f, "DATUM-SW", 4.10f, -3.2f, 246.0f)
            )
            _uiState.update {
                it.copy(
                    targetPoints = freshPoints,
                    processingState = ProcessingState.IDLE,
                    lastActionNotification = "OPTICAL TRACKING REALIGNED [4 PINS]"
                )
            }
            checkEdgeAlignment()
        }
    }

    /**
     * [Clear Screen Button]: Instantly dismisses active overlays, selected face highlights,
     * and temporary drawing guides without losing workspace data.
     */
    fun toggleOverlayVisibility() {
        _uiState.update {
            val next = !it.overlaysVisible
            it.copy(
                overlaysVisible = next,
                lastActionNotification = if (next) "OVERLAYS RESTORED" else "HUD SCREEN CLEARED"
            )
        }
    }

    /**
     * [Auto Button (One-Touch Analysis)]:
     * Automatically captures the feed, calculates dimensions, maps corners/edges,
     * and builds the structured 3D model geometry automatically. Once finished,
     * locks the model so the user can immediately tap any face to open the Paint Property Palette.
     */
    fun runAutoAnalysis() {
        if (_uiState.value.processingState == ProcessingState.AUTO_ANALYZING) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    processingState = ProcessingState.AUTO_ANALYZING,
                    lastActionNotification = "AUTO-ANALYSIS: SCANNING STRUCTURAL VECTORS..."
                )
            }
            delay(600)
            _uiState.update {
                it.copy(lastActionNotification = "AUTO-ANALYSIS: COMPUTING 3D BOUNDS & DEPTH...")
            }
            delay(700)

            val fovW = _uiState.value.currentRangeMeters * 0.95f
            val fovH = _uiState.value.currentRangeMeters * 1.45f
            val wM = (0.78f - 0.22f) * fovW
            val hM = (0.72f - 0.26f) * fovH
            val autoArea = wM * hM

            val face1 = SpatialFace(
                id = "FACE_${UUID.randomUUID().toString().take(8)}",
                label = "ELEVATION WALL [MAIN]",
                vertices = listOf(
                    0.22f to 0.26f,
                    0.78f to 0.26f,
                    0.78f to 0.72f,
                    0.22f to 0.72f
                ),
                areaSqM = autoArea,
                estimatedDistanceM = _uiState.value.currentRangeMeters,
                surfaceColor = _uiState.value.activeJobMode.primaryColor,
                coverageFactor = _uiState.value.activeJobMode.defaultPrimaryValue
            )

            val autoPoints = listOf(
                HudTargetPoint(UUID.randomUUID().toString(), 0.22f, 0.26f, "CORNER-NW", 4.25f, 2.5f, 245.0f),
                HudTargetPoint(UUID.randomUUID().toString(), 0.78f, 0.26f, "CORNER-NE", 4.25f, 2.5f, 251.0f),
                HudTargetPoint(UUID.randomUUID().toString(), 0.78f, 0.72f, "CORNER-SE", 4.25f, -3.5f, 251.0f),
                HudTargetPoint(UUID.randomUUID().toString(), 0.22f, 0.72f, "CORNER-SW", 4.25f, -3.5f, 245.0f)
            )

            _uiState.update {
                it.copy(
                    targetPoints = autoPoints,
                    spatialFaces = listOf(face1),
                    selectedFaceId = face1.id,
                    isModelLocked = true,
                    showFacePropertySheet = true,
                    processingState = ProcessingState.SUCCESS,
                    lastActionNotification = "AUTO 3D MODEL LOCKED // %.2f m²".format(autoArea),
                    pinAcquiredTrigger = System.currentTimeMillis()
                )
            }

            delay(2500)
            if (_uiState.value.processingState == ProcessingState.SUCCESS) {
                _uiState.update { it.copy(processingState = ProcessingState.IDLE) }
            }
        }
    }

    /**
     * [Save Button]: Packages the current frame data, calculated dimensions, active job mode,
     * and local cache into the structured JSON workspace export format.
     */
    fun saveWorkspace() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    processingState = ProcessingState.SAVING,
                    lastActionNotification = "PACKAGING WORKSPACE TO JSON..."
                )
            }
            delay(500)

            val state = _uiState.value
            val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val facesToExport = if (state.spatialFaces.isNotEmpty()) {
                state.spatialFaces
            } else {
                listOf(
                    SpatialFace(
                        id = "FACE_01",
                        label = "MANUAL SURVEY PLANE",
                        vertices = state.targetPoints.map { it.normX to it.normY },
                        areaSqM = state.measuredAreaSqM,
                        estimatedDistanceM = state.currentRangeMeters,
                        surfaceColor = state.activeJobMode.primaryColor,
                        coverageFactor = state.activeJobMode.defaultPrimaryValue
                    )
                )
            }

            val workspace = ProjectWorkspace(
                projectId = "PROJ_${System.currentTimeMillis()}",
                projectName = state.projectName,
                createdAtIso = isoDate,
                jobMode = state.activeJobMode,
                faces = facesToExport,
                totalAreaSqM = if (state.spatialFaces.isNotEmpty()) state.spatialFaces.map { it.areaSqM }.sum() else state.measuredAreaSqM,
                laserRangeM = state.currentRangeMeters,
                pitchDeg = state.simulatedPitchDeg,
                rollDeg = state.simulatedRollDeg,
                azimuthDeg = state.simulatedAzimuthDeg
            )

            val jsonOutput = workspace.toFormattedJson()

            _uiState.update {
                it.copy(
                    workspaceExportJson = jsonOutput,
                    showSaveWorkspaceDialog = true,
                    processingState = ProcessingState.SUCCESS,
                    lastActionNotification = "WORKSPACE SAVED & EXPORTED"
                )
            }

            delay(2000)
            if (_uiState.value.processingState == ProcessingState.SUCCESS) {
                _uiState.update { it.copy(processingState = ProcessingState.IDLE) }
            }
        }
    }

    fun dismissSaveWorkspaceDialog() {
        _uiState.update { it.copy(showSaveWorkspaceDialog = false) }
    }

    fun selectSpatialFace(faceId: String?) {
        _uiState.update {
            it.copy(
                selectedFaceId = faceId,
                showFacePropertySheet = faceId != null
            )
        }
    }

    fun updateSelectedFaceColor(color: Color) {
        _uiState.update { state ->
            val updatedFaces = state.spatialFaces.map { face ->
                if (face.id == state.selectedFaceId) {
                    face.copy(surfaceColor = color)
                } else {
                    face
                }
            }
            state.copy(
                spatialFaces = updatedFaces,
                lastActionNotification = "SURFACE PALETTE UPDATED"
            )
        }
    }

    fun closeFacePropertySheet() {
        _uiState.update { it.copy(showFacePropertySheet = false) }
    }

    fun cycleBatteryState() {
        _uiState.update { state ->
            val (nextPercent, nextCharging, nextVoltage) = when {
                !state.isBatteryCharging && state.batteryPercent > 50 -> Triple(42, false, 3.82f)
                !state.isBatteryCharging && state.batteryPercent in 21..50 -> Triple(15, false, 3.65f)
                !state.isBatteryCharging && state.batteryPercent <= 20 -> Triple(95, true, 4.35f)
                else -> Triple(88, false, 4.12f)
            }
            state.copy(
                batteryPercent = nextPercent,
                isBatteryCharging = nextCharging,
                batteryVoltage = nextVoltage,
                lastActionNotification = if (nextCharging) "BATTERY: CHARGING [${nextPercent}% • ${nextVoltage}V]" else "BATTERY: ${nextPercent}% [${nextVoltage}V]"
            )
        }
    }
}

