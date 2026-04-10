package com.example.sports_court_rater.ui.home

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

    // StateFlow that collects from the repository's Flow and applies sorting
    val courts: StateFlow<List<Court>> = combine(
        repository.getAllCourts(),
        _sortType
    ) { courts, sortType ->
        when (sortType) {
            SortType.RATING -> courts.sortedByDescending { it.rating }
            SortType.NEW -> courts.reversed() // Assuming newer are at the end of the list from repo
            SortType.NEAR -> courts // For now, near sorting requires location which we might add later
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refreshCourts()
    }

    fun setSortType(sortType: SortType) {
        _sortType.value = sortType
    }

    fun refreshCourts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.refreshCourts()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchLocationName(court: Court) {
        viewModelScope.launch {
            repository.fetchAndSaveLocationName(court)
        }
    }
}
