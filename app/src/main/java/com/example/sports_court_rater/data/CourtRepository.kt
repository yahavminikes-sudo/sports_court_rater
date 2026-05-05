package com.example.sports_court_rater.data

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.Review
import com.example.sports_court_rater.User
import com.example.sports_court_rater.data.local.CourtDao
import com.example.sports_court_rater.data.local.UserDao
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
import javax.inject.Inject
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.utils.ObjectUtils

class CourtRepository @Inject constructor(
    private val courtDao: CourtDao,
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore,
    private val remoteDataSource: CollectionReference,
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) {

    suspend fun refreshCourts() {
        try {
            val snapshot = remoteDataSource.get(Source.SERVER).await()
            val courts = snapshot.toObjects(Court::class.java)

            courtDao.deleteAll()
            courtDao.insertAll(courts)

            val userIds = courts.map { it.creatorId }.distinct()
            for (userId in userIds) {
                fetchAndCacheUser(userId, forceRefresh = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun fetchAndCacheUser(userId: String, forceRefresh: Boolean = false): User? {
        if (userId.isEmpty()) return null
        return try {
            val source = if (forceRefresh) Source.SERVER else Source.DEFAULT
            val userSnapshot = firestore.collection("users").document(userId).get(source).await()
            val user = userSnapshot.toObject(User::class.java)
            if (user != null) {
                userDao.insert(user)
            }
            user
        } catch (e: Exception) {
            userDao.getUserById(userId)
        }
    }

    private suspend fun fetchAndCacheCourt(courtId: String, forceRefresh: Boolean = false): Court? {
        if (courtId.isEmpty()) return null
        return try {
            val source = if (forceRefresh) Source.SERVER else Source.DEFAULT
            val snapshot = remoteDataSource.document(courtId).get(source).await()
            val court = snapshot.toObject(Court::class.java)
            if (court != null) {
                courtDao.insert(court)
            }
            court
        } catch (e: Exception) {
            courtDao.getById(courtId)
        }
    }

    fun getAllCourts(): Flow<List<Court>> {
        return courtDao.getAll().map { courts ->
            val userCache = mutableMapOf<String, User?>()
            for (court in courts) {
                court.locationName = getLocationName(court.latitude, court.longitude)
                court.averageRating = calculateAverageRating(court.id, court.rating)
                
                val user = userCache.getOrPut(court.creatorId) {
                    userDao.getUserById(court.creatorId) ?: fetchAndCacheUser(court.creatorId)
                }

                if (user != null) {
                    court.creatorName = user.displayName
                    court.creatorImageUrl = user.profilePictureUrl
                } else {
                    court.creatorName = "Anonymous"
                }
            }
            courts
        }
    }

    suspend fun getCourtById(id: String): Court? {
        return withContext(Dispatchers.IO) {
            val court = courtDao.getById(id) ?: fetchAndCacheCourt(id)
            court?.let {
                it.locationName = getLocationName(it.latitude, it.longitude)
                it.averageRating = calculateAverageRating(it.id, it.rating)
                val user = userDao.getUserById(it.creatorId) ?: fetchAndCacheUser(it.creatorId)
                if (user != null) {
                    it.creatorName = user.displayName
                    it.creatorImageUrl = user.profilePictureUrl
                } else {
                    it.creatorName = "Anonymous"
                }
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

    suspend fun getCourtsByCreatorId(creatorId: String): List<Court> {
        return withContext(Dispatchers.IO) {
            val localCourts = courtDao.getByCreatorId(creatorId)
            val user = userDao.getUserById(creatorId) ?: fetchAndCacheUser(creatorId)
            
            for (court in localCourts) {
                court.locationName = getLocationName(court.latitude, court.longitude)
                court.averageRating = calculateAverageRating(court.id, court.rating)
                if (user != null) {
                    court.creatorName = user.displayName
                    court.creatorImageUrl = user.profilePictureUrl
                } else {
                    court.creatorName = "Anonymous"
                }
            }

            try {
                val snapshot = remoteDataSource.whereEqualTo("creatorId", creatorId).get().await()
                val remoteCourts = snapshot.toObjects(Court::class.java)

                if (remoteCourts.isNotEmpty()) {
                    courtDao.insertAll(remoteCourts)
                }

                val finalCourts = if (remoteCourts.isNotEmpty()) remoteCourts else localCourts
                for (court in finalCourts) {
                    court.locationName = getLocationName(court.latitude, court.longitude)
                    court.averageRating = calculateAverageRating(court.id, court.rating)
                    if (user != null) {
                        court.creatorName = user.displayName
                        court.creatorImageUrl = user.profilePictureUrl
                    } else {
                        court.creatorName = "Anonymous"
                    }
                }
                finalCourts
            } catch (e: Exception) {
                localCourts
            }
        }
    }

    suspend fun getReviewsByCreatorId(creatorId: String): List<Review> {
        return try {
            val snapshot = firestore.collection("reviews")
                .whereEqualTo("creatorId", creatorId)
                .get()
                .await()
            val reviews = snapshot.toObjects(Review::class.java)
            val user = userDao.getUserById(creatorId) ?: fetchAndCacheUser(creatorId)
            
            val courtCache = mutableMapOf<String, Court?>()
            for (review in reviews) {
                if (user != null) {
                    review.creatorName = user.displayName
                    review.creatorImageUrl = user.profilePictureUrl
                } else {
                    review.creatorName = "Anonymous"
                }

                val court = courtCache.getOrPut(review.courtId) {
                    fetchAndCacheCourt(review.courtId, forceRefresh = true)
                }
                if (court != null) {
                    review.courtName = court.courtName
                }
            }
            reviews
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
            val reviews = snapshot.toObjects(Review::class.java)

            val court = courtDao.getById(courtId) ?: fetchAndCacheCourt(courtId)

            val userCache = mutableMapOf<String, User?>()
            for (review in reviews) {
                val user = userCache.getOrPut(review.creatorId) {
                    userDao.getUserById(review.creatorId) ?: fetchAndCacheUser(review.creatorId)
                }
                if (user != null) {
                    review.creatorName = user.displayName
                    review.creatorImageUrl = user.profilePictureUrl
                } else {
                    review.creatorName = "Anonymous"
                }
                if (court != null) {
                    review.courtName = court.courtName
                }
            }
            reviews
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

    suspend fun saveUser(user: User) {
        firestore.collection("users").document(user.userId).set(user).await()
        userDao.insert(user)
    }

    suspend fun getUserById(userId: String): User? {
        return userDao.getUserById(userId) ?: fetchAndCacheUser(userId)
    }

    private fun extractPublicId(url: String): String? {
        try {
            val uploadIndex = url.indexOf("/upload/")
            if (uploadIndex == -1) return null
            val afterUpload = url.substring(uploadIndex + "/upload/".length)
            val versionRegex = Regex("^v\\d+/")
            val withoutVersion = afterUpload.replaceFirst(versionRegex, "")
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

    suspend fun deleteCourt(courtId: String, imageUrl: String) {
        remoteDataSource.document(courtId).delete().await()
        courtDao.deleteById(courtId)
        if (imageUrl.isNotEmpty()) {
            try {
                val publicId = extractPublicId(imageUrl)
                if (publicId != null) {
                    withContext(Dispatchers.IO) {
                        MediaManager.get().getCloudinary().uploader().destroy(publicId, ObjectUtils.emptyMap())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
