package com.example.sports_court_rater.data

import com.example.sports_court_rater.Court
import com.example.sports_court_rater.data.local.CourtDao
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CourtRepository @Inject constructor(
    private val courtDao: CourtDao,
    private val remoteDataSource: CollectionReference
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
            // In a real app, you'd want to propagate this error or log it
            e.printStackTrace()
        }
    }

    /**
     * Returns the data strictly from the Room DAO (local cache).
     */
    fun getAllCourts(): Flow<List<Court>> {
        return courtDao.getAll()
    }
}
