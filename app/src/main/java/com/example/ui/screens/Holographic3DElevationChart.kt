package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ElevationProfile
import com.example.ui.theme.HoloArcCore
import com.example.ui.theme.HoloEndPinAccent
import com.example.ui.theme.HoloHudBorder
import com.example.ui.theme.HoloHudSurface
import com.example.ui.theme.HoloSlabEdgeGlow
import com.example.ui.theme.HoloStartPinAccent
import com.example.ui.theme.SplashGlassTextPrimary
import com.example.ui.theme.SplashGlassTextSecondary

/**
 * Translucent Liquid Glass 3D Elevation & Terrain Profile Strip rendered over the
 * floating 3D map slab, showing highland climb from START -> END and syncing a
 * live marker with the 3D Route Flyover progress.
 */
@Composable
fun Holographic3DElevationChart(
    profile: ElevationProfile,
    flyoverProgress: Float?,
    modifier: Modifier = Modifier
) {
    if (profile.samples.size < 2) return

    Surface(
        color = HoloHudSurface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, HoloHudBorder),
        shadowElevation = 5.dp,
        modifier = modifier
            .widthIn(min = 210.dp, max = 310.dp)
            .semantics(mergeDescendants = true) {}
            .testTag("map_3d_elevation_chart")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terrain,
                        contentDescription = null,
                        tint = HoloSlabEdgeGlow,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "ELEVATION PROFILE",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        color = HoloSlabEdgeGlow
                    )
                }
                Text(
                    text = "+${profile.gainMeters}m · Peak ${profile.maxMeters}m",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SplashGlassTextPrimary
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                val samples = profile.samples
                val minE = (profile.minMeters - 25).toDouble()
                val maxE = (profile.maxMeters + 25).toDouble().coerceAtLeast(minE + 50.0)
                val elevSpan = (maxE - minE).coerceAtLeast(1.0)
                val maxDist = samples.last().distanceKm.coerceAtLeast(0.01)

                val linePath = Path()
                val fillPath = Path()
                samples.forEachIndexed { idx, sample ->
                    val x = ((sample.distanceKm / maxDist).toFloat().coerceIn(0f, 1f)) * size.width
                    val normY = ((sample.elevationMeters - minE) / elevSpan).toFloat().coerceIn(0f, 1f)
                    val y = size.height - normY * (size.height - 4.dp.toPx()) - 2.dp.toPx()
                    if (idx == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, size.height)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(size.width, size.height)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            HoloSlabEdgeGlow.copy(alpha = 0.42f),
                            HoloStartPinAccent.copy(alpha = 0.08f)
                        )
                    )
                )
                drawPath(
                    path = linePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(HoloStartPinAccent, HoloSlabEdgeGlow, HoloEndPinAccent)
                    ),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Live flyover cursor on elevation chart
                if (flyoverProgress != null) {
                    val f = flyoverProgress.coerceIn(0f, 1f)
                    val idxFloat = f * (samples.lastIndex)
                    val i0 = idxFloat.toInt().coerceIn(0, samples.lastIndex)
                    val i1 = (i0 + 1).coerceAtMost(samples.lastIndex)
                    val frac = idxFloat - i0
                    val interpElev = samples[i0].elevationMeters +
                        (samples[i1].elevationMeters - samples[i0].elevationMeters) * frac
                    val cx = f * size.width
                    val normY = ((interpElev - minE) / elevSpan).toFloat().coerceIn(0f, 1f)
                    val cy = size.height - normY * (size.height - 4.dp.toPx()) - 2.dp.toPx()

                    drawLine(
                        color = HoloArcCore.copy(alpha = 0.65f),
                        start = Offset(cx, 0f),
                        end = Offset(cx, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawCircle(color = HoloSlabEdgeGlow, radius = 4.5.dp.toPx(), center = Offset(cx, cy))
                    drawCircle(color = HoloArcCore, radius = 2.2.dp.toPx(), center = Offset(cx, cy))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "START ${profile.samples.first().elevationMeters.toInt()}m",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = HoloStartPinAccent,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (flyoverProgress != null) "FLYOVER ${(flyoverProgress * 100).toInt()}%" else "MIN ${profile.minMeters}m",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = SplashGlassTextSecondary
                )
                Text(
                    text = "END ${profile.samples.last().elevationMeters.toInt()}m",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = HoloEndPinAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
