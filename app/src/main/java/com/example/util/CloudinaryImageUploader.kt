package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Parsed Cloudinary credentials derived from `BuildConfig.CLOUDINARY_URL`
 * (`cloudinary://<api_key>:<api_secret>@<cloud_name>`).
 */
data class CloudinaryConfig(
    val cloudName: String,
    val apiKey: String,
    val apiSecret: String
)

/**
 * Storage-optimized upload presets balancing crisp visual sharpness with the 1 GB free tier.
 *
 * Each preset performs EXIF-aware downscaling and high-clarity WebP compression BEFORE upload
 * (up to ~300 KB for reviews and attractions, ~120 KB for avatars) and avoids a second aggressive
 * lossy pass on Cloudinary so photos stay sharp while reducing 5 MB+ originals by ~94%.
 */
enum class ImageUploadPreset(
    val maxEdgePx: Int,
    val targetMaxBytes: Int,
    val folder: String,
    val cropSquare: Boolean,
    val fallbackBase64MaxPx: Int
) {
    AVATAR(
        maxEdgePx = 640,
        targetMaxBytes = 120_000,
        folder = "dumingag/avatars",
        cropSquare = true,
        fallbackBase64MaxPx = 220
    ),
    REVIEW(
        maxEdgePx = 1440,
        targetMaxBytes = 295_000,
        folder = "dumingag/reviews",
        cropSquare = false,
        fallbackBase64MaxPx = 360
    ),
    ATTRACTION(
        maxEdgePx = 1600,
        targetMaxBytes = 300_000,
        folder = "dumingag/attractions",
        cropSquare = false,
        fallbackBase64MaxPx = 360
    );

    /**
     * Incoming dimension guardrail executed by Cloudinary during ingestion (paired with `format=webp`)
     * without re-applying an aggressive second lossy quality downgrade over our already-optimized WebP.
     */
    val incomingTransformation: String
        get() = "c_limit,w_$maxEdgePx,h_$maxEdgePx"
}

/**
 * Outcome of compressing and uploading an image via [CloudinaryImageUploader].
 */
data class ImageUploadResult(
    val photoData: String,
    val uploadedToCloud: Boolean,
    val originalBytes: Long,
    val finalBytes: Long,
    val statusMessage: String
)

/**
 * Handles storage-efficient on-device image compression and signed Cloudinary uploads for all
 * user-supplied photos across Dumingag (traveler reviews, attraction covers/galleries, and
 * user profile avatars).
 */
object CloudinaryImageUploader {

