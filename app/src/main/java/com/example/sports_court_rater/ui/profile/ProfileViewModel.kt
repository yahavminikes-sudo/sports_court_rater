package com.example.sports_court_rater.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.Review
import com.example.sports_court_rater.User
import com.example.sports_court_rater.data.CourtRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
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
    private val firestore: FirebaseFirestore
) : ViewModel() {

    val currentUser: FirebaseUser? get() = auth.currentUser

    private val _userCourts = MutableStateFlow<List<Court>>(emptyList())
    val userCourts: StateFlow<List<Court>> = _userCourts.asStateFlow()

    private val _userReviews = MutableStateFlow<List<Review>>(emptyList())
    val userReviews: StateFlow<List<Review>> = _userReviews.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
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
            if (_userCourts.value.isEmpty() && _userReviews.value.isEmpty()) {
                _isLoading.value = true
            }
            
            try {
                val courts = repository.getCourtsByCreatorId(userId)
                    .sortedByDescending { it.date }
                val reviews = repository.getReviewsByCreatorId(userId)
                    .sortedByDescending { it.date }
                
                _userCourts.value = courts
                _userReviews.value = reviews
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(newName: String, newPhotoUri: Uri?) {
        val user = auth.currentUser ?: return
        val userId = user.uid

        viewModelScope.launch {
            _isLoading.value = true
            try {
                var photoUrl = user.photoUrl?.toString()

                if (newPhotoUri != null) {
                    photoUrl = repository.uploadImage(newPhotoUri, "profile_pics")
                }

                val profileUpdates = userProfileChangeRequest {
                    displayName = newName
                    if (photoUrl != null) {
                        photoUri = Uri.parse(photoUrl)
                    }
                }
                user.updateProfile(profileUpdates).await()

                val updatedUser = User(
                    userId = userId,
                    displayName = newName,
                    profilePictureUrl = photoUrl ?: ""
                )
                repository.saveUser(updatedUser)

                _updateResult.value = Result.success(Unit)
                loadUserData()
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

    fun updateReview(reviewId: String, rating: Float, comment: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val reviewSnapshot = firestore.collection("reviews").document(reviewId).get().await()
                val existingReview = reviewSnapshot.toObject(Review::class.java) ?: return@launch
                
                val updatedReview = existingReview.copy(
                    rating = rating,
                    comment = comment
                )
                repository.saveReview(updatedReview)
                loadUserData()
            } catch (e: Exception) {
                _updateResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteReview(review: Review) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteReview(review)
                loadUserData()
            } catch (e: Exception) {
                _deleteResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteCourt(court: Court) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteCourt(court.id, court.imageUrl)
                _deleteResult.value = Result.success(Unit)
                loadUserData()
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
