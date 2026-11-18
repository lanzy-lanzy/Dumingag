package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DumingagSeedData
import com.example.data.OfflineRouteEngine
import com.example.data.repository.AttractionRepository
import com.example.data.repository.DumingagCloudRepository
import com.example.data.repository.DumingagOfflineRepository
import com.example.data.repository.WeatherRepository
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.AttractionCategory
import com.example.model.CommunityReview
import com.example.model.EmergencyContact
import com.example.model.ItineraryItem
import com.example.model.OfflineRoutePlan
import com.example.model.SavedDocument
import com.example.model.TransitMethod
import com.example.model.TravelMode
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.WeatherSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DumingagViewModel(
    private val cloudRepo: DumingagCloudRepository,
    private val offlineRepo: DumingagOfflineRepository,
    private val attractionRepo: AttractionRepository,
    private val weatherRepo: WeatherRepository,
    val currentUserId: String,
    val userDisplayName: String,
    val userEmail: String = "",
    val isEmailVerified: Boolean = false
) : ViewModel() {

    private val isBootstrappedAdmin: Boolean =
        userEmail.equals(ADMIN_EMAIL, ignoreCase = true) && isEmailVerified

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(AttractionCategory.ALL)
    val selectedCategory: StateFlow<AttractionCategory> = _selectedCategory.asStateFlow()

    private val _selectedAttraction = MutableStateFlow<Attraction?>(null)
    val selectedAttraction: StateFlow<Attraction?> = _selectedAttraction.asStateFlow()

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

    private var hasAttemptedTransitSeed = false
    private var hasAttemptedAttractionSeed = false
    private var hasAttemptedEmergencySeed = false

    val isAdmin: StateFlow<Boolean> = cloudRepo.observeIsAdminDoc(currentUserId)
        .map { hasAdminDoc -> isBootstrappedAdmin || hasAdminDoc }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), isBootstrappedAdmin)

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

    val savedAttractionIds: StateFlow<Set<String>> = offlineRepo.savedAttractionIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptySet())

    val savedDocuments: StateFlow<List<SavedDocument>> = offlineRepo.savedDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val filteredAttractions: StateFlow<List<Attraction>> = combine(
        attractionRepo.allAttractions,
        _searchQuery,
        _selectedCategory,
        language
    ) { allAttractions, query, category, lang ->
        val trimmed = query.trim().lowercase()
        allAttractions.filter { attraction ->
            val catMatch = category == AttractionCategory.ALL || attraction.category == category
            val queryMatch = trimmed.isEmpty() ||
                attraction.name.get(lang).lowercase().contains(trimmed) ||
                attraction.name.en.lowercase().contains(trimmed) ||
                attraction.barangay.lowercase().contains(trimmed) ||
                attraction.subtitle.get(lang).lowercase().contains(trimmed) ||
                attraction.description.get(lang).lowercase().contains(trimmed)
            catMatch && queryMatch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), DumingagSeedData.attractions)

    val allAttractions: StateFlow<List<Attraction>> = attractionRepo.allAttractions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), DumingagSeedData.attractions)

    val emergencyContacts: StateFlow<List<EmergencyContact>> = combine(
        cloudRepo.observePublicEmergencyContacts().catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing emergency contacts", firestoreError ?: error)
            emit(emptyList())
        },
        isAdmin
    ) { list, adminActive ->
        if (list.isEmpty()) {
            if (adminActive && !hasAttemptedEmergencySeed) {
                hasAttemptedEmergencySeed = true
                viewModelScope.launch {
                    cloudRepo.seedDefaultEmergencyContactsIfEmpty(
                        userId = currentUserId,
                        defaults = DumingagSeedData.emergencyContacts
                    )
                }
            }
            DumingagSeedData.emergencyContacts
        } else {
            list
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT),
        DumingagSeedData.emergencyContacts
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

    val reviewsState: StateFlow<UiState<List<CommunityReview>>> = cloudRepo.observePublicReviews()
        .map<List<CommunityReview>, UiState<List<CommunityReview>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing reviews", firestoreError ?: error)
            emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load reviews"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val transitState: StateFlow<UiState<List<TransitMethod>>> = cloudRepo.observePublicTransitMethods()
        .map<List<TransitMethod>, UiState<List<TransitMethod>>> { list ->
            if (list.isEmpty() && !hasAttemptedTransitSeed) {
                hasAttemptedTransitSeed = true
                viewModelScope.launch {
                    cloudRepo.seedDefaultTransitMethodsIfEmpty(
                        userId = currentUserId,
                        defaults = DumingagSeedData.defaultTransitMethods
                    )
                }
            }
            UiState.Success(list)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing transit methods", firestoreError ?: error)
            emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load transit routes"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    init {
        viewModelScope.launch {
            attractionRepo.seedAttractionsIfEmpty()
            offlineRepo.seedStarterDocumentsIfNeeded()
            cloudRepo.ensureUserProfile(currentUserId, userDisplayName)
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
    }

    fun selectAttraction(attraction: Attraction?) {
        _selectedAttraction.value = attraction
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            offlineRepo.setLanguage(lang)
            cloudRepo.updateUserPreferences(currentUserId, lang.code, offlinePackReady.value)
        }
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
        category: AttractionCategory,
        latitude: Double,
        longitude: Double,
        openHours: String,
        entryFeeEn: String,
        bestTimeEn: String,
        transitHintEn: String,
        baseRating: Double,
        imageKey: String,
        photoData: String?,
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
                category = category.name,
                latitude = latitude,
                longitude = longitude,
                openHours = openHours,
                entryFeeEn = entryFeeEn,
                bestTimeEn = bestTimeEn,
                transitHintEn = transitHintEn,
                baseRating = baseRating,
                imageKey = imageKey,
                photoData = photoData,
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

    fun deleteAdminAttraction(attractionId: String) {
        if (!isAdmin.value) {
            _statusMessage.value = "Admin authorization required"
            return
        }
        viewModelScope.launch {
            val res = cloudRepo.deleteCloudAttraction(currentUserId, attractionId)
            res.onSuccess {
                attractionRepo.deleteAttraction(attractionId)
                if (_selectedAttraction.value?.id == attractionId) {
                    _selectedAttraction.value = null
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
            offlineRepo.downloadFullOfflinePack()
            cloudRepo.updateUserPreferences(currentUserId, language.value.code, true)
            _statusMessage.value = "Dumingag Offline Map & Trail Pack ready"
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

    fun submitReview(attraction: Attraction, rating: Int, comment: String, visitTag: String) {
        viewModelScope.launch {
            val res = cloudRepo.addReview(
                userId = currentUserId,
                authorName = effectiveDisplayName.value,
                attractionId = attraction.id,
                attractionName = attraction.name.en,
                rating = rating,
                comment = comment,
                visitTag = visitTag
            )
            res.onSuccess {
                _statusMessage.value = "Thank you! Review published."
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to post review"
            }
        }
    }

    fun updateReview(reviewId: String, rating: Int, comment: String, visitTag: String) {
        viewModelScope.launch {
            val res = cloudRepo.updateReview(currentUserId, reviewId, rating, comment, visitTag)
            res.onSuccess {
                _statusMessage.value = "Review updated"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to update review"
            }
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            val res = cloudRepo.deleteReview(currentUserId, reviewId)
            res.onSuccess {
                _statusMessage.value = "Review removed"
            }
        }
    }

    fun refreshTransit() {
        viewModelScope.launch {
            val res = cloudRepo.refreshTransitMethodsFromServer()
            res.onSuccess { list ->
                if (list.isEmpty()) {
                    cloudRepo.seedDefaultTransitMethodsIfEmpty(
                        userId = currentUserId,
                        defaults = DumingagSeedData.defaultTransitMethods
                    )
                }
                _statusMessage.value = "Transit schedules synced from Firestore"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to sync transit schedules from Firestore"
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
        const val ADMIN_EMAIL = "bigbren480@gmail.com"
    }
}

