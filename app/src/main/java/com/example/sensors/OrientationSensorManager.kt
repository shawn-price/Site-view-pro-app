package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class SensorOrientation(
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val azimuthDeg: Float = 184f,
    val isSensorActive: Boolean = false
)

class OrientationSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotationVector = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _orientationFlow = MutableStateFlow(SensorOrientation())
    val orientationFlow: StateFlow<SensorOrientation> = _orientationFlow.asStateFlow()

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var hasGravity = false
    private var hasGeomagnetic = false

    fun startListening() {
        if (sensorManager == null) return

        if (rotationVector != null) {
            sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            magnetometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
            val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

            val normalizedAzimuth = (azimuth + 360f) % 360f

            _orientationFlow.value = SensorOrientation(
                pitchDeg = -pitch,
                rollDeg = -roll,
                azimuthDeg = normalizedAzimuth,
                isSensorActive = true
            )
            return
        }

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, 3)
            hasGravity = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagnetic, 0, 3)
            hasGeomagnetic = true
        }

        if (hasGravity && hasGeomagnetic) {
            val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                val normalizedAzimuth = (azimuth + 360f) % 360f

                _orientationFlow.value = SensorOrientation(
                    pitchDeg = -pitch,
                    rollDeg = -roll,
                    azimuthDeg = normalizedAzimuth,
                    isSensorActive = true
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
