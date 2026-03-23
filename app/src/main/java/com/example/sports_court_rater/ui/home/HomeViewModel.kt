package com.example.sports_court_rater.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sports_court_rater.CourtPost
import com.example.sports_court_rater.data.CourtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CourtRepository
) : ViewModel() {

    // StateFlow that collects from the repository's Flow
    val courts: StateFlow<List<CourtPost>> = repository.getAllCourts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Optionally refresh data when ViewModel is first created
        refreshCourts()
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
