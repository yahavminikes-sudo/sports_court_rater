package com.example.sports_court_rater.ui.addcourt

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sports_court_rater.AuthRepository
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.data.CourtRepository
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _latitude = MutableLiveData<Double?>()
    val latitude: LiveData<Double?> = _latitude

    private val _longitude = MutableLiveData<Double?>()
    val longitude: LiveData<Double?> = _longitude

    private val _selectedSport = MutableLiveData<String>("basketball")
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
    }

    fun setSport(sport: String) {
        _selectedSport.value = sport
    }

    fun setImageUri(uri: Uri?) {
        _imageUri.value = uri
    }

    /**
     * Publishes a new court post by uploading the image (if present),
     * constructing the Court object, and saving it to Firebase.
     */
    fun postCourt(name: String, description: String, rating: Float) {
        val lat = _latitude.value
        val lng = _longitude.value
        val sport = _selectedSport.value ?: "basketball"
        val uri = _imageUri.value

        if (lat == null || lng == null) {
            _uiState.value = AddCourtState.Error("Location is required. Please use current location.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AddCourtState.Loading
            try {
                // 1. Upload Image and retrieve download URL (Suspend function in Repository)
                val imageUrl = if (uri != null) {
                    courtRepository.uploadImage(uri)
                } else {
                    ""
                }

                // 2. Construct Court object (using UUID for ID)
                val creatorId = authRepository.getCurrentUser()?.uid ?: ""
                val court = Court(
                    id = UUID.randomUUID().toString(),
                    creatorId = creatorId,
                    courtName = name,
                    sportType = sport,
                    latitude = lat,
                    longitude = lng,
                    imageUrl = imageUrl,
                    rating = rating,
                    description = description
                )

                // 3. Save this new Court object to the remote Firebase database
                courtRepository.saveCourt(court)

                // 4. Notify UI via StateFlow when finished
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
