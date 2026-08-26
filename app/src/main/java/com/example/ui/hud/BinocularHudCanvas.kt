package com.example.ui.hud

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.model.HudFilterMode
import com.example.model.JobMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BinocularHudCanvas(
    jobMode: JobMode,
    filterMode: HudFilterMode,
    pitchDeg: Float,
    rollDeg: Float,
    azimuthDeg: Float,
    laserRangerActive: Boolean,
    isHudGridVisible: Boolean,
    isHorizonVisible: Boolean,
    isMilScaleVisible: Boolean,
    zoomLevel: Float,
    isCrosshairAligned: Boolean = false,
    alignedEdgeName: String? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_anim")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    val primaryColor = when (filterMode) {
        HudFilterMode.TACTICAL_OPTIC -> jobMode.primaryColor
        HudFilterMode.NIGHT_VISION -> Color(0xFF22C55E) // NVG Green
        HudFilterMode.FLIR_THERMAL -> Color(0xFFFF5722) // FLIR Heat Orange
        HudFilterMode.BLUEPRINT -> Color(0xFF60A5FA) // Blueprint Blue
    }

    val secondaryColor = when (filterMode) {
        HudFilterMode.TACTICAL_OPTIC -> jobMode.secondaryColor
        HudFilterMode.NIGHT_VISION -> Color(0xFF86EFAC)
        HudFilterMode.FLIR_THERMAL -> Color(0xFFFDE047)
        HudFilterMode.BLUEPRINT -> Color(0xFF93C5FD)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // 1. Draw Binocular Dual-Aperture Vignette & Darkened Perimeters
        drawBinocularVignette(w, h, cx, cy, primaryColor, filterMode)

        // 2. Draw Corner Tactical Brackets & Framing
        drawCornerBrackets(w, h, primaryColor)

        // 3. Top Azimuth / Compass Tape
        if (isMilScaleVisible) {
            drawAzimuthCompassTape(w, cx, azimuthDeg, primaryColor, secondaryColor)
            drawVerticalElevationScale(w, h, cy, pitchDeg, primaryColor, secondaryColor)
        }

        // 4. Alignment Grid (Mode-specific)
        if (isHudGridVisible) {
            drawAlignmentGrid(w, h, cx, cy, jobMode, primaryColor)
        }

        // 5. Artificial Horizon and Pitch Ladder (Rotates with Roll, Translates with Pitch)
        if (isHorizonVisible) {
            drawArtificialHorizon(w, h, cx, cy, pitchDeg, rollDeg, primaryColor, secondaryColor)
        }

        // 6. Tactical Center Reticle & Laser Crosshairs
        drawCenterTacticalReticle(
            cx = cx,
            cy = cy,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            pulseAlpha = pulseAlpha,
            sweepAngle = sweepAngle,
            laserRangerActive = laserRangerActive,
            jobMode = jobMode,
            zoomLevel = zoomLevel,
            isCrosshairAligned = isCrosshairAligned,
            alignedEdgeName = alignedEdgeName
        )
    }
}

