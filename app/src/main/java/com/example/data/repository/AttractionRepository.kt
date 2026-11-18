package com.example.data.repository

import android.content.Context
import com.example.data.DumingagSeedData
import com.example.data.local.AttractionDao
import com.example.data.local.AttractionEntity
import com.example.data.local.DumingagDatabase
import com.example.model.Attraction
import com.example.model.AttractionCategory
import com.example.model.LocalizedText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AttractionRepository(private val attractionDao: AttractionDao) {

    constructor(context: Context) : this(
        DumingagDatabase.getInstance(context.applicationContext).attractionDao()
    )

    val allAttractions: Flow<List<Attraction>> = attractionDao.observeAllAttractions()
        .map { entities ->
            if (entities.isEmpty()) DumingagSeedData.attractions
            else entities.map { it.toDomain() }
        }

    fun observeAttractionsByCategory(category: AttractionCategory): Flow<List<Attraction>> {
        return if (category == AttractionCategory.ALL) {
            allAttractions
        } else {
            attractionDao.observeByCategory(category.name)
                .map { entities -> entities.map { it.toDomain() } }
        }
    }

    fun searchAttractions(query: String): Flow<List<Attraction>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            allAttractions
        } else {
            attractionDao.searchAttractions(trimmed)
                .map { entities -> entities.map { it.toDomain() } }
        }
    }

    fun observeFilteredAttractions(
        query: String,
        category: AttractionCategory
    ): Flow<List<Attraction>> {
        val trimmed = query.trim()
        val categoryName = category.takeIf { it != AttractionCategory.ALL }?.name
        return attractionDao.observeFilteredAttractions(trimmed, categoryName)
            .map { entities -> entities.map { it.toDomain() } }
    }

    fun observeAttractionById(id: String): Flow<Attraction?> =
        attractionDao.observeById(id).map { entity -> entity?.toDomain() }

    suspend fun getAttractionById(id: String): Attraction? =
        attractionDao.getById(id)?.toDomain()

    suspend fun seedAttractionsIfEmpty() {
        if (attractionDao.count() == 0) {
            attractionDao.insertAll(DumingagSeedData.attractions.map { it.toEntity() })
        }
    }

    suspend fun insertAttraction(attraction: Attraction, isSavedOffline: Boolean = false) {
        attractionDao.insert(attraction.toEntity(isSavedOffline = isSavedOffline))
    }

    suspend fun insertAttractions(attractions: List<Attraction>, isSavedOffline: Boolean = false) {
        attractionDao.insertAll(attractions.map { it.toEntity(isSavedOffline = isSavedOffline) })
    }

    suspend fun updateAttraction(attraction: Attraction, isSavedOffline: Boolean = false) {
        attractionDao.update(attraction.toEntity(isSavedOffline = isSavedOffline))
    }

    suspend fun setSavedOffline(id: String, saved: Boolean) {
        attractionDao.setSavedOffline(id, saved)
    }

    suspend fun deleteAttraction(id: String) {
        attractionDao.deleteById(id)
    }

    suspend fun syncFromCloud(cloudAttractions: List<Attraction>) {
        if (cloudAttractions.isEmpty()) return
        val existingSavedMap = attractionDao.getAllOnce().associate { it.id to it.isSavedOffline }
        val mergedEntities = cloudAttractions.map { attraction ->
            attraction.toEntity(isSavedOffline = existingSavedMap[attraction.id] ?: false)
        }
        attractionDao.insertAll(mergedEntities)
        val activeIds = cloudAttractions.map { it.id }
        if (activeIds.isNotEmpty()) {
            attractionDao.deleteNotIn(activeIds)
        }
    }

    private fun Attraction.toEntity(isSavedOffline: Boolean = false): AttractionEntity =
        AttractionEntity(
            id = id,
            mapNodeId = mapNodeId,
            name = name.en,
            nameCeb = name.ceb,
            nameTl = name.tl,
            nameEs = name.es,
            subtitle = subtitle.en,
            subtitleCeb = subtitle.ceb,
            subtitleTl = subtitle.tl,
            subtitleEs = subtitle.es,
            description = description.en,
            descriptionCeb = description.ceb,
            descriptionTl = description.tl,
            descriptionEs = description.es,
            historyNote = historyNote.en,
            historyNoteCeb = historyNote.ceb,
            historyNoteTl = historyNote.tl,
            historyNoteEs = historyNote.es,
            barangay = barangay,
            category = category.name,
            latitude = latitude,
            longitude = longitude,
            mapX = mapX,
            mapY = mapY,
            imageRes = imageRes,
            openHours = openHours,
            entryFee = entryFee.en,
            entryFeeCeb = entryFee.ceb,
            entryFeeTl = entryFee.tl,
            entryFeeEs = entryFee.es,
            bestTime = bestTime.en,
            bestTimeCeb = bestTime.ceb,
            bestTimeTl = bestTime.tl,
            bestTimeEs = bestTime.es,
            transitHint = transitHint.en,
            transitHintCeb = transitHint.ceb,
            transitHintTl = transitHint.tl,
            transitHintEs = transitHint.es,
            baseRating = baseRating,
            imageKey = imageKey,
            photoData = photoData,
            isSavedOffline = isSavedOffline,
            updatedAtMillis = System.currentTimeMillis()
        )

    private fun AttractionEntity.toDomain(): Attraction =
        Attraction(
            id = id,
            mapNodeId = mapNodeId,
            name = LocalizedText(name, nameCeb, nameTl, nameEs),
            subtitle = LocalizedText(subtitle, subtitleCeb, subtitleTl, subtitleEs),
            description = LocalizedText(description, descriptionCeb, descriptionTl, descriptionEs),
            historyNote = LocalizedText(historyNote, historyNoteCeb, historyNoteTl, historyNoteEs),
            barangay = barangay,
            category = enumValues<AttractionCategory>().firstOrNull { it.name == category }
                ?: AttractionCategory.LANDMARK,
            latitude = latitude,
            longitude = longitude,
            mapX = mapX,
            mapY = mapY,
            imageRes = imageRes,
            openHours = openHours,
            entryFee = LocalizedText(entryFee, entryFeeCeb, entryFeeTl, entryFeeEs),
            bestTime = LocalizedText(bestTime, bestTimeCeb, bestTimeTl, bestTimeEs),
            transitHint = LocalizedText(transitHint, transitHintCeb, transitHintTl, transitHintEs),
            baseRating = baseRating,
            imageKey = imageKey,
            photoData = photoData
        )
}
