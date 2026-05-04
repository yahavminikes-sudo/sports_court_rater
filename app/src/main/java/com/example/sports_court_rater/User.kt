package com.example.sports_court_rater

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val userId: String = "",
    val displayName: String = "",
    val profilePictureUrl: String = ""
)
