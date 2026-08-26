package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject

enum class CaptureMode(val label: String, val badge: String) {
    PHOTO("PHOTO", "STILL"),
    VIDEO("VIDEO", "SCAN")
}

enum class ProcessingState {
    IDLE,
    SCANNING,
    AUTO_ANALYZING,
    SAVING,
    SUCCESS,
    ERROR
}

data class SpatialFace(
    val id: String,
    val label: String,
    val vertices: List<Pair<Float, Float>>, // Normalized coordinates (0f..1f)
    val areaSqM: Float,
    val estimatedDistanceM: Float,
    val surfaceColor: Color = Color(0xFF00FF41),
    val coverageFactor: Float = 10.5f
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("label", label)
            put("area_sqm", areaSqM.toDouble())
            put("estimated_distance_m", estimatedDistanceM.toDouble())
            put("surface_color_hex", String.format("#%08X", surfaceColor.toArgb()))
            put("coverage_factor", coverageFactor.toDouble())
            val vertArray = JSONArray()
            vertices.forEach { v ->
                vertArray.put(JSONObject().apply {
                    put("x", v.first.toDouble())
                    put("y", v.second.toDouble())
                })
            }
            put("vertices", vertArray)
        }
    }
}

data class ProjectWorkspace(
    val projectId: String,
    val projectName: String,
    val createdAtIso: String,
    val jobMode: JobMode,
    val faces: List<SpatialFace>,
    val totalAreaSqM: Float,
    val laserRangeM: Float,
    val pitchDeg: Float,
    val rollDeg: Float,
    val azimuthDeg: Float
) {
    fun toFormattedJson(): String {
        val root = JSONObject().apply {
            put("project_id", projectId)
            put("project_name", projectName)
            put("timestamp_iso", createdAtIso)
            put("active_job_mode", jobMode.title)
            put("total_area_sqm", totalAreaSqM.toDouble())
            put("telemetry", JSONObject().apply {
                put("laser_range_m", laserRangeM.toDouble())
                put("pitch_deg", pitchDeg.toDouble())
                put("roll_deg", rollDeg.toDouble())
                put("azimuth_deg", azimuthDeg.toDouble())
            })
            val facesArray = JSONArray()
            faces.forEach { face ->
                facesArray.put(face.toJson())
            }
            put("spatial_faces", facesArray)
        }
        return root.toString(2)
    }
}
