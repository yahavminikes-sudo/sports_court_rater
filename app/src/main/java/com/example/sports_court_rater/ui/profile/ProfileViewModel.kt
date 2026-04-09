package com.example.sports_court_rater.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.Review
import com.example.sports_court_rater.data.CourtRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: CourtRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ViewModel() {

    val currentUser: FirebaseUser? get() = auth.currentUser

    private val _userCourts = MutableStateFlow<List<Court>>(emptyList())
    val userCourts: StateFlow<List<Court>> = _userCourts.asStateFlow()

    private val _userReviews = MutableStateFlow<List<Review>>(emptyList())
    val userReviews: StateFlow<List<Review>> = _userReviews.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _updateResult = MutableStateFlow<Result<Unit>?>(null)
    val updateResult: StateFlow<Result<Unit>?> = _updateResult.asStateFlow()

    private val _deleteResult = MutableStateFlow<Result<Unit>?>(null)
    val deleteResult: StateFlow<Result<Unit>?> = _deleteResult.asStateFlow()

    init {
        loadUserData()
    }

    fun loadUserData() {
        val userId = currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            
            val courtsDeferred = repository.getCourtsByCreatorId(userId)
            val reviewsDeferred = repository.getReviewsByCreatorId(userId)
            
            _userCourts.value = courtsDeferred
            _userReviews.value = reviewsDeferred
            
            _isLoading.value = false
        }
    }

    /**
     * Updates the user profile with a new name and/or a new profile photo.
     */
    fun updateProfile(newName: String, newPhotoUri: Uri?) {
        val user = auth.currentUser ?: return
        val userId = user.uid

        viewModelScope.launch {
            _isLoading.value = true
            try {
                var photoUrl = user.photoUrl?.toString()

                // 1. Upload new photo to Firebase Storage if provided
                if (newPhotoUri != null) {
                    val storageRef = storage.reference.child("profile_pics/$userId.jpg")
                    storageRef.putFile(newPhotoUri).await()
                    photoUrl = storageRef.downloadUrl.await().toString()
                }

                // 2. Update Firebase Auth Profile
                val profileUpdates = userProfileChangeRequest {
                    displayName = newName
                    if (photoUrl != null) {
                        photoUri = Uri.parse(photoUrl)
                    }
                }
                user.updateProfile(profileUpdates).await()

                // 3. Update cached user info in Firestore
                val userUpdates = mapOf(
                    "displayName" to newName,
                    "profilePictureUrl" to (photoUrl ?: "")
                )
                firestore.collection("users").document(userId).update(userUpdates).await()

                _updateResult.value = Result.success(Unit)
                loadUserData() // Refresh local data
            } catch (e: Exception) {
                _updateResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateDisplayName(newName: String) {
        updateProfile(newName, null)
    }

    fun deleteCourt(court: Court) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteCourt(court.id, court.imageUrl)
                _deleteResult.value = Result.success(Unit)
                loadUserData() // Refresh the list
            } catch (e: Exception) {
                _deleteResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetUpdateResult() {
        _updateResult.value = null
    }

    fun resetDeleteResult() {
        _deleteResult.value = null
    }

    fun logout() {
        auth.signOut()
    }
}
