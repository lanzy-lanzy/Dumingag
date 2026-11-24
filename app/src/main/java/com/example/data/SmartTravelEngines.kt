package com.example.data

import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.AttractionCategory
import com.example.model.CommunityReview
import com.example.model.EmergencyContact
import com.example.model.ItineraryItem
import com.example.model.ManagedCategory
import com.example.model.TourismEvent
import com.example.model.TourismEventSeverity
import com.example.model.TourismEventType
import com.example.model.TrailConditionReport
import com.example.model.TransitMethod
import com.example.model.TripExpense
import com.example.model.WeatherSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class WeatherSuitabilityTone {
    GREAT,
    RAIN_FRIENDLY,
    MORNING_PICK,
    TRAIL_CAUTION
}

data class WeatherSuitabilityBadge(
    val label: String,
    val reason: String,
    val tone: WeatherSuitabilityTone
)

data class UnifiedSearchResults(
    val query: String,
    val attractions: List<Attraction>,
    val transitMethods: List<TransitMethod>,
    val emergencyContacts: List<EmergencyContact>,
    val tourismEvents: List<TourismEvent>,
    val reviews: List<CommunityReview>
) {
    val totalMatches: Int
        get() = attractions.size + transitMethods.size + emergencyContacts.size +
            tourismEvents.size + reviews.size
}

data class AdminInsightsSummary(
    val totalAttractions: Int,
    val totalReviews: Int,
    val averageCommunityRating: Double,
    val photoReviewCount: Int,
    val verifiedVisitReviewCount: Int,
    val officialRepliesCount: Int,
    val activeEventsCount: Int,
    val activeTrailReportsCount: Int,
    val topRatedSpotName: String,
    val topRatedSpotRating: Double,
    val mostReviewedSpotName: String,
    val mostReviewedSpotCount: Int,
    val categoryBreakdown: List<Pair<String, Int>>
)

object SmartTravelEngines {

    val starterTourismEvents: List<TourismEvent> = listOf(
        TourismEvent(
            id = "evt_organic_market_day",
            title = "Dumingag Organic Tabo & Harvest Market",
            summary = "Heirloom red & black rice, mountain honey, organic vegetables, and kakanin straight from Dumingag farmer cooperatives.",
            eventType = TourismEventType.MARKET_DAY.code,
            severity = TourismEventSeverity.INFO.code,
            dateLabel = "Every Saturday · 05:30 AM – 11:30 AM",
            locationName = "Dumingag Public Market Plaza, Poblacion",
            attractionId = "organic_village",
            active = true
        ),
        TourismEvent(
            id = "evt_highland_trail_advisory",
            title = "Highland Falls Trail Advisory",
            summary = "Expect damp mossy steps after afternoon showers near Danlugan Falls & Mt. Timolan. Morning treks before 11:00 AM recommended.",
            eventType = TourismEventType.TRAIL_ADVISORY.code,
            severity = TourismEventSeverity.ADVISORY.code,
            dateLabel = "Daily Trail Watch · Updated by Tourism Office",
            locationName = "Brgy. Danlugan & Timolan Ridge",
            attractionId = "danlugan_falls",
            active = true
        )
    )

    /**
     * Evaluates live weather conditions against an attraction's category and outdoor profile
     * to provide an actionable traveler recommendation badge.
     */
    fun evaluateWeatherSuitability(
        attraction: Attraction,
        weather: WeatherSnapshot?,
        hourOfDay24: Int = 9
    ): WeatherSuitabilityBadge {
        if (weather == null) {
            return WeatherSuitabilityBadge(
                label = "Morning pick",
                reason = "Best visited ${attraction.bestTime.en.lowercase()}",
                tone = WeatherSuitabilityTone.MORNING_PICK
            )
        }

        val isStormOrHeavyRain = weather.weatherCode >= 80 || weather.weatherCode in 63..69 || weather.windSpeedKmh >= 30.0
        val isLightRainOrDrizzle = weather.weatherCode in 51..62
        val isOutdoorTrail = attraction.category == AttractionCategory.NATURE ||
            attraction.categoryCode.contains("WATER", ignoreCase = true) ||
            attraction.categoryCode.contains("FALL", ignoreCase = true) ||
            attraction.categoryCode.contains("MOUNTAIN", ignoreCase = true) ||
            attraction.name.en.contains("Falls", ignoreCase = true) ||
            attraction.name.en.contains("Peak", ignoreCase = true) ||
            attraction.name.en.contains("Cave", ignoreCase = true)
        val isIndoorOrSheltered = attraction.category == AttractionCategory.HISTORICAL ||
            attraction.category == AttractionCategory.CULTURAL ||
            attraction.name.en.contains("Church", ignoreCase = true) ||
            attraction.name.en.contains("Plaza", ignoreCase = true) ||
            attraction.name.en.contains("Museum", ignoreCase = true)

        return when {
            isStormOrHeavyRain && isOutdoorTrail -> WeatherSuitabilityBadge(
                label = "Trail caution",
                reason = "Slippery highland path · ${weather.temperatureC.toInt()}°C rain",
                tone = WeatherSuitabilityTone.TRAIL_CAUTION
            )
            (isStormOrHeavyRain || isLightRainOrDrizzle) && isIndoorOrSheltered -> WeatherSuitabilityBadge(
                label = "Rain-friendly",
                reason = "Sheltered heritage & cultural stop during rain",
                tone = WeatherSuitabilityTone.RAIN_FRIENDLY
            )
            isLightRainOrDrizzle && isOutdoorTrail -> WeatherSuitabilityBadge(
                label = "Trail caution",
                reason = "Light rain — bring grip footwear & dry bag",
                tone = WeatherSuitabilityTone.TRAIL_CAUTION
            )
            attraction.bestTime.en.contains("morning", ignoreCase = true) && hourOfDay24 in 12..16 && weather.temperatureC >= 31.0 ->
                WeatherSuitabilityBadge(
                    label = "Morning pick",
                    reason = "Cooler before noon · ${weather.temperatureC.toInt()}°C now",
                    tone = WeatherSuitabilityTone.MORNING_PICK
                )
            else -> WeatherSuitabilityBadge(
                label = "Great right now",
                reason = "Clear ${weather.temperatureC.toInt()}°C · ideal for exploring",
                tone = WeatherSuitabilityTone.GREAT
            )
        }
    }

