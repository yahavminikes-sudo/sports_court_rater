package com.example.sports_court_rater.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.User

@Database(entities = [Court::class, User::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courtDao(): CourtDao
    abstract fun userDao(): UserDao
}
