package com.example.sports_court_rater.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sports_court_rater.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<User>)

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteById(userId: String)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
