package com.example

import com.example.util.CloudinaryImageUploader
import com.example.util.ImageUploadPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudinaryImageUploaderTest {

    @Test
    fun parseCloudinaryUrl_extractsValidCredentials() {
        val parsed = CloudinaryImageUploader.parseCloudinaryUrl(
            "cloudinary://1234567890:mySecretKey_XYZ@demo-cloud"
        )
        assertNotNull(parsed)
        assertEquals("demo-cloud", parsed!!.cloudName)
        assertEquals("1234567890", parsed.apiKey)
        assertEquals("mySecretKey_XYZ", parsed.apiSecret)
    }

    @Test
    fun parseCloudinaryUrl_handlesEnvVarPrefixQuotesAndTrailingSlash() {
        val parsed = CloudinaryImageUploader.parseCloudinaryUrl(
            "CLOUDINARY_URL=cloudinary://9876543210:AbC-123_secret@my-dumingag-cloud/"
        )
        assertNotNull(parsed)
        assertEquals("my-dumingag-cloud", parsed!!.cloudName)
        assertEquals("9876543210", parsed.apiKey)
        assertEquals("AbC-123_secret", parsed.apiSecret)

        val quoted = CloudinaryImageUploader.parseCloudinaryUrl(
            "\"CLOUDINARY_URL=cloudinary://111:sec_#2@cloud9\""
        )
        assertNotNull(quoted)
        assertEquals("cloud9", quoted!!.cloudName)
        assertEquals("111", quoted.apiKey)
        assertEquals("sec_#2", quoted.apiSecret)
    }

    @Test
    fun parseCloudinaryUrl_rejectsBlankPlaceholderOrMalformed() {
        assertNull(CloudinaryImageUploader.parseCloudinaryUrl(""))
        assertNull(CloudinaryImageUploader.parseCloudinaryUrl(null))
        assertNull(CloudinaryImageUploader.parseCloudinaryUrl("cloudinary://<api_key>:<api_secret>@<cloud_name>"))
        assertNull(CloudinaryImageUploader.parseCloudinaryUrl("https://api.cloudinary.com/v1_1/demo"))
        assertNull(CloudinaryImageUploader.parseCloudinaryUrl("cloudinary://onlykey@demo"))
    }

    @Test
    fun generateSignature_matchesOfficialCloudinarySha1Spec() {
        // Verified against Cloudinary docs example:
        // eager=w_400,h_300,c_pad|w_260,h_200,c_crop&public_id=sample_image&timestamp=1315060510 + abcd
        val sig = CloudinaryImageUploader.generateSignature(
            params = mapOf(
                "timestamp" to "1315060510",
                "public_id" to "sample_image",
                "eager" to "w_400,h_300,c_pad|w_260,h_200,c_crop",
                "api_key" to "should_be_ignored",
                "file" to "should_be_ignored"
            ),
            apiSecret = "abcd"
        )
        assertEquals("bfd09f95f331f558cbd1320e67aa8d488770583e", sig)
    }

    @Test
    fun isRemoteImageUrl_distinguishesHttpsFromLegacyBase64() {
        assertTrue(CloudinaryImageUploader.isRemoteImageUrl("https://res.cloudinary.com/demo/image/upload/v1/sample.webp"))
        assertTrue(CloudinaryImageUploader.isRemoteImageUrl("http://res.cloudinary.com/demo/image/upload/sample.jpg"))
        assertFalse(CloudinaryImageUploader.isRemoteImageUrl("/9j/4AAQSkZJRgABAQAAAQABAAD"))
        assertFalse(CloudinaryImageUploader.isRemoteImageUrl("data:image/jpeg;base64,/9j/4AAQ"))
        assertFalse(CloudinaryImageUploader.isRemoteImageUrl(""))
        assertFalse(CloudinaryImageUploader.isRemoteImageUrl(null))
    }

    @Test
    fun presets_enforceStorageOptimizedWebpIncomingTransformations() {
        assertEquals(
            "c_limit,w_640,h_640",
            ImageUploadPreset.AVATAR.incomingTransformation
        )
        assertEquals(
            "c_limit,w_1440,h_1440",
            ImageUploadPreset.REVIEW.incomingTransformation
        )
        assertEquals(
            "c_limit,w_1600,h_1600",
            ImageUploadPreset.ATTRACTION.incomingTransformation
        )
        assertTrue(ImageUploadPreset.AVATAR.targetMaxBytes <= 125_000)
        assertTrue(ImageUploadPreset.REVIEW.targetMaxBytes <= 300_000)
        assertTrue(ImageUploadPreset.ATTRACTION.targetMaxBytes <= 300_000)
    }

    @Test
    fun formatByteSize_formatsMegabytesAndKilobytesCleanly() {
        assertEquals("5.0 MB", CloudinaryImageUploader.formatByteSize(5L * 1024L * 1024L))
        assertEquals("88 KB", CloudinaryImageUploader.formatByteSize(88L * 1024L))
        assertEquals("512 B", CloudinaryImageUploader.formatByteSize(512L))
    }
}
