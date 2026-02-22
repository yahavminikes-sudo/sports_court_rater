package com.example.sports_court_rater

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val userId: String,
    val displayName: String,
    val profilePictureUrl: String
)
