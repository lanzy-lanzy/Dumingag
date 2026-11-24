package com.example

import com.example.data.DumingagSeedData
import com.example.data.SmartTravelEngines
import com.example.data.WeatherSuitabilityTone
import com.example.model.AppLanguage
import com.example.model.AttractionCategory
import com.example.model.CommunityReview
import com.example.model.ItineraryItem
import com.example.model.ManagedCategory
import com.example.model.PackingItem
import com.example.model.TourismEvent
import com.example.model.TourismEventSeverity
import com.example.model.TourismEventType
import com.example.model.TrailConditionReport
import com.example.model.TripExpense
import com.example.model.WeatherSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartFeaturesAndSyncTest {

    @Test
    fun weatherSuitabilityFlagsOutdoorTrailCautionInHeavyRain() {
        val waterfall = DumingagSeedData.attractions.first { it.category == AttractionCategory.NATURE }
        val heavyRain = WeatherSnapshot(
            temperatureC = 24.0,
            apparentTempC = 26.0,
            humidityPct = 92,
            windSpeedKmh = 32.0,
            weatherCode = 81,
            isFromCache = false,
            updatedAtMillis = 1_700_000_000_000L,
            dailyForecasts = emptyList(),
            conditionDescription = "Heavy rain showers"
        )

        val badge = SmartTravelEngines.evaluateWeatherSuitability(waterfall, heavyRain)
        assertEquals(WeatherSuitabilityTone.TRAIL_CAUTION, badge.tone)
        assertTrue(badge.label.contains("caution", ignoreCase = true))
    }

    @Test
    fun weatherSuitabilityRecommendsIndoorHeritageDuringRain() {
        val heritage = DumingagSeedData.attractions.first { it.category == AttractionCategory.HISTORICAL }
        val drizzle = WeatherSnapshot(
            temperatureC = 26.0,
            apparentTempC = 28.0,
            humidityPct = 86,
            windSpeedKmh = 12.0,
            weatherCode = 55,
            isFromCache = false,
            updatedAtMillis = 1_700_000_000_000L,
            dailyForecasts = emptyList(),
            conditionDescription = "Moderate drizzle"
        )

        val badge = SmartTravelEngines.evaluateWeatherSuitability(heritage, drizzle)
        assertEquals(WeatherSuitabilityTone.RAIN_FRIENDLY, badge.tone)
        assertTrue(badge.label.contains("Rain-friendly", ignoreCase = true))
    }

    @Test
    fun unifiedSearchFindsMatchesAcrossAttractionsTransitHotlinesEventsAndReviews() {
        val attractions = DumingagSeedData.attractions
        val transit = DumingagSeedData.defaultTransitMethods
        val hotlines = DumingagSeedData.emergencyContacts
        val events = SmartTravelEngines.starterTourismEvents
        val reviews = listOf(
            CommunityReview(
                id = "rev1",
                authorName = "Maria Santos",
                attractionId = attractions.first().id,
                attractionName = attractions.first().name.en,
                rating = 5L,
                comment = "Organic rice and amazing trail views in Dumingag!",
                visitTag = "Eco-Tour"
            )
        )

        val result = SmartTravelEngines.performUnifiedSearch(
            query = "organic",
            attractions = attractions,
            transitMethods = transit,
            emergencyContacts = hotlines,
            tourismEvents = events,
            reviews = reviews,
            language = AppLanguage.EN
        )

        assertTrue("Expected attraction match for 'organic'", result.attractions.isNotEmpty())
        assertTrue("Expected event match for 'organic'", result.tourismEvents.isNotEmpty())
        assertTrue("Expected review match for 'organic'", result.reviews.isNotEmpty())
        assertTrue(result.totalMatches >= 3)

        val hotlineResult = SmartTravelEngines.performUnifiedSearch(
            query = "911",
            attractions = attractions,
            transitMethods = transit,
            emergencyContacts = hotlines,
            tourismEvents = events,
            reviews = reviews,
            language = AppLanguage.EN
        )
        assertTrue("Expected emergency contact match for '911'", hotlineResult.emergencyContacts.isNotEmpty())

        val transitResult = SmartTravelEngines.performUnifiedSearch(
            query = "tricycle",
            attractions = attractions,
            transitMethods = transit,
            emergencyContacts = hotlines,
            tourismEvents = events,
            reviews = reviews,
            language = AppLanguage.EN
        )
        assertTrue("Expected transit route match for 'tricycle'", transitResult.transitMethods.isNotEmpty())
    }

    @Test
    fun shareableItineraryFormatsStopsBudgetAndPackingChecklist() {
        val spot = DumingagSeedData.attractions.first()
        val items = listOf(
            ItineraryItem(
                id = "stop1",
                attractionId = spot.id,
                attractionName = spot.name.en,
                plannedDateMillis = 1_735_689_600_000L,
                notes = "Start early with guide"
            )
        )
        val expenses = listOf(
            TripExpense(id = "e1", title = "Habal-habal ride", category = "Transit", amountPhp = 150.0),
            TripExpense(id = "e2", title = "Organic lunch", category = "Meals", amountPhp = 220.0)
        )
        val packing = listOf(
            PackingItem("p1", "Rain poncho", "Gear", "Showers", true, true),
            PackingItem("p2", "Trail shoes", "Footwear", "Grip", false, false)
        )

        val text = SmartTravelEngines.formatShareableItineraryText(
            travelerName = "Alex",
            itinerary = items,
            attractions = DumingagSeedData.attractions,
            expenses = expenses,
            packingItems = packing,
            budgetLimitPhp = 1000.0
        )

        assertTrue(text.contains("Alex"))
        assertTrue(text.contains(spot.name.en))
        assertTrue(text.contains("370"))
        assertTrue(text.contains("630"))
        assertTrue(text.contains("1/2 ready"))
        assertTrue(text.contains("Trail shoes"))
    }

    @Test
    fun adminInsightsComputesAggregatesCorrectly() {
        val spots = DumingagSeedData.attractions.take(3)
        val reviews = listOf(
            CommunityReview(
                id = "r1",
                attractionId = spots[0].id,
                attractionName = spots[0].name.en,
                rating = 5L,
                photoData = "data:image/jpeg;base64,AAA",
                verifiedVisit = true,
                adminReply = "Thank you for visiting!",
                helpfulUserIds = listOf("u1", "u2")
            ),
            CommunityReview(
                id = "r2",
                attractionId = spots[0].id,
                attractionName = spots[0].name.en,
                rating = 4L,
                photoData = "",
                verifiedVisit = false,
                adminReply = "",
                helpfulUserIds = listOf("u3")
            )
        )
        val categories = listOf(
            ManagedCategory(id = "c1", code = "NATURE", name = "Nature & Falls")
        )
        val events = listOf(
            TourismEvent(
                id = "ev1",
                title = "Organic Festival",
                eventType = TourismEventType.FESTIVAL.code,
                severity = TourismEventSeverity.INFO.code,
                active = true
            ),
            TourismEvent(
                id = "ev2",
                title = "Past Event",
                active = false
            )
        )
        val reports = listOf(
            TrailConditionReport(id = "t1", attractionId = spots[0].id, conditionTag = "Clear trail")
        )

        val summary = SmartTravelEngines.computeAdminInsights(
            attractions = spots,
            reviews = reviews,
            categories = categories,
            events = events,
            trailReports = reports
        )

        assertEquals(3, summary.totalAttractions)
        assertEquals(2, summary.totalReviews)
        assertEquals(4.5, summary.averageCommunityRating, 0.01)
        assertEquals(1, summary.photoReviewCount)
        assertEquals(1, summary.verifiedVisitReviewCount)
        assertEquals(1, summary.officialRepliesCount)
        assertEquals(1, summary.activeEventsCount)
        assertEquals(1, summary.activeTrailReportsCount)
        assertEquals(spots[0].name.en, summary.mostReviewedSpotName)
        assertEquals(2, summary.mostReviewedSpotCount)
    }
}
