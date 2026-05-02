package com.example.sports_court_rater.ui.editpost

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.data.CourtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditPostViewModel @Inject constructor(
    private val courtRepository: CourtRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 1. Receives existing Court object via safeArgs
    val court: Court? = savedStateHandle["court"]

    private val _latitude = MutableLiveData<Double?>(court?.latitude)
    val latitude: LiveData<Double?> = _latitude

    private val _longitude = MutableLiveData<Double?>(court?.longitude)
    val longitude: LiveData<Double?> = _longitude

    private val _locationName = MutableLiveData<String?>()
    val locationName: LiveData<String?> = _locationName

    sealed class EditPostState {
        object Idle : EditPostState()
        object Loading : EditPostState()
        object Success : EditPostState()
        data class Error(val message: String) : EditPostState()
    }

    private val _uiState = MutableStateFlow<EditPostState>(EditPostState.Idle)
    val uiState: StateFlow<EditPostState> = _uiState.asStateFlow()

    init {
        // Load initial location name
        court?.let {
            setLocation(it.latitude, it.longitude)
        }
    }

    fun setLocation(lat: Double, lng: Double) {
        _latitude.value = lat
        _longitude.value = lng
        viewModelScope.launch {
            _locationName.value = courtRepository.getLocationName(lat, lng)
        }
    }

    /**
     * Updates the existing court post.
     */
    fun updatePost(
        newName: String,
        newSport: String,
        newRating: Float,
        newDescription: String,
        newImageUri: Uri?
    ) {
        val currentCourt = court ?: run {
            _uiState.value = EditPostState.Error("Court data not found.")
            return
        }

        val lat = _latitude.value ?: currentCourt.latitude
        val lng = _longitude.value ?: currentCourt.longitude

        viewModelScope.launch {
            _uiState.value = EditPostState.Loading
            try {
                // If newImageUri is not null, upload the new image
                val imageUrl = if (newImageUri != null) {
                    courtRepository.uploadImage(newImageUri)
                } else {
                    currentCourt.imageUrl
                }

                val updatedCourt = currentCourt.copy(
                    courtName = newName,
                    sportType = newSport,
                    rating = newRating,
                    description = newDescription,
                    imageUrl = imageUrl,
                    latitude = lat,
                    longitude = lng
                )

                courtRepository.saveCourt(updatedCourt)
                _uiState.value = EditPostState.Success
            } catch (e: Exception) {
                _uiState.value = EditPostState.Error(e.message ?: "Failed to update court.")
            }
        }
    }

    /**
     * Deletes the post and its associated image from Firebase.
     */
    fun deletePost(postId: String, imageUrl: String) {
        viewModelScope.launch {
            _uiState.value = EditPostState.Loading
            try {
                // 1. & 2. Delete from remote Firebase and Storage via Repository
                courtRepository.deleteCourt(postId, imageUrl)
                
                // 3. Update UI state
                _uiState.value = EditPostState.Success
            } catch (e: Exception) {
                _uiState.value = EditPostState.Error(e.message ?: "Failed to delete court.")
            }
        }
    }
}
