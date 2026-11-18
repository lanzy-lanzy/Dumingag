package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AttractionRepository
import com.example.data.repository.DumingagOfflineRepository
import com.example.model.Attraction
import com.example.model.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AttractionDetailViewModel(
    private val attractionId: String,
    private val attractionRepository: AttractionRepository,
    private val offlineRepository: DumingagOfflineRepository
) : ViewModel() {

    val uiState: StateFlow<UiState<Attraction>> = attractionRepository
        .observeAttractionById(attractionId)
        .map { attraction ->
            if (attraction != null) {
                UiState.Success(attraction)
            } else {
                UiState.Error("Attraction \"$attractionId\" was not found in the offline Room database.")
            }
        }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(UiState.Error(error.localizedMessage ?: "Failed to load attraction details"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val isSavedOffline: StateFlow<Boolean> = offlineRepository.savedAttractionIds
        .map { ids -> ids.contains(attractionId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = false
        )

    init {
        ensureSeeded()
    }

    fun ensureSeeded() {
        viewModelScope.launch {
            attractionRepository.seedAttractionsIfEmpty()
        }
    }

    fun toggleSavedOffline() {
        viewModelScope.launch {
            offlineRepository.toggleAttractionSavedOffline(
                attractionId = attractionId,
                currentlySaved = isSavedOffline.value
            )
        }
    }
}