private fun DrawScope.drawBinocularVignette(
    w: Float,
    h: Float,
    cx: Float,
    cy: Float,
    accentColor: Color,
    filterMode: HudFilterMode
) {
    // Binocular Dual Optics Radii
    val binocularRadius = (h * 0.46f).coerceAtLeast(w * 0.38f)
    val separation = (w * 0.16f).coerceAtMost(binocularRadius * 0.6f)
    val leftCenterX = cx - separation
    val rightCenterX = cx + separation

    // Ambient night/thermal/optic tint over viewport
    when (filterMode) {
        HudFilterMode.NIGHT_VISION -> {
            drawRect(
                color = Color(0x28052E16),
                size = size
            )
        }
        HudFilterMode.FLIR_THERMAL -> {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x307F1D1D), Color(0x183B0764), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = binocularRadius * 1.5f
                )
            )
        }
        HudFilterMode.BLUEPRINT -> {
            drawRect(
                color = Color(0x221E3A8A),
                size = size
            )
        }
        else -> {}
    }

    // Outer framing subtle rings around binocular lenses
    drawCircle(
        color = accentColor.copy(alpha = 0.22f),
        radius = binocularRadius,
        center = Offset(leftCenterX, cy),
        style = Stroke(width = 2.dp.toPx())
    )
    drawCircle(
        color = accentColor.copy(alpha = 0.22f),
        radius = binocularRadius,
        center = Offset(rightCenterX, cy),
        style = Stroke(width = 2.dp.toPx())
    )

    // Mil-marked outer perimeter ticks on left and right lens circles
    val tickRadius = binocularRadius - 8.dp.toPx()
    for (angle in 0 until 360 step 15) {
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)
        val isMajor = angle % 45 == 0
        val tickLen = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
        val tickAlpha = if (isMajor) 0.5f else 0.25f

        // Left lens ticks (outer half)
        if (cosA < 0.2f) {
            val start = Offset(leftCenterX + (tickRadius - tickLen) * cosA, cy + (tickRadius - tickLen) * sinA)
            val end = Offset(leftCenterX + tickRadius * cosA, cy + tickRadius * sinA)
            drawLine(
                color = accentColor.copy(alpha = tickAlpha),
                start = start,
                end = end,
                strokeWidth = if (isMajor) 1.8.dp.toPx() else 1.dp.toPx()
            )
        }

        // Right lens ticks (outer half)
        if (cosA > -0.2f) {
            val start = Offset(rightCenterX + (tickRadius - tickLen) * cosA, cy + (tickRadius - tickLen) * sinA)
            val end = Offset(rightCenterX + tickRadius * cosA, cy + tickRadius * sinA)
            drawLine(
                color = accentColor.copy(alpha = tickAlpha),
                start = start,
                end = end,
                strokeWidth = if (isMajor) 1.8.dp.toPx() else 1.dp.toPx()
            )
        }
    }
}

private fun DrawScope.drawCornerBrackets(w: Float, h: Float, color: Color) {
    val bracketSize = 28.dp.toPx()
    val padX = 16.dp.toPx()
    val padY = 56.dp.toPx()
    val stroke = 2.dp.toPx()
    val bracketAlpha = 0.7f

    // Top-Left
    val pTL = Path().apply {
        moveTo(padX, padY + bracketSize)
        lineTo(padX, padY)
        lineTo(padX + bracketSize, padY)
    }
    drawPath(pTL, color.copy(alpha = bracketAlpha), style = Stroke(stroke))

    // Top-Right
    val pTR = Path().apply {
        moveTo(w - padX - bracketSize, padY)
        lineTo(w - padX, padY)
        lineTo(w - padX, padY + bracketSize)
    }
    drawPath(pTR, color.copy(alpha = bracketAlpha), style = Stroke(stroke))

    // Bottom-Left
    val pBL = Path().apply {
        moveTo(padX, h - padY - bracketSize)
        lineTo(padX, h - padY)
        lineTo(padX + bracketSize, h - padY)
    }
    drawPath(pBL, color.copy(alpha = bracketAlpha), style = Stroke(stroke))

    // Bottom-Right
    val pBR = Path().apply {
        moveTo(w - padX - bracketSize, h - padY)
        lineTo(w - padX, h - padY)
        lineTo(w - padX, h - padY - bracketSize)
    }
    drawPath(pBR, color.copy(alpha = bracketAlpha), style = Stroke(stroke))
}