    /**
     * Unified quick search across Attractions, Transit Routes, Emergency Hotlines,
     * Tourism Events, and Traveler Reviews.
     */
    fun performUnifiedSearch(
        query: String,
        attractions: List<Attraction>,
        transitMethods: List<TransitMethod>,
        emergencyContacts: List<EmergencyContact>,
        tourismEvents: List<TourismEvent>,
        reviews: List<CommunityReview>,
        language: AppLanguage
    ): UnifiedSearchResults {
        val q = query.trim().lowercase(Locale.US)
        if (q.isBlank()) {
            return UnifiedSearchResults(
                query = "",
                attractions = emptyList(),
                transitMethods = emptyList(),
                emergencyContacts = emptyList(),
                tourismEvents = emptyList(),
                reviews = emptyList()
            )
        }

        val matchedAttractions = attractions.filter { spot ->
            spot.name.get(language).lowercase(Locale.US).contains(q) ||
                spot.name.en.lowercase(Locale.US).contains(q) ||
                spot.subtitle.get(language).lowercase(Locale.US).contains(q) ||
                spot.barangay.lowercase(Locale.US).contains(q) ||
                spot.categoryCode.lowercase(Locale.US).contains(q)
        }

        val matchedTransit = transitMethods.filter { transit ->
            transit.name.lowercase(Locale.US).contains(q) ||
                transit.routeSummary.lowercase(Locale.US).contains(q) ||
                transit.operatorName.lowercase(Locale.US).contains(q) ||
                transit.terminalLocation.lowercase(Locale.US).contains(q) ||
                transit.fareRangePhp.lowercase(Locale.US).contains(q) ||
                transit.modeCategory.lowercase(Locale.US).contains(q)
        }

        val matchedHotlines = emergencyContacts.filter { contact ->
            contact.title.get(language).lowercase(Locale.US).contains(q) ||
                contact.department.lowercase(Locale.US).contains(q) ||
                contact.phoneNumber.lowercase(Locale.US).contains(q) ||
                contact.priorityLevel.lowercase(Locale.US).contains(q)
        }

        val matchedEvents = tourismEvents.filter { event ->
            event.active && (
                event.title.lowercase(Locale.US).contains(q) ||
                    event.summary.lowercase(Locale.US).contains(q) ||
                    event.locationName.lowercase(Locale.US).contains(q) ||
                    event.eventType.lowercase(Locale.US).contains(q)
                )
        }

        val matchedReviews = reviews.filter { rev ->
            rev.attractionName.lowercase(Locale.US).contains(q) ||
                rev.comment.lowercase(Locale.US).contains(q) ||
                rev.authorName.lowercase(Locale.US).contains(q) ||
                rev.visitTag.lowercase(Locale.US).contains(q)
        }.take(6)

        return UnifiedSearchResults(
            query = query.trim(),
            attractions = matchedAttractions,
            transitMethods = matchedTransit,
            emergencyContacts = matchedHotlines,
            tourismEvents = matchedEvents,
            reviews = matchedReviews
        )
    }

    /**
     * Formats a clean, shareable trip itinerary & budget summary for native Android sharing.
     */
    fun formatShareableItineraryText(
        travelerName: String = "Dumingag Traveler",
        itinerary: List<ItineraryItem>,
        attractions: List<Attraction>,
        expenses: List<TripExpense>,
        packingItems: List<com.example.model.PackingItem> = emptyList(),
        budgetLimitPhp: Double = 2500.0
    ): String {
        val dateFmt = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US)
        val spotById = attractions.associateBy { it.id }
        val totalSpent = expenses.sumOf { it.amountPhp }
        val remaining = (budgetLimitPhp - totalSpent).coerceAtLeast(0.0)

