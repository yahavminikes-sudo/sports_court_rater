package com.example.sports_court_rater

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
@Entity(tableName = "courts")
data class Court(
    @PrimaryKey 
    val id: String = UUID.randomUUID().toString(),
    val creatorId: String = "",
    val courtName: String = "",
    val sportType: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String = "",
    val rating: Float = 0f,
    val description: String = ""
) : Parcelable {
    @Ignore
    @get:Exclude
    @IgnoredOnParcel
    var locationName: String? = null
}
