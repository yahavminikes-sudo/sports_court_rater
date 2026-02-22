package com.example.sports_court_rater.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "court_posts")
data class CourtPost(
    @PrimaryKey
    val id: String,
    val name: String,
    val address: String,
    val rating: Float
)