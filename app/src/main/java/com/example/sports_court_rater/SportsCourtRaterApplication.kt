package com.example.sports_court_rater

import android.app.Application
import com.cloudinary.android.MediaManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SportsCourtRaterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Cloudinary
        val config = mapOf(
            "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
            "api_key" to BuildConfig.CLOUDINARY_API_KEY,
            "api_secret" to BuildConfig.CLOUDINARY_API_SECRET,
            "secure" to true
        )
        MediaManager.init(this, config)
    }
}
