package com.example.sports_court_rater

import java.util.UUID

data class Review(
    val id: String = UUID.randomUUID().toString(),
    val courtId: String = "",
    val creatorId: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val date: String = "",
    val courtName: String = "" // Helpful for profile display
)
