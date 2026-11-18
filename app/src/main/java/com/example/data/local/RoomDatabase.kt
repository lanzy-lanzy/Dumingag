package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

import androidx.room.Update

@Entity(tableName = "attractions")
data class AttractionEntity(
    @PrimaryKey val id: String,
    val mapNodeId: String,
    val name: String,
    val nameCeb: String,
    val nameTl: String,
    val nameEs: String,
    val subtitle: String,
    val subtitleCeb: String,
    val subtitleTl: String,
    val subtitleEs: String,
    val description: String,
    val descriptionCeb: String,
    val descriptionTl: String,
    val descriptionEs: String,
    val historyNote: String,
    val historyNoteCeb: String,
    val historyNoteTl: String,
    val historyNoteEs: String,
    val barangay: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val mapX: Float,
    val mapY: Float,
    val imageRes: Int,
    val openHours: String,
    val entryFee: String,
    val entryFeeCeb: String,
    val entryFeeTl: String,
    val entryFeeEs: String,
    val bestTime: String,
    val bestTimeCeb: String,
    val bestTimeTl: String,
    val bestTimeEs: String,
    val transitHint: String,
    val transitHintCeb: String,
    val transitHintTl: String,
    val transitHintEs: String,
    val baseRating: Double,
    val imageKey: String = "waterfall",
    val photoData: String = "",
    val isSavedOffline: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_attractions")
data class SavedAttractionEntity(
    @PrimaryKey val attractionId: String,
    val savedAtMillis: Long
)

@Entity(tableName = "cached_weather")
data class CachedWeatherEntity(
    @PrimaryKey val id: Int = 1,
    val temperatureC: Double,
    val apparentTempC: Double,
    val humidityPct: Int,
    val windSpeedKmh: Double,
    val weatherCode: Int,
    val dailyJson: String,
    val updatedAtMillis: Long
)

@Entity(tableName = "saved_documents")
data class SavedDocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val contentBody: String,
    val fileUri: String?,
    val savedAtMillis: Long,
    val isBundled: Boolean
)

@Entity(tableName = "offline_itineraries")
data class OfflineItineraryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val attractionId: String,
    val attractionName: String,
    val plannedDateMillis: Long,
    val notes: String,
    val reminderEnabled: Boolean,
    val calendarSynced: Boolean
)

@Dao
interface AttractionDao {
    @Query("SELECT * FROM attractions ORDER BY baseRating DESC, name ASC")
    fun observeAllAttractions(): Flow<List<AttractionEntity>>

    @Query("SELECT * FROM attractions WHERE category = :category ORDER BY baseRating DESC, name ASC")
    fun observeByCategory(category: String): Flow<List<AttractionEntity>>

    @Query(
        "SELECT * FROM attractions WHERE name LIKE '%' || :query || '%' " +
            "OR description LIKE '%' || :query || '%' " +
            "OR barangay LIKE '%' || :query || '%' " +
            "OR category LIKE '%' || :query || '%' " +
            "ORDER BY baseRating DESC"
    )
    fun searchAttractions(query: String): Flow<List<AttractionEntity>>

    @Query(
        """
        SELECT * FROM attractions
        WHERE (:category IS NULL OR category = :category)
          AND (
            :query = ''
            OR name LIKE '%' || :query || '%' COLLATE NOCASE
            OR nameCeb LIKE '%' || :query || '%' COLLATE NOCASE
            OR nameTl LIKE '%' || :query || '%' COLLATE NOCASE
            OR nameEs LIKE '%' || :query || '%' COLLATE NOCASE
            OR category LIKE '%' || :query || '%' COLLATE NOCASE
            OR REPLACE(category, '_', ' ') LIKE '%' || :query || '%' COLLATE NOCASE
            OR barangay LIKE '%' || :query || '%' COLLATE NOCASE
            OR description LIKE '%' || :query || '%' COLLATE NOCASE
          )
        ORDER BY baseRating DESC, name ASC
        """
    )
    fun observeFilteredAttractions(query: String, category: String?): Flow<List<AttractionEntity>>

    @Query("SELECT * FROM attractions WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<AttractionEntity?>

    @Query("SELECT * FROM attractions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AttractionEntity?

    @Query("SELECT COUNT(*) FROM attractions")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attraction: AttractionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attractions: List<AttractionEntity>)

    @Update
    suspend fun update(attraction: AttractionEntity)

    @Query("UPDATE attractions SET isSavedOffline = :saved WHERE id = :id")
    suspend fun setSavedOffline(id: String, saved: Boolean)

    @Query("SELECT * FROM attractions")
    suspend fun getAllOnce(): List<AttractionEntity>

    @Query("DELETE FROM attractions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM attractions WHERE id NOT IN (:activeIds)")
    suspend fun deleteNotIn(activeIds: List<String>)
}

@Dao
interface SavedAttractionDao {
    @Query("SELECT attractionId FROM saved_attractions")
    fun observeSavedIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: SavedAttractionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(entities: List<SavedAttractionEntity>)

    @Query("DELETE FROM saved_attractions WHERE attractionId = :id")
    suspend fun remove(id: String)
}

@Dao
interface WeatherDao {
    @Query("SELECT * FROM cached_weather WHERE id = 1")
    fun observeWeather(): Flow<CachedWeatherEntity?>

    @Query("SELECT * FROM cached_weather WHERE id = 1")
    suspend fun getWeather(): CachedWeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeather(entity: CachedWeatherEntity)
}

@Dao
interface SavedDocumentDao {
    @Query("SELECT * FROM saved_documents ORDER BY isBundled DESC, savedAtMillis DESC")
    fun observeDocuments(): Flow<List<SavedDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: SavedDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(docs: List<SavedDocumentEntity>)

    @Query("DELETE FROM saved_documents WHERE id = :id AND isBundled = 0")
    suspend fun deleteCustomDocument(id: String)
}

@Dao
interface OfflineItineraryDao {
    @Query("SELECT * FROM offline_itineraries WHERE userId = :userId ORDER BY plannedDateMillis ASC")
    fun observeForUser(userId: String): Flow<List<OfflineItineraryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceForUser(items: List<OfflineItineraryEntity>)

    @Query("DELETE FROM offline_itineraries WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(
    entities = [
        AttractionEntity::class,
        SavedAttractionEntity::class,
        CachedWeatherEntity::class,
        SavedDocumentEntity::class,
        OfflineItineraryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class DumingagDatabase : RoomDatabase() {
    abstract fun attractionDao(): AttractionDao
    abstract fun savedAttractionDao(): SavedAttractionDao
    abstract fun weatherDao(): WeatherDao
    abstract fun savedDocumentDao(): SavedDocumentDao
    abstract fun offlineItineraryDao(): OfflineItineraryDao

    companion object {
        @Volatile
        private var INSTANCE: DumingagDatabase? = null

        fun getInstance(context: Context): DumingagDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DumingagDatabase::class.java,
                    "dumingag_offline.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
