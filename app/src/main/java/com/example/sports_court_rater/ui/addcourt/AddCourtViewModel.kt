package com.example.sports_court_rater.ui.addcourt

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sports_court_rater.AuthRepository
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.data.CourtRepository
import com.example.sports_court_rater.utils.LocationHelper
import com.example.sports_court_rater.utils.LocationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddCourtViewModel @Inject constructor(
    private val courtRepository: CourtRepository,
    private val authRepository: AuthRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _latitude = MutableLiveData<Double?>()
    private val _longitude = MutableLiveData<Double?>()

    private val _locationName = MutableLiveData<String?>()
    val locationName: LiveData<String?> = _locationName

    private val _selectedSport = MutableLiveData<String>("כדורסל")
    val selectedSport: LiveData<String> = _selectedSport

    private val _imageUri = MutableLiveData<Uri?>()
    val imageUri: LiveData<Uri?> = _imageUri

    sealed class AddCourtState {
        object Idle : AddCourtState()
        object Loading : AddCourtState()
        object Success : AddCourtState()
        data class Error(val message: String) : AddCourtState()
    }

    private val _uiState = MutableStateFlow<AddCourtState>(AddCourtState.Idle)
    val uiState: StateFlow<AddCourtState> = _uiState.asStateFlow()

    fun setLocation(lat: Double, lng: Double) {
        _latitude.value = lat
        _longitude.value = lng
        fetchLocationName(lat, lng)
    }

    private fun fetchLocationName(lat: Double, lng: Double) {
        viewModelScope.launch {
            val result = locationHelper.reverseGeocode(lat, lng)
            if (result != null) {
                val name = buildString {
                    result.neighborhood?.let { append(it) }
                    if (result.neighborhood != null && result.city != null) append(", ")
                    result.city?.let { append(it) }
                }
                _locationName.value = if (name.isNotEmpty()) name else null
            }
        }
    }

    fun setSport(sport: String) {
        _selectedSport.value = sport
    }

    fun setImageUri(uri: Uri?) {
        _imageUri.value = uri
    }

    fun postCourt(name: String, description: String, rating: Float) {
        val lat = _latitude.value
        val lng = _longitude.value
        val locationName = _locationName.value ?: ""
        val sport = _selectedSport.value ?: "כדורסל"
        val uri = _imageUri.value

        if (lat == null || lng == null) {
            _uiState.value = AddCourtState.Error("Location is required. Please use current location.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AddCourtState.Loading
            try {
                val imageUrl = if (uri != null) {
                    courtRepository.uploadImage(uri)
                } else {
                    ""
                }

                val creatorId = authRepository.getCurrentUser()?.uid ?: ""
                val court = Court(
                    id = UUID.randomUUID().toString(),
                    creatorId = creatorId,
                    courtName = name,
                    sportType = sport,
                    locationName = locationName,
                    latitude = lat,
                    longitude = lng,
                    imageUrl = imageUrl,
                    rating = rating,
                    description = description
                )

                courtRepository.saveCourt(court)
                _uiState.value = AddCourtState.Success
            } catch (e: Exception) {
                _uiState.value = AddCourtState.Error(e.message ?: "An unexpected error occurred.")
            }
        }
    }

    fun resetState() {
        _uiState.value = AddCourtState.Idle
    }
}
