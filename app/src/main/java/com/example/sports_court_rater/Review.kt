package com.example.sports_court_rater

import com.google.firebase.firestore.Exclude

data class Review(
    val id: String = "",
    val courtId: String = "",
    val creatorId: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val date: String = "",
) {
    @get:Exclude
    var creatorName: String = ""

    @get:Exclude
    var creatorImageUrl: String = ""

    @get:Exclude
    @set:Exclude
    var courtName: String = ""
}
