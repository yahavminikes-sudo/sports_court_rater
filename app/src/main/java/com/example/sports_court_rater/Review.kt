package com.example.sports_court_rater

data class Review(
    val id: String = "",
    val courtId: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val creatorImageUrl: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val date: String = "",
    val courtName: String = "" // Helpful for profile display
)
