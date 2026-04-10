package com.example.sports_court_rater.ui.courtinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.User
import com.example.sports_court_rater.data.CourtRepository
import com.example.sports_court_rater.data.remote.RetrofitInstance
import com.example.sports_court_rater.data.remote.WeatherResponse
import com.example.sports_court_rater.utils.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CourtInfoViewModel @Inject constructor(
    private val repository: CourtRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _court = MutableStateFlow<Court?>(null)
    val court: StateFlow<Court?> = _court.asStateFlow()

    private val _weather = MutableStateFlow<WeatherResponse?>(null)
    val weather: StateFlow<WeatherResponse?> = _weather.asStateFlow()

    private val _creator = MutableStateFlow<User?>(null)
    val creator: StateFlow<User?> = _creator.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isCreator = MutableStateFlow(false)
    val isCreator: StateFlow<Boolean> = _isCreator.asStateFlow()

    private val _deleteResult = MutableStateFlow<Result<Unit>?>(null)
    val deleteResult: StateFlow<Result<Unit>?> = _deleteResult.asStateFlow()

    fun loadCourtDetails(courtId: String) {
        viewModelScope.launch {
            val courtDetails = repository.getCourtById(courtId)
            if (courtDetails != null) {
                _court.value = courtDetails
                if (courtDetails.locationName.isEmpty()) {
                    val name = repository.fetchAndSaveLocationName(courtDetails)
                    if (name != null) {
                        _court.value = courtDetails.copy(locationName = name)
                    }
                }

                _isCreator.value = courtDetails.creatorId == auth.currentUser?.uid
                fetchWeather(courtDetails.latitude, courtDetails.longitude)
                fetchCreatorInfo(courtDetails.creatorId)
            } else {
                _error.value = "Court not found"
            }
        }
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getCurrentWeather(
                    lat = lat,
                    lon = lon,
                    apiKey = "811c9ff3ea3e838042a8911134381d3f"
                )
                _weather.value = response
            } catch (e: Exception) {
                _error.value = "Failed to fetch weather: ${e.message}"
            }
        }
    }

    private fun fetchCreatorInfo(userId: String) {
        if (userId.isEmpty()) return
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(userId).get().await()
                val user = snapshot.toObject(User::class.java)
                _creator.value = user
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteCourt() {
        val currentCourt = _court.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteCourt(currentCourt.id, currentCourt.imageUrl)
                _deleteResult.value = Result.success(Unit)
            } catch (e: Exception) {
                _deleteResult.value = Result.failure(e)
            }
        }
    }

    fun resetDeleteResult() {
        _deleteResult.value = null
    }
}