private fun DrawScope.drawAzimuthCompassTape(
    w: Float,
    cx: Float,
    azimuthDeg: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val topY = 76.dp.toPx()
    val tapeWidth = w * 0.72f
    val startX = cx - tapeWidth / 2f
    val endX = cx + tapeWidth / 2f

    // Baseline
    drawLine(
        color = primaryColor.copy(alpha = 0.35f),
        start = Offset(startX, topY),
        end = Offset(endX, topY),
        strokeWidth = 1.2.dp.toPx()
    )

    // Center indicator triangle
    val triPath = Path().apply {
        moveTo(cx, topY + 8.dp.toPx())
        lineTo(cx - 5.dp.toPx(), topY + 16.dp.toPx())
        lineTo(cx + 5.dp.toPx(), topY + 16.dp.toPx())
        close()
    }
    drawPath(triPath, color = secondaryColor)

    // Degree Ticks along the azimuth line
    val degStepPx = (tapeWidth / 60f) // 60 degrees visible across tape
    for (offsetDeg in -30..30 step 5) {
        val currentDeg = ((azimuthDeg + offsetDeg + 360) % 360).toInt()
        val x = cx + offsetDeg * degStepPx
        if (x in (startX + 10f)..(endX - 10f)) {
            val isMajor = currentDeg % 15 == 0
            val tickHeight = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
            drawLine(
                color = if (isMajor) secondaryColor.copy(alpha = 0.8f) else primaryColor.copy(alpha = 0.4f),
                start = Offset(x, topY),
                end = Offset(x, topY - tickHeight),
                strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
            )
        }
    }
}

private fun DrawScope.drawVerticalElevationScale(
    w: Float,
    h: Float,
    cy: Float,
    pitchDeg: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val rightX = w - 24.dp.toPx()
    val scaleHeight = h * 0.40f
    val topY = cy - scaleHeight / 2f
    val bottomY = cy + scaleHeight / 2f

    drawLine(
        color = primaryColor.copy(alpha = 0.35f),
        start = Offset(rightX, topY),
        end = Offset(rightX, bottomY),
        strokeWidth = 1.2.dp.toPx()
    )

    // Center pitch marker
    val triPath = Path().apply {
        moveTo(rightX - 8.dp.toPx(), cy)
        lineTo(rightX - 16.dp.toPx(), cy - 5.dp.toPx())
        lineTo(rightX - 16.dp.toPx(), cy + 5.dp.toPx())
        close()
    }
    drawPath(triPath, color = secondaryColor)

    val stepPx = (scaleHeight / 40f) // 40 degrees visible
    for (offsetDeg in -20..20 step 5) {
        val y = cy - (offsetDeg - pitchDeg * 0.5f) * stepPx
        if (y in topY..bottomY) {
            val isMajor = offsetDeg % 10 == 0
            val tickLen = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
            drawLine(
                color = if (isMajor) secondaryColor.copy(alpha = 0.8f) else primaryColor.copy(alpha = 0.4f),
                start = Offset(rightX, y),
                end = Offset(rightX - tickLen, y),
                strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
            )
        }
    }
}