        return buildString {
            appendLine("🌿 Dumingag Travel Plan — ${travelerName.ifBlank { "Traveler" }}")
            appendLine("Zamboanga del Sur, Philippines")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine("📍 PLANNED STOPS (${itinerary.size})")
            if (itinerary.isEmpty()) {
                appendLine("• No stops scheduled yet — explore Dumingag's waterfalls & organic farms!")
            } else {
                itinerary.sortedBy { it.plannedDateMillis }.forEachIndexed { idx, item ->
                    val spot = spotById[item.attractionId]
                    val dateStr = if (item.plannedDateMillis > 0L) {
                        dateFmt.format(Date(item.plannedDateMillis))
                    } else {
                        "Flexible time"
                    }
                    appendLine("${idx + 1}. ${item.attractionName}")
                    appendLine("   🕒 $dateStr")
                    if (spot != null) {
                        appendLine("   📌 Brgy. ${spot.barangay} · Fee: ${spot.entryFee.en}")
                        appendLine("   🛺 ${spot.transitHint.en}")
                    }
                    if (item.notes.isNotBlank()) {
                        appendLine("   📝 ${item.notes}")
                    }
                }
            }
            appendLine()
            appendLine("💰 TRIP BUDGET SUMMARY")
            appendLine("• Budget Target: ₱${String.format(Locale.US, "%,.0f", budgetLimitPhp)}")
            appendLine("• Logged Spent: ₱${String.format(Locale.US, "%,.0f", totalSpent)} (${expenses.size} items)")
            appendLine("• Remaining: ₱${String.format(Locale.US, "%,.0f", remaining)}")
            if (expenses.isNotEmpty()) {
                appendLine()
                appendLine("Recent Expenses:")
                expenses.take(5).forEach { exp ->
                    appendLine("• ${exp.title} (${exp.category}): ₱${String.format(Locale.US, "%,.0f", exp.amountPhp)}")
                }
            }
            if (packingItems.isNotEmpty()) {
                val packedCount = packingItems.count { it.checked }
                appendLine()
                appendLine("🎒 PACKING CHECKLIST ($packedCount/${packingItems.size} ready)")
                packingItems.filter { !it.checked }.take(4).forEach { item ->
                    appendLine("• [ ] ${item.label}")
                }
            }
            appendLine()
            append("Shared via Dumingag Travel Guide")
        }
    }

    /**
     * Computes summary analytics for the Admin Console Insights Overview card.
     */
    fun computeAdminInsights(
        attractions: List<Attraction>,
        reviews: List<CommunityReview>,
        categories: List<ManagedCategory>,
        events: List<TourismEvent>,
        trailReports: List<TrailConditionReport>
    ): AdminInsightsSummary {
        val avgRating = if (reviews.isEmpty()) {
            if (attractions.isEmpty()) 0.0 else attractions.map { it.baseRating }.average()
        } else {
            reviews.map { it.rating.toDouble() }.average()
        }

        val reviewsBySpot = reviews.groupBy { it.attractionId }
        val spotRatings = attractions.map { spot ->
            val spotReviews = reviewsBySpot[spot.id].orEmpty()
            val rating = if (spotReviews.isNotEmpty()) {
                spotReviews.map { it.rating.toDouble() }.average()
            } else {
                spot.baseRating
            }
            Triple(spot.name.en, rating, spotReviews.size)
        }

        val topRated = spotRatings.maxByOrNull { it.second }
        val mostReviewed = spotRatings.maxByOrNull { it.third }

        val categoryLabelByCode = categories.associate { it.effectiveCode to it.name }
        val breakdown = attractions
            .groupBy { it.categoryCode.ifBlank { it.category.name } }
            .entries
            .map { (code, items) ->
                val label = categoryLabelByCode[code] ?: code.replace('_', ' ')
                    .lowercase(Locale.US)
                    .replaceFirstChar { it.uppercaseChar() }
                label to items.size
            }
            .sortedByDescending { it.second }

        return AdminInsightsSummary(
            totalAttractions = attractions.size,
            totalReviews = reviews.size,
            averageCommunityRating = avgRating,
            photoReviewCount = reviews.count { it.photoData.isNotBlank() },
            verifiedVisitReviewCount = reviews.count { it.verifiedVisit },
            officialRepliesCount = reviews.count { it.adminReply.isNotBlank() },
            activeEventsCount = events.count { it.active },
            activeTrailReportsCount = trailReports.size,
            topRatedSpotName = topRated?.first ?: "—",
            topRatedSpotRating = topRated?.second ?: 0.0,
            mostReviewedSpotName = if ((mostReviewed?.third ?: 0) > 0) mostReviewed?.first ?: "—" else "No reviews yet",
            mostReviewedSpotCount = mostReviewed?.third ?: 0,
            categoryBreakdown = breakdown
        )
    }
}
