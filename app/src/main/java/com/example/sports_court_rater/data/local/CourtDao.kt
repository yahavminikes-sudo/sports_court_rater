package com.example.sports_court_rater.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.example.sports_court_rater.Court
import kotlinx.coroutines.flow.Flow

@Dao
interface CourtDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(court: Court)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courts: List<Court>)

    @Query("SELECT * FROM courts")
    fun getAll(): Flow<List<Court>>

    @Query("SELECT * FROM courts WHERE id = :id")
    suspend fun getById(id: String): Court?

    @Delete
    suspend fun delete(court: Court)

    @Query("DELETE FROM courts WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM courts")
    suspend fun deleteAll()
}
