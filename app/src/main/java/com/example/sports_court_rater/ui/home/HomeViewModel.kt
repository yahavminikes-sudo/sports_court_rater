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

    val courts: StateFlow<List<Court>> = combine(
        repository.getAllCourts(),
        _sortType
    ) { courts, sortType ->
        when (sortType) {
            SortType.RATING -> courts.sortedByDescending { it.rating }
            SortType.NEW -> courts.reversed()
            SortType.NEAR -> courts
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

    fun refreshCourts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.refreshCourts()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}
