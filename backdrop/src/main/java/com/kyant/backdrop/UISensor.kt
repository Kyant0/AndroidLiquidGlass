package com.kyant.backdrop

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun rememberUISensor(): UISensor {
    val context = LocalContext.current
    val uiSensor = remember { UISensor(context) }

    DisposableEffect(Unit) {
        uiSensor.start()
        onDispose { uiSensor.stop() }
    }

    return uiSensor
}

class UISensor(context: Context) {

    var gravityAngle: Float by mutableFloatStateOf(45f)
        private set
    var gravity: Offset by mutableStateOf(Offset.Zero)
        private set
    var tilt: Offset by mutableStateOf(Offset.Zero)
        private set

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val listener = object : SensorEventListener {

        private val rotationMatrix = FloatArray(9)
        private val orientationAngles = FloatArray(3)

        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val x = event.values[0]
                    val y = event.values[1]
                    val norm = sqrt(x * x + y * y + 9.81f * 9.81f)

                    val alpha = 0.5f
                    gravityAngle = gravityAngle * (1f - alpha) + atan2(y, x) * (180f / PI).toFloat() * alpha
                    gravity = gravity * (1f - alpha) + Offset(x / norm, y / norm) * alpha
                }

                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    // orientationAngles[1] is pitch (tilt around x-axis), orientationAngles[2] is roll (tilt around y-axis)
                    val roll = orientationAngles[2]
                    val pitch = orientationAngles[1]
                    tilt = Offset(
                        roll.coerceIn(-1.5f, 1.5f),
                        pitch.coerceIn(-1.5f, 1.5f)
                    )
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        rotationVector?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
    }
}
