package com.example

import com.example.model.CommunityReview
import com.example.ui.components.ReviewSortOption
import com.example.ui.components.buildReviewSummariesByAttraction
import com.example.ui.components.filterAndSortReviews
import com.example.ui.components.ratingQualitativeLabel
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapReviewSummaryTest {

    @Test
    fun buildReviewSummariesByAttraction_aggregatesOnlyReviewedAttractionsAccurately() {
        val reviews = listOf(
            CommunityReview(
                id = "r1",
                userId = "u1",
                authorName = "Maria",
                attractionId = "spot_falls",
                attractionName = "Dumingag Falls",
                rating = 5L,
                comment = "Crystal clear water and easy trail.",
                visitTag = "Eco-Tour",
                createdAt = Timestamp(1_700_000_100L, 0)
            ),
            CommunityReview(
                id = "r2",
                userId = "u2",
                authorName = "Carlo",
                attractionId = "spot_falls",
                attractionName = "Dumingag Falls",
                rating = 4L,
                comment = "Great morning breeze, bring water shoes.",
                visitTag = "Family",
                createdAt = Timestamp(1_700_000_300L, 0)
            ),
            CommunityReview(
                id = "r3",
                userId = "u3",
                authorName = "Lina",
                attractionId = "spot_falls",
                attractionName = "Dumingag Falls",
                rating = 3L,
                comment = "Slightly muddy after heavy rain.",
                visitTag = "Eco-Tour",
                createdAt = Timestamp(1_700_000_200L, 0)
            ),
            CommunityReview(
                id = "r4",
                userId = "u4",
                authorName = "Ben",
                attractionId = "spot_farm",
                attractionName = "Organic Village",
                rating = 5L,
                comment = "Fresh heirloom rice and warm hospitality.",
                visitTag = "Foodie",
                createdAt = Timestamp(1_700_000_400L, 0)
            )
        )

        val summaries = buildReviewSummariesByAttraction(reviews)

        // Only attractions with actual reviews appear in map summaries
        assertEquals(2, summaries.size)
        assertFalse(summaries.containsKey("spot_without_reviews"))

        val falls = summaries["spot_falls"]
        assertNotNull(falls)
        assertEquals(3, falls!!.reviewCount)
        assertEquals(4.0, falls.averageRating, 0.001)
        assertEquals("4.0", falls.formattedAverage)
        assertEquals(4, falls.roundedStars)
        assertEquals(1, falls.distribution[5])
        assertEquals(1, falls.distribution[4])
        assertEquals(1, falls.distribution[3])
        assertEquals(0, falls.distribution[2])
        assertEquals(0, falls.distribution[1])
        // 2 out of 3 reviews are >= 4 stars -> 67%
        assertEquals(67, falls.recommendationPercent)
        // Eco-Tour appears twice, Family once
        assertEquals("Eco-Tour" to 2, falls.topTripTags.first())
        assertEquals("r2", falls.latestReview?.id)

        val farm = summaries["spot_farm"]
        assertNotNull(farm)
        assertEquals(1, farm!!.reviewCount)
        assertEquals(100, farm.recommendationPercent)
    }

    @Test
    fun filterAndSortReviews_filtersByStarAndTagAndSortsDeterministically() {
        val reviews = listOf(
            CommunityReview(
                id = "a",
                attractionId = "spot_1",
                rating = 3L,
                visitTag = "Solo",
                comment = "Nice view",
                createdAt = Timestamp(100L, 0)
            ),
            CommunityReview(
                id = "b",
                attractionId = "spot_1",
                rating = 5L,
                visitTag = "Eco-Tour",
                comment = "Amazing falls",
                createdAt = Timestamp(300L, 0)
            ),
            CommunityReview(
                id = "c",
                attractionId = "spot_1",
                rating = 5L,
                visitTag = "Family",
                comment = "Kid friendly",
                createdAt = Timestamp(200L, 0)
            )
        )

        val newest = filterAndSortReviews(reviews, null, null, ReviewSortOption.NEWEST)
        assertEquals(listOf("b", "c", "a"), newest.map { it.id })

        val lowest = filterAndSortReviews(reviews, null, null, ReviewSortOption.LOWEST)
        assertEquals(listOf("a", "b", "c"), lowest.map { it.id })

        val fiveStarOnly = filterAndSortReviews(reviews, 5, null, ReviewSortOption.HIGHEST)
        assertEquals(listOf("b", "c"), fiveStarOnly.map { it.id })

        val familyOnly = filterAndSortReviews(reviews, 5, "Family", ReviewSortOption.NEWEST)
        assertEquals(listOf("c"), familyOnly.map { it.id })
    }

    @Test
    fun ratingQualitativeLabel_mapsEveryStarTier() {
        assertEquals("Exceptional", ratingQualitativeLabel(5))
        assertEquals("Great", ratingQualitativeLabel(4))
        assertEquals("Good", ratingQualitativeLabel(3))
        assertEquals("Fair", ratingQualitativeLabel(2))
        assertEquals("Needs Improvement", ratingQualitativeLabel(1))
        assertTrue(buildReviewSummariesByAttraction(emptyList()).isEmpty())
    }
}
