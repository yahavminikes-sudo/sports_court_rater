package com.example.sports_court_rater.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.example.sports_court_rater.CourtPost
import kotlinx.coroutines.flow.Flow

@Dao
interface CourtPostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: CourtPost)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<CourtPost>)

    @Query("SELECT * FROM court_posts")
    fun getAll(): Flow<List<CourtPost>>

    @Delete
    suspend fun delete(post: CourtPost)
    
    @Query("DELETE FROM court_posts")
    suspend fun deleteAll()
}