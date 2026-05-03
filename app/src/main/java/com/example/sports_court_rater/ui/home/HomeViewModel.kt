package com.example.sports_court_rater.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.data.CourtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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
    private val _isRefreshing = MutableStateFlow(true)

    val isLoading: StateFlow<Boolean> = combine(
        _isRefreshing,
        _sortType,
        _userLocation
    ) { refreshing, sort, location ->
        refreshing || (sort == SortType.NEAR && location == null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val courts: StateFlow<List<Court>> = combine(
        repository.getAllCourts(),
        _sortType,
        _userLocation
    ) { courts, sortType, location ->
        when (sortType) {
            SortType.RATING -> courts.sortedByDescending { it.averageRating }
            SortType.NEW -> courts.sortedByDescending { it.date }
            SortType.NEAR -> {
                if (location == null) courts
                else {
                    courts.map { court ->
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            location.first, location.second,
                            court.latitude, court.longitude,
                            results
                        )
                        court to results[0]
                    }.sortedBy { it.second }.map { it.first }
                }
            }
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
            _isRefreshing.value = true
            try {
                repository.refreshCourts()
            } catch (e: Exception) {
                // UI will handle empty state or errors via the courts flow
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
