package com.example.sports_court_rater

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "court_posts")
data class CourtPost(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val creatorId: String = "",
    val courtName: String = "",
    val sportType: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String = "",
    val rating: Float = 0f,
    val description: String = ""
)
