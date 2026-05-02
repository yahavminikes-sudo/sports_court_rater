package com.example.sports_court_rater.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.data.CourtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortType {
    RATING, NEW, NEAR
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CourtRepository
) : ViewModel() {

    private val _sortType = MutableStateFlow(SortType.RATING)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)

    val courts: StateFlow<List<Court>> = combine(
        repository.getAllCourts(),
        _sortType,
        _userLocation
    ) { courts, sortType, location ->
        when (sortType) {
            SortType.RATING -> courts.sortedByDescending {
                if (it.averageRating > 0) it.averageRating else it.rating
            }
            SortType.NEW -> courts.sortedByDescending { it.date }
            SortType.NEAR -> if (location != null) {
                courts.sortedBy { court ->
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        location.first, location.second,
                        court.latitude, court.longitude,
                        results
                    )
                    results[0]
                }
            } else {
                courts
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isLoading = MutableStateFlow(true) // Start as true to show skeleton immediately
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refreshCourts()
    }

    fun setSortType(sortType: SortType) {
        _sortType.value = sortType
    }

    fun setUserLocation(lat: Double, lng: Double) {
        _userLocation.value = Pair(lat, lng)
    }

    fun refreshCourts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.refreshCourts()
            } catch (e: Exception) {
                // Handle error if needed
            } finally {
                _isLoading.value = false
            }
        }
    }
}
