package com.example.sports_court_rater.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface CourtPostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: CourtPost)

    @Query("SELECT * FROM court_posts")
    fun getAll(): Flow<List<CourtPost>>

    @Delete
    suspend fun delete(post: CourtPost)
}