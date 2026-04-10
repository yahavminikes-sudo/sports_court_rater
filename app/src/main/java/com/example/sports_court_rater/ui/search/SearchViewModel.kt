package com.example.sports_court_rater.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.data.CourtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: CourtRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedSport = MutableStateFlow("all")
    val selectedSport: StateFlow<String> = _selectedSport

    private val _minimumRating = MutableStateFlow(0f)
    val minimumRating: StateFlow<Float> = _minimumRating

    val availableSports: StateFlow<List<String>> = repository.getAllCourts()
        .map { courts ->
            val sports = courts.map { it.sportType }.distinct().filter { it.isNotEmpty() }.sorted()
            listOf("all") + sports
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf("all")
        )

    val filteredCourts: StateFlow<List<Court>> = combine(
        repository.getAllCourts(),
        _searchQuery,
        _selectedSport,
        _minimumRating
    ) { courts, query, sport, minRating ->
        courts.filter { court ->
            val matchesQuery = court.courtName.contains(query, ignoreCase = true)
            val matchesSport = sport == "all" || court.sportType == sport
            val matchesRating = court.rating >= minRating

            matchesQuery && matchesSport && matchesRating
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSportSelected(sport: String) {
        _selectedSport.value = sport
    }

    fun onMinimumRatingChanged(rating: Float) {
        _minimumRating.value = rating
    }
}
