package com.example.sports_court_rater

import com.google.firebase.firestore.DocumentId
import java.util.UUID

data class Review(
    @DocumentId
    val id: String = UUID.randomUUID().toString(),
    val courtId: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val creatorImageUrl: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val date: String = "",
    val courtName: String = "" // Helpful for profile display
)
