package com.example

import com.example.data.DumingagSeedData
import com.example.ui.screens.HOLO_DEFAULT_PITCH_DEG
import com.example.ui.screens.HOLO_DEFAULT_YAW_DEG
import com.example.ui.screens.Vec3
import com.example.ui.screens.buildElevated3DPolyline
import com.example.ui.screens.computeHoloGeoBounds
import com.example.ui.screens.computeSlabTileRange
import com.example.ui.screens.computeSlabTileZoom
import com.example.ui.screens.formatCoordLabel
import com.example.ui.screens.geoToSlabLocal
import com.example.ui.screens.isInsideSlab
import com.example.ui.screens.project3DPoint
import com.example.ui.screens.sample3DPolylineAtFraction
import com.example.ui.screens.tileRectInSlabTexture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Holographic3DProjectionTest {

    @Test
    fun startAndEndCoordinatesMapInsideFloatingSlabBounds() {
        val start = DumingagSeedData.CENTER_LAT to DumingagSeedData.CENTER_LON
        val spot = DumingagSeedData.attractions.first { it.id == "danlugan_falls" }
        val end = spot.latitude to spot.longitude

        val bounds = computeHoloGeoBounds(listOf(start, end))
        val startLocal = geoToSlabLocal(start.first, start.second, bounds, z = 0f)
        val endLocal = geoToSlabLocal(end.first, end.second, bounds, z = 0f)

        assertTrue(isInsideSlab(startLocal))
        assertTrue(isInsideSlab(endLocal))
        assertEquals("8.1650°N, 123.3465°E", formatCoordLabel(8.1650, 123.3465))
    }

    @Test
    fun realMapTileGridCoversEntireSlabAndAlignsWithPinCoordinates() {
        val start = DumingagSeedData.CENTER_LAT to DumingagSeedData.CENTER_LON
        val spot = DumingagSeedData.attractions.first { it.id == "danlugan_falls" }
        val end = spot.latitude to spot.longitude

        val bounds = computeHoloGeoBounds(listOf(start, end))
        val zoom = computeSlabTileZoom(bounds)
        assertTrue(zoom in 11..16)

        val grid = computeSlabTileRange(bounds, zoom)
        assertTrue(grid.maxTx >= grid.minTx)
        assertTrue(grid.maxTy >= grid.minTy)

        val texSize = 768f
        val nwRect = tileRectInSlabTexture(zoom, grid.minTx, grid.minTy, bounds, texSize)
        val seRect = tileRectInSlabTexture(zoom, grid.maxTx, grid.maxTy, bounds, texSize)

        // The tile grid must fully cover [0..texSize] in both X and Y with zero gaps
        assertTrue(nwRect.left <= 0.5f)
        assertTrue(nwRect.top <= 0.5f)
        assertTrue(seRect.right >= texSize - 0.5f)
        assertTrue(seRect.bottom >= texSize - 0.5f)
        assertTrue(nwRect.right > nwRect.left)
        assertTrue(nwRect.bottom > nwRect.top)
    }

    @Test
    fun perspectiveProjectionElevatesPinsUpwardAndPreservesDepthOrdering() {
        val ground = project3DPoint(
            point = Vec3(0f, 0f, 0f),
            yawDeg = HOLO_DEFAULT_YAW_DEG,
            pitchDeg = HOLO_DEFAULT_PITCH_DEG,
            zoom = 1f,
            floatOffsetY = 0f,
            viewportWidth = 900f,
            viewportHeight = 900f
        )
        val beaconApex = project3DPoint(
            point = Vec3(0f, 0f, 0.25f),
            yawDeg = HOLO_DEFAULT_YAW_DEG,
            pitchDeg = HOLO_DEFAULT_PITCH_DEG,
            zoom = 1f,
            floatOffsetY = 0f,
            viewportWidth = 900f,
            viewportHeight = 900f
        )
        assertTrue(beaconApex.screen.x.isFinite() && beaconApex.screen.y.isFinite())
        assertTrue(beaconApex.screen.y < ground.screen.y)

        // With yaw = 0, South (+y) is closer to the camera than North (-y)
        val northEdge = project3DPoint(
            point = Vec3(0f, -1f, 0f),
            yawDeg = 0f,
            pitchDeg = HOLO_DEFAULT_PITCH_DEG,
            zoom = 1f,
            floatOffsetY = 0f,
            viewportWidth = 900f,
            viewportHeight = 900f
        )
        val southEdge = project3DPoint(
            point = Vec3(0f, 1f, 0f),
            yawDeg = 0f,
            pitchDeg = HOLO_DEFAULT_PITCH_DEG,
            zoom = 1f,
            floatOffsetY = 0f,
            viewportWidth = 900f,
            viewportHeight = 900f
        )
        assertTrue(southEdge.depth > northEdge.depth)
        assertTrue(southEdge.perspectiveScale > northEdge.perspectiveScale)
        assertTrue(southEdge.screen.y > northEdge.screen.y)
    }

    @Test
    fun elevatedPolylineConnectsExactEndpointsWithParabolicArcPeak() {
        val start = Vec3(-0.55f, 0.35f, 0.008f)
        val end = Vec3(0.62f, -0.40f, 0.008f)
        val arc = buildElevated3DPolyline(listOf(start, end), subdivisionsPerSegment = 20)

        assertEquals(21, arc.size)
        assertEquals(start.x, arc.first().x, 0.0001f)
        assertEquals(start.y, arc.first().y, 0.0001f)
        assertEquals(start.z, arc.first().z, 0.0001f)
        assertEquals(end.x, arc.last().x, 0.0001f)
        assertEquals(end.y, arc.last().y, 0.0001f)
        assertEquals(end.z, arc.last().z, 0.0001f)

        val mid = sample3DPolylineAtFraction(arc, 0.5f)
        assertTrue(mid.z > 0.18f)
    }
}
