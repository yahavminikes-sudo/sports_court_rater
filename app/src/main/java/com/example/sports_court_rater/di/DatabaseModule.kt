package com.example.sports_court_rater.di

import android.content.Context
import androidx.room.Room
import com.example.sports_court_rater.data.local.AppDatabase
import com.example.sports_court_rater.data.local.CourtDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

    @Provides
    fun provideCourtDao(database: AppDatabase): CourtDao {
        return database.courtDao()
    }
}
