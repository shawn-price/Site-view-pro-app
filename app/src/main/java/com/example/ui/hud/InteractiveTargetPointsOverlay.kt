package com.example.ui.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HudTargetPoint
import com.example.model.JobMode
import com.example.model.SpatialFace
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun InteractiveTargetPointsOverlay(
    targetPoints: List<HudTargetPoint>,
    selectedPointId: String?,
    jobMode: JobMode,
    currentRangeMeters: Float,
    spatialFaces: List<SpatialFace> = emptyList(),
    selectedFaceId: String? = null,
    onTapCanvas: (normX: Float, normY: Float) -> Unit,
    onSelectPoint: (String?) -> Unit,
    onDeletePoint: (String) -> Unit,
    onSelectFace: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pin_pulse")
    val pinRingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pinRingScale"
    )
    val pinRingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pinRingAlpha"
    )

    val primaryColor = jobMode.primaryColor
    val secondaryColor = jobMode.secondaryColor

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val normX = offset.x / size.width
                    val normY = offset.y / size.height
                    // Check if tapped near an existing point
                    val hitRadiusNorm = 32.dp.toPx() / size.width
                    val tappedPoint = targetPoints.firstOrNull { pt ->
                        val dx = pt.normX - normX
                        val dy = pt.normY - normY
                        sqrt(dx * dx + dy * dy) < hitRadiusNorm
                    }

                    if (tappedPoint != null) {
                        onSelectPoint(if (selectedPointId == tappedPoint.id) null else tappedPoint.id)
                    } else {
                        // Check if tapped inside spatial face
                        val tappedFace = spatialFaces.firstOrNull { face ->
                            normX in 0.20f..0.80f && normY in 0.24f..0.76f
                        }
                        if (tappedFace != null) {
                            onSelectFace(tappedFace.id)
                        } else {
                            onTapCanvas(normX, normY)
                        }
                    }
                }
            }
            .testTag("interactive_hud_target_layer")
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // 1. Draw Connecting Laser Survey Lines & Polygonal Shading & 3D Spatial Faces
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw 3D Spatial Faces if present
            spatialFaces.forEach { face ->
                val isFaceSelected = face.id == selectedFaceId
                if (face.vertices.size >= 3) {
                    val facePath = Path().apply {
                        moveTo(face.vertices[0].first * widthPx, face.vertices[0].second * heightPx)
                        for (i in 1 until face.vertices.size) {
                            lineTo(face.vertices[i].first * widthPx, face.vertices[i].second * heightPx)
                        }
                        close()
                    }
                    drawPath(
                        path = facePath,
                        color = face.surfaceColor.copy(alpha = if (isFaceSelected) 0.35f else 0.20f)
                    )
                    drawPath(
                        path = facePath,
                        color = if (isFaceSelected) Color.White else face.surfaceColor,
                        style = Stroke(width = if (isFaceSelected) 2.5.dp.toPx() else 1.8.dp.toPx())
                    )
                }
            }

            drawSurveyPolygonsAndLines(
                points = targetPoints,
                w = widthPx,
                h = heightPx,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                currentRangeMeters = currentRangeMeters
            )
        }

        // 2. Render Pin Target Badges & Interactive Reticles
        targetPoints.forEach { point ->
            val isSelected = point.id == selectedPointId
            val pxX = point.normX * widthPx
            val pxY = point.normY * heightPx

            val pinSizeDp = if (isSelected) 36.dp else 26.dp

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (pxX - (pinSizeDp.toPx() / 2f)).roundToInt(),
                            (pxY - (pinSizeDp.toPx() / 2f)).roundToInt()
                        )
                    }
                    .size(pinSizeDp)
                    .clickable {
                        onSelectPoint(if (isSelected) null else point.id)
                    }
                    .testTag("pin_${point.label}")
            ) {
                // Outer pulsing ring for selected pin
                if (isSelected) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = primaryColor.copy(alpha = pinRingAlpha),
                            radius = (size.minDimension / 2f) * pinRingScale,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // Center tactical point disc
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            if (isSelected) primaryColor else Color(0xCC0F172A)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.5.dp,
                            color = if (isSelected) Color.White else primaryColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "Target Locked",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(secondaryColor)
                        )
                    }
                }
            }

            // Compact Tactical Pin Label next to pin
            val labelOffsetXDp = if (point.normX > 0.7f) (-95).dp else 20.dp
            val labelOffsetYDp = (-18).dp
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (pxX + labelOffsetXDp.toPx()).roundToInt(),
                            (pxY + labelOffsetYDp.toPx()).roundToInt()
                        )
                    }
                    .background(Color(0xFF0A0C0B), RoundedCornerShape(4.dp))
                    .border(1.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${point.label} • ${point.estimatedDistanceM}m",
                    color = secondaryColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 3. Selected Point Detail Card Inspector Popup
        val selectedPoint = targetPoints.firstOrNull { it.id == selectedPointId }
        AnimatedVisibility(
            visible = selectedPoint != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        ) {
            if (selectedPoint != null) {
                Surface(
                    color = Color(0xFF0A0C0B),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor),
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )

                        Column {
                            Text(
                                text = "SURVEY PIN: ${selectedPoint.label}",
                                color = primaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Dist: %.2fm | Pitch: %+.1f° | Azim: %.1f°".format(
                                    selectedPoint.estimatedDistanceM,
                                    selectedPoint.pitchAngleDeg,
                                    selectedPoint.azimuthDeg
                                ),
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        IconButton(
                            onClick = { onDeletePoint(selectedPoint.id) },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("delete_selected_pin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Pin",
                                tint = Color(0xFFEF4444)
                            )
                        }

                        IconButton(
                            onClick = { onSelectPoint(null) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawSurveyPolygonsAndLines(
    points: List<HudTargetPoint>,
    w: Float,
    h: Float,
    primaryColor: Color,
    secondaryColor: Color,
    currentRangeMeters: Float
) {
    if (points.isEmpty()) return

    val strokeWidth = 2.dp.toPx()
    val laserDash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f)

    // Draw enclosed translucent polygon if >= 3 points
    if (points.size >= 3) {
        val polyPath = Path().apply {
            moveTo(points[0].normX * w, points[0].normY * h)
            for (i in 1 until points.size) {
                lineTo(points[i].normX * w, points[i].normY * h)
            }
            close()
        }
        drawPath(
            path = polyPath,
            color = primaryColor.copy(alpha = 0.12f)
        )
        drawPath(
            path = polyPath,
            color = primaryColor.copy(alpha = 0.45f),
            style = Stroke(width = 1.2.dp.toPx(), pathEffect = laserDash)
        )
    }

    // Draw laser lines between consecutive points
    for (i in 0 until points.size - 1) {
        val p1 = points[i]
        val p2 = points[i + 1]
        val start = Offset(p1.normX * w, p1.normY * h)
        val end = Offset(p2.normX * w, p2.normY * h)

        // Glowing under-line
        drawLine(
            color = primaryColor.copy(alpha = 0.35f),
            start = start,
            end = end,
            strokeWidth = strokeWidth * 2.5f,
            cap = StrokeCap.Round
        )

        // Crisp laser survey line
        drawLine(
            color = secondaryColor.copy(alpha = 0.9f),
            start = start,
            end = end,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }

    // Connect last point to first if >= 3
    if (points.size >= 3) {
        val pLast = points.last()
        val pFirst = points.first()
        val start = Offset(pLast.normX * w, pLast.normY * h)
        val end = Offset(pFirst.normX * w, pFirst.normY * h)

        drawLine(
            color = primaryColor.copy(alpha = 0.5f),
            start = start,
            end = end,
            strokeWidth = strokeWidth,
            pathEffect = laserDash
        )
    }
}
