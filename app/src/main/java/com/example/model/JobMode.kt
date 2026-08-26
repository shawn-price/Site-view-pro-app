package com.example.model

import androidx.compose.ui.graphics.Color

enum class JobMode(
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color,
    val metricPrimaryLabel: String,
    val metricPrimaryUnit: String,
    val metricSecondaryLabel: String,
    val metricSecondaryUnit: String,
    val metricTertiaryLabel: String,
    val metricTertiaryUnit: String,
    val defaultPrimaryValue: Float,
    val defaultSecondaryValue: Float,
    val defaultTertiaryValue: Float
) {
    PAINTING(
        title = "PAINTING",
        subtitle = "Surface Coverage & Wet Mil Survey",
        primaryColor = Color(0xFF00FF41), // Specialist Matrix Green
        secondaryColor = Color(0xFF4DFF79),
        glowColor = Color(0x6600FF41),
        metricPrimaryLabel = "Coverage Rate",
        metricPrimaryUnit = "m²/L",
        metricSecondaryLabel = "Coats Required",
        metricSecondaryUnit = "layers",
        metricTertiaryLabel = "Est. Wet Film",
        metricTertiaryUnit = "µm",
        defaultPrimaryValue = 10.5f,
        defaultSecondaryValue = 2.0f,
        defaultTertiaryValue = 120.0f
    ),
    PLASTERING(
        title = "PLASTERING",
        subtitle = "Mix Ratio & Render Depth Survey",
        primaryColor = Color(0xFFFFB800), // Industrial Amber
        secondaryColor = Color(0xFFFFD000),
        glowColor = Color(0x66FFB800),
        metricPrimaryLabel = "Render Depth",
        metricPrimaryUnit = "mm",
        metricSecondaryLabel = "Mix Ratio",
        metricSecondaryUnit = "parts",
        metricTertiaryLabel = "Plumb Tolerance",
        metricTertiaryUnit = "mm/m",
        defaultPrimaryValue = 12.0f,
        defaultSecondaryValue = 3.0f,
        defaultTertiaryValue = 1.5f
    ),
    SCREEDING(
        title = "SCREEDING",
        subtitle = "Datum Level & Fall Slope Survey",
        primaryColor = Color(0xFF00E5FF), // Precision Cyan
        secondaryColor = Color(0xFF66EFFF),
        glowColor = Color(0x6600E5FF),
        metricPrimaryLabel = "Screed Bed Depth",
        metricPrimaryUnit = "mm",
        metricSecondaryLabel = "Fall Gradient",
        metricSecondaryUnit = "%",
        metricTertiaryLabel = "Datum Tolerance",
        metricTertiaryUnit = "mm",
        defaultPrimaryValue = 65.0f,
        defaultSecondaryValue = 1.67f,
        defaultTertiaryValue = 3.0f
    )
}

enum class HudFilterMode(val label: String, val badge: String) {
    TACTICAL_OPTIC("Optic", "OPT"),
    NIGHT_VISION("NVG 3G", "NVG"),
    FLIR_THERMAL("Thermal", "FLIR"),
    BLUEPRINT("Blueprint", "CAD")
}

data class HudTargetPoint(
    val id: String,
    val normX: Float, // 0.0f to 1.0f on viewport
    val normY: Float, // 0.0f to 1.0f on viewport
    val label: String,
    val estimatedDistanceM: Float,
    val pitchAngleDeg: Float,
    val azimuthDeg: Float,
    val timestampMs: Long = System.currentTimeMillis()
)