    private const val TAG = "CloudinaryUploader"
    private const val FALLBACK_BASE64_CHAR_LIMIT = 45_000

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Returns true if [value] is an HTTP(S) cloud image URL (e.g., Cloudinary `secure_url`)
     * rather than a legacy Base64-encoded image string.
     */
    fun isRemoteImageUrl(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val trimmed = value.trim()
        return trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true)
    }

    /**
     * Parses a Cloudinary environment URL into [CloudinaryConfig].
     *
     * Supports both the bare URL (`cloudinary://<api_key>:<api_secret>@<cloud_name>`) and the full
     * dashboard copy-paste format (`CLOUDINARY_URL=cloudinary://<api_key>:<api_secret>@<cloud_name>`),
     * with optional surrounding quotes, whitespace, or trailing path/slash.
     * Returns null if missing, malformed, or still set to a placeholder template.
     */
    fun parseCloudinaryUrl(rawUrl: String?): CloudinaryConfig? {
        if (rawUrl.isNullOrBlank()) return null
        var cleaned = rawUrl.trim().trim('"', '\'').trim()

        // Handle Cloudinary Dashboard's "Copy API Environment variable" format ("CLOUDINARY_URL=cloudinary://...")
        val schemeIdx = cleaned.indexOf("cloudinary://", ignoreCase = true)
        if (schemeIdx < 0) return null
        cleaned = cleaned.substring(schemeIdx).trim().trim('"', '\'').trim()

        val authorityAndRest = cleaned.substring("cloudinary://".length)
        val atIndex = authorityAndRest.lastIndexOf('@')
        if (atIndex <= 0 || atIndex >= authorityAndRest.lastIndex) return null

        val userInfo = authorityAndRest.substring(0, atIndex)
        val hostPart = authorityAndRest.substring(atIndex + 1)
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .trim()

        val colonIndex = userInfo.indexOf(':')
        if (colonIndex <= 0 || colonIndex >= userInfo.lastIndex) return null

        val rawKey = userInfo.substring(0, colonIndex).trim()
        val rawSecret = userInfo.substring(colonIndex + 1).trim()
        val apiKey = runCatching { URLDecoder.decode(rawKey, "UTF-8") }.getOrDefault(rawKey).trim()
        val apiSecret = runCatching { URLDecoder.decode(rawSecret, "UTF-8") }.getOrDefault(rawSecret).trim()
        val cloudName = hostPart.trim()

        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) return null
        if (cloudName.contains('<') || apiKey.contains('<') || apiSecret.contains('<')) return null
        if (cloudName.contains('>') || apiKey.contains('>') || apiSecret.contains('>')) return null

        return CloudinaryConfig(
            cloudName = cloudName,
            apiKey = apiKey,
            apiSecret = apiSecret
        )
    }

    /**
     * Resolves the current app's Cloudinary credentials from `BuildConfig.CLOUDINARY_URL`.
     */
    fun currentConfig(): CloudinaryConfig? = parseCloudinaryUrl(BuildConfig.CLOUDINARY_URL)

    /**
     * Computes Cloudinary's required SHA-1 upload signature over alphabetically sorted parameters.
     *
     * Per Cloudinary REST Upload API specification:
     * exclude `file`, `api_key`, `resource_type`, `cloud_name`, and blank values; sort keys
     * lexicographically; join as `key=value&...`; append `apiSecret`; SHA-1 hex (lowercase).
     */
    fun generateSignature(params: Map<String, String>, apiSecret: String): String {
        val canonical = params.entries
            .filter { (k, v) ->
                v.isNotBlank() &&
                    k != "file" &&
                    k != "api_key" &&
                    k != "resource_type" &&
                    k != "cloud_name"
            }
            .sortedBy { it.key }
            .joinToString("&") { (k, v) -> "$k=$v" }

        val toHash = canonical + apiSecret
        val digest = MessageDigest.getInstance("SHA-1")
            .digest(toHash.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(Locale.US, byte) }
    }

    /**
     * Formats a byte count into a concise human-readable string (e.g., `5.2 MB`, `84 KB`).
     */
    fun formatByteSize(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L)
        return when {
            safe >= 1_048_576L -> String.format(Locale.US, "%.1f MB", safe / 1_048_576.0)
            safe >= 1_024L -> "${(safe / 1024.0).roundToInt()} KB"
            else -> "$safe B"
        }
    }

    /**
     * Compresses an image from [uri] on-device (fixing EXIF rotation, downscaling to [preset]
     * bounds, and encoding compact WebP) and uploads it to Cloudinary so every traveler can view it.
     *
     * If Cloudinary is unreachable (e.g., offline in rural Dumingag) or unconfigured, falls back
     * gracefully to a tightly compressed Base64 payload under Firestore's size ceiling.
     */
    suspend fun uploadImage(
        context: Context,
        uri: Uri,
        preset: ImageUploadPreset
    ): ImageUploadResult? = withContext(Dispatchers.IO) {
        val originalBytes = queryUriByteSize(context, uri)
        val compressed = compressUriToOptimizedWebp(context, uri, preset)
        val config = currentConfig()

        var cloudFailureReason: String? = null
        if (compressed != null && config != null) {
            val cloudResponse = uploadBytesToCloudinary(
                imageBytes = compressed.bytes,
                mimeType = compressed.mimeType,
                fileExtension = compressed.extension,
                config = config,
                preset = preset
            )
            if (cloudResponse.secureUrl != null) {
                val storedBytes = cloudResponse.storedBytes.takeIf { it > 0L } ?: compressed.bytes.size.toLong()
                val savingsLabel = if (originalBytes > storedBytes && originalBytes > 0L) {
                    "Cloud saved · ${formatByteSize(originalBytes)} → ${formatByteSize(storedBytes)} (WebP)"
                } else {
                    "Cloud saved · ${formatByteSize(storedBytes)} (WebP)"
                }
                return@withContext ImageUploadResult(
                    photoData = cloudResponse.secureUrl,
                    uploadedToCloud = true,
                    originalBytes = originalBytes,
                    finalBytes = storedBytes,
                    statusMessage = savingsLabel
                )
            } else {
                cloudFailureReason = cloudResponse.errorReason
            }
        }

        // Graceful offline / unconfigured fallback so users can still attach photos without losing work
        val fallbackBase64 = compressUriToFallbackBase64(context, uri, preset)
        if (!fallbackBase64.isNullOrBlank()) {
            val approxBytes = (fallbackBase64.length * 3L) / 4L
            val reason = when {
                config == null -> "Cloudinary URL not set in Secrets · Local compact"
                !cloudFailureReason.isNullOrBlank() -> "Cloud fallback ($cloudFailureReason) · Local"
                else -> "Offline fallback · Local"
            }
            return@withContext ImageUploadResult(
                photoData = fallbackBase64,
                uploadedToCloud = false,
                originalBytes = originalBytes,
                finalBytes = approxBytes,
                statusMessage = "$reason (${formatByteSize(approxBytes)})"
            )
        }

        null
    }

    private data class CompressedPayload(
        val bytes: ByteArray,
        val mimeType: String,
        val extension: String
    )

    private data class CloudinaryUploadResponse(
        val secureUrl: String?,
        val storedBytes: Long = 0L,
        val errorReason: String? = null
    )

    private fun queryUriByteSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                val len = afd.length
                if (len > 0L) len else 0L
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun readExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (_: Exception) {
            bitmap
        }
    }

    private fun compressUriToOptimizedWebp(
        context: Context,
        uri: Uri,
        preset: ImageUploadPreset
    ): CompressedPayload? {
        return try {
            val resolver = context.contentResolver
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
            val origW = boundsOpts.outWidth
            val origH = boundsOpts.outHeight
            if (origW <= 0 || origH <= 0) return null

            var sampleSize = 1
            while ((origW / sampleSize) > preset.maxEdgePx * 2 || (origH / sampleSize) > preset.maxEdgePx * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            val orientation = readExifOrientation(context, uri)
            val upright = applyExifOrientation(decoded, orientation)
            if (upright != decoded) decoded.recycle()

            val prepared = prepareScaledBitmap(upright, preset.maxEdgePx, preset.cropSquare)
            if (prepared != upright) upright.recycle()

            val webpFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }

            var workingBitmap = prepared
            var bestBytes: ByteArray? = null
            val qualitySteps = intArrayOf(88, 82, 76, 70, 64)

            // Pass 1: Step down high-clarity WebP quality gently until within the ~300 KB budget
            for (q in qualitySteps) {
                val out = ByteArrayOutputStream()
                val ok = workingBitmap.compress(webpFormat, q, out)
                if (!ok) continue
                val bytes = out.toByteArray()
                bestBytes = bytes
                if (bytes.size <= preset.targetMaxBytes) {
                    break
                }
            }

            // Pass 2: Only if an extremely detailed photo is still above targetMaxBytes, gently trim dimensions
            var currentScaleAttempt = 0
            while (bestBytes != null && bestBytes.size > preset.targetMaxBytes && currentScaleAttempt < 2) {
                currentScaleAttempt++
                val nextW = (workingBitmap.width * 0.85f).roundToInt().coerceAtLeast(480)
                val nextH = (workingBitmap.height * 0.85f).roundToInt().coerceAtLeast(480)
                val smaller = Bitmap.createScaledBitmap(workingBitmap, nextW, nextH, true)
                if (workingBitmap != smaller) workingBitmap.recycle()
                workingBitmap = smaller

                val out = ByteArrayOutputStream()
                if (workingBitmap.compress(webpFormat, 68, out)) {
                    bestBytes = out.toByteArray()
                }
            }

            // Fallback to high-clarity JPEG if WebP encoder unexpectedly failed on an older OEM device
            val finalPayload = if (bestBytes != null && bestBytes.isNotEmpty()) {
                CompressedPayload(bestBytes, "image/webp", "webp")
            } else {
                val jpegOut = ByteArrayOutputStream()
                workingBitmap.compress(Bitmap.CompressFormat.JPEG, 82, jpegOut)
                CompressedPayload(jpegOut.toByteArray(), "image/jpeg", "jpg")
            }

            workingBitmap.recycle()
            if (finalPayload.bytes.isEmpty()) null else finalPayload
        } catch (e: Exception) {
            Log.w(TAG, "Image compression failed: ${e.message}")
            null
        }
    }

    private fun prepareScaledBitmap(source: Bitmap, maxEdgePx: Int, cropSquare: Boolean): Bitmap {
        val cropped = if (cropSquare && source.width != source.height) {
            val side = min(source.width, source.height)
            val xOff = (source.width - side) / 2
            val yOff = (source.height - side) / 2
            Bitmap.createBitmap(source, xOff, yOff, side, side)
        } else {
            source
        }

        val longest = max(cropped.width, cropped.height)
        if (longest <= maxEdgePx) return cropped

        val ratio = maxEdgePx.toFloat() / longest.toFloat()
        val targetW = (cropped.width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (cropped.height * ratio).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(cropped, targetW, targetH, true)
        if (cropped != source && cropped != scaled) cropped.recycle()
        return scaled
    }

    private fun uploadBytesToCloudinary(
        imageBytes: ByteArray,
        mimeType: String,
        fileExtension: String,
        config: CloudinaryConfig,
        preset: ImageUploadPreset
    ): CloudinaryUploadResponse {
        return try {
            val timestamp = (System.currentTimeMillis() / 1000L).toString()
            val signedParams = mapOf(
                "folder" to preset.folder,
                "format" to "webp",
                "timestamp" to timestamp,
                "transformation" to preset.incomingTransformation
            )
            val signature = generateSignature(signedParams, config.apiSecret)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    name = "file",
                    filename = "dumingag_${timestamp}.$fileExtension",
                    body = imageBytes.toRequestBody(mimeType.toMediaType())
                )
                .addFormDataPart("api_key", config.apiKey)
                .addFormDataPart("timestamp", timestamp)
                .addFormDataPart("folder", preset.folder)
                .addFormDataPart("format", "webp")
                .addFormDataPart("transformation", preset.incomingTransformation)
                .addFormDataPart("signature", signature)
                .build()

            val endpoint = "https://api.cloudinary.com/v1_1/${config.cloudName}/image/upload"
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val apiMsg = runCatching {
                        JSONObject(bodyStr).optJSONObject("error")?.optString("message").orEmpty()
                    }.getOrDefault("")
                    val shortErr = if (apiMsg.isNotBlank()) "HTTP ${response.code}: ${apiMsg.take(60)}" else "HTTP ${response.code}"
                    Log.w(TAG, "Cloudinary upload failed ($shortErr): ${bodyStr.take(240)}")
                    return CloudinaryUploadResponse(secureUrl = null, errorReason = shortErr)
                }
                val json = JSONObject(bodyStr)
                val secureUrl = json.optString("secure_url").trim()
                val storedBytes = json.optLong("bytes", imageBytes.size.toLong())
                if (secureUrl.startsWith("https://")) {
                    CloudinaryUploadResponse(secureUrl = secureUrl, storedBytes = storedBytes)
                } else {
                    CloudinaryUploadResponse(secureUrl = null, errorReason = "Missing secure_url")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloudinary upload exception: ${e.message}")
            CloudinaryUploadResponse(secureUrl = null, errorReason = "Network error")
        }
    }

    private fun compressUriToFallbackBase64(
        context: Context,
        uri: Uri,
        preset: ImageUploadPreset
    ): String? {
        return try {
            val resolver = context.contentResolver
            val maxPx = preset.fallbackBase64MaxPx
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
            val origW = boundsOpts.outWidth
            val origH = boundsOpts.outHeight
            if (origW <= 0 || origH <= 0) return null

            var sampleSize = 1
            while ((origW / sampleSize) > maxPx * 2 || (origH / sampleSize) > maxPx * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            val upright = applyExifOrientation(decoded, readExifOrientation(context, uri))
            if (upright != decoded) decoded.recycle()

            val scaled = prepareScaledBitmap(upright, maxPx, preset.cropSquare)
            if (scaled != upright) upright.recycle()

            var quality = 70
            var result: String? = null
            while (quality >= 34) {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                if (b64.length <= FALLBACK_BASE64_CHAR_LIMIT) {
                    result = b64
                    break
                }
                quality -= 12
            }
            scaled.recycle()
            result
        } catch (_: Exception) {
            null
        }
    }
}
