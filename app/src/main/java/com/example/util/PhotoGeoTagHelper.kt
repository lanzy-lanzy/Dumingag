package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.DumingagSeedData
import com.example.data.OfflineRouteEngine
import com.example.model.Attraction
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.floor

enum class GeoTagSource(val label: String) {
    PHOTO_EXIF("Photo EXIF GPS"),
    LIVE_GPS("Live Device GPS"),
    LAST_KNOWN_GPS("Last Known GPS"),
    FALLBACK_LOCATION("Dumingag Reference GPS")
}

data class CapturedPhotoTarget(
    val file: File,
    val uri: Uri
)

data class PhotoGeoTagResult(
    val latitude: Double,
    val longitude: Double,
    val source: GeoTagSource,
    val nearestNodeId: String,
    val nearestNodeName: String,
    val suggestedBarangay: String?,
    val exifWrittenToFile: Boolean
) {
    val formattedCoordinates: String
        get() {
            val latHem = if (latitude >= 0) "N" else "S"
            val lonHem = if (longitude >= 0) "E" else "W"
            return String.format(
                Locale.US,
                "%.5f° %s, %.5f° %s",
                abs(latitude),
                latHem,
                abs(longitude),
                lonHem
            )
        }
}

object PhotoGeoTagHelper {

    fun createTempCaptureTarget(context: Context): CapturedPhotoTarget {
        val dir = File(context.cacheDir, "captured_photos").apply { mkdirs() }
        val file = File(dir, "attraction_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return CapturedPhotoTarget(file = file, uri = uri)
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Converts a decimal degree coordinate into EXIF rational DMS format ("deg/1,min/1,sec*10000/10000").
     */
    fun decimalToExifDms(decimal: Double): String {
        val positive = abs(decimal)
        var degrees = floor(positive).toInt()
        val minutesFull = (positive - degrees) * 60.0
        var minutes = floor(minutesFull).toInt()
        var secondsScaled = kotlin.math.round((minutesFull - minutes) * 60.0 * 10000.0).toLong().coerceAtLeast(0L)
        if (secondsScaled >= 600_000L) {
            secondsScaled -= 600_000L
            minutes += 1
        }
        if (minutes >= 60) {
            minutes -= 60
            degrees += 1
        }
        return "$degrees/1,$minutes/1,$secondsScaled/10000"
    }

    /**
     * Extracts embedded EXIF GPS coordinates from an image [Uri] if present and valid.
     */
    suspend fun extractExifLatLon(context: Context, uri: Uri): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val latLong = FloatArray(2)
                    @Suppress("DEPRECATION")
                    val hasGps = exif.getLatLong(latLong)
                    if (hasGps) {
                        val lat = latLong[0].toDouble()
                        val lon = latLong[1].toDouble()
                        if (isValidNonZeroCoordinate(lat, lon)) {
                            return@withContext lat to lon
                        }
                    }
                }
                null
            } catch (_: Exception) {
                null
            }
        }

    /**
     * Embeds GPS latitude/longitude EXIF metadata directly into a captured JPEG [File].
     */
    suspend fun writeExifLatLon(file: File, latitude: Double, longitude: Double): Boolean =
        withContext(Dispatchers.IO) {
            if (!file.exists() || !isValidNonZeroCoordinate(latitude, longitude)) return@withContext false
            try {
                val exif = ExifInterface(file.absolutePath)
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, decimalToExifDms(latitude))
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (latitude >= 0.0) "N" else "S")
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, decimalToExifDms(longitude))
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (longitude >= 0.0) "E" else "W")
                exif.saveAttributes()
                true
            } catch (_: Exception) {
                false
            }
        }

    /**
     * Resolves the freshest live device GPS coordinate (FusedLocationProvider with LocationManager fallback).
     */
    @SuppressLint("MissingPermission")
    suspend fun resolveDeviceLocation(context: Context): Pair<Pair<Double, Double>, GeoTagSource>? {
        if (!hasLocationPermission(context)) return null

        val fused = LocationServices.getFusedLocationProviderClient(context)
        val freshLocation: Location? = withTimeoutOrNull(4500L) {
            suspendCancellableCoroutine { cont ->
                val tokenSource = CancellationTokenSource()
                cont.invokeOnCancellation { tokenSource.cancel() }
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }
        }
        if (freshLocation != null && isValidNonZeroCoordinate(freshLocation.latitude, freshLocation.longitude)) {
            return (freshLocation.latitude to freshLocation.longitude) to GeoTagSource.LIVE_GPS
        }

        val lastFused: Location? = withTimeoutOrNull(2000L) {
            suspendCancellableCoroutine { cont ->
                fused.lastLocation
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }
        }
        if (lastFused != null && isValidNonZeroCoordinate(lastFused.latitude, lastFused.longitude)) {
            return (lastFused.latitude to lastFused.longitude) to GeoTagSource.LAST_KNOWN_GPS
        }

        return withContext(Dispatchers.IO) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val candidates = listOfNotNull(
                    runCatching { lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull(),
                    runCatching { lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull(),
                    runCatching { lm?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) }.getOrNull()
                ).filter { isValidNonZeroCoordinate(it.latitude, it.longitude) }
                val best = candidates.maxByOrNull { it.time }
                if (best != null) {
                    (best.latitude to best.longitude) to GeoTagSource.LAST_KNOWN_GPS
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Automatically geo-tags a captured or uploaded photo:
     * 1. Reads EXIF GPS from the image if embedded.
     * 2. Otherwise captures current device GPS (or last known GPS).
     * 3. Falls back to the traveler's known Dumingag reference location.
     * 4. Embeds GPS EXIF metadata into [capturedFile] when provided and resolves nearest map node & barangay.
     */
    suspend fun autoGeoTagPhoto(
        context: Context,
        uri: Uri,
        capturedFile: File? = null,
        fallbackLatLon: Pair<Double, Double> = DumingagSeedData.CENTER_LAT to DumingagSeedData.CENTER_LON,
        existingAttractions: List<Attraction> = DumingagSeedData.attractions
    ): PhotoGeoTagResult {
        val exifCoords = extractExifLatLon(context, uri)
        val (resolvedCoords, source) = if (exifCoords != null) {
            exifCoords to GeoTagSource.PHOTO_EXIF
        } else {
            val deviceLoc = resolveDeviceLocation(context)
            if (deviceLoc != null) {
                deviceLoc
            } else {
                val safeFallback = if (isValidNonZeroCoordinate(fallbackLatLon.first, fallbackLatLon.second)) {
                    fallbackLatLon
                } else {
                    DumingagSeedData.CENTER_LAT to DumingagSeedData.CENTER_LON
                }
                safeFallback to GeoTagSource.FALLBACK_LOCATION
            }
        }

        val (lat, lon) = resolvedCoords
        val exifWritten = if (capturedFile != null) {
            writeExifLatLon(capturedFile, lat, lon)
        } else {
            false
        }

        val (nearestNodeId, nearestNodeName, suggestedBarangay) = resolveNearestNodeAndBarangay(
            latitude = lat,
            longitude = lon,
            existingAttractions = existingAttractions
        )

        return PhotoGeoTagResult(
            latitude = lat,
            longitude = lon,
            source = source,
            nearestNodeId = nearestNodeId,
            nearestNodeName = nearestNodeName,
            suggestedBarangay = suggestedBarangay,
            exifWrittenToFile = exifWritten
        )
    }

    fun isValidNonZeroCoordinate(lat: Double, lon: Double): Boolean {
        if (lat.isNaN() || lon.isNaN()) return false
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return false
        if (abs(lat) < 0.0001 && abs(lon) < 0.0001) return false
        return true
    }

    /**
     * Pure-Kotlin nearest offline map node & Dumingag barangay resolver for automatic geo-tagging.
     */
    fun resolveNearestNodeAndBarangay(
        latitude: Double,
        longitude: Double,
        existingAttractions: List<Attraction> = DumingagSeedData.attractions
    ): Triple<String, String, String?> {
        val nearestNode = OfflineRouteEngine.findNearestNode(latitude, longitude)
        val nearestAttraction = existingAttractions.minByOrNull { spot ->
            OfflineRouteEngine.haversineKm(latitude, longitude, spot.latitude, spot.longitude)
        }
        val suggestedBarangay = if (nearestAttraction != null) {
            val distKm = OfflineRouteEngine.haversineKm(
                latitude,
                longitude,
                nearestAttraction.latitude,
                nearestAttraction.longitude
            )
            if (distKm <= 12.0) nearestAttraction.barangay else null
        } else {
            null
        }
        return Triple(nearestNode.id, nearestNode.name.en, suggestedBarangay)
    }
}
