package com.example.data

import com.example.model.Attraction
import com.example.model.AttractionCategory
import com.example.model.EcoBadge
import com.example.model.ElevationProfile
import com.example.model.ElevationSample
import com.example.model.FareEstimateOption
import com.example.model.ItineraryItem
import com.example.model.LocalSpecialty
import com.example.model.OptimizedItineraryResult
import com.example.model.PackingItem
import com.example.model.PhraseEntry
import com.example.model.TrailConditionReport
import com.example.model.WeatherSnapshot
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object TravelCompanionEngines {

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthR = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2.0) * sin(dLat / 2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2.0) * sin(dLon / 2.0)
        return earthR * 2.0 * atan2(sqrt(a), sqrt((1.0 - a).coerceAtLeast(0.0)))
    }

    /**
     * Computes geographic initial bearing (0° = North, 90° = East, 180° = South, 270° = West).
     */
    fun computeBearingDegrees(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Float {
        val lat1 = Math.toRadians(fromLat)
        val lat2 = Math.toRadians(toLat)
        val dLon = Math.toRadians(toLon - fromLon)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val bearing = (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
        return bearing.toFloat()
    }

    fun cardinalDirection(degrees: Float): String {
        val normalized = ((degrees % 360f) + 360f) % 360f
        val labels = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val idx = ((normalized + 22.5f) / 45f).toInt() % labels.size
        return labels[idx]
    }

    /**
     * Deterministic topographic elevation model for Dumingag, Zamboanga del Sur:
     * - Poblacion / valley floor ~185 m
     * - Mt. Timolan / Malindang ridge (8.1360, 123.3110) ~1,040 m
     * - Danlugan & highland falls (~8.185, 123.325) ~460 m
     * - Organic highland terraces (~8.192, 123.368) ~390 m
     */
    fun estimateElevationMeters(lat: Double, lon: Double): Double {
        val baseValley = 185.0
        fun gaussianPeak(pLat: Double, pLon: Double, heightM: Double, radiusDeg: Double): Double {
            val dLat = lat - pLat
            val dLon = lon - pLon
            val d2 = dLat * dLat + dLon * dLon
            return heightM * exp(-d2 / (2.0 * radiusDeg * radiusDeg))
        }
        val mtTimolan = gaussianPeak(8.1360, 123.3110, 845.0, 0.022)
        val danluganRidge = gaussianPeak(8.1860, 123.3210, 290.0, 0.018)
        val northernTerraces = gaussianPeak(8.1950, 123.3620, 225.0, 0.020)
        val southernFoothills = gaussianPeak(8.1480, 123.3720, 190.0, 0.022)
        val undulatingDetail = 18.0 * sin(lat * 260.0) * cos(lon * 260.0)
        return (baseValley + mtTimolan + danluganRidge + northernTerraces + southernFoothills + undulatingDetail)
            .coerceIn(140.0, 1180.0)
    }

    fun buildElevationProfile(
        waypoints: List<Pair<Double, Double>>,
        samplesPerSegment: Int = 8
    ): ElevationProfile {
        if (waypoints.isEmpty()) {
            return ElevationProfile(emptyList(), 185, 185, 0, 0)
        }
        if (waypoints.size == 1) {
            val elev = estimateElevationMeters(waypoints.first().first, waypoints.first().second)
            val eInt = elev.roundToInt()
            return ElevationProfile(
                samples = listOf(ElevationSample(0.0, elev)),
                minMeters = eInt,
                maxMeters = eInt,
                gainMeters = 0,
                lossMeters = 0
            )
        }

        val samples = mutableListOf<ElevationSample>()
        var cumulativeKm = 0.0
        val steps = samplesPerSegment.coerceAtLeast(2)

        for (i in 0 until waypoints.lastIndex) {
            val (lat1, lon1) = waypoints[i]
            val (lat2, lon2) = waypoints[i + 1]
            val segKm = haversineKm(lat1, lon1, lat2, lon2)
            val startStep = if (i == 0) 0 else 1
            for (s in startStep..steps) {
                val t = s.toDouble() / steps.toDouble()
                val lat = lat1 + (lat2 - lat1) * t
                val lon = lon1 + (lon2 - lon1) * t
                val distKm = cumulativeKm + segKm * t
                samples.add(ElevationSample(distKm, estimateElevationMeters(lat, lon)))
            }
            cumulativeKm += segKm
        }

        var minE = Double.MAX_VALUE
        var maxE = Double.MIN_VALUE
        var gain = 0.0
        var loss = 0.0
        samples.forEachIndexed { idx, sample ->
            minE = min(minE, sample.elevationMeters)
            maxE = max(maxE, sample.elevationMeters)
            if (idx > 0) {
                val delta = sample.elevationMeters - samples[idx - 1].elevationMeters
                if (delta > 0) gain += delta else loss += -delta
            }
        }
        return ElevationProfile(
            samples = samples,
            minMeters = minE.roundToInt(),
            maxMeters = maxE.roundToInt(),
            gainMeters = gain.roundToInt(),
            lossMeters = loss.roundToInt()
        )
    }

    /**
     * Estimates realistic local Dumingag fares (PHP) & travel durations for Tricycle,
     * Habal-Habal (motorcycle), and Van/Multicab based on road distance & terrain.
     */
    fun estimateDumingagFares(
        distanceKm: Double,
        isMountainRoute: Boolean
    ): List<FareEstimateOption> {
        val effectiveRoadKm = (distanceKm * if (isMountainRoute) 1.32 else 1.18).coerceAtLeast(0.5)

        val trikeMin = (15 + (effectiveRoadKm * 8.0)).roundToInt().coerceAtLeast(15)
        val trikeMax = (trikeMin + if (effectiveRoadKm > 3.5) 25 else 15)
        val trikeMins = ((effectiveRoadKm / 20.0) * 60.0).roundToInt().coerceAtLeast(4)

        val habalBase = if (isMountainRoute) 40.0 else 25.0
        val habalRate = if (isMountainRoute) 18.0 else 12.0
        val habalMin = (habalBase + effectiveRoadKm * habalRate).roundToInt().coerceAtLeast(30)
        val habalMax = (habalMin * 1.25).roundToInt().coerceAtLeast(habalMin + 15)
        val habalMins = ((effectiveRoadKm / (if (isMountainRoute) 24.0 else 32.0)) * 60.0).roundToInt().coerceAtLeast(4)

        val vanMin = (25 + effectiveRoadKm * 7.5).roundToInt().coerceAtLeast(25)
        val vanMax = (vanMin + 20)
        val vanMins = ((effectiveRoadKm / 36.0) * 60.0).roundToInt().coerceAtLeast(5)

        val options = mutableListOf<FareEstimateOption>()
        if (!isMountainRoute && effectiveRoadKm <= 7.0) {
            options.add(
                FareEstimateOption(
                    modeName = "Town Tricycle",
                    fareRangePhp = "₱$trikeMin – ₱$trikeMax",
                    minFarePhp = trikeMin,
                    maxFarePhp = trikeMax,
                    durationText = formatDuration(trikeMins),
                    suitabilityNote = "Best for Poblacion & paved town roads"
                )
            )
        }
        options.add(
            FareEstimateOption(
                modeName = "Habal-Habal (Motorcycle)",
                fareRangePhp = "₱$habalMin – ₱$habalMax",
                minFarePhp = habalMin,
                maxFarePhp = habalMax,
                durationText = formatDuration(habalMins),
                suitabilityNote = if (isMountainRoute) "Recommended for steep highland & falls trails" else "Fast point-to-point local ride"
            )
        )
        options.add(
            FareEstimateOption(
                modeName = "Multicab / Commuter Van",
                fareRangePhp = "₱$vanMin – ₱$vanMax",
                minFarePhp = vanMin,
                maxFarePhp = vanMax,
                durationText = formatDuration(vanMins),
                suitabilityNote = if (isMountainRoute) "Drop-off at highway junction; short walk/habal ahead" else "Budget shared fare along highway corridors"
            )
        )
        return options
    }

    private fun formatDuration(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} hr"
        else -> "${minutes / 60} hr ${minutes % 60} min"
    }

    /**
     * Generates a smart weather- and itinerary-aware packing checklist for Dumingag.
     */
    fun generateSmartPackingChecklist(
        weather: WeatherSnapshot?,
        itineraryNames: List<String>,
        checkedIds: Set<String>
    ): List<PackingItem> {
        val maxPrecip = weather?.dailyForecasts?.maxOfOrNull { it.precipChancePct } ?: 25
        val weatherCode = weather?.weatherCode ?: 2
        val isRainLikely = maxPrecip >= 45 || weatherCode >= 51
        val isHot = (weather?.temperatureC ?: 28.0) >= 30.0
        val namesJoined = itineraryNames.joinToString(" ").lowercase()
        val hasWaterOrFalls = namesJoined.contains("fall") || namesJoined.contains("spring") || namesJoined.contains("river")
        val hasMountainOrTrail = namesJoined.contains("mt") || namesJoined.contains("peak") || namesJoined.contains("timolan") || namesJoined.contains("cave")

        val raw = buildList {
            add(
                PackingItem(
                    id = "pack_rain_poncho",
                    label = if (isRainLikely) "Compact Rain Poncho & Dry Bag" else "Foldable Umbrella",
                    category = "Weather",
                    reason = if (isRainLikely) "Forecast shows $maxPrecip% rain chance in Dumingag" else "Sudden highland showers are common in afternoons",
                    isWeatherRecommended = isRainLikely,
                    checked = false
                )
            )
            add(
                PackingItem(
                    id = "pack_grip_footwear",
                    label = "Grip Trail Sandals / Hiking Shoes",
                    category = "Footwear",
                    reason = if (isRainLikely || hasMountainOrTrail) "Wet stone steps near waterfalls & highland ridges" else "Comfortable walking on eco-trails & farm paths",
                    isWeatherRecommended = isRainLikely || hasMountainOrTrail,
                    checked = false
                )
            )
            add(
                PackingItem(
                    id = "pack_refill_bottle",
                    label = "Reusable Water Tumbler (1L)",
                    category = "Eco-Gear",
                    reason = if (isHot) "High heat (${weather?.temperatureC?.roundToInt()}°C) — stay hydrated" else "Supports Dumingag's zero-plastic organic municipality ordinance",
                    isWeatherRecommended = isHot,
                    checked = false
                )
            )
            add(
                PackingItem(
                    id = "pack_cash_small",
                    label = "Small PHP Bills & Coins",
                    category = "Essentials",
                    reason = "Habal-habal fares, organic farm stands & eco-fees are cash-only",
                    isWeatherRecommended = false,
                    checked = false
                )
            )
            add(
                PackingItem(
                    id = "pack_powerbank",
                    label = "Charged Powerbank & Offline Map",
                    category = "Tech",
                    reason = "GPS navigation & 3D map viewing in remote mountain barangays",
                    isWeatherRecommended = false,
                    checked = false
                )
            )
            add(
                PackingItem(
                    id = "pack_repellent",
                    label = "Eco Insect Repellent & Sunscreen",
                    category = "Health",
                    reason = if (isHot) "Strong midday tropical UV & forest trails" else "Forest canopy protection near waterfalls & caves",
                    isWeatherRecommended = isHot,
                    checked = false
                )
            )
            if (hasWaterOrFalls || itineraryNames.isEmpty()) {
                add(
                    PackingItem(
                        id = "pack_quickdry",
                        label = "Quick-Dry Towel & Extra Shirt",
                        category = "Waterfalls",
                        reason = "Great after visiting Danlugan Falls or highland springs",
                        isWeatherRecommended = false,
                        checked = false
                    )
                )
            }
            if (hasMountainOrTrail) {
                add(
                    PackingItem(
                        id = "pack_headlamp",
                        label = "Light Jacket & Compact Flashlight",
                        category = "Highlands",
                        reason = "Cooler ridge breeze & shaded cave/forest trails",
                        isWeatherRecommended = true,
                        checked = false
                    )
                )
            }
        }
        return raw.map { it.copy(checked = it.id in checkedIds) }
    }

    /**
     * Nearest-neighbor TSP route optimizer for planned itinerary stops starting from [userLatLon].
     */
    fun optimizeItineraryOrder(
        userLatLon: Pair<Double, Double>,
        items: List<ItineraryItem>,
        attractions: List<Attraction>
    ): OptimizedItineraryResult {
        if (items.size <= 1) {
            return OptimizedItineraryResult(items, 0.0, 0.0, 0.0)
        }
        val byId = attractions.associateBy { it.id }

        fun pathDistance(sequence: List<ItineraryItem>): Double {
            var curLat = userLatLon.first
            var curLon = userLatLon.second
            var sum = 0.0
            sequence.forEach { stop ->
                val attr = byId[stop.attractionId]
                if (attr != null) {
                    sum += haversineKm(curLat, curLon, attr.latitude, attr.longitude)
                    curLat = attr.latitude
                    curLon = attr.longitude
                }
            }
            return sum
        }

        val originalKm = pathDistance(items)
        val remaining = items.toMutableList()
        val ordered = mutableListOf<ItineraryItem>()
        var curLat = userLatLon.first
        var curLon = userLatLon.second

        while (remaining.isNotEmpty()) {
            val bestIdx = remaining.indices.minByOrNull { idx ->
                val attr = byId[remaining[idx].attractionId]
                if (attr != null) {
                    haversineKm(curLat, curLon, attr.latitude, attr.longitude)
                } else {
                    Double.MAX_VALUE
                }
            } ?: 0
            val next = remaining.removeAt(bestIdx)
            ordered.add(next)
            byId[next.attractionId]?.let {
                curLat = it.latitude
                curLon = it.longitude
            }
        }

        val optimizedKm = pathDistance(ordered)
        val savedKm = (originalKm - optimizedKm).coerceAtLeast(0.0)
        return OptimizedItineraryResult(
            orderedItems = ordered,
            originalDistanceKm = originalKm,
            optimizedDistanceKm = optimizedKm,
            savedDistanceKm = savedKm
        )
    }

    /**
     * Computes Eco-Traveler Passport badges from GPS check-ins, reviews, trail reports, and offline readiness.
     */
    fun computeEcoBadges(
        checkedInIds: Set<String>,
        userReviewCount: Int,
        trailReportCount: Int,
        offlinePackReady: Boolean,
        attractions: List<Attraction>
    ): List<EcoBadge> {
        val checkedAttractions = attractions.filter { it.id in checkedInIds }
        val natureCount = checkedAttractions.count { it.category == AttractionCategory.NATURE }
        val farmCount = checkedAttractions.count { it.category == AttractionCategory.ORGANIC_FARM }
        val heritageCount = checkedAttractions.count {
            it.category == AttractionCategory.HISTORICAL || it.category == AttractionCategory.CULTURAL
        }
        val totalCheckIns = checkedInIds.size

        return listOf(
            EcoBadge(
                id = "badge_first_stamp",
                title = "Highland Explorer",
                subtitle = "Check in at your first Dumingag destination",
                unlocked = totalCheckIns >= 1,
                progressText = "${totalCheckIns.coerceAtMost(1)}/1 stamp",
                iconKey = "explore"
            ),
            EcoBadge(
                id = "badge_nature_chaser",
                title = "Waterfall & Peak Chaser",
                subtitle = "Check in at 2 natural wonders or mountain ridges",
                unlocked = natureCount >= 2,
                progressText = "${natureCount.coerceAtMost(2)}/2 nature",
                iconKey = "nature"
            ),
            EcoBadge(
                id = "badge_organic_steward",
                title = "Organic Farm Steward",
                subtitle = "Visit an organic farm or sustainable agriculture site",
                unlocked = farmCount >= 1,
                progressText = "${farmCount.coerceAtMost(1)}/1 farm",
                iconKey = "eco"
            ),
            EcoBadge(
                id = "badge_heritage_keeper",
                title = "Subanen & Heritage Keeper",
                subtitle = "Explore a cultural or historical Dumingag landmark",
                unlocked = heritageCount >= 1,
                progressText = "${heritageCount.coerceAtMost(1)}/1 heritage",
                iconKey = "heritage"
            ),
            EcoBadge(
                id = "badge_trail_guardian",
                title = "Trail Guardian",
                subtitle = "Post a trail condition report or traveler review",
                unlocked = (trailReportCount + userReviewCount) >= 1,
                progressText = "${(trailReportCount + userReviewCount).coerceAtMost(1)}/1 report",
                iconKey = "shield"
            ),
            EcoBadge(
                id = "badge_offline_ready",
                title = "Zero-Signal Voyager",
                subtitle = "Download the Dumingag Offline Map & Safety Pack",
                unlocked = offlinePackReady,
                progressText = if (offlinePackReady) "Unlocked" else "Pack pending",
                iconKey = "offline"
            )
        )
    }

    val starterTrailReports: List<TrailConditionReport> = listOf(
        TrailConditionReport(
            id = "seed_rep_1",
            attractionId = "attr_danlugan_falls",
            conditionTag = "Strong waterfall flow",
            crowdLevel = "Quiet",
            note = "Morning mist is crisp and the stone path is clean; bring grip sandals near the lower basin.",
            reporterName = "Local Eco-Guide",
            reportedAtMillis = System.currentTimeMillis() - 6 * 3600_000L
        ),
        TrailConditionReport(
            id = "seed_rep_2",
            attractionId = "attr_mt_timolan",
            conditionTag = "Clear trail",
            crowdLevel = "Quiet",
            note = "Good visibility at sunrise; breezy along the ridge, light jacket recommended.",
            reporterName = "Dumingag Mountaineers",
            reportedAtMillis = System.currentTimeMillis() - 14 * 3600_000L
        ),
        TrailConditionReport(
            id = "seed_rep_3",
            attractionId = "attr_organic_village",
            conditionTag = "Clear trail",
            crowdLevel = "Moderate",
            note = "Fresh organic red rice and highland vegetables available at the morning farm cooperative.",
            reporterName = "Municipal Eco-Desk",
            reportedAtMillis = System.currentTimeMillis() - 20 * 3600_000L
        )
    )
}

