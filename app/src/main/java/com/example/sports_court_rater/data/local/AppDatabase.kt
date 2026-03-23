package com.example.sports_court_rater.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.sports_court_rater.Court

@Database(entities = [Court::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courtDao(): CourtDao
}
