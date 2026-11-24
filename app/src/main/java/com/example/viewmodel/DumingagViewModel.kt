package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DumingagSeedData
import com.example.data.OfflineRouteEngine
import com.example.data.SmartTravelEngines
import com.example.data.TravelCompanionEngines
import com.example.data.local.TravelGuideCacheStatus
import com.example.data.repository.AiConciergeMessage
import com.example.data.repository.AiConciergeRepository
import com.example.data.repository.AttractionRepository
import com.example.data.repository.DumingagCloudRepository
import com.example.data.repository.DumingagOfflineRepository
import com.example.data.repository.WeatherRepository
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.Attraction
import com.example.model.AttractionCategory
import com.example.model.CategoryCatalog
import com.example.model.CommunityReview
import com.example.model.ManagedCategory
import com.example.model.EcoBadge
import com.example.model.EmergencyContact
import com.example.model.ItineraryItem
import com.example.model.OfflineRoutePlan
import com.example.model.PackingItem
import com.example.model.SavedDocument
import com.example.model.TileDownloadProgress
import com.example.model.TourismEvent
import com.example.model.TrailConditionReport
import com.example.model.TransitMethod
import com.example.model.TravelMode
import com.example.model.TripExpense
import com.example.model.UiState
import com.example.model.ManagedUser
import com.example.model.UserProfile
import com.example.model.WeatherSnapshot
import com.example.util.ConciergeVoicePersona
import com.example.util.GeminiExpressiveVoicePlayer
import com.example.util.VoicePlaybackUiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class DumingagViewModel(
    private val cloudRepo: DumingagCloudRepository,
    private val offlineRepo: DumingagOfflineRepository,
    private val attractionRepo: AttractionRepository,
    private val weatherRepo: WeatherRepository,
    val currentUserId: String,
    val userDisplayName: String,
    val userEmail: String = "",
    val isEmailVerified: Boolean = false,
    private val aiConciergeRepo: AiConciergeRepository = AiConciergeRepository()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(AttractionCategory.ALL)
    val selectedCategory: StateFlow<AttractionCategory> = _selectedCategory.asStateFlow()

    private val _selectedCategoryCode = MutableStateFlow("ALL")
    val selectedCategoryCode: StateFlow<String> = _selectedCategoryCode.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    private val _selectedAttraction = MutableStateFlow<Attraction?>(null)
    val selectedAttraction: StateFlow<Attraction?> = _selectedAttraction.asStateFlow()

    private val _markedAttractions = MutableStateFlow<List<Attraction>>(emptyList())
    val markedAttractions: StateFlow<List<Attraction>> = _markedAttractions.asStateFlow()

    private val _activeRoute = MutableStateFlow<OfflineRoutePlan?>(null)
    val activeRoute: StateFlow<OfflineRoutePlan?> = _activeRoute.asStateFlow()

    private val _activeStepIndex = MutableStateFlow(0)
    val activeStepIndex: StateFlow<Int> = _activeStepIndex.asStateFlow()

    private val _travelMode = MutableStateFlow(TravelMode.HABAL_HABAL)
    val travelMode: StateFlow<TravelMode> = _travelMode.asStateFlow()

    private val _userLatLon = MutableStateFlow(DumingagSeedData.CENTER_LAT to DumingagSeedData.CENTER_LON)
    val userLatLon: StateFlow<Pair<Double, Double>> = _userLatLon.asStateFlow()

    private val _weatherState = MutableStateFlow<UiState<WeatherSnapshot>>(UiState.Loading)
    val weatherState: StateFlow<UiState<WeatherSnapshot>> = _weatherState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _tileDownloadProgress = MutableStateFlow(TileDownloadProgress())
    val tileDownloadProgress: StateFlow<TileDownloadProgress> = _tileDownloadProgress.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastSyncedAtMillis = MutableStateFlow(System.currentTimeMillis())
    val lastSyncedAtMillis: StateFlow<Long> = _lastSyncedAtMillis.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<AiConciergeMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiConciergeMessage>> = _aiMessages.asStateFlow()

    private val _aiStreamingText = MutableStateFlow("")
    val aiStreamingText: StateFlow<String> = _aiStreamingText.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _aiVoiceState = MutableStateFlow(VoicePlaybackUiState())
    val aiVoiceState: StateFlow<VoicePlaybackUiState> = _aiVoiceState.asStateFlow()

    private val geminiVoicePlayer = GeminiExpressiveVoicePlayer()
    private var aiConciergeJob: Job? = null
    private var aiVoiceJob: Job? = null

    private var hasAttemptedTransitSeed = false
    private var hasAttemptedAttractionSeed = false
    private var hasAttemptedEmergencySeed = false
    private var hasAttemptedCategorySeed = false
    private var hasAttemptedEventsSeed = false

    val isAdmin: StateFlow<Boolean> = cloudRepo.observeAdminAccess(currentUserId, userEmail, isEmailVerified)
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val userProfile: StateFlow<UserProfile?> = cloudRepo.observeUserProfile(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing user profile", error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    val effectiveDisplayName: StateFlow<String> = userProfile
        .map { profile ->
            profile?.displayName?.takeIf { it.isNotBlank() } ?: userDisplayName
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), userDisplayName)

    val favoriteAttractionIds: StateFlow<Set<String>> = userProfile
        .map { profile -> profile?.favoriteAttractionIds?.toSet().orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptySet())

    val favoriteAttractions: StateFlow<List<Attraction>> = combine(
        attractionRepo.allAttractions,
        favoriteAttractionIds
    ) { all, favIds ->
        all.filter { it.id in favIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val language: StateFlow<AppLanguage> = offlineRepo.selectedLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), AppLanguage.EN)

    val forceOfflineMode: StateFlow<Boolean> = offlineRepo.forceOfflineMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), false)

    val isNetworkConnected: StateFlow<Boolean> = offlineRepo.observeNetworkConnected()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), true)

    val isEffectiveOffline: StateFlow<Boolean> = combine(
        forceOfflineMode,
        isNetworkConnected
    ) { forced, connected -> forced || !connected }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), false)

    val offlinePackReady: StateFlow<Boolean> = offlineRepo.offlinePackReady
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), false)

    val offlinePackUpdatedAt: StateFlow<Long> = offlineRepo.offlinePackUpdatedAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), 0L)

    val travelGuideCacheStatus: StateFlow<TravelGuideCacheStatus> = offlineRepo.travelGuideCacheStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), TravelGuideCacheStatus())

    val themeMode: StateFlow<AppThemeMode> = offlineRepo.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), AppThemeMode.SYSTEM)

    val tripExpenses: StateFlow<List<TripExpense>> = offlineRepo.tripExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val checkedInAttractionIds: StateFlow<Set<String>> = offlineRepo.checkedInAttractionIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptySet())

    val trailReports: StateFlow<List<TrailConditionReport>> = offlineRepo.trailReports
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT),
            TravelCompanionEngines.starterTrailReports
        )

    val savedAttractionIds: StateFlow<Set<String>> = offlineRepo.savedAttractionIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptySet())

    val savedDocuments: StateFlow<List<SavedDocument>> = offlineRepo.savedDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    private val enrichedAttractionsFlow = combine(
        attractionRepo.allAttractions,
        offlineRepo.cachedAttractionGalleries
    ) { spots, galleries ->
        spots.map { spot ->
            val gallery = galleries[spot.id].orEmpty()
            if (gallery.isNotEmpty() && spot.galleryPhotos.isEmpty()) {
                spot.copy(galleryPhotos = gallery)
            } else {
                spot
            }
        }
    }

    val filteredAttractions: StateFlow<List<Attraction>> = combine(
        enrichedAttractionsFlow,
        _searchQuery,
        _selectedCategoryCode,
        language,
        combine(_showFavoritesOnly, favoriteAttractionIds) { favOnly, favIds -> favOnly to favIds }
    ) { allAttractions, query, categoryCode, lang, (favOnly, favIds) ->
        val trimmed = query.trim().lowercase()
        val normalizedSelected = categoryCode.trim().uppercase().ifBlank { "ALL" }
        allAttractions.filter { attraction ->
            val catMatch = normalizedSelected == "ALL" ||
                attraction.categoryCode.equals(normalizedSelected, ignoreCase = true) ||
                attraction.category.name.equals(normalizedSelected, ignoreCase = true)
            val favMatch = !favOnly || attraction.id in favIds
            val queryMatch = trimmed.isEmpty() ||
                attraction.name.get(lang).lowercase().contains(trimmed) ||
                attraction.name.en.lowercase().contains(trimmed) ||
                attraction.barangay.lowercase().contains(trimmed) ||
                attraction.categoryCode.lowercase().replace('_', ' ').contains(trimmed) ||
                attraction.subtitle.get(lang).lowercase().contains(trimmed) ||
                attraction.description.get(lang).lowercase().contains(trimmed)
            catMatch && favMatch && queryMatch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), DumingagSeedData.attractions)

    val allAttractions: StateFlow<List<Attraction>> = enrichedAttractionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), DumingagSeedData.attractions)

    private sealed interface CloudListResult<out T> {
        data object Pending : CloudListResult<Nothing>
        data class Loaded<T>(val items: List<T>) : CloudListResult<T>
        data class Failed(val message: String) : CloudListResult<Nothing>
    }

    private val cloudEmergencyContactsFlow = cloudRepo.observePublicEmergencyContacts()
        .map<List<EmergencyContact>, CloudListResult<EmergencyContact>> { CloudListResult.Loaded(it) }
        .onStart { emit(CloudListResult.Pending) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing emergency contacts; falling back to DataStore cache", firestoreError ?: error)
            emit(CloudListResult.Failed((firestoreError ?: error).message ?: "Offline"))
        }

    val emergencyContacts: StateFlow<List<EmergencyContact>> = combine(
        cloudEmergencyContactsFlow,
        offlineRepo.cachedEmergencyContacts,
        isEffectiveOffline,
        isAdmin
    ) { cloudResult, cachedContacts, offline, adminActive ->
        when {
            !offline && cloudResult is CloudListResult.Loaded && cloudResult.items.isNotEmpty() -> {
                if (cloudResult.items != cachedContacts) {
                    viewModelScope.launch {
                        offlineRepo.cacheEmergencyContacts(cloudResult.items)
                    }
                }
                cloudResult.items
            }
            !offline && cloudResult is CloudListResult.Loaded && cloudResult.items.isEmpty() -> {
                if (adminActive && !hasAttemptedEmergencySeed) {
                    hasAttemptedEmergencySeed = true
                    viewModelScope.launch {
                        cloudRepo.seedDefaultEmergencyContactsIfEmpty(
                            userId = currentUserId,
                            defaults = DumingagSeedData.emergencyContacts
                        )
                    }
                }
                cachedContacts.ifEmpty { DumingagSeedData.emergencyContacts }
            }
            else -> cachedContacts.ifEmpty { DumingagSeedData.emergencyContacts }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT),
        DumingagSeedData.emergencyContacts
    )

    private val cloudCategoriesFlow = cloudRepo.observePublicCategories()
        .map<List<ManagedCategory>, CloudListResult<ManagedCategory>> { CloudListResult.Loaded(it) }
        .onStart { emit(CloudListResult.Pending) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing attraction categories; falling back to defaults", firestoreError ?: error)
            emit(CloudListResult.Failed((firestoreError ?: error).message ?: "Offline"))
        }

    val managedCategories: StateFlow<List<ManagedCategory>> = combine(
        cloudCategoriesFlow,
        isEffectiveOffline,
        isAdmin
    ) { cloudResult, offline, adminActive ->
        when {
            !offline && cloudResult is CloudListResult.Loaded && cloudResult.items.isNotEmpty() -> {
                cloudResult.items
            }
            !offline && cloudResult is CloudListResult.Loaded && cloudResult.items.isEmpty() -> {
                if (adminActive && !hasAttemptedCategorySeed) {
                    hasAttemptedCategorySeed = true
                    viewModelScope.launch {
                        cloudRepo.seedDefaultCategoriesIfEmpty(
                            userId = currentUserId,
                            defaults = CategoryCatalog.defaultCategories
                        )
                    }
                }
                CategoryCatalog.defaultCategories
            }
            else -> CategoryCatalog.defaultCategories
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT),
        CategoryCatalog.defaultCategories
    )

    val itinerariesState: StateFlow<UiState<List<ItineraryItem>>> = cloudRepo.observeItineraries(currentUserId)
        .map<List<ItineraryItem>, UiState<List<ItineraryItem>>> { list ->
            viewModelScope.launch { offlineRepo.mirrorItinerariesOffline(list) }
            UiState.Success(list)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing itineraries", firestoreError ?: error)
            val fallback = offlineRepo.observeOfflineItineraries(currentUserId).first()
            if (fallback.isNotEmpty()) {
                emit(UiState.Success(fallback))
            } else {
                emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load itinerary"))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    private val cloudReviewsFlow = cloudRepo.observePublicReviews()
        .map<List<CommunityReview>, CloudListResult<CommunityReview>> { CloudListResult.Loaded(it) }
        .onStart { emit(CloudListResult.Pending) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing reviews; falling back to offline DataStore cache", firestoreError ?: error)
            emit(CloudListResult.Failed((firestoreError ?: error).message ?: "Failed to load reviews"))
        }

    val reviewsState: StateFlow<UiState<List<CommunityReview>>> = combine(
        cloudReviewsFlow,
        offlineRepo.cachedReviews,
        isEffectiveOffline
    ) { cloudResult, cachedReviews, offline ->
        when {
            !offline && cloudResult is CloudListResult.Loaded -> {
                if (cloudResult.items != cachedReviews) {
                    viewModelScope.launch { offlineRepo.cacheReviews(cloudResult.items) }
                }
                _lastSyncedAtMillis.value = System.currentTimeMillis()
                UiState.Success(cloudResult.items)
            }
            offline || cloudResult is CloudListResult.Failed -> UiState.Success(cachedReviews)
            cachedReviews.isNotEmpty() -> UiState.Success(cachedReviews)
            else -> UiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    private val cloudTourismEventsFlow = cloudRepo.observePublicTourismEvents()
        .map<List<TourismEvent>, CloudListResult<TourismEvent>> { CloudListResult.Loaded(it) }
        .onStart { emit(CloudListResult.Pending) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing tourism events; falling back to cache", firestoreError ?: error)
            emit(CloudListResult.Failed((firestoreError ?: error).message ?: "Offline"))
        }

    val tourismEvents: StateFlow<List<TourismEvent>> = combine(
        cloudTourismEventsFlow,
        offlineRepo.cachedTourismEvents,
        isEffectiveOffline,
        isAdmin
    ) { cloudResult, cachedEvents, offline, adminActive ->
        when {
            !offline && cloudResult is CloudListResult.Loaded && cloudResult.items.isNotEmpty() -> {
                if (cloudResult.items != cachedEvents) {
                    viewModelScope.launch { offlineRepo.cacheTourismEvents(cloudResult.items) }
                }
                cloudResult.items
            }
            !offline && cloudResult is CloudListResult.Loaded && cloudResult.items.isEmpty() -> {
                if (adminActive && !hasAttemptedEventsSeed) {
                    hasAttemptedEventsSeed = true
                    viewModelScope.launch {
                        cloudRepo.seedDefaultTourismEventsIfEmpty(
                            userId = currentUserId,
                            defaults = SmartTravelEngines.starterTourismEvents
                        )
                    }
                }
                cachedEvents.ifEmpty { SmartTravelEngines.starterTourismEvents }
            }
            else -> cachedEvents.ifEmpty { SmartTravelEngines.starterTourismEvents }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT),
        SmartTravelEngines.starterTourismEvents
    )

    private val cloudTransitMethodsFlow = cloudRepo.observePublicTransitMethods()
        .map<List<TransitMethod>, CloudListResult<TransitMethod>> { CloudListResult.Loaded(it) }
        .onStart { emit(CloudListResult.Pending) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing transit methods; falling back to DataStore cache", firestoreError ?: error)
            emit(CloudListResult.Failed((firestoreError ?: error).message ?: "Failed to load transit routes"))
        }

    val transitState: StateFlow<UiState<List<TransitMethod>>> = combine(
        cloudTransitMethodsFlow,
        offlineRepo.cachedTransitMethods,
        isEffectiveOffline,
        isAdmin
    ) { cloudResult, cachedTransit, offline, admin ->
        when {
            !offline && cloudResult is CloudListResult.Loaded && cloudResult.items.isNotEmpty() -> {
                if (cloudResult.items != cachedTransit) {
                    viewModelScope.launch {
                        offlineRepo.cacheTransitMethods(cloudResult.items)
                    }
                }
                UiState.Success(cloudResult.items)
            }
            !offline && cloudResult is CloudListResult.Loaded && cloudResult.items.isEmpty() -> {
                if (admin && !hasAttemptedTransitSeed) {
                    hasAttemptedTransitSeed = true
                    viewModelScope.launch {
                        cloudRepo.seedDefaultTransitMethodsIfEmpty(
                            currentUserId,
                            DumingagSeedData.defaultTransitMethods
                        )
                    }
                }
                UiState.Success(cachedTransit.ifEmpty { DumingagSeedData.defaultTransitMethods })
            }
            offline || cloudResult is CloudListResult.Failed -> {
                val offlineList = cachedTransit.ifEmpty { DumingagSeedData.defaultTransitMethods }
                UiState.Success(offlineList)
            }
            cachedTransit.isNotEmpty() -> UiState.Success(cachedTransit)
            else -> UiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val smartPackingItems: StateFlow<List<PackingItem>> = combine(
        _weatherState,
        itinerariesState,
        offlineRepo.checkedPackingIds
    ) { wState, itState, checkedIds ->
        val snapshot = (wState as? UiState.Success)?.data
        val names = (itState as? UiState.Success)?.data?.map { it.attractionName }.orEmpty()
        TravelCompanionEngines.generateSmartPackingChecklist(snapshot, names, checkedIds)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT),
        TravelCompanionEngines.generateSmartPackingChecklist(null, emptyList(), emptySet())
    )

    val ecoBadges: StateFlow<List<EcoBadge>> = combine(
        checkedInAttractionIds,
        reviewsState,
        trailReports,
        offlinePackReady,
        allAttractions
    ) { checkedIn, revState, reports, packReady, spots ->
        val userReviewCount = (revState as? UiState.Success)?.data?.count { it.userId == currentUserId } ?: 0
        val userReportCount = reports.count { it.reporterName == effectiveDisplayName.value || !it.id.startsWith("seed_") }
        TravelCompanionEngines.computeEcoBadges(
            checkedInIds = checkedIn,
            userReviewCount = userReviewCount,
            trailReportCount = userReportCount,
            offlinePackReady = packReady,
            attractions = spots
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT),
        TravelCompanionEngines.computeEcoBadges(emptySet(), 0, 0, false, DumingagSeedData.attractions)
    )

    init {
        viewModelScope.launch {
            offlineRepo.seedEssentialGuideCacheIfNeeded()
            val cachedTiles = offlineRepo.countCachedMapTiles()
            _tileDownloadProgress.value = _tileDownloadProgress.value.copy(cachedTilesCount = cachedTiles)
        }
        viewModelScope.launch {
            cloudRepo.ensureUserProfile(currentUserId, userDisplayName, userEmail).onFailure {
                _statusMessage.value = "Unable to initialize your traveler profile: ${it.localizedMessage}"
            }
        }
        viewModelScope.launch {
            attractionRepo.seedAttractionsIfEmpty()
            offlineRepo.seedStarterDocumentsIfNeeded()
            refreshWeather()
        }
        viewModelScope.launch {
            combine(
                cloudRepo.observePublicAttractions().catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing cloud attractions", firestoreError ?: error)
                    emit(emptyList())
                },
                isAdmin
            ) { cloudAttractions, adminActive ->
                cloudAttractions to adminActive
            }.collect { (cloudAttractions, adminActive) ->
                if (cloudAttractions.isEmpty()) {
                    if (adminActive && !hasAttemptedAttractionSeed) {
                        hasAttemptedAttractionSeed = true
                        cloudRepo.seedDefaultAttractionsIfEmpty(
                            userId = currentUserId,
                            defaults = DumingagSeedData.attractions
                        )
                    }
                } else {
                    attractionRepo.syncFromCloud(cloudAttractions)
                    val galleryMap = cloudAttractions
                        .filter { it.galleryPhotos.isNotEmpty() }
                        .associate { it.id to it.galleryPhotos }
                    offlineRepo.cacheAttractionGalleries(galleryMap)
                    _lastSyncedAtMillis.value = System.currentTimeMillis()
                    val currentlySelected = _selectedAttraction.value
                    if (currentlySelected != null) {
                        _selectedAttraction.value = cloudAttractions.firstOrNull { it.id == currentlySelected.id }
                    }
                }
            }
        }
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun setCategory(category: AttractionCategory) {
        _selectedCategory.value = category
        _selectedCategoryCode.value = category.name
    }

    fun setCategoryCode(code: String) {
        val normalized = code.trim().uppercase().ifBlank { "ALL" }
        _selectedCategoryCode.value = normalized
        _selectedCategory.value = if (normalized == "ALL") {
            AttractionCategory.ALL
        } else {
            enumValues<AttractionCategory>().firstOrNull { it.name == normalized }
                ?: AttractionCategory.ALL
        }
    }

    fun toggleFavoritesOnly() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun setFavoritesOnly(enabled: Boolean) {
        _showFavoritesOnly.value = enabled
    }

    fun selectAttraction(attraction: Attraction?) {
        if (attraction == null) {
            _markedAttractions.value = emptyList()
            _selectedAttraction.value = null
        } else {
            val current = _markedAttractions.value
            val updated = if (current.any { it.id == attraction.id }) current else current + attraction
            _markedAttractions.value = updated
            _selectedAttraction.value = attraction
        }
    }

    fun toggleMarkedAttraction(attraction: Attraction) {
        if (_activeRoute.value != null) {
            _activeRoute.value = null
            _activeStepIndex.value = 0
        }
        val current = _markedAttractions.value
        val exists = current.any { it.id == attraction.id }
        val updated = if (exists) {
            current.filterNot { it.id == attraction.id }
        } else {
            current + attraction
        }
        _markedAttractions.value = updated
        _selectedAttraction.value = updated.lastOrNull()
    }

    fun removeMarkedAttraction(attraction: Attraction) {
        val updated = _markedAttractions.value.filterNot { it.id == attraction.id }
        _markedAttractions.value = updated
        _selectedAttraction.value = updated.lastOrNull()
    }

    fun clearMarkedAttractions() {
        _markedAttractions.value = emptyList()
        _selectedAttraction.value = null
    }

    fun toggleFavoriteAttraction(attraction: Attraction) {
        viewModelScope.launch {
            val res = cloudRepo.toggleFavoriteAttraction(
                userId = currentUserId,
                attractionId = attraction.id,
                displayNameFallback = effectiveDisplayName.value
            )
            res.onSuccess { added ->
                _statusMessage.value = if (added) {
                    "Added ${attraction.name.en} to Favorites"
                } else {
                    "Removed ${attraction.name.en} from Favorites"
                }
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to update favorites"
            }
        }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            offlineRepo.setLanguage(lang)
            cloudRepo.updateUserPreferences(currentUserId, lang.code, offlinePackReady.value)
        }
    }

    suspend fun loadManagedUsers(): List<ManagedUser> {
        check(isAdmin.value) { "Admin access required" }
        return cloudRepo.loadManagedUsers()
    }

    fun updateProfile(displayName: String, photoData: String?, clearPhoto: Boolean = false) {
        viewModelScope.launch {
            val res = cloudRepo.updateUserProfile(
                userId = currentUserId,
                displayName = displayName,
                photoData = photoData,
                clearPhoto = clearPhoto
            )
            res.onSuccess {
                _statusMessage.value = "Traveler profile updated"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to update profile"
            }
        }
    }

    fun saveAdminAttraction(
        attractionId: String?,
        mapNodeId: String,
        nameEn: String,
        subtitleEn: String,
        descriptionEn: String,
        historyNoteEn: String,
        barangay: String,
        categoryCode: String,
        latitude: Double,
        longitude: Double,
        openHours: String,
        entryFeeEn: String,
        bestTimeEn: String,
        transitHintEn: String,
        baseRating: Double,
        imageKey: String,
        photoData: String?,
        galleryPhotos: List<String> = emptyList(),
        clearPhoto: Boolean = false
    ) {
        if (!isAdmin.value) {
            _statusMessage.value = "Admin authorization required"
            return
        }
        viewModelScope.launch {
            val res = cloudRepo.upsertCloudAttraction(
                userId = currentUserId,
                attractionId = attractionId,
                mapNodeId = mapNodeId,
                nameEn = nameEn,
                subtitleEn = subtitleEn,
                descriptionEn = descriptionEn,
                historyNoteEn = historyNoteEn,
                barangay = barangay,
                category = categoryCode,
                latitude = latitude,
                longitude = longitude,
                openHours = openHours,
                entryFeeEn = entryFeeEn,
                bestTimeEn = bestTimeEn,
                transitHintEn = transitHintEn,
                baseRating = baseRating,
                imageKey = imageKey,
                photoData = photoData,
                galleryPhotos = galleryPhotos,
                clearPhoto = clearPhoto
            )
            res.onSuccess {
                _statusMessage.value = if (attractionId.isNullOrBlank()) {
                    "Local attraction published"
                } else {
                    "Local attraction updated"
                }
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Failed to save attraction"
            }
        }
    }

    fun saveAdminCategory(
        categoryId: String?,
        code: String,
        name: String,
        iconKey: String,
        sortOrder: Long
    ) {
        if (!isAdmin.value) {
            _statusMessage.value = "Admin authorization required"
            return
        }
        viewModelScope.launch {
            val res = cloudRepo.upsertCloudCategory(
                userId = currentUserId,
                categoryId = categoryId,
                code = code,
                name = name,
                iconKey = iconKey,
                sortOrder = sortOrder
            )
            res.onSuccess {
                _statusMessage.value = if (categoryId.isNullOrBlank()) {
                    "Category \"$name\" added"
                } else {
                    "Category \"$name\" updated"
                }
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Failed to save category"
            }
        }
    }

    fun deleteAdminCategory(categoryId: String) {
        if (!isAdmin.value) {
            _statusMessage.value = "Admin authorization required"
            return
        }
        viewModelScope.launch {
            val res = cloudRepo.deleteCloudCategory(currentUserId, categoryId)
            res.onSuccess {
                if (_selectedCategoryCode.value.equals(categoryId, ignoreCase = true)) {
                    setCategoryCode("ALL")
                }
                _statusMessage.value = "Category removed"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Failed to remove category"
            }
        }
    }

    fun deleteAdminAttraction(attractionId: String) {
        if (!isAdmin.value) {
            _statusMessage.value = "Admin authorization required"
            return
        }
        viewModelScope.launch {
            val res = cloudRepo.deleteCloudAttraction(currentUserId, attractionId)
            res.onSuccess {
                attractionRepo.deleteAttraction(attractionId)
                val remainingMarks = _markedAttractions.value.filterNot { it.id == attractionId }
                _markedAttractions.value = remainingMarks
                if (_selectedAttraction.value?.id == attractionId) {
                    _selectedAttraction.value = remainingMarks.lastOrNull()
                }
                _statusMessage.value = "Attraction removed"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Failed to remove attraction"
            }
        }
    }

    fun saveAdminEmergencyContact(
        contactId: String?,
        titleEn: String,
        department: String,
        phoneNumber: String,
        availability: String,
        priorityLevel: String,
        email: String?,
        websiteUrl: String?
    ) {
        if (!isAdmin.value) {
            _statusMessage.value = "Admin authorization required"
            return
        }
        viewModelScope.launch {
            val res = cloudRepo.upsertCloudEmergencyContact(
                userId = currentUserId,
                contactId = contactId,
                titleEn = titleEn,
                department = department,
                phoneNumber = phoneNumber,
                availability = availability,
                priorityLevel = priorityLevel,
                email = email,
                websiteUrl = websiteUrl
            )
            res.onSuccess {
                _statusMessage.value = if (contactId.isNullOrBlank()) {
                    "Emergency contact published"
                } else {
                    "Emergency contact updated"
                }
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Failed to save emergency contact"
            }
        }
    }

    fun deleteAdminEmergencyContact(contactId: String) {
        if (!isAdmin.value) {
            _statusMessage.value = "Admin authorization required"
            return
        }
        viewModelScope.launch {
            val res = cloudRepo.deleteCloudEmergencyContact(currentUserId, contactId)
            res.onSuccess {
                _statusMessage.value = "Emergency contact removed"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Failed to remove emergency contact"
            }
        }
    }

    fun toggleForceOffline(enabled: Boolean) {
        viewModelScope.launch {
            offlineRepo.setForceOfflineMode(enabled)
            refreshWeather()
        }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            _weatherState.value = UiState.Loading
            val snapshot = weatherRepo.refreshDumingagWeather(forceOfflineMode.value)
            _weatherState.value = UiState.Success(snapshot)
            offlineRepo.syncWidgetWeather(snapshot)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            offlineRepo.setThemeMode(mode)
            _statusMessage.value = "Theme set to ${mode.label}"
        }
    }

    fun togglePackingItem(itemId: String) {
        viewModelScope.launch {
            offlineRepo.togglePackingItem(itemId)
        }
    }

    fun addTripExpense(title: String, category: String, amountPhp: Double) {
        viewModelScope.launch {
            offlineRepo.addTripExpense(title, category, amountPhp)
            _statusMessage.value = "Logged ₱${"%,.0f".format(amountPhp)} ($category)"
        }
    }

    fun deleteTripExpense(expenseId: String) {
        viewModelScope.launch {
            offlineRepo.deleteTripExpense(expenseId)
        }
    }

    fun checkInAttraction(attraction: Attraction) {
        viewModelScope.launch {
            offlineRepo.recordAttractionCheckIn(attraction.id)
            _statusMessage.value = "Eco-Passport stamped for ${attraction.name.en}!"
        }
    }

    fun addTrailReport(attractionId: String, conditionTag: String, crowdLevel: String, note: String) {
        viewModelScope.launch {
            offlineRepo.addTrailReport(
                attractionId = attractionId,
                conditionTag = conditionTag,
                crowdLevel = crowdLevel,
                note = note,
                reporterName = effectiveDisplayName.value
            )
            _statusMessage.value = "Trail condition report posted"
        }
    }

    fun preDownloadOfflineMapTiles() {
        if (_tileDownloadProgress.value.isRunning) return
        viewModelScope.launch {
            if (isEffectiveOffline.value) {
                _statusMessage.value = "Turn off Force Offline Mode & connect to download map tiles"
                return@launch
            }
            _tileDownloadProgress.value = _tileDownloadProgress.value.copy(
                isRunning = true,
                downloaded = 0,
                total = 1
            )
            val available = offlineRepo.preDownloadOfflineMapTiles { done, total ->
                _tileDownloadProgress.value = _tileDownloadProgress.value.copy(
                    isRunning = true,
                    downloaded = done,
                    total = total
                )
            }
            val totalDisk = offlineRepo.countCachedMapTiles()
            _tileDownloadProgress.value = TileDownloadProgress(
                isRunning = false,
                downloaded = available,
                total = available,
                cachedTilesCount = totalDisk
            )
            _statusMessage.value = "Cached $totalDisk real OpenStreetMap tiles for offline 2D & 3D viewing"
        }
    }

    fun updateUserLocation(lat: Double, lon: Double) {
        _userLatLon.value = lat to lon
        val active = _activeRoute.value
        if (active != null) {
            startOfflineNavigation(active.destination, _travelMode.value)
        }
    }

    fun setTravelMode(mode: TravelMode) {
        _travelMode.value = mode
        val active = _activeRoute.value
        if (active != null) {
            startOfflineNavigation(active.destination, mode)
        }
    }

    fun startOfflineNavigation(destination: Attraction, mode: TravelMode = _travelMode.value) {
        val (lat, lon) = _userLatLon.value
        val origin = OfflineRouteEngine.findNearestNode(lat, lon)
        val plan = OfflineRouteEngine.computeRoute(origin.id, destination, mode)
        _activeRoute.value = plan
        _activeStepIndex.value = 0
        _selectedAttraction.value = destination
        if (_markedAttractions.value.none { it.id == destination.id }) {
            _markedAttractions.value = _markedAttractions.value + destination
        }
    }

    fun clearActiveNavigation() {
        _activeRoute.value = null
        _activeStepIndex.value = 0
    }

    fun stepNavigation(delta: Int) {
        val stepsCount = _activeRoute.value?.steps?.size ?: return
        _activeStepIndex.value = (_activeStepIndex.value + delta).coerceIn(0, (stepsCount - 1).coerceAtLeast(0))
    }

    fun toggleAttractionSavedOffline(attraction: Attraction) {
        viewModelScope.launch {
            val currentlySaved = savedAttractionIds.value.contains(attraction.id)
            offlineRepo.toggleAttractionSavedOffline(attraction.id, currentlySaved)
            _statusMessage.value = if (currentlySaved) "Removed from offline cache" else "Saved ${attraction.name.en} offline"
        }
    }

    fun downloadFullOfflinePack() {
        viewModelScope.launch {
            val currentTransit = (transitState.value as? UiState.Success)?.data.orEmpty()
            val currentContacts = emergencyContacts.value
            offlineRepo.downloadFullOfflinePack(
                latestTransit = currentTransit,
                latestEmergencyContacts = currentContacts
            )
            cloudRepo.updateUserPreferences(currentUserId, language.value.code, true)
            _statusMessage.value = "Offline Map, Transit & Emergency Pack ready"
        }
    }

    fun saveCustomDocument(title: String, category: String, body: String, uri: String? = null) {
        viewModelScope.launch {
            offlineRepo.saveCustomDocument(title, category, body, uri)
            _statusMessage.value = "Document saved to Offline Vault"
        }
    }

    fun deleteCustomDocument(id: String) {
        viewModelScope.launch {
            offlineRepo.deleteCustomDocument(id)
        }
    }

    fun addItineraryStop(
        attraction: Attraction,
        plannedDateMillis: Long,
        notes: String,
        reminderEnabled: Boolean,
        onCreated: (ItineraryItem) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = cloudRepo.addItineraryItem(
                userId = currentUserId,
                attractionId = attraction.id,
                attractionName = attraction.name.en,
                plannedDateMillis = plannedDateMillis,
                notes = notes,
                reminderEnabled = reminderEnabled
            )
            res.onSuccess { newId ->
                val created = ItineraryItem(
                    id = newId,
                    userId = currentUserId,
                    attractionId = attraction.id,
                    attractionName = attraction.name.en,
                    plannedDateMillis = plannedDateMillis,
                    notes = notes,
                    reminderEnabled = reminderEnabled,
                    calendarSynced = false
                )
                onCreated(created)
                _statusMessage.value = "Added ${attraction.name.en} to itinerary"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Could not add itinerary item"
            }
        }
    }

    fun markCalendarSynced(item: ItineraryItem) {
        viewModelScope.launch {
            cloudRepo.updateItineraryStatus(
                userId = currentUserId,
                itineraryId = item.id,
                reminderEnabled = item.reminderEnabled,
                calendarSynced = true
            )
        }
    }

    fun toggleItineraryReminder(item: ItineraryItem) {
        viewModelScope.launch {
            cloudRepo.updateItineraryStatus(
                userId = currentUserId,
                itineraryId = item.id,
                reminderEnabled = !item.reminderEnabled,
                calendarSynced = item.calendarSynced
            )
        }
    }

    fun deleteItineraryItem(itemId: String) {
        viewModelScope.launch {
            cloudRepo.deleteItineraryItem(currentUserId, itemId)
        }
    }

    fun submitReview(
        attraction: Attraction,
        rating: Int,
        comment: String,
        visitTag: String,
        photoData: String? = null
    ) {
        viewModelScope.launch {
            val isVerified = checkedInAttractionIds.value.contains(attraction.id)
            val res = cloudRepo.addReview(
                userId = currentUserId,
                authorName = effectiveDisplayName.value,
                attractionId = attraction.id,
                attractionName = attraction.name.en,
                rating = rating,
                comment = comment,
                visitTag = visitTag,
                photoData = photoData,
                verifiedVisit = isVerified
            )
            res.onSuccess {
                _statusMessage.value = if (isVerified) {
                    "Thank you! Verified Visit review published."
                } else {
                    "Thank you! Review published."
                }
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to post review"
            }
        }
    }

    fun updateReview(
        reviewId: String,
        rating: Int,
        comment: String,
        visitTag: String,
        photoData: String? = null,
        clearPhoto: Boolean = false
    ) {
        viewModelScope.launch {
            val res = cloudRepo.updateReview(
                userId = currentUserId,
                reviewId = reviewId,
                rating = rating,
                comment = comment,
                visitTag = visitTag,
                photoData = photoData,
                clearPhoto = clearPhoto
            )
            res.onSuccess {
                _statusMessage.value = "Review updated"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to update review"
            }
        }
    }

    fun toggleReviewHelpful(review: CommunityReview) {
        viewModelScope.launch {
            val currentlyHelpful = review.helpfulUserIds.contains(currentUserId)
            val res = cloudRepo.toggleReviewHelpful(currentUserId, review.id, currentlyHelpful)
            res.onFailure { err ->
                _statusMessage.value = err.message ?: "Could not update helpful vote"
            }
        }
    }

    fun postAdminReviewReply(reviewId: String, replyText: String) {
        if (!isAdmin.value) {
            _statusMessage.value = "Admin authorization required"
            return
        }
        viewModelScope.launch {
            val res = cloudRepo.postAdminReviewReply(
                reviewId = reviewId,
                replyText = replyText,
                adminLabel = "Official Dumingag Tourism"
            )
            res.onSuccess {
                _statusMessage.value = if (replyText.isBlank()) {
                    "Official reply cleared"
                } else {
                    "Official Dumingag Tourism reply posted"
                }
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to save official reply"
            }
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            val res = cloudRepo.deleteReview(currentUserId, reviewId)
            res.onSuccess {
                _statusMessage.value = "Review removed"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to delete review"
            }
        }
    }

    fun saveAdminEvent(
        eventId: String?,
        title: String,
        summary: String,
        eventType: String,
        severity: String,
        dateLabel: String,
        locationName: String,
        attractionId: String,
        active: Boolean
    ) {
        if (!isAdmin.value) {
            _statusMessage.value = "Admin authorization required"
            return
        }
        viewModelScope.launch {
            val res = cloudRepo.upsertTourismEvent(
                userId = currentUserId,
                eventId = eventId,
                title = title,
                summary = summary,
                eventType = eventType,
                severity = severity,
                dateLabel = dateLabel,
                locationName = locationName,
                attractionId = attractionId,
                active = active
            )
            res.onSuccess {
                _statusMessage.value = if (eventId.isNullOrBlank()) {
                    "Tourism event/advisory published"
                } else {
                    "Tourism event/advisory updated"
                }
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to save tourism event"
            }
        }
    }

    fun deleteAdminEvent(eventId: String) {
        if (!isAdmin.value) {
            _statusMessage.value = "Admin authorization required"
            return
        }
        viewModelScope.launch {
            val res = cloudRepo.deleteTourismEvent(currentUserId, eventId)
            res.onSuccess {
                _statusMessage.value = "Tourism event removed"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to remove tourism event"
            }
        }
    }

    fun refreshAllData() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val snapshot = weatherRepo.refreshDumingagWeather(forceOfflineMode.value)
                _weatherState.value = UiState.Success(snapshot)
                offlineRepo.syncWidgetWeather(snapshot)
                if (!isEffectiveOffline.value) {
                    cloudRepo.refreshTransitMethodsFromServer().onSuccess { list ->
                        if (list.isNotEmpty()) {
                            offlineRepo.cacheTransitMethods(list)
                        }
                    }
                    _lastSyncedAtMillis.value = System.currentTimeMillis()
                    _statusMessage.value = "Dumingag guide synced"
                } else {
                    _statusMessage.value = "Offline mode · using locally cached guide data"
                }
                delay(250L)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun sendAiConciergeMessage(prompt: String) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty() || _isAiLoading.value) return
        if (isEffectiveOffline.value) {
            _aiError.value = "AI Concierge needs an internet connection. Turn off Force Offline Mode or reconnect to ask Gemini."
            return
        }

        val existingMessages = _aiMessages.value
        val isRetryingLastUserMessage = existingMessages.lastOrNull()?.let { it.isUser && it.text == cleanPrompt } == true
        val historyBefore = if (isRetryingLastUserMessage) {
            existingMessages.dropLast(1)
        } else {
            existingMessages
        }
        if (!isRetryingLastUserMessage) {
            val userMsg = AiConciergeMessage(
                id = "u_${UUID.randomUUID().toString().take(8)}",
                isUser = true,
                text = cleanPrompt
            )
            _aiMessages.value = existingMessages + userMsg
        }
        _aiStreamingText.value = ""
        _aiError.value = null
        _isAiLoading.value = true

        aiConciergeJob?.cancel()
        aiConciergeJob = viewModelScope.launch {
            try {
                val contextBlock = buildAiContextSummary()
                val finalReply = aiConciergeRepo.streamConciergeReply(
                    userPrompt = cleanPrompt,
                    conversationHistory = historyBefore,
                    liveDumingagContext = contextBlock
                ) { formattedSoFar ->
                    _aiStreamingText.value = formattedSoFar
                }
                if (finalReply.isNotEmpty()) {
                    val replyMsg = AiConciergeMessage(
                        id = "a_${UUID.randomUUID().toString().take(8)}",
                        isUser = false,
                        text = finalReply
                    )
                    _aiMessages.value = _aiMessages.value + replyMsg
                    if (_aiVoiceState.value.autoSpeakEnabled) {
                        speakAiMessage(replyMsg.id, replyMsg.text)
                    }
                } else {
                    _aiError.value = "Received an empty response. Please try asking again."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Log.e(TAG, "AI Concierge failed", t)
                _aiError.value = t.localizedMessage
                    ?.takeIf { it.isNotBlank() }
                    ?: "AI Concierge could not reply right now (${t.javaClass.simpleName})."
            } finally {
                _aiStreamingText.value = ""
                _isAiLoading.value = false
            }
        }
    }

    fun selectAiVoicePersona(persona: ConciergeVoicePersona) {
        if (_aiVoiceState.value.selectedPersona == persona) return
        stopAiVoice()
        _aiVoiceState.value = _aiVoiceState.value.copy(
            selectedPersona = persona,
            voiceError = null
        )
    }

    fun toggleAiAutoSpeak() {
        val next = !_aiVoiceState.value.autoSpeakEnabled
        _aiVoiceState.value = _aiVoiceState.value.copy(
            autoSpeakEnabled = next,
            voiceError = null
        )
        if (!next) {
            stopAiVoice()
        }
    }

    fun speakAiMessage(messageId: String, text: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        // Tapping the currently active message stops voice playback cleanly
        if (_aiVoiceState.value.activeMessageId == messageId &&
            (_aiVoiceState.value.isSynthesizing || _aiVoiceState.value.isSpeaking)
        ) {
            stopAiVoice()
            return
        }

        if (isEffectiveOffline.value) {
            _aiVoiceState.value = _aiVoiceState.value.copy(
                voiceError = "Gemini expressive voice needs an internet connection."
            )
            return
        }

        stopAiVoice()
        val persona = _aiVoiceState.value.selectedPersona
        _aiVoiceState.value = _aiVoiceState.value.copy(
            activeMessageId = messageId,
            isSynthesizing = true,
            isSpeaking = false,
            voiceError = null
        )

        aiVoiceJob = viewModelScope.launch {
            try {
                geminiVoicePlayer.speakMessage(
                    messageId = messageId,
                    messageText = cleanText,
                    persona = persona,
                    onPlaybackStarted = {
                        _aiVoiceState.value = _aiVoiceState.value.copy(
                            activeMessageId = messageId,
                            isSynthesizing = false,
                            isSpeaking = true
                        )
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Log.e(TAG, "Gemini expressive voice failed", t)
                _aiVoiceState.value = _aiVoiceState.value.copy(
                    voiceError = t.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Could not play Gemini expressive voice right now."
                )
            } finally {
                if (_aiVoiceState.value.activeMessageId == messageId) {
                    _aiVoiceState.value = _aiVoiceState.value.copy(
                        activeMessageId = null,
                        isSynthesizing = false,
                        isSpeaking = false
                    )
                }
            }
        }
    }

    fun stopAiVoice() {
        aiVoiceJob?.cancel()
        aiVoiceJob = null
        geminiVoicePlayer.stop()
        _aiVoiceState.value = _aiVoiceState.value.copy(
            activeMessageId = null,
            isSynthesizing = false,
            isSpeaking = false
        )
    }

    fun clearAiChat() {
        aiConciergeJob?.cancel()
        stopAiVoice()
        _aiMessages.value = emptyList()
        _aiStreamingText.value = ""
        _aiError.value = null
        _isAiLoading.value = false
        _aiVoiceState.value = _aiVoiceState.value.copy(voiceError = null)
    }

    override fun onCleared() {
        aiConciergeJob?.cancel()
        aiVoiceJob?.cancel()
        geminiVoicePlayer.release()
        super.onCleared()
    }

    private fun buildAiContextSummary(): String {
        val weather = (_weatherState.value as? UiState.Success)?.data
        val spots = allAttractions.value
        val transit = (transitState.value as? UiState.Success)?.data.orEmpty()
        val activeEvents = tourismEvents.value.filter { it.active }
        val reviews = (reviewsState.value as? UiState.Success)?.data.orEmpty()

        return buildString {
            if (weather != null) {
                appendLine("Weather: ${weather.temperatureC.toInt()}°C, ${weather.conditionDescription.ifBlank { "tropical highland" }}, humidity ${weather.humidityPct}%, wind ${weather.windSpeedKmh} km/h.")
            } else {
                appendLine("Weather: tropical highland climate (live reading pending).")
            }
            appendLine("Spots:")
            spots.take(12).forEach { s ->
                val spotReviews = reviews.filter { it.attractionId == s.id }
                val rating = if (spotReviews.isNotEmpty()) {
                    String.format(java.util.Locale.US, "%.1f/5(%d)", spotReviews.map { it.rating.toDouble() }.average(), spotReviews.size)
                } else {
                    String.format(java.util.Locale.US, "%.1f/5", s.baseRating)
                }
                appendLine("- ${s.name.en} (${s.categoryCode}, Brgy. ${s.barangay}): $rating, Fee ${s.entryFee.en}, ${s.openHours}, Best ${s.bestTime.en}")
            }
            if (transit.isNotEmpty()) {
                appendLine("Transit:")
                transit.take(6).forEach { t ->
                    appendLine("- ${t.name} (${t.modeCategory}): ${t.fareRangePhp}, ${t.durationText}, ${t.routeSummary}")
                }
            }
            if (activeEvents.isNotEmpty()) {
                appendLine("Events:")
                activeEvents.take(3).forEach { e ->
                    appendLine("- ${e.title} (${e.dateLabel}, ${e.locationName}): ${e.summary.take(100)}")
                }
            }
        }
    }

    fun refreshTransit() {
        viewModelScope.launch {
            if (isEffectiveOffline.value) {
                val cachedTransit = offlineRepo.getCachedTransitMethods()
                val cachedContacts = offlineRepo.getCachedEmergencyContacts()
                _statusMessage.value =
                    "Offline mode · ${cachedTransit.size} transit routes & ${cachedContacts.size} emergency contacts loaded from cache"
                return@launch
            }
            val res = cloudRepo.refreshTransitMethodsFromServer()
            res.onSuccess { list ->
                if (list.isNotEmpty()) {
                    offlineRepo.cacheTransitMethods(list)
                } else if (isAdmin.value) {
                    cloudRepo.seedDefaultTransitMethodsIfEmpty(
                        userId = currentUserId,
                        defaults = DumingagSeedData.defaultTransitMethods
                    )
                }
                _statusMessage.value = "Transit schedules synced & cached offline"
            }.onFailure {
                val cachedCount = offlineRepo.getCachedTransitMethods().size
                _statusMessage.value = if (cachedCount > 0) {
                    "Showing $cachedCount cached offline transit routes"
                } else {
                    "Unable to sync transit schedules from Firestore"
                }
            }
        }
    }

    fun addTransitMethod(
        name: String,
        modeCategory: String,
        operatorName: String,
        routeSummary: String,
        scheduleText: String,
        fareRangePhp: String,
        durationText: String,
        terminalLocation: String,
        travelTip: String,
        serviceStatus: String
    ) {
        if (!isAdmin.value) {
            _statusMessage.value = "Admin authorization required"
            return
        }
        viewModelScope.launch {
            val res = cloudRepo.addTransitMethod(
                userId = currentUserId,
                name = name,
                modeCategory = modeCategory,
                operatorName = operatorName,
                routeSummary = routeSummary,
                scheduleText = scheduleText,
                fareRangePhp = fareRangePhp,
                durationText = durationText,
                terminalLocation = terminalLocation,
                travelTip = travelTip,
                serviceStatus = serviceStatus
            )
            res.onSuccess {
                _statusMessage.value = "Transit method published to Firestore"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to publish transit method"
            }
        }
    }

    fun updateTransitMethod(
        transitId: String,
        name: String,
        modeCategory: String,
        operatorName: String,
        routeSummary: String,
        scheduleText: String,
        fareRangePhp: String,
        durationText: String,
        terminalLocation: String,
        travelTip: String,
        serviceStatus: String
    ) {
        if (!isAdmin.value) {
            _statusMessage.value = "Admin authorization required"
            return
        }
        viewModelScope.launch {
            val res = cloudRepo.updateTransitMethod(
                userId = currentUserId,
                transitId = transitId,
                name = name,
                modeCategory = modeCategory,
                operatorName = operatorName,
                routeSummary = routeSummary,
                scheduleText = scheduleText,
                fareRangePhp = fareRangePhp,
                durationText = durationText,
                terminalLocation = terminalLocation,
                travelTip = travelTip,
                serviceStatus = serviceStatus
            )
            res.onSuccess {
                _statusMessage.value = "Transit method updated"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to update transit method"
            }
        }
    }

    fun deleteTransitMethod(transitId: String) {
        if (!isAdmin.value) {
            _statusMessage.value = "Admin authorization required"
            return
        }
        viewModelScope.launch {
            val res = cloudRepo.deleteTransitMethod(currentUserId, transitId)
            res.onSuccess {
                _statusMessage.value = "Transit method removed"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to remove transit method"
            }
        }
    }

    fun postStatusMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private companion object {
        const val TAG = "DumingagVM"
        const val STOP_TIMEOUT = 5000L
    }
}

