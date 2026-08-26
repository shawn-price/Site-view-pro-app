package com.example.ui.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HudFilterMode
import com.example.model.JobMode
import com.example.viewmodel.HudUiState

@Composable
fun TopTacticalStatusHeader(
    uiState: HudUiState,
    pitchDeg: Float,
    rollDeg: Float,
    azimuthDeg: Float,
    onBatteryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeMode = uiState.activeJobMode
    val primaryColor by animateColorAsState(
        targetValue = activeMode.primaryColor,
        animationSpec = tween(300),
        label = "primaryColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Hardware Header: Live Status + Coordinates + Battery/Signal Telemetry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Rec Indicator, System ID & Coordinates
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626).copy(alpha = pulseAlpha))
                    )
                    Text(
                        text = "SITEVIEW PRO // ${activeMode.title}",
                        color = primaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                }

                Text(
                    text = "CAM_01.EXE_LIVE",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Box(
                    modifier = Modifier
                        .background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                        .border(1.dp, primaryColor.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "51°30'26.5\"N 0°07'39.9\"W",
                        color = primaryColor,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Right: Battery Indicator, Signal & Laser Rangefinder Status
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tactical Battery Indicator
                    TacticalBatteryIndicator(
                        percent = uiState.batteryPercent,
                        isCharging = uiState.isBatteryCharging,
                        voltage = uiState.batteryVoltage,
                        primaryColor = primaryColor,
                        onClick = onBatteryClick
                    )

                    // Signal Telemetry
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SIG",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "LTE.4",
                            color = primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Laser Rangefinder Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .background(Color(0xFF0A0C0B), RoundedCornerShape(4.dp))
                        .border(1.dp, if (uiState.laserRangerActive) Color(0xFFEF4444) else Color(0xFF262C2A), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (uiState.laserRangerActive) Color(0xFFEF4444) else Color(0xFF64748B))
                    )
                    Text(
                        text = if (uiState.laserRangerActive) "LRF: %.2fm".format(uiState.currentRangeMeters) else "LRF: STBY",
                        color = if (uiState.laserRangerActive) Color(0xFFFCA5A5) else Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Secondary Telemetry Bar: Azimuth, Pitch, Roll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val cardinal = getCardinalDirection(azimuthDeg)
            Text(
                text = "AZ: %03.0f° %s | PIT: %+.1f° | ROL: %+.1f°".format(azimuthDeg, cardinal, pitchDeg, rollDeg),
                color = Color(0xFFCBD5E1),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "48m EL • SAT: 11",
                color = Color(0xFF64748B),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Tactical Graphical Battery Indicator for Military/Industrial HUD.
 * Renders custom battery chassis, cathode node, dynamic color level fill,
 * charging bolt icon, and voltage telemetry.
 */
@Composable
fun TacticalBatteryIndicator(
    percent: Int,
    isCharging: Boolean,
    voltage: Float,
    primaryColor: Color,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bat_blink")
    val warningBlink by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bat_warn"
    )

    val batteryColor = when {
        isCharging -> Color(0xFF00E5FF)
        percent > 50 -> primaryColor
        percent in 21..50 -> Color(0xFFFFB800)
        else -> Color(0xFFEF4444).copy(alpha = warningBlink)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 3.dp, vertical = 1.dp)
            .testTag("tactical_battery_indicator")
    ) {
        // Battery Canvas Drawing
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(11.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(22.dp, 11.dp)) {
                val w = size.width
                val h = size.height

                val bodyWidth = w - 3.dp.toPx()
                val bodyHeight = h

                // 1. Draw outer chassis body
                drawRoundRect(
                    color = batteryColor.copy(alpha = 0.5f),
                    topLeft = Offset(0f, 0f),
                    size = Size(bodyWidth, bodyHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // 2. Draw cathode terminal cap on right side
                val capWidth = 2.dp.toPx()
                val capHeight = bodyHeight * 0.45f
                val capTop = (bodyHeight - capHeight) / 2f
                drawRoundRect(
                    color = batteryColor.copy(alpha = 0.7f),
                    topLeft = Offset(bodyWidth + 0.5f, capTop),
                    size = Size(capWidth, capHeight),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )

                // 3. Draw charge fill bar inside body
                val padding = 1.8.dp.toPx()
                val maxFillWidth = bodyWidth - (padding * 2)
                val fillWidth = (maxFillWidth * (percent.coerceIn(0, 100) / 100f)).coerceAtLeast(1.5f)
                val fillHeight = bodyHeight - (padding * 2)

                drawRoundRect(
                    color = batteryColor,
                    topLeft = Offset(padding, padding),
                    size = Size(fillWidth, fillHeight),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
            }

            // Charging Bolt Overlay
            if (isCharging) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Charging",
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        // Percentage & Label
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "$percent%",
                    color = batteryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun BottomDynamicTelemetryCard(
    uiState: HudUiState,
    modifier: Modifier = Modifier
) {
    val activeMode = uiState.activeJobMode
    val primaryColor by animateColorAsState(
        targetValue = activeMode.primaryColor,
        animationSpec = tween(300),
        label = "primaryColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = Color(0xFF0A0C0B).copy(alpha = 0.95f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Hardware Style Active Target & Live Metric Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color.Transparent
                    )
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "ACTIVE TARGET",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (uiState.targetPoints.size >= 3) "SURVEY_POLY_${uiState.targetPoints.size}P" else uiState.jobCalculationSummary,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${activeMode.metricPrimaryLabel} ${activeMode.metricPrimaryUnit}".uppercase(),
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = when (activeMode) {
                            JobMode.PAINTING -> "%.2f".format(uiState.paintingCoverageRate)
                            JobMode.PLASTERING -> "%.1f".format(uiState.plasteringDepthMm)
                            JobMode.SCREEDING -> "%.0f".format(uiState.screedingDepthMm)
                        },
                        color = primaryColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Divider Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(primaryColor.copy(alpha = 0.25f))
            )

            // Mode-specific Metric Trio Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (activeMode) {
                    JobMode.PAINTING -> {
                        MetricBadge(
                            label = activeMode.metricPrimaryLabel,
                            value = "%.1f".format(uiState.paintingCoverageRate),
                            unit = activeMode.metricPrimaryUnit,
                            accentColor = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadge(
                            label = activeMode.metricSecondaryLabel,
                            value = "%.0f".format(uiState.paintingCoats),
                            unit = activeMode.metricSecondaryUnit,
                            accentColor = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadge(
                            label = activeMode.metricTertiaryLabel,
                            value = "%.0f".format(uiState.paintingWetMil),
                            unit = activeMode.metricTertiaryUnit,
                            accentColor = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    JobMode.PLASTERING -> {
                        MetricBadge(
                            label = activeMode.metricPrimaryLabel,
                            value = "%.1f".format(uiState.plasteringDepthMm),
                            unit = activeMode.metricPrimaryUnit,
                            accentColor = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadge(
                            label = activeMode.metricSecondaryLabel,
                            value = "1:%.0f".format(uiState.plasteringMixRatio),
                            unit = activeMode.metricSecondaryUnit,
                            accentColor = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadge(
                            label = activeMode.metricTertiaryLabel,
                            value = "±%.1f".format(uiState.plasteringPlumbTol),
                            unit = activeMode.metricTertiaryUnit,
                            accentColor = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    JobMode.SCREEDING -> {
                        MetricBadge(
                            label = activeMode.metricPrimaryLabel,
                            value = "%.0f".format(uiState.screedingDepthMm),
                            unit = activeMode.metricPrimaryUnit,
                            accentColor = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadge(
                            label = activeMode.metricSecondaryLabel,
                            value = "%.2f".format(uiState.screedingFallGradient),
                            unit = activeMode.metricSecondaryUnit,
                            accentColor = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadge(
                            label = activeMode.metricTertiaryLabel,
                            value = "±%.0f".format(uiState.screedingToleranceMm),
                            unit = activeMode.metricTertiaryUnit,
                            accentColor = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBadge(
    label: String,
    value: String,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF111413), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF262C2A), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            color = Color(0xFF94A3B8),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = unit,
                color = Color(0xFF64748B),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 1.dp)
            )
        }
    }
}

@Composable
fun TacticalSideControlRail(
    uiState: HudUiState,
    onSetZoom: (Float) -> Unit,
    onSetFilter: (HudFilterMode) -> Unit,
    onToggleTorch: () -> Unit,
    onToggleLaserRanger: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleHorizon: () -> Unit,
    onUndoPin: () -> Unit,
    onClearPins: () -> Unit,
    onCaptureSnapshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = uiState.activeJobMode.primaryColor

    Column(
        modifier = modifier
            .background(Color(0xD00A0C0B), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF262C2A), RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Magnification Selector
        val zoomLevels = listOf(1.0f, 2.0f, 5.0f)
        val nextZoom = when (uiState.zoomLevel) {
            1.0f -> 2.0f
            2.0f -> 5.0f
            else -> 1.0f
        }
        TacticalIconButton(
            label = "%.0fX".format(uiState.zoomLevel),
            isActive = uiState.zoomLevel > 1.0f,
            activeColor = primaryColor,
            onClick = { onSetZoom(nextZoom) },
            testTag = "zoom_toggle_button"
        )

        // Optical Filter Selector (Cycles Optic -> NVG -> FLIR -> Blueprint)
        val nextFilter = when (uiState.filterMode) {
            HudFilterMode.TACTICAL_OPTIC -> HudFilterMode.NIGHT_VISION
            HudFilterMode.NIGHT_VISION -> HudFilterMode.FLIR_THERMAL
            HudFilterMode.FLIR_THERMAL -> HudFilterMode.BLUEPRINT
            HudFilterMode.BLUEPRINT -> HudFilterMode.TACTICAL_OPTIC
        }
        TacticalIconButton(
            label = uiState.filterMode.badge,
            isActive = uiState.filterMode != HudFilterMode.TACTICAL_OPTIC,
            activeColor = when (uiState.filterMode) {
                HudFilterMode.NIGHT_VISION -> Color(0xFF00FF41)
                HudFilterMode.FLIR_THERMAL -> Color(0xFFFF5722)
                HudFilterMode.BLUEPRINT -> Color(0xFF00E5FF)
                else -> primaryColor
            },
            onClick = { onSetFilter(nextFilter) },
            testTag = "filter_toggle_button"
        )

        // Laser Rangefinder Toggle
        TacticalIconButton(
            icon = Icons.Default.Straighten,
            isActive = uiState.laserRangerActive,
            activeColor = Color(0xFFEF4444),
            onClick = onToggleLaserRanger,
            testTag = "laser_ranger_button"
        )

        // Torch Toggle
        TacticalIconButton(
            icon = if (uiState.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
            isActive = uiState.isTorchOn,
            activeColor = Color(0xFFFFD000),
            onClick = onToggleTorch,
            testTag = "torch_toggle_button"
        )

        // Grid Toggle
        TacticalIconButton(
            icon = Icons.Default.GridOn,
            isActive = uiState.isHudGridVisible,
            activeColor = primaryColor,
            onClick = onToggleGrid,
            testTag = "grid_toggle_button"
        )

        // Undo Pin
        TacticalIconButton(
            icon = Icons.Default.Undo,
            isActive = uiState.targetPoints.isNotEmpty(),
            activeColor = Color(0xFFE2E8F0),
            onClick = onUndoPin,
            testTag = "undo_pin_button"
        )
    }
}

@Composable
private fun TacticalIconButton(
    icon: ImageVector? = null,
    label: String? = null,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.15f) else Color(0xFF111413))
            .border(
                width = 1.dp,
                color = if (isActive) activeColor else Color(0xFF262C2A),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) activeColor else Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        } else if (label != null) {
            Text(
                text = label,
                color = if (isActive) activeColor else Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun getCardinalDirection(deg: Float): String {
    val d = (deg + 360) % 360
    return when {
        d >= 337.5 || d < 22.5 -> "N"
        d < 67.5 -> "NE"
        d < 112.5 -> "E"
        d < 157.5 -> "SE"
        d < 202.5 -> "S"
        d < 247.5 -> "SW"
        d < 292.5 -> "W"
        else -> "NW"
    }
}

