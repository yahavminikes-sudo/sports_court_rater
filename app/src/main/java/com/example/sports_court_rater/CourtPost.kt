package com.example.sports_court_rater

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "court_posts")
data class CourtPost(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val creatorId: String,
    val courtName: String,
    val sportType: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String,
    val rating: Float,
    val description: String
)
