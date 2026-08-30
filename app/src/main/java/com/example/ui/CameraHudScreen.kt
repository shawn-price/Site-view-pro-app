package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sensors.OrientationSensorManager
import com.example.ui.camera.CameraPreviewView
import com.example.ui.hud.BinocularHudCanvas
import com.example.ui.hud.BottomDynamicTelemetryCard
import com.example.ui.hud.InteractiveTargetPointsOverlay
import com.example.ui.hud.JobModeSelectorBar
import com.example.ui.hud.JobSpecBottomSheet
import com.example.ui.hud.MilitaryBinocularZoomBar
import com.example.ui.hud.PaintPropertyPaletteSheet
import com.example.ui.hud.ProjectControlsBar
import com.example.ui.hud.TacticalSideControlRail
import com.example.ui.hud.TopTacticalStatusHeader
import com.example.viewmodel.CameraHudViewModel
import kotlinx.coroutines.delay

@Composable
fun CameraHudScreen(
    viewModel: CameraHudViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hapticManager = remember { com.example.haptics.TacticalHapticManager(context) }

    val sensorManager = remember { OrientationSensorManager(context) }
    val orientationState by sensorManager.orientationFlow.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        sensorManager.startListening()
        onDispose {
            sensorManager.stopListening()
        }
    }

    LaunchedEffect(orientationState) {
        if (orientationState.isSensorActive) {
            viewModel.updateOrientationFromSensor(
                pitch = orientationState.pitchDeg,
                roll = orientationState.rollDeg,
                azimuth = orientationState.azimuthDeg
            )
        }
    }

    // Haptic feedback trigger when crosshairs align with detected structural edges / horizon datum
    LaunchedEffect(uiState.edgeAlignmentTrigger) {
        if (uiState.edgeAlignmentTrigger > 0L) {
            hapticManager.triggerEdgeAlignmentHaptic()
        }
    }

    // Haptic feedback trigger when toggling between Job Modes (Painting, Plastering, Screeding)
    LaunchedEffect(uiState.jobModeSwitchTrigger) {
        if (uiState.jobModeSwitchTrigger > 0L) {
            hapticManager.triggerJobModeSwitchHaptic()
        }
    }

    // Haptic feedback trigger when acquiring/placing survey pins
    LaunchedEffect(uiState.pinAcquiredTrigger) {
        if (uiState.pinAcquiredTrigger > 0L) {
            hapticManager.triggerPinAcquiredHaptic()
        }
    }

    val pitchDeg = uiState.simulatedPitchDeg
    val rollDeg = uiState.simulatedRollDeg
    val azimuthDeg = uiState.simulatedAzimuthDeg
    val primaryColor = uiState.activeJobMode.primaryColor

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("siteview_pro_hud_screen"),
        containerColor = Color(0xFF0A0C0B)
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Base Layer: Live Camera Preview or Simulated Scene
            CameraPreviewView(
                filterMode = uiState.filterMode,
                jobMode = uiState.activeJobMode,
                zoomLevel = uiState.zoomLevel,
                isTorchOn = uiState.isTorchOn,
                snapshotFlashTrigger = uiState.snapshotFlashTrigger,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Custom HUD Canvas: Binocular Frames, Horizon, Mils & Reticles (Respects overlaysVisible)
            if (uiState.overlaysVisible) {
                BinocularHudCanvas(
                    jobMode = uiState.activeJobMode,
                    filterMode = uiState.filterMode,
                    pitchDeg = pitchDeg,
                    rollDeg = rollDeg,
                    azimuthDeg = azimuthDeg,
                    laserRangerActive = uiState.laserRangerActive,
                    isHudGridVisible = uiState.isHudGridVisible,
                    isHorizonVisible = uiState.isHorizonVisible,
                    isMilScaleVisible = uiState.isMilScaleVisible,
                    zoomLevel = uiState.zoomLevel,
                    isCrosshairAligned = uiState.isCrosshairAlignedWithEdge,
                    alignedEdgeName = uiState.alignedEdgeName,
                    modifier = Modifier.fillMaxSize()
                )

                // 3. Interactive Target Points Touch & Survey Layer (Pins & 3D Spatial Faces)
                InteractiveTargetPointsOverlay(
                    targetPoints = uiState.targetPoints,
                    selectedPointId = uiState.selectedPointId,
                    jobMode = uiState.activeJobMode,
                    currentRangeMeters = uiState.currentRangeMeters,
                    spatialFaces = uiState.spatialFaces,
                    selectedFaceId = uiState.selectedFaceId,
                    onTapCanvas = { nx, ny -> viewModel.addTargetPoint(nx, ny) },
                    onSelectPoint = { id -> viewModel.selectPoint(id) },
                    onDeletePoint = { id -> viewModel.deletePoint(id) },
                    onSelectFace = { faceId -> viewModel.selectSpatialFace(faceId) },
                    onPinchZoom = { delta -> viewModel.onPinchZoom(delta) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 4. Tactical Left Side Control Toolbar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            ) {
                TacticalSideControlRail(
                    uiState = uiState,
                    onSetZoom = { z -> viewModel.setZoomLevel(z) },
                    onSetFilter = { f -> viewModel.setFilterMode(f) },
                    onToggleTorch = { viewModel.toggleTorch() },
                    onToggleLaserRanger = { viewModel.toggleLaserRanger() },
                    onToggleGrid = { viewModel.toggleGrid() },
                    onToggleHorizon = { viewModel.toggleHorizon() },
                    onUndoPin = { viewModel.undoLastPoint() },
                    onClearPins = { viewModel.clearAllPoints() },
                    onCaptureSnapshot = { viewModel.triggerSnapshotFlash() }
                )
            }

            // 5. Top Telemetry & Status Bar + Project Controls Deck
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopTacticalStatusHeader(
                    uiState = uiState,
                    pitchDeg = pitchDeg,
                    rollDeg = rollDeg,
                    azimuthDeg = azimuthDeg,
                    onBatteryClick = { viewModel.cycleBatteryState() },
                    onToggleTorch = { viewModel.toggleTorch() }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Floating Project Controls Action Bar Deck [NEW, RESTART, CLEAR, SAVE, AUTO]
                ProjectControlsBar(
                    processingState = uiState.processingState,
                    overlaysVisible = uiState.overlaysVisible,
                    themeColor = primaryColor,
                    onNewProject = { viewModel.openNewProjectDialog() },
                    onRestartCapture = { viewModel.restartCaptureBuffer() },
                    onToggleClearScreen = { viewModel.toggleOverlayVisibility() },
                    onSaveWorkspace = { viewModel.saveWorkspace() },
                    onAutoAnalyze = { viewModel.runAutoAnalysis() }
                )

                // Notification Toast Banner
                var visibleNotification by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(uiState.lastActionNotification) {
                    if (uiState.lastActionNotification != null) {
                        visibleNotification = uiState.lastActionNotification
                        delay(2200)
                        visibleNotification = null
                    }
                }

                AnimatedVisibility(
                    visible = visibleNotification != null,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    if (visibleNotification != null) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(Color(0xE60A0C0B), RoundedCornerShape(6.dp))
                                .border(1.dp, primaryColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = visibleNotification!!,
                                color = primaryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // 6. Bottom Floating Controls: Telemetry Card + Job Mode Selector / Face Paint Palette
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // If a 3D face is selected, show Paint Property Palette
                val selectedFace = uiState.spatialFaces.firstOrNull { it.id == uiState.selectedFaceId }
                if (uiState.showFacePropertySheet && selectedFace != null) {
                    PaintPropertyPaletteSheet(
                        face = selectedFace,
                        onColorSelected = { color -> viewModel.updateSelectedFaceColor(color) },
                        onClose = { viewModel.closeFacePropertySheet() },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                } else {
                    // Military Binocular Magnification Quick-Bar (1x, 2x, 4x, 8x)
                    if (uiState.overlaysVisible) {
                        MilitaryBinocularZoomBar(
                            currentZoom = uiState.zoomLevel,
                            onSelectZoom = { z -> viewModel.setZoomLevel(z) },
                            activeColor = primaryColor
                        )

                        // Dynamic Telemetry Metrics Card
                        BottomDynamicTelemetryCard(
                            uiState = uiState
                        )
                    }

                    // Floating Job Mode Selector Toggle Bar [Painting, Plastering, Screeding]
                    JobModeSelectorBar(
                        activeMode = uiState.activeJobMode,
                        onSelectMode = { mode -> viewModel.setJobMode(mode) },
                        onOpenSpecEditor = { viewModel.setJobSpecEditorVisible(true) }
                    )
                }

                // Hardware Camera Shutter Trigger
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { viewModel.triggerSnapshotFlash() }
                        .testTag("capture_snapshot_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            // 7. Modal BottomSheet for Adjusting Job Specs
            if (uiState.showJobSpecEditor) {
                JobSpecBottomSheet(
                    uiState = uiState,
                    onDismiss = { viewModel.setJobSpecEditorVisible(false) },
                    onSavePainting = { cov, coats, wet ->
                        viewModel.updatePaintingParams(cov, coats, wet)
                    },
                    onSavePlastering = { depth, mix, tol ->
                        viewModel.updatePlasteringParams(depth, mix, tol)
                    },
                    onSaveScreeding = { depth, fall, tol ->
                        viewModel.updateScreedingParams(depth, fall, tol)
                    }
                )
            }

            // 8. New Project Initialization Dialog
            if (uiState.showNewProjectDialog) {
                var projectNameInput by remember { mutableStateOf("SURVEY_${uiState.activeJobMode.title}_01") }
                AlertDialog(
                    onDismissRequest = { viewModel.dismissNewProjectDialog() },
                    title = {
                        Text(
                            text = "INITIALIZE NEW WORKSPACE",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Clearing current spatial vectors, frame buffer, and 3D faces to initialize a fresh project session.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            OutlinedTextField(
                                value = projectNameInput,
                                onValueChange = { projectNameInput = it },
                                label = { Text("Project Name / Site ID", fontSize = 10.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFF333333),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.createNewProject(projectNameInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text("INITIALIZE", color = Color(0xFF0A0C0B), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissNewProjectDialog() }) {
                            Text("CANCEL", color = Color.White.copy(alpha = 0.6f))
                        }
                    },
                    containerColor = Color(0xFF0A0C0B),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // 9. Save Workspace JSON Export Dialog
            if (uiState.showSaveWorkspaceDialog && uiState.workspaceExportJson != null) {
                val jsonString = uiState.workspaceExportJson!!
                AlertDialog(
                    onDismissRequest = { viewModel.dismissSaveWorkspaceDialog() },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STRUCTURED WORKSPACE JSON",
                                color = Color(0xFF00E5FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "Export package ready for SiteView Pro CAD & Estimator sync:",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF050706))
                                    .border(1.dp, Color(0xFF1E2824), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = jsonString,
                                    color = Color(0xFF00FF41),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = ClipData.newPlainText("SiteView Workspace JSON", jsonString)
                                    clipboard?.setPrimaryClip(clip)
                                    viewModel.dismissSaveWorkspaceDialog()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = Color(0xFF0A0C0B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("COPY JSON", color = Color(0xFF0A0C0B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissSaveWorkspaceDialog() }) {
                            Text("DONE", color = Color.White.copy(alpha = 0.6f))
                        }
                    },
                    containerColor = Color(0xFF0A0C0B),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    }
}