object DumingagPhrasebook {
    val categories = listOf("Greetings", "Transit & Fare", "Directions", "Emergency")

    val phrases: List<PhraseEntry> = listOf(
        PhraseEntry(
            id = "phr_hello",
            category = "Greetings",
            english = "Good morning / Thank you",
            cebuano = "Maayong buntag / Daghang salamat",
            tagalog = "Magandang umaga / Maraming salamat",
            subanen = "Gupia g'ndaw / Salamat",
            pronunciation = "Mah-AH-yong BOON-tag / Dahg-hang sah-LAH-mat"
        ),
        PhraseEntry(
            id = "phr_beautiful",
            category = "Greetings",
            english = "Dumingag is very beautiful!",
            cebuano = "Kanindot kaayo sa Dumingag!",
            tagalog = "Napakaganda ng Dumingag!",
            subanen = "Landu g'pia su Dumingag!",
            pronunciation = "Kah-NEEN-dot kah-AH-yo sah Doo-MEE-ngag"
        ),
        PhraseEntry(
            id = "phr_fare",
            category = "Transit & Fare",
            english = "How much is the fare to the falls?",
            cebuano = "Tagpila ang plete padulong sa busay?",
            tagalog = "Magkano ang pamasahe papuntang talon?",
            subanen = "Pila su plete dutun sa busay?",
            pronunciation = "Tahg-PEE-lah ahng PLEH-teh pah-doo-LONG sah BOO-sai"
        ),
        PhraseEntry(
            id = "phr_stop",
            category = "Transit & Fare",
            english = "Please stop here at the plaza.",
            cebuano = "Lugar lang dinhi sa plaza.",
            tagalog = "Para po dito sa plaza.",
            subanen = "Pundo dini sa plaza.",
            pronunciation = "Loo-GAHR lahng DEEN-hee sah PLAH-zah"
        ),
        PhraseEntry(
            id = "phr_where",
            category = "Directions",
            english = "Where is the road to Poblacion?",
            cebuano = "Asa ang dalan padulong sa Poblacion?",
            tagalog = "Saan ang daan papuntang Poblacion?",
            subanen = "Ain su dalan dutun sa Poblacion?",
            pronunciation = "AH-sah ahng DAH-lahn pah-doo-LONG sah Pob-lah-SYON"
        ),
        PhraseEntry(
            id = "phr_far",
            category = "Directions",
            english = "Is the trail still far?",
            cebuano = "Layo pa ba ang dalan?",
            tagalog = "Malayo pa ba ang daan?",
            subanen = "Mayu pa ba su dalan?",
            pronunciation = "Lah-YO pah bah ahng DAH-lahn"
        ),
        PhraseEntry(
            id = "phr_help",
            category = "Emergency",
            english = "Please help, we need medical assistance.",
            cebuano = "Tabang palihug, nagkinahanglan mi ug tabang medikal.",
            tagalog = "Tulong po, kailangan namin ng tulong medikal.",
            subanen = "Tabang amu, kailangan nami tabang.",
            pronunciation = "Tah-BAHNG pah-LEE-hoog, nahg-kee-nah-HAHNG-lahn mee"
        ),
        PhraseEntry(
            id = "phr_clinic",
            category = "Emergency",
            english = "Where is the nearest health center or police station?",
            cebuano = "Asa ang pinakaduol nga health center o police station?",
            tagalog = "Saan ang pinakamalapit na health center o estasyon ng pulis?",
            subanen = "Ain su m'dani nga health center?",
            pronunciation = "AH-sah ahng pee-nah-kah-DOO-ol ngah health center"
        )
    )
}

