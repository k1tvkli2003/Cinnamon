package com.cinnamon.app.ui.components

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.cinnamon.app.ui.feedback.LocalCinnamonFeedbackPreferences

/**
 * Phase 6: Next-Gen Processing, Voice & Audio
 * 
 * Gyroscope UI Parallax effect. Adds 3D depth to the screen
 * based on the device's tilt.
 */
fun Modifier.gyroscopeParallax(intensity: Float = 15f): Modifier = composed {
    val context = LocalContext.current
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion
    var pitch by remember { mutableFloatStateOf(0f) }
    var roll by remember { mutableFloatStateOf(0f) }

    DisposableEffect(reduceMotion) {
        val sensorManager = if (reduceMotion) null else {
            context.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager
        }
        val gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    
                    // orientationAngles[1] = pitch, orientationAngles[2] = roll
                    pitch = orientationAngles[1]
                    roll = orientationAngles[2]
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (gyroSensor != null) {
            sensorManager.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            pitch = 0f
            roll = 0f
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val animPitch by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (reduceMotion) 0f else pitch * intensity,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "animPitch"
    )
    val animRoll by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (reduceMotion) 0f else roll * intensity,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "animRoll"
    )

    // Apply the extracted pitch and roll to the UI layer with spring animation
    graphicsLayer {
        rotationX = animPitch
        rotationY = animRoll
        cameraDistance = 12 * density
    }
}
