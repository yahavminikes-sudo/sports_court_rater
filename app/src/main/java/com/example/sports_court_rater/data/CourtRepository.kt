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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.utils.ObjectUtils

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
     * Enriches courts with location names and average ratings on the fly.
     */
    fun getAllCourts(): Flow<List<Court>> {
        return courtDao.getAll().map { courts ->
            // Use a for loop to handle suspending calls within the suspending map block
            for (court in courts) {
                court.locationName = getLocationName(court.latitude, court.longitude)
                court.averageRating = calculateAverageRating(court.id, court.rating)
            }
            courts
        }
    }

    suspend fun getCourtById(id: String): Court? {
        return withContext(Dispatchers.IO) {
            val court = courtDao.getById(id)
            court?.let {
                it.locationName = getLocationName(it.latitude, it.longitude)
                it.averageRating = calculateAverageRating(it.id, it.rating)
            }
            court
        }
    }

    private suspend fun calculateAverageRating(courtId: String, initialRating: Float): Float {
        val reviews = getReviewsByCourtId(courtId)
        val allRatings = reviews.map { it.rating }.toMutableList()
        allRatings.add(initialRating)
        return if (allRatings.isNotEmpty()) allRatings.average().toFloat() else initialRating
    }

    suspend fun getLocationName(lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        try {
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

    suspend fun getCoordinatesFromAddress(address: String): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale("he", "IL"))
                val addresses = geocoder.getFromLocationName(address, 1)
                if (!addresses.isNullOrEmpty()) {
                    val location = addresses[0]
                    Pair(location.latitude, location.longitude)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
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
            for (court in localCourts) {
                court.locationName = getLocationName(court.latitude, court.longitude)
                court.averageRating = calculateAverageRating(court.id, court.rating)
            }

            // 2. Fetch from Firestore to sync
            try {
                val snapshot = remoteDataSource.whereEqualTo("creatorId", creatorId).get().await()
                val remoteCourts = snapshot.toObjects(Court::class.java)

                // Update local cache if needed
                if (remoteCourts.isNotEmpty()) {
                    courtDao.insertAll(remoteCourts)
                }

                val finalCourts = if (remoteCourts.isNotEmpty()) remoteCourts else localCourts
                for (court in finalCourts) {
                    court.locationName = getLocationName(court.latitude, court.longitude) 
                    court.averageRating = calculateAverageRating(court.id, court.rating)
                }
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
    }

    suspend fun deleteReview(review: Review) {
        firestore.collection("reviews").document(review.id).delete().await()
    }

    suspend fun uploadImage(uri: Uri, folder: String = "court_images"): String = suspendCancellableCoroutine { continuation ->
        MediaManager.get().upload(uri)
            .option("folder", folder)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                    val url = resultData?.get("secure_url") as? String ?: ""
                    continuation.resume(url)
                }
                override fun onError(requestId: String?, error: ErrorInfo?) {
                    continuation.resumeWithException(Exception(error?.description ?: "Unknown error uploading image"))
                }
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }

    suspend fun saveCourt(court: Court) {
        remoteDataSource.document(court.id).set(court).await()
        courtDao.insert(court)
    }

    private fun extractPublicId(url: String): String? {
        try {
            val uploadIndex = url.indexOf("/upload/")
            if (uploadIndex == -1) return null
            val afterUpload = url.substring(uploadIndex + "/upload/".length)
            
            // Remove version tag if present (e.g., v1234567890/)
            val versionRegex = Regex("^v\\d+/")
            val withoutVersion = afterUpload.replaceFirst(versionRegex, "")
            
            // Remove extension
            val extensionIndex = withoutVersion.lastIndexOf('.')
            return if (extensionIndex != -1) {
                withoutVersion.substring(0, extensionIndex)
            } else {
                withoutVersion
            }
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Deletes the court record from Firestore and Room, and removes the image from Storage.
     */
    suspend fun deleteCourt(courtId: String, imageUrl: String) {
        // 1. Delete from Firestore
        remoteDataSource.document(courtId).delete().await()

        // 2. Delete from Room
        courtDao.deleteById(courtId)

        // 3. Delete image from Cloudinary if it exists
        if (imageUrl.isNotEmpty()) {
            try {
                val publicId = extractPublicId(imageUrl)
                if (publicId != null) {
                    withContext(Dispatchers.IO) {
                        MediaManager.get().getCloudinary().uploader().destroy(publicId, ObjectUtils.emptyMap())
                    }
                }
            } catch (e: Exception) {
                // If the image is already gone or link is invalid, we proceed
                e.printStackTrace()
            }
        }
    }
}