object DumingagLocalFoodGuide {
    val specialties: List<LocalSpecialty> = listOf(
        LocalSpecialty(
            id = "food_organic_rice",
            name = "Dumingag Organic Red & Black Rice",
            category = "Organic Harvest",
            whereToFind = "Poblacion Organic Trading Post & Farm Co-ops",
            priceHint = "₱75 – ₱95 / kg",
            scheduleNote = "Best fresh stock every Saturday & Sunday Tabo",
            description = "Chemical-free heritage upland rice championed by Dumingag's nationwide sustainable organic agriculture program.",
            highlightTag = "Signature Pasalubong"
        ),
        LocalSpecialty(
            id = "food_native_coffee",
            name = "Mt. Timolan Roasted Highland Coffee",
            category = "Artisan Brew",
            whereToFind = "Poblacion Public Market & Barangay Eco-Stalls",
            priceHint = "₱150 – ₱220 / 250g pack",
            scheduleNote = "Daily 6:00 AM – 5:00 PM",
            description = "Small-batch Robusta and Excelsa beans sun-dried by upland farming families along the Mt. Timolan foothills.",
            highlightTag = "Local Roaster"
        ),
        LocalSpecialty(
            id = "food_tablea",
            name = "Pure Zamboanga Cacao Tablea",
            category = "Delicacy",
            whereToFind = "Dumingag Municipal Market & Cooperative Center",
            priceHint = "₱120 – ₱180 / roll",
            scheduleNote = "Freshly stone-ground every Thursday & Sunday",
            description = "Rich unsweetened roasted cacao rounds for traditional hot sikwate breakfast paired with suman.",
            highlightTag = "Morning Favorite"
        ),
        LocalSpecialty(
            id = "food_puto_sikwate",
            name = "Puto Maya, Suman & Sikwate",
            category = "Street Breakfast",
            whereToFind = "Poblacion Gymnasium & Night/Morning Market",
            priceHint = "₱35 – ₱60 / serving",
            scheduleNote = "5:30 AM – 9:30 AM daily; Night Market 5:30 PM+",
            description = "Warm glutinous rice cooked in ginger-infused coconut milk served beside ripe mango slices and hot local cacao.",
            highlightTag = "Must-Try Dish"
        ),
        LocalSpecialty(
            id = "food_tabo_produce",
            name = "Sunday Tabo Highland Organic Produce",
            category = "Market Day",
            whereToFind = "Dumingag Poblacion Public Market Grounds",
            priceHint = "₱25 – ₱100 / bundle",
            scheduleNote = "Main Tabo: Sundays 5:00 AM – 2:00 PM",
            description = "Vibrant weekly farmers' market featuring heirloom squash, wild ferns (pako), native honey, and woven Subanen crafts.",
            highlightTag = "Weekly Market"
        )
    )
}
