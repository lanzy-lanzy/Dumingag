package com.example

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.data.DumingagSeedData
import com.example.data.local.TravelGuideCacheDataStore
import com.example.model.EmergencyContact
import com.example.model.LocalizedText
import com.example.model.TransitMethod
import com.example.model.TransitModeCategory
import com.example.model.TransitServiceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TravelGuideCacheDataStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createCache(
        file: File,
        scope: CoroutineScope
    ): TravelGuideCacheDataStore {
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file }
        )
        return TravelGuideCacheDataStore(store)
    }

    @Test
    fun savesAndRestoresTransitMethodsAndEmergencyContactsAcrossOfflineRestart() = runTest {
        val prefsFile = File(tempFolder.newFolder("offline_restart"), "travel_guide_cache.preferences_pb")
        val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val customTransit = listOf(
            TransitMethod(
                id = "tr_pagadian_van",
                userId = "admin_1",
                name = "Dumingag - Pagadian Express Van",
                modeCategory = TransitModeCategory.BUS_VAN.code,
                operatorName = "ZDS Highland Transport",
                routeSummary = "Poblacion Terminal -> Molave -> Pagadian Agora",
                scheduleText = "05:00 AM - 05:30 PM (Every 30 min)",
                fareRangePhp = "PHP 120 - 150",
                durationText = "1 hr 15 min",
                terminalLocation = "Dumingag Integrated Public Terminal",
                travelTip = "Arrive 15 minutes early for front seats.",
                serviceStatus = TransitServiceStatus.OPERATIONAL.code,
                visibility = "public"
            )
        )
        val customContacts = listOf(
            EmergencyContact(
                id = "ec_mdrrmo_247",
                title = LocalizedText(
                    en = "Dumingag MDRRMO Rescue",
                    ceb = "Dumingag MDRRMO Tabang",
                    tl = "Dumingag MDRRMO Saklolo",
                    es = "Rescate MDRRMO Dumingag"
                ),
                department = "Disaster Risk Reduction & Rescue",
                phoneNumber = "+63 917 555 0199",
                availability = "24/7 Rapid Response",
                priorityLevel = "CRITICAL",
                email = "mdrrmo@dumingag.gov.ph",
                websiteUrl = "https://dumingag.com"
            )
        )

        try {
            val writerCache = createCache(prefsFile, writerScope)
            writerCache.saveTransitMethods(customTransit, updatedAtMillis = 1_700_000_000_000L)
            writerCache.saveEmergencyContacts(customContacts, updatedAtMillis = 1_700_000_050_000L)

            val status = writerCache.cacheStatus.first()
            assertTrue(status.isReadyOffline)
            assertEquals(1, status.transitCount)
            assertEquals(1, status.emergencyContactCount)
            assertEquals(1_700_000_050_000L, status.lastUpdatedAtMillis)
        } finally {
            writerScope.cancel()
        }

        // Simulate reopening the app completely offline on a fresh DataStore instance
        val readerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val readerCache = createCache(prefsFile, readerScope)
            val restoredTransit = readerCache.getCachedTransitSnapshot()
            val restoredContacts = readerCache.getCachedEmergencyContactsSnapshot()

            assertEquals(1, restoredTransit.size)
            assertEquals("Dumingag - Pagadian Express Van", restoredTransit.first().name)
            assertEquals("PHP 120 - 150", restoredTransit.first().fareRangePhp)
            assertEquals("Dumingag Integrated Public Terminal", restoredTransit.first().terminalLocation)

            assertEquals(1, restoredContacts.size)
            assertEquals("+63 917 555 0199", restoredContacts.first().phoneNumber)
            assertEquals("Dumingag MDRRMO Tabang", restoredContacts.first().title.ceb)
            assertEquals("mdrrmo@dumingag.gov.ph", restoredContacts.first().email)
            assertEquals("https://dumingag.com", restoredContacts.first().websiteUrl)
        } finally {
            readerScope.cancel()
        }
    }

    @Test
    fun seedEssentialsPopulatesEmptyCacheWithoutOverwritingSyncedRecords() = runTest {
        val prefsFile = File(tempFolder.newFolder("seed_behavior"), "travel_guide_cache.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val cache = createCache(prefsFile, scope)
            assertFalse(cache.cacheStatus.first().isReadyOffline)

            // First launch offline: seeds defaults into DataStore
            cache.seedEssentialsIfEmpty(
                defaultTransit = DumingagSeedData.defaultTransitMethods,
                defaultContacts = DumingagSeedData.emergencyContacts
            )
            val seededTransit = cache.getCachedTransitSnapshot()
            val seededContacts = cache.getCachedEmergencyContactsSnapshot()
            assertEquals(DumingagSeedData.defaultTransitMethods.size, seededTransit.size)
            assertEquals(DumingagSeedData.emergencyContacts.size, seededContacts.size)
            assertTrue(cache.cacheStatus.first().isReadyOffline)

            // Live cloud sync updates emergency contact hotline
            val updatedContact = DumingagSeedData.emergencyContacts.first().copy(
                phoneNumber = "+63 998 111 2233"
            )
            cache.saveEmergencyContacts(listOf(updatedContact))

            // Subsequent app launch seeding must NOT overwrite the fresher synced hotline
            cache.seedEssentialsIfEmpty(
                defaultTransit = DumingagSeedData.defaultTransitMethods,
                defaultContacts = DumingagSeedData.emergencyContacts
            )
            val afterReSeed = cache.getCachedEmergencyContactsSnapshot()
            assertEquals(1, afterReSeed.size)
            assertEquals("+63 998 111 2233", afterReSeed.first().phoneNumber)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun corruptedCacheJsonRecoversSafelyWithoutCrashing() = runTest {
        val prefsFile = File(tempFolder.newFolder("corrupt_json"), "travel_guide_cache.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val rawStore = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { prefsFile }
            )
            rawStore.edit { prefs ->
                prefs[stringPreferencesKey("cached_transit_methods_json")] = "{not-valid-json!!!"
                prefs[stringPreferencesKey("cached_emergency_contacts_json")] = "[[broken"
            }

            val cache = TravelGuideCacheDataStore(rawStore)
            assertTrue(cache.getCachedTransitSnapshot().isEmpty())
            assertTrue(cache.getCachedEmergencyContactsSnapshot().isEmpty())
            assertFalse(cache.cacheStatus.first().isReadyOffline)
        } finally {
            scope.cancel()
        }
    }
}