private fun DrawScope.drawAlignmentGrid(
    w: Float,
    h: Float,
    cx: Float,
    cy: Float,
    jobMode: JobMode,
    primaryColor: Color
) {
    val gridEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 8.dp.toPx()), 0f)
    val gridAlpha = 0.16f

    // Grid spacing
    val step = when (jobMode) {
        JobMode.PAINTING -> 48.dp.toPx()
        JobMode.PLASTERING -> 36.dp.toPx()
        JobMode.SCREEDING -> 40.dp.toPx()
    }

    // Vertical grid lines
    var x = cx - step * 3
    while (x <= cx + step * 3) {
        if (x >= 20f && x <= w - 20f) {
            drawLine(
                color = primaryColor.copy(alpha = gridAlpha),
                start = Offset(x, cy - step * 3.5f),
                end = Offset(x, cy + step * 3.5f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = gridEffect
            )
        }
        x += step
    }

    // Horizontal grid lines
    var y = cy - step * 3
    while (y <= cy + step * 3) {
        if (y >= 80f && y <= h - 80f) {
            drawLine(
                color = primaryColor.copy(alpha = gridAlpha),
                start = Offset(cx - step * 3.5f, y),
                end = Offset(cx + step * 3.5f, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = gridEffect
            )
        }
        y += step
    }
}

private fun DrawScope.drawArtificialHorizon(
    w: Float,
    h: Float,
    cx: Float,
    cy: Float,
    pitchDeg: Float,
    rollDeg: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    // Artificial Horizon responds to roll (rotation) and pitch (vertical offset)
    val pitchOffsetY = (pitchDeg * 3.5.dp.toPx()).coerceIn(-h * 0.25f, h * 0.25f)

    rotate(degrees = rollDeg, pivot = Offset(cx, cy)) {
        translate(top = pitchOffsetY) {
            val horizonWidth = 140.dp.toPx()
            val gap = 36.dp.toPx()

            // Left Horizon Line
            drawLine(
                color = secondaryColor.copy(alpha = 0.75f),
                start = Offset(cx - horizonWidth, cy),
                end = Offset(cx - gap, cy),
                strokeWidth = 2.dp.toPx()
            )
            // Left downward vertical tick
            drawLine(
                color = secondaryColor.copy(alpha = 0.75f),
                start = Offset(cx - horizonWidth, cy),
                end = Offset(cx - horizonWidth, cy + 10.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )

            // Right Horizon Line
            drawLine(
                color = secondaryColor.copy(alpha = 0.75f),
                start = Offset(cx + gap, cy),
                end = Offset(cx + horizonWidth, cy),
                strokeWidth = 2.dp.toPx()
            )
            // Right downward vertical tick
            drawLine(
                color = secondaryColor.copy(alpha = 0.75f),
                start = Offset(cx + horizonWidth, cy),
                end = Offset(cx + horizonWidth, cy + 10.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )

            // Pitch Ladder rungs (+10°, -10°)
            val rung10Offset = 40.dp.toPx()
            val rungWidth = 50.dp.toPx()

            // +10 Deg Rung
            val rungDash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f)
            drawLine(
                color = primaryColor.copy(alpha = 0.45f),
                start = Offset(cx - rungWidth, cy - rung10Offset),
                end = Offset(cx - gap * 0.6f, cy - rung10Offset),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = primaryColor.copy(alpha = 0.45f),
                start = Offset(cx + gap * 0.6f, cy - rung10Offset),
                end = Offset(cx + rungWidth, cy - rung10Offset),
                strokeWidth = 1.5.dp.toPx()
            )

            // -10 Deg Rung (dashed for negative pitch)
            drawLine(
                color = primaryColor.copy(alpha = 0.45f),
                start = Offset(cx - rungWidth, cy + rung10Offset),
                end = Offset(cx - gap * 0.6f, cy + rung10Offset),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = rungDash
            )
            drawLine(
                color = primaryColor.copy(alpha = 0.45f),
                start = Offset(cx + gap * 0.6f, cy + rung10Offset),
                end = Offset(cx + rungWidth, cy + rung10Offset),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = rungDash
            )
        }
    }
}

private fun DrawScope.drawCenterTacticalReticle(
    cx: Float,
    cy: Float,
    primaryColor: Color,
    secondaryColor: Color,
    pulseAlpha: Float,
    sweepAngle: Float,
    laserRangerActive: Boolean,
    jobMode: JobMode,
    zoomLevel: Float,
    isCrosshairAligned: Boolean = false,
    alignedEdgeName: String? = null
) {
    val reticleRadius = 42.dp.toPx()

    // Alignment Locked Glow & Target Framing Box
    if (isCrosshairAligned) {
        val lockSize = 28.dp.toPx()
        drawRect(
            color = Color(0xFF00FF41).copy(alpha = 0.25f),
            topLeft = Offset(cx - lockSize, cy - lockSize),
            size = Size(lockSize * 2, lockSize * 2)
        )
        drawRect(
            color = Color(0xFF00FF41),
            topLeft = Offset(cx - lockSize, cy - lockSize),
            size = Size(lockSize * 2, lockSize * 2),
            style = Stroke(width = 2.dp.toPx())
        )
    }

    // Outer reticle circle with segment gaps
    for (i in 0 until 4) {
        val startA = i * 90f + 12f
        val sweepA = 66f
        drawArc(
            color = if (isCrosshairAligned) Color(0xFF00FF41) else primaryColor.copy(alpha = 0.55f),
            startAngle = startA,
            sweepAngle = sweepA,
            useCenter = false,
            topLeft = Offset(cx - reticleRadius, cy - reticleRadius),
            size = Size(reticleRadius * 2, reticleRadius * 2),
            style = Stroke(width = if (isCrosshairAligned) 2.2.dp.toPx() else 1.5.dp.toPx())
        )
    }

    // Inner targeting crosshairs with center open gap
    val crossHairLen = 26.dp.toPx()
    val centerGap = 9.dp.toPx()

    // Up
    drawLine(
        color = secondaryColor.copy(alpha = 0.85f),
        start = Offset(cx, cy - centerGap - crossHairLen),
        end = Offset(cx, cy - centerGap),
        strokeWidth = 1.8.dp.toPx()
    )
    // Down
    drawLine(
        color = secondaryColor.copy(alpha = 0.85f),
        start = Offset(cx, cy + centerGap),
        end = Offset(cx, cy + centerGap + crossHairLen),
        strokeWidth = 1.8.dp.toPx()
    )
    // Left
    drawLine(
        color = secondaryColor.copy(alpha = 0.85f),
        start = Offset(cx - centerGap - crossHairLen, cy),
        end = Offset(cx - centerGap, cy),
        strokeWidth = 1.8.dp.toPx()
    )
    // Right
    drawLine(
        color = secondaryColor.copy(alpha = 0.85f),
        start = Offset(cx + centerGap, cy),
        end = Offset(cx + centerGap + crossHairLen, cy),
        strokeWidth = 1.8.dp.toPx()
    )

    // Mode-specific reticle embellishment
    when (jobMode) {
        JobMode.PAINTING -> {
            // Coating spread ring
            val spreadRadius = 24.dp.toPx()
            drawCircle(
                color = primaryColor.copy(alpha = 0.35f),
                radius = spreadRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f))
            )
        }
        JobMode.PLASTERING -> {
            // Plumb cross & square guide
            val boxSize = 16.dp.toPx()
            drawRect(
                color = primaryColor.copy(alpha = 0.35f),
                topLeft = Offset(cx - boxSize, cy - boxSize),
                size = Size(boxSize * 2, boxSize * 2),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        JobMode.SCREEDING -> {
            // Datum diamond
            val diamondSize = 18.dp.toPx()
            val dPath = Path().apply {
                moveTo(cx, cy - diamondSize)
                lineTo(cx + diamondSize, cy)
                lineTo(cx, cy + diamondSize)
                lineTo(cx - diamondSize, cy)
                close()
            }
            drawPath(dPath, color = primaryColor.copy(alpha = 0.35f), style = Stroke(width = 1.dp.toPx()))
        }
    }

    // Active Laser Rangefinder Dot & Pulsing Lock Ring
    if (laserRangerActive) {
        // Center laser dot (Bright pulsing beam)
        drawCircle(
            color = Color(0xFFEF4444).copy(alpha = pulseAlpha), // Red laser core
            radius = 3.2.dp.toPx(),
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFFEF4444).copy(alpha = pulseAlpha * 0.4f),
            radius = 8.dp.toPx(),
            center = Offset(cx, cy)
        )

        // Rotating tactical lock-on arc
        drawArc(
            color = secondaryColor.copy(alpha = 0.6f),
            startAngle = sweepAngle,
            sweepAngle = 45f,
            useCenter = false,
            topLeft = Offset(cx - (reticleRadius + 8.dp.toPx()), cy - (reticleRadius + 8.dp.toPx())),
            size = Size((reticleRadius + 8.dp.toPx()) * 2, (reticleRadius + 8.dp.toPx()) * 2),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = secondaryColor.copy(alpha = 0.6f),
            startAngle = sweepAngle + 180f,
            sweepAngle = 45f,
            useCenter = false,
            topLeft = Offset(cx - (reticleRadius + 8.dp.toPx()), cy - (reticleRadius + 8.dp.toPx())),
            size = Size((reticleRadius + 8.dp.toPx()) * 2, (reticleRadius + 8.dp.toPx()) * 2),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
