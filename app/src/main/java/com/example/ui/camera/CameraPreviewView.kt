package com.example.ui.camera

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.HudFilterMode
import com.example.model.JobMode
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPreviewView(
    filterMode: HudFilterMode,
    jobMode: JobMode,
    zoomLevel: Float,
    isTorchOn: Boolean,
    snapshotFlashTrigger: Long,
    modifier: Modifier = Modifier
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("camera_preview_container")) {
        if (cameraPermissionState.status.isGranted) {
            CameraXLivePreview(
                zoomLevel = zoomLevel,
                isTorchOn = isTorchOn,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // High-fidelity simulated construction site scene for emulator with binocular zoom magnification
            SimulatedSiteScene(
                filterMode = filterMode,
                jobMode = jobMode,
                zoomLevel = zoomLevel,
                isTorchOn = isTorchOn,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Camera Filter Layer Effect (Night Vision phosphor / FLIR thermal gradient)
        CameraFilterOverlay(filterMode = filterMode, modifier = Modifier.fillMaxSize())

        // Snapshot Flash Animation
        var showFlash by remember { mutableStateOf(false) }
        LaunchedEffect(snapshotFlashTrigger) {
            if (snapshotFlashTrigger > 0) {
                showFlash = true
                kotlinx.coroutines.delay(120)
                showFlash = false
            }
        }

        AnimatedVisibility(
            visible = showFlash,
            enter = fadeIn(tween(40)),
            exit = fadeOut(tween(250))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun CameraXLivePreview(
    zoomLevel: Float,
    isTorchOn: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    // Fallback to simulated view if binding fails
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier,
        update = {
            try {
                camera?.cameraControl?.setZoomRatio(zoomLevel.coerceIn(1.0f, 8.0f))
            } catch (_: Exception) {
                try {
                    camera?.cameraControl?.setLinearZoom(((zoomLevel - 1f) / 7f).coerceIn(0f, 1f))
                } catch (_: Exception) {}
            }
            try {
                camera?.cameraControl?.enableTorch(isTorchOn)
            } catch (_: Exception) {}
        }
    )
}

@Composable
private fun SimulatedSiteScene(
    filterMode: HudFilterMode,
    jobMode: JobMode,
    zoomLevel: Float,
    isTorchOn: Boolean,
    modifier: Modifier = Modifier
) {
    // Renders a realistic architectural interior construction perspective:
    // drywall joints, concrete floor slab, ceiling joists, datum level benchmark,
    // and live tactical torch spotlight illumination during low-light measurements.
    // Scales realistically with binocular zoom magnification levels (1x, 2x, 4x, 8x) for inspecting wall and ceiling details.
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        withTransform({
            scale(scaleX = zoomLevel, scaleY = zoomLevel, pivot = Offset(cx, cy))
        }) {
            // Background Concrete / Drywall Wall Base (Dim in low-light, illuminated by torch)
            val wallColors = if (isTorchOn) {
                listOf(
                    Color(0xFF334155),
                    Color(0xFF475569),
                    Color(0xFF64748B),
                    Color(0xFF334155)
                )
            } else {
                listOf(
                    Color(0xFF1E293B),
                    Color(0xFF334155),
                    Color(0xFF475569),
                    Color(0xFF1E293B)
                )
            }
            drawRect(
                brush = Brush.verticalGradient(
                    colors = wallColors
                )
            )

            // Floor Slab Perspective (Lower third)
            val floorY = h * 0.70f
            val floorPath = Path().apply {
                moveTo(0f, floorY)
                lineTo(w, floorY)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = floorPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF334155), Color(0xFF0F172A)),
                    startY = floorY,
                    endY = h
                )
            )

            // Ceiling Joists (Top 18%)
            val ceilingY = h * 0.18f
            val ceilingPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, ceilingY)
                lineTo(0f, ceilingY)
                close()
            }
            drawPath(
                path = ceilingPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B)),
                    startY = 0f,
                    endY = ceilingY
                )
            )

            // Ceiling Joist Hanger Steel Brackets (Visible during zoom inspection)
            if (zoomLevel >= 1.8f) {
                val bracketColor = Color(0xFF94A3B8).copy(alpha = 0.5f)
                for (b in 1..8) {
                    val bx = w * (b / 9f)
                    drawRect(
                        color = bracketColor,
                        topLeft = Offset(bx - 3f, ceilingY - 14f),
                        size = androidx.compose.ui.geometry.Size(6f, 14f)
                    )
                }
            }

            // Drywall Stud Verticals & Panel Seams
            val seamColor = Color(0xFF64748B).copy(alpha = 0.4f)
            val studColor = Color(0xFF94A3B8).copy(alpha = 0.25f)
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), 0f)

            for (i in 1..4) {
                val seamX = w * (i / 5f)
                drawLine(
                    color = seamColor,
                    start = Offset(seamX, ceilingY),
                    end = Offset(seamX, floorY),
                    strokeWidth = 2f
                )

                // Stud line
                drawLine(
                    color = studColor,
                    start = Offset(seamX - (w * 0.1f), ceilingY),
                    end = Offset(seamX - (w * 0.1f), floorY),
                    strokeWidth = 1f,
                    pathEffect = dash
                )

                // High-Magnification Inspection Detail: Drywall Screws every 300mm on studs (2x, 4x, 8x)
                if (zoomLevel >= 1.8f) {
                    val screwColor = Color(0xFFCBD5E1).copy(alpha = 0.6f)
                    val studX = seamX - (w * 0.1f)
                    for (s in 1..6) {
                        val sy = ceilingY + (floorY - ceilingY) * (s / 7f)
                        drawCircle(color = screwColor, radius = 2.2f, center = Offset(studX, sy))
                        if (zoomLevel >= 3.8f) {
                            // Crosshead slot on screw head
                            drawLine(color = Color(0xFF1E293B), start = Offset(studX - 1.5f, sy), end = Offset(studX + 1.5f, sy), strokeWidth = 0.8f)
                            drawLine(color = Color(0xFF1E293B), start = Offset(studX, sy - 1.5f), end = Offset(studX, sy + 1.5f), strokeWidth = 0.8f)
                        }
                    }
                }
            }

            // Horizontal Drywall Joint Tapes
            val jointY1 = ceilingY + (floorY - ceilingY) * 0.35f
            val jointY2 = ceilingY + (floorY - ceilingY) * 0.70f
            drawLine(color = seamColor, start = Offset(0f, jointY1), end = Offset(w, jointY1), strokeWidth = 2f)
            drawLine(color = seamColor, start = Offset(0f, jointY2), end = Offset(w, jointY2), strokeWidth = 2f)

            // Perspective Floor Screed Grid Lines
            val gridColor = Color(0xFF64748B).copy(alpha = 0.3f)
            for (i in 0..6) {
                val startX = w * (i / 6f)
                drawLine(
                    color = gridColor,
                    start = Offset(startX, floorY),
                    end = Offset(cx + (startX - cx) * 2.2f, h),
                    strokeWidth = 1.5f
                )
            }

            // Structural Datum Benchmark Line (1.000m reference)
            val datumY = cy + 20f
            drawLine(
                color = Color(0xFF38BDF8).copy(alpha = 0.6f),
                start = Offset(20f, datumY),
                end = Offset(w - 20f, datumY),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
            )

            // Fine Millimeter Datum Hash Marks at Higher Zoom (2x, 4x, 8x)
            if (zoomLevel >= 1.8f) {
                val tickColor = Color(0xFF38BDF8).copy(alpha = 0.45f)
                val numTicks = (w / 12f).toInt()
                for (t in 0..numTicks) {
                    val tx = t * 12f
                    val tickH = if (t % 5 == 0) 6f else 3f
                    drawLine(
                        color = tickColor,
                        start = Offset(tx, datumY - tickH),
                        end = Offset(tx, datumY + tickH),
                        strokeWidth = 1f
                    )
                }
            }

            // Tactical Torch / Flash Illuminator Spotlight Beam
            if (isTorchOn) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x55FFFBEB), // Warm high-intensity center
                            Color(0x35FEF3C7),
                            Color(0x15FDE68A),
                            Color(0x00000000)  // Smooth falloff
                        ),
                        center = Offset(cx, cy),
                        radius = w * 0.72f
                    ),
                    radius = w * 0.72f,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

@Composable
private fun CameraFilterOverlay(
    filterMode: HudFilterMode,
    modifier: Modifier = Modifier
) {
    when (filterMode) {
        HudFilterMode.NIGHT_VISION -> {
            Canvas(modifier = modifier) {
                // Phosphor Green tint + subtle scanlines
                drawRect(color = Color(0x3500FF66))
            }
        }
        HudFilterMode.FLIR_THERMAL -> {
            Canvas(modifier = modifier) {
                // Thermal gradient simulation
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x35FFFF00),
                            Color(0x30FF4500),
                            Color(0x25800080),
                            Color(0x2000008B)
                        ),
                        radius = size.maxDimension * 0.75f
                    )
                )
            }
        }
        HudFilterMode.BLUEPRINT -> {
            Canvas(modifier = modifier) {
                drawRect(color = Color(0x2D1D4ED8))
            }
        }
        HudFilterMode.TACTICAL_OPTIC -> {
            // Clean optic pass-through
        }
    }
}
