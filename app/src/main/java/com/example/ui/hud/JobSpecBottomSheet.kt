package com.example.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.JobMode
import com.example.viewmodel.HudUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobSpecBottomSheet(
    uiState: HudUiState,
    onDismiss: () -> Unit,
    onSavePainting: (coverage: Float, coats: Float, wetMil: Float) -> Unit,
    onSavePlastering: (depthMm: Float, mixRatio: Float, plumbTol: Float) -> Unit,
    onSaveScreeding: (depthMm: Float, fallGradient: Float, toleranceMm: Float) -> Unit
) {
    val mode = uiState.activeJobMode
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var paintCoverage by remember(uiState) { mutableFloatStateOf(uiState.paintingCoverageRate) }
    var paintCoats by remember(uiState) { mutableFloatStateOf(uiState.paintingCoats) }
    var paintWetMil by remember(uiState) { mutableFloatStateOf(uiState.paintingWetMil) }

    var plasterDepth by remember(uiState) { mutableFloatStateOf(uiState.plasteringDepthMm) }
    var plasterMix by remember(uiState) { mutableFloatStateOf(uiState.plasteringMixRatio) }
    var plasterTol by remember(uiState) { mutableFloatStateOf(uiState.plasteringPlumbTol) }

    var screedDepth by remember(uiState) { mutableFloatStateOf(uiState.screedingDepthMm) }
    var screedFall by remember(uiState) { mutableFloatStateOf(uiState.screedingFallGradient) }
    var screedTol by remember(uiState) { mutableFloatStateOf(uiState.screedingToleranceMm) }

    val primaryColor = mode.primaryColor

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0A0C0B),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF262C2A))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${mode.title} PARAMETERS",
                        color = primaryColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = mode.subtitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            // Mode-specific sliders
            when (mode) {
                JobMode.PAINTING -> {
                    SpecSliderRow(
                        title = "COVERAGE RATE",
                        valueStr = "%.1f m²/L".format(paintCoverage),
                        value = paintCoverage,
                        range = 6.0f..18.0f,
                        steps = 24,
                        accentColor = primaryColor,
                        onValueChange = { paintCoverage = it }
                    )
                    SpecSliderRow(
                        title = "COAT LAYERS",
                        valueStr = "%.0f Coats".format(paintCoats),
                        value = paintCoats,
                        range = 1.0f..4.0f,
                        steps = 2,
                        accentColor = primaryColor,
                        onValueChange = { paintCoats = it }
                    )
                    SpecSliderRow(
                        title = "EST. WET FILM THICKNESS",
                        valueStr = "%.0f µm (%.1f mils)".format(paintWetMil, paintWetMil / 25.4f),
                        value = paintWetMil,
                        range = 60.0f..250.0f,
                        steps = 19,
                        accentColor = primaryColor,
                        onValueChange = { paintWetMil = it }
                    )
                }
                JobMode.PLASTERING -> {
                    SpecSliderRow(
                        title = "RENDER BASE DEPTH",
                        valueStr = "%.1f mm".format(plasterDepth),
                        value = plasterDepth,
                        range = 5.0f..30.0f,
                        steps = 25,
                        accentColor = primaryColor,
                        onValueChange = { plasterDepth = it }
                    )
                    SpecSliderRow(
                        title = "MIX RATIO (BINDER : SAND)",
                        valueStr = "1 : %.0f parts".format(plasterMix),
                        value = plasterMix,
                        range = 2.0f..6.0f,
                        steps = 3,
                        accentColor = primaryColor,
                        onValueChange = { plasterMix = it }
                    )
                    SpecSliderRow(
                        title = "PLUMB TOLERANCE",
                        valueStr = "±%.1f mm/m".format(plasterTol),
                        value = plasterTol,
                        range = 0.5f..5.0f,
                        steps = 9,
                        accentColor = primaryColor,
                        onValueChange = { plasterTol = it }
                    )
                }
                JobMode.SCREEDING -> {
                    SpecSliderRow(
                        title = "SCREED BED THICKNESS",
                        valueStr = "%.0f mm".format(screedDepth),
                        value = screedDepth,
                        range = 25.0f..120.0f,
                        steps = 19,
                        accentColor = primaryColor,
                        onValueChange = { screedDepth = it }
                    )
                    SpecSliderRow(
                        title = "FALL / DRAINAGE GRADIENT",
                        valueStr = "%.2f %% (1 : %.0f)".format(screedFall, if (screedFall > 0) (100f / screedFall) else 0f),
                        value = screedFall,
                        range = 0.5f..4.0f,
                        steps = 14,
                        accentColor = primaryColor,
                        onValueChange = { screedFall = it }
                    )
                    SpecSliderRow(
                        title = "DATUM LEVEL TOLERANCE",
                        valueStr = "±%.0f mm (SR2)".format(screedTol),
                        value = screedTol,
                        range = 1.0f..10.0f,
                        steps = 9,
                        accentColor = primaryColor,
                        onValueChange = { screedTol = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Apply Button
            Button(
                onClick = {
                    when (mode) {
                        JobMode.PAINTING -> onSavePainting(paintCoverage, paintCoats, paintWetMil)
                        JobMode.PLASTERING -> onSavePlastering(plasterDepth, plasterMix, plasterTol)
                        JobMode.SCREEDING -> onSaveScreeding(screedDepth, screedFall, screedTol)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_job_specs_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF0A0C0B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "APPLY CALIBRATION",
                    color = Color(0xFF0A0C0B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SpecSliderRow(
    title: String,
    valueStr: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111413), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF262C2A), RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = valueStr,
                color = accentColor,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(0xFF262C2A)
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
