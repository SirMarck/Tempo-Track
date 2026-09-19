package com.example.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

data class DeviceTilt(
    val pitch: Float = 0f, // Rotação X (-4..+4 deg)
    val roll: Float = 0f   // Rotação Y (-4..+4 deg)
)

/**
 * Observa o giroscópio/acelerômetro com low-pass filter e limites estritos (+/- 4 graus).
 * Desliga automaticamente quando a tela não estiver visível para economia de bateria.
 * Guia de Redesign v2 - Seção 4.2 e Prompt 8
 */
@Composable
fun rememberDeviceTilt(): State<DeviceTilt> {
    val context = LocalContext.current
    val tiltState = remember { mutableStateOf(DeviceTilt()) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensor == null || sensorManager == null) {
            // Sem sensor disponível: fallback autônomo
            return@DisposableEffect onDispose {}
        }

        var smoothedPitch = 0f
        var smoothedRoll = 0f
        val alpha = 0.15f // Low-pass filter smoothing

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                var rawPitch = 0f
                var rawRoll = 0f

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    rawPitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    rawRoll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    rawPitch = event.values[1] * 3.5f
                    rawRoll = -event.values[0] * 3.5f
                }

                // Aplica low-pass filter
                smoothedPitch = smoothedPitch + alpha * (rawPitch - smoothedPitch)
                smoothedRoll = smoothedRoll + alpha * (rawRoll - smoothedRoll)

                // Clamp estrito de +/- 4 graus
                val clampedPitch = smoothedPitch.coerceIn(-4f, 4f)
                val clampedRoll = smoothedRoll.coerceIn(-4f, 4f)

                tiltState.value = DeviceTilt(pitch = clampedPitch, roll = clampedRoll)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Frequência moderada (~30Hz = SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return tiltState
}
