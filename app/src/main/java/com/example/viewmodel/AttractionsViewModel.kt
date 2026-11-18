package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AttractionRepository
import com.example.data.repository.DumingagOfflineRepository
import com.example.model.Attraction
import com.example.model.AttractionCategory
import com.example.model.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AttractionsViewModel(
    private val attractionRepository: AttractionRepository,
    private val offlineRepository: DumingagOfflineRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(AttractionCategory.ALL)
    val selectedCategory: StateFlow<AttractionCategory> = _selectedCategory.asStateFlow()

    val savedAttractionIds: StateFlow<Set<String>> = offlineRepository.savedAttractionIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val uiState: StateFlow<UiState<List<Attraction>>> = combine(
        _searchQuery,
        _selectedCategory
    ) { query, category -> query.trim() to category }
        .flatMapLatest { (query, category) ->
            attractionRepository.observeFilteredAttractions(query, category)
                .map { list -> UiState.Success(list) as UiState<List<Attraction>> }
        }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(UiState.Error(error.localizedMessage ?: "Failed to load attractions from Room"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    init {
        refreshAttractions()
    }

    fun refreshAttractions() {
        viewModelScope.launch {
            attractionRepository.seedAttractionsIfEmpty()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: AttractionCategory) {
        _selectedCategory.value = category
    }

    fun toggleSavedOffline(attraction: Attraction) {
        viewModelScope.launch {
            val isSaved = savedAttractionIds.value.contains(attraction.id)
            offlineRepository.toggleAttractionSavedOffline(attraction.id, isSaved)
        }
    }
}
