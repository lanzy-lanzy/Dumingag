package com.example

import com.example.data.DumingagSeedData
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.AttractionCategory
import com.example.model.CategoryCatalog
import com.example.model.ManagedCategory
import com.example.util.GeoTagSource
import com.example.util.PhotoGeoTagHelper
import com.example.util.PhotoGeoTagResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminCategoryAndGeoTagTest {

    @Test
    fun normalizeCode_formatsSpacesPunctuationAndReservedAll() {
        assertEquals("ECO_RESORTS", CategoryCatalog.normalizeCode("  Eco-Resorts & Spas  ".take(14)))
        assertEquals("RIVER_CAMP", CategoryCatalog.normalizeCode("River & Camp!"))
        assertEquals("CUSTOM_ALL", CategoryCatalog.normalizeCode("all"))
        assertEquals("CUSTOM_ALL", CategoryCatalog.normalizeCode("  ALL  "))
        assertEquals("", CategoryCatalog.normalizeCode("a"))
        assertEquals("", CategoryCatalog.normalizeCode("   "))
        val veryLong = CategoryCatalog.normalizeCode("A".repeat(60))
        assertEquals(40, veryLong.length)
        assertTrue(CategoryCatalog.isValidCode(veryLong))
    }

    @Test
    fun isValidCode_rejectsMalformedAndReservedAll() {
        assertTrue(CategoryCatalog.isValidCode("NATURE"))
        assertTrue(CategoryCatalog.isValidCode("COFFEE_SHOPS_2"))
        assertFalse(CategoryCatalog.isValidCode("ALL"))
        assertFalse(CategoryCatalog.isValidCode("A"))
        assertFalse(CategoryCatalog.isValidCode("nature"))
        assertFalse(CategoryCatalog.isValidCode("HAS SPACE"))
        assertFalse(CategoryCatalog.isValidCode("HAS-DASH"))
        assertFalse(CategoryCatalog.isValidCode("A".repeat(41)))
    }

    @Test
    fun categoryLabelForCode_resolvesCustomAdminRenamedAndLocalizedDefaults() {
        val managed = CategoryCatalog.defaultCategories.map { cat ->
            if (cat.code == AttractionCategory.NATURE.name) {
                cat.copy(name = "Waterfalls & Forests")
            } else {
                cat
            }
        } + ManagedCategory(
            id = "COFFEE_SPOTS",
            code = "COFFEE_SPOTS",
            name = "Highland Coffee Spots",
            iconKey = "restaurant",
            sortOrder = 5L
        )

        // Custom category returns admin-configured name
        assertEquals(
            "Highland Coffee Spots",
            AppStrings.categoryLabelForCode("COFFEE_SPOTS", managed, AppLanguage.EN)
        )
        assertEquals(
            "Highland Coffee Spots",
            AppStrings.categoryLabelForCode("COFFEE_SPOTS", managed, AppLanguage.CEB)
        )

        // Admin-renamed built-in category uses the custom admin name
        assertEquals(
            "Waterfalls & Forests",
            AppStrings.categoryLabelForCode("NATURE", managed, AppLanguage.CEB)
        )

        // Unmodified built-in category still localizes cleanly
        assertEquals(
            "Kasaysayan",
            AppStrings.categoryLabelForCode("HISTORICAL", managed, AppLanguage.CEB)
        )
        assertEquals(
            "Bukid",
            AppStrings.categoryLabelForCode("ORGANIC_FARM", managed, AppLanguage.TL)
        )
        assertEquals(
            "Todo",
            AppStrings.categoryLabelForCode("ALL", managed, AppLanguage.ES)
        )

        // Unknown deleted category falls back to a human-readable title
        assertEquals(
            "Secret Falls",
            AppStrings.categoryLabelForCode("SECRET_FALLS", managed, AppLanguage.EN)
        )
    }

    @Test
    fun decimalToExifDms_formatsValidExifRationalTriplet() {
        // 8.1650° = 8 deg, 9 min, 54.0000 sec
        val dms = PhotoGeoTagHelper.decimalToExifDms(8.1650)
        assertEquals("8/1,9/1,540000/10000", dms)

        // Negative coordinate should still format absolute value (ref N/S/E/W is stored separately)
        val negativeDms = PhotoGeoTagHelper.decimalToExifDms(-123.5)
        assertEquals("123/1,30/1,0/10000", negativeDms)
    }

    @Test
    fun isValidNonZeroCoordinate_validatesBoundsAndZeroOrigin() {
        assertTrue(PhotoGeoTagHelper.isValidNonZeroCoordinate(8.1650, 123.3465))
        assertTrue(PhotoGeoTagHelper.isValidNonZeroCoordinate(-33.8688, 151.2093))
        assertFalse(PhotoGeoTagHelper.isValidNonZeroCoordinate(0.0, 0.0))
        assertFalse(PhotoGeoTagHelper.isValidNonZeroCoordinate(95.0, 123.3465))
        assertFalse(PhotoGeoTagHelper.isValidNonZeroCoordinate(8.1650, 190.0))
        assertFalse(PhotoGeoTagHelper.isValidNonZeroCoordinate(Double.NaN, 123.3465))
    }

    @Test
    fun resolveNearestNodeAndBarangay_mapsDumingagCoordinateToClosestLandmark() {
        val sampleSpot = DumingagSeedData.attractions.first()
        val (nodeId, nodeName, barangay) = PhotoGeoTagHelper.resolveNearestNodeAndBarangay(
            latitude = sampleSpot.latitude,
            longitude = sampleSpot.longitude,
            existingAttractions = DumingagSeedData.attractions
        )

        assertTrue(nodeId.isNotBlank())
        assertTrue(nodeName.isNotBlank())
        assertNotNull(barangay)
        assertEquals(sampleSpot.barangay, barangay)

        val formatted = PhotoGeoTagResult(
            latitude = 8.16501,
            longitude = 123.34652,
            source = GeoTagSource.LIVE_GPS,
            nearestNodeId = nodeId,
            nearestNodeName = nodeName,
            suggestedBarangay = barangay,
            exifWrittenToFile = true
        ).formattedCoordinates
        assertEquals("8.16501° N, 123.34652° E", formatted)
    }
}
