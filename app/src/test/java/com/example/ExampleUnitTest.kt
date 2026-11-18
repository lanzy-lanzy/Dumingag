package com.example

import com.example.model.AttractionCategory
import com.example.model.AttractionImageMapper
import com.example.model.CloudAttraction
import com.example.model.CloudEmergencyContact
import org.junit.Assert.*
import org.junit.Test

/**
 * Local unit tests for Dumingag Travel Guide domain mappers.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun cloudAttraction_mapsToDomainWithPresetAndCustomPhoto() {
    val cloud = CloudAttraction(
      id = "custom_spot_1",
      mapNodeId = "node_poblacion",
      nameEn = "Guintananan Peak",
      subtitleEn = "Sunrise Ridge",
      descriptionEn = "Scenic mountain overlook in Dumingag.",
      historyNoteEn = "Community conservation site.",
      barangay = "Guintananan",
      category = "NATURE",
      latitude = 8.18,
      longitude = 123.33,
      openHours = "05:00 AM - 06:00 PM",
      entryFeeEn = "PHP 20",
      bestTimeEn = "Sunrise",
      transitHintEn = "Habal-habal from Poblacion",
      baseRating = 4.9,
      imageKey = "highland",
      photoData = "base64payload"
    )
    val domain = cloud.toDomain()
    assertEquals("custom_spot_1", domain.id)
    assertEquals(AttractionCategory.NATURE, domain.category)
    assertEquals("highland", domain.imageKey)
    assertEquals("base64payload", domain.photoData)
    assertEquals(AttractionImageMapper.resForKey("highland"), domain.imageRes)
  }

  @Test
  fun cloudEmergencyContact_normalizesPriorityAndMapsDomain() {
    val cloud = CloudEmergencyContact(
      id = "lgu_rescue",
      titleEn = "Dumingag MDRRMO",
      department = "Municipal Disaster Office",
      phoneNumber = "0917-555-0101",
      availability = "24/7",
      priorityLevel = "CRITICAL",
      websiteUrl = "https://dumingag.com"
    )
    val domain = cloud.toDomain()
    assertEquals("lgu_rescue", domain.id)
    assertEquals("CRITICAL", domain.priorityLevel)
    assertEquals("0917-555-0101", domain.phoneNumber)
    assertEquals("https://dumingag.com", domain.websiteUrl)
  }
}
