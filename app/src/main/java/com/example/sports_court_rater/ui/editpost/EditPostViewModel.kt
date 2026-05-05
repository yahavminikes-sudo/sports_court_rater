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

    fun deletePost(postId: String, imageUrl: String) {
        viewModelScope.launch {
            _uiState.value = EditPostState.Loading
            try {
                courtRepository.deleteCourt(postId, imageUrl)
                
                _uiState.value = EditPostState.Success
            } catch (e: Exception) {
                _uiState.value = EditPostState.Error(e.message ?: "Failed to delete court.")
            }
        }
    }
}
