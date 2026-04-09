package com.example.sports_court_rater.data

import android.net.Uri
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.Review
import com.example.sports_court_rater.data.local.CourtDao
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class CourtRepository @Inject constructor(
    private val courtDao: CourtDao,
    private val firestore: FirebaseFirestore,
    private val remoteDataSource: CollectionReference,
    private val storage: FirebaseStorage
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
     */
    fun getAllCourts(): Flow<List<Court>> {
        return courtDao.getAll()
    }

    suspend fun getCourtById(id: String): Court? {
        return courtDao.getById(id)
    }

    /**
     * Fetches courts created by a specific user from Firestore.
     */
    suspend fun getCourtsByCreatorId(creatorId: String): List<Court> {
        return try {
            val snapshot = remoteDataSource.whereEqualTo("creatorId", creatorId).get().await()
            snapshot.toObjects(Court::class.java)
        } catch (e: Exception) {
            emptyList()
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
