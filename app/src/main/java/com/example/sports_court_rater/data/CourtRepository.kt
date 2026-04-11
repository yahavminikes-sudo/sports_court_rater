package com.example.sports_court_rater.data

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.Review
import com.example.sports_court_rater.data.local.CourtDao
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

class CourtRepository @Inject constructor(
    private val courtDao: CourtDao,
    private val firestore: FirebaseFirestore,
    private val remoteDataSource: CollectionReference,
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) {

    /**
     * Fetches the latest list of Court from Firebase (forcing server fetch)
     * and inserts them into the Room database after clearing the local cache.
     */
    suspend fun refreshCourts() {
        try {
            // Force fetch from server to bypass Firebase's internal cache
            val snapshot = remoteDataSource.get(Source.SERVER).await()
            val courts = snapshot.toObjects(Court::class.java)

            // Single source of truth: Update Room
            courtDao.deleteAll()
            courtDao.insertAll(courts)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Returns the data strictly from the Room DAO (local cache).
     * Enriches courts with location names on the fly.
     */
    fun getAllCourts(): Flow<List<Court>> {
        return courtDao.getAll().map { courts ->
            withContext(Dispatchers.IO) {
                courts.forEach { court ->
                    court.locationName = getLocationName(court.latitude, court.longitude)
                }
                courts
            }
        }
    }

    suspend fun getCourtById(id: String): Court? {
        return withContext(Dispatchers.IO) {
            val court = courtDao.getById(id)
            court?.let {
                it.locationName = getLocationName(it.latitude, it.longitude)
            }
            court
        }
    }

    private fun getLocationName(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale("he", "IL"))
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val street = address.thoroughfare
                val number = address.subThoroughfare
                val city = address.locality

                when {
                    street != null && number != null && city != null -> "$street $number, $city"
                    street != null && city != null -> "$street, $city"
                    city != null -> city
                    else -> address.getAddressLine(0)
                        ?: "$lat, $lng"
                }
            } else {
                "$lat, $lng"
            }
        } catch (e: Exception) {
            "$lat, $lng"
        }
    }

    /**
     * Fetches courts created by a specific user.
     * Tries local cache first, then Firestore.
     */
    suspend fun getCourtsByCreatorId(creatorId: String): List<Court> {
        return withContext(Dispatchers.IO) {
            // 1. Check local cache first (guarantees newly created courts show up)
            val localCourts = courtDao.getByCreatorId(creatorId)
            localCourts.forEach { it.locationName = getLocationName(it.latitude, it.longitude) }

            // 2. Fetch from Firestore to sync
            try {
                val snapshot = remoteDataSource.whereEqualTo("creatorId", creatorId).get().await()
                val remoteCourts = snapshot.toObjects(Court::class.java)

                // Update local cache if needed
                if (remoteCourts.isNotEmpty()) {
                    courtDao.insertAll(remoteCourts)
                }

                val finalCourts = if (remoteCourts.isNotEmpty()) remoteCourts else localCourts
                finalCourts.forEach { it.locationName = getLocationName(it.latitude, it.longitude) }
                finalCourts
            } catch (e: Exception) {
                localCourts // Fallback to local on error
            }
        }
    }

    /**
     * Fetches reviews created by a specific user from Firestore.
     */
    suspend fun getReviewsByCreatorId(creatorId: String): List<Review> {
        return try {
            val snapshot = firestore.collection("reviews")
                .whereEqualTo("creatorId", creatorId)
                .get()
                .await()
            snapshot.toObjects(Review::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getReviewsByCourtId(courtId: String): List<Review> {
        return try {
            val snapshot = firestore.collection("reviews")
                .whereEqualTo("courtId", courtId)
                .get()
                .await()
            snapshot.toObjects(Review::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveReview(review: Review) {
        firestore.collection("reviews").document(review.id).set(review).await()

        // Update court rating
        updateCourtRating(review.courtId)
    }

    suspend fun deleteReview(review: Review) {
        firestore.collection("reviews").document(review.id).delete().await()
        updateCourtRating(review.courtId)
    }

    private suspend fun updateCourtRating(courtId: String) {
        val reviews = getReviewsByCourtId(courtId)
        val averageRating = if (reviews.isNotEmpty()) {
            reviews.map { it.rating }.average().toFloat()
        } else {
            0f
        }

        // Update ONLY the rating field in the existing Firestore document
        try {
            remoteDataSource.document(courtId).update("rating", averageRating).await()
        } catch (e: Exception) {
            // Document might not exist or ID mismatch
            e.printStackTrace()
        }

        // Also update local Room database
        val court = courtDao.getById(courtId)
        if (court != null) {
            courtDao.insert(court.copy(rating = averageRating))
        }
    }

    suspend fun uploadImage(uri: Uri): String {
        val fileName = "court_images/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(fileName)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun saveCourt(court: Court) {
        remoteDataSource.document(court.id).set(court).await()
        courtDao.insert(court)
    }

    /**
     * Deletes the court record from Firestore and Room, and removes the image from Storage.
     */
    suspend fun deleteCourt(courtId: String, imageUrl: String) {
        // 1. Delete from Firestore
        remoteDataSource.document(courtId).delete().await()

        // 2. Delete from Room
        courtDao.deleteById(courtId)

        // 3. Delete image from Firebase Storage if it exists
        if (imageUrl.isNotEmpty()) {
            try {
                val storageRef = storage.getReferenceFromUrl(imageUrl)
                storageRef.delete().await()
            } catch (e: Exception) {
                // If the image is already gone or link is invalid, we proceed
                e.printStackTrace()
            }
        }
    }
}
