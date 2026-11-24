package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.TravelCompanionEngines
import com.example.model.AppLanguage
import com.example.model.Attraction
import kotlin.math.roundToInt

/**
 * Live Hardware Compass & Target Bearing HUD backed by Android SensorManager
 * (TYPE_ROTATION_VECTOR with accelerometer + magnetometer fallback).
 * Displays real-time device heading, directional pointer toward the focused
 * Dumingag landmark, and live distance countdown.
 */
@Composable
fun CompassBearingHud(
    userLatLon: Pair<Double, Double>,
    targetAttraction: Attraction?,
    language: AppLanguage,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var azimuthDeg by remember { mutableFloatStateOf(0f) }
    var hasLiveSensor by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasGeo = false
        val rotMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                        SensorManager.getOrientation(rotMatrix, orientation)
                        val deg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                        azimuthDeg = smoothAngle(azimuthDeg, deg)
                        hasLiveSensor = true
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasGravity = true
                        if (hasGeo && SensorManager.getRotationMatrix(rotMatrix, null, gravity, geomagnetic)) {
                            SensorManager.getOrientation(rotMatrix, orientation)
                            val deg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                            azimuthDeg = smoothAngle(azimuthDeg, deg)
                            hasLiveSensor = true
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        hasGeo = true
                        if (hasGravity && SensorManager.getRotationMatrix(rotMatrix, null, gravity, geomagnetic)) {
                            SensorManager.getOrientation(rotMatrix, orientation)
                            val deg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                            azimuthDeg = smoothAngle(azimuthDeg, deg)
                            hasLiveSensor = true
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (sensorManager != null) {
            if (rotationSensor != null) {
                sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
            } else if (accelSensor != null && magSensor != null) {
                sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
                sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
            }
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val targetBearing = remember(userLatLon, targetAttraction) {
        targetAttraction?.let {
            TravelCompanionEngines.computeBearingDegrees(
                userLatLon.first,
                userLatLon.second,
                it.latitude,
                it.longitude
            )
        }
    }
    val distanceKm = remember(userLatLon, targetAttraction) {
        targetAttraction?.let {
            TravelCompanionEngines.haversineKm(
                userLatLon.first,
                userLatLon.second,
                it.latitude,
                it.longitude
            )
        }
    }
    val relativeArrowDeg = if (targetBearing != null) {
        ((targetBearing - azimuthDeg) + 360f) % 360f
    } else {
        (-azimuthDeg + 360f) % 360f
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outline

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.45f)),
        shadowElevation = 6.dp,
        modifier = modifier
            .widthIn(min = 210.dp, max = 276.dp)
            .semantics { contentDescription = "Live compass and bearing heads-up display" }
            .testTag("map_compass_hud")
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Rotating Compass Dial + Bearing Arrow
            Canvas(modifier = Modifier.size(44.dp)) {
                val r = size.minDimension * 0.46f
                drawCircle(
                    color = primaryColor.copy(alpha = 0.12f),
                    radius = r,
                    center = center
                )
                drawCircle(
                    color = outlineColor.copy(alpha = 0.55f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // North tick rotated by -azimuth
                rotate(-azimuthDeg, center) {
                    drawLine(
                        color = Color(0xFFD9383A),
                        start = Offset(center.x, center.y - r + 2.dp.toPx()),
                        end = Offset(center.x, center.y - r + 7.dp.toPx()),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                // Pointer needle toward selected destination (or North if no pin selected)
                rotate(relativeArrowDeg, center) {
                    val needle = Path().apply {
                        moveTo(center.x, center.y - r * 0.72f)
                        lineTo(center.x + 5.5.dp.toPx(), center.y + 4.dp.toPx())
                        lineTo(center.x, center.y + 0.5.dp.toPx())
                        lineTo(center.x - 5.5.dp.toPx(), center.y + 4.dp.toPx())
                        close()
                    }
                    drawPath(
                        path = needle,
                        color = if (targetAttraction != null) secondaryColor else primaryColor
                    )
                }
                drawCircle(color = primaryColor, radius = 2.5.dp.toPx(), center = center)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                val headingText = "${azimuthDeg.roundToInt()}° ${TravelCompanionEngines.cardinalDirection(azimuthDeg)}"
                Text(
                    text = if (hasLiveSensor) "LIVE COMPASS · $headingText" else "BEARING HUD · $headingText",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (targetAttraction != null && distanceKm != null && targetBearing != null) {
                    Text(
                        text = targetAttraction.name.get(language),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val distLabel = if (distanceKm < 1.0) {
                        "${(distanceKm * 1000.0).roundToInt()} m away"
                    } else {
                        "${"%.2f".format(distanceKm)} km away"
                    }
                    Text(
                        text = "$distLabel · Bearing ${targetBearing.roundToInt()}° ${TravelCompanionEngines.cardinalDirection(targetBearing)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "Tap any landmark pin for bearing",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("map_compass_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close compass HUD",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun smoothAngle(current: Float, target: Float, alpha: Float = 0.22f): Float {
    var delta = target - current
    while (delta > 180f) delta -= 360f
    while (delta < -180f) delta += 360f
    return ((current + delta * alpha) + 360f) % 360f
}
