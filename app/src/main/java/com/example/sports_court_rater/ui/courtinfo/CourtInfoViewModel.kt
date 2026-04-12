package com.example.sports_court_rater.ui.courtinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.Review
import com.example.sports_court_rater.User
import com.example.sports_court_rater.data.CourtRepository
import com.example.sports_court_rater.data.remote.RetrofitInstance
import com.example.sports_court_rater.data.remote.WeatherResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CourtInfoViewModel @Inject constructor(
    private val repository: CourtRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _court = MutableStateFlow<Court?>(null)
    val court: StateFlow<Court?> = _court.asStateFlow()

    private val _weather = MutableStateFlow<WeatherResponse?>(null)
    val weather: StateFlow<WeatherResponse?> = _weather.asStateFlow()

    private val _creator = MutableStateFlow<User?>(null)
    val creator: StateFlow<User?> = _creator.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

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
                _isCreator.value = courtDetails.creatorId == auth.currentUser?.uid
                fetchWeather(courtDetails.latitude, courtDetails.longitude)
                
                _creator.value = User(
                    userId = courtDetails.creatorId,
                    displayName = courtDetails.creatorName.ifEmpty { "Anonymous" },
                    profilePictureUrl = courtDetails.creatorImageUrl
                )
                
                fetchReviews(courtId)
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

    private fun fetchReviews(courtId: String) {
        viewModelScope.launch {
            try {
                val reviewsList = repository.getReviewsByCourtId(courtId)
                _reviews.value = reviewsList.sortedByDescending { it.date }
            } catch (e: Exception) {
                _error.value = "Failed to fetch reviews: ${e.message}"
            }
        }
    }

    fun addReview(rating: Float, comment: String) {
        val currentCourt = _court.value ?: return
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            try {
                val review = Review(
                    id = UUID.randomUUID().toString(),
                    courtId = currentCourt.id,
                    creatorId = currentUser.uid,
                    creatorName = currentUser.displayName ?: "Anonymous",
                    creatorImageUrl = currentUser.photoUrl?.toString() ?: "",
                    rating = rating,
                    comment = comment,
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    courtName = currentCourt.courtName
                )
                repository.saveReview(review)
                loadCourtDetails(currentCourt.id)
            } catch (e: Exception) {
                _error.value = "Failed to add review: ${e.message}"
            }
        }
    }

    fun updateReview(reviewId: String, rating: Float, comment: String) {
        if (reviewId.isEmpty()) {
            _error.value = "Failed to update review: Invalid ID"
            return
        }
        val currentCourt = _court.value ?: return
        viewModelScope.launch {
            try {
                val reviewSnapshot = firestore.collection("reviews").document(reviewId).get().await()
                val existingReview = reviewSnapshot.toObject(Review::class.java) ?: return@launch
                
                val updatedReview = existingReview.copy(
                    rating = rating,
                    comment = comment
                )
                repository.saveReview(updatedReview)
                loadCourtDetails(currentCourt.id)
            } catch (e: Exception) {
                _error.value = "Failed to update review: ${e.message}"
            }
        }
    }

    fun deleteReview(review: Review) {
        if (review.id.isEmpty()) {
            _error.value = "Failed to delete review: Invalid ID"
            return
        }
        val currentCourt = _court.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteReview(review)
                loadCourtDetails(currentCourt.id)
            } catch (e: Exception) {
                _error.value = "Failed to delete review: ${e.message}"
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
