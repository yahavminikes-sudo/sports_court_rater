package com.example.sports_court_rater.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    @SerialName("main") val main: Main,
    @SerialName("weather") val weather: List<WeatherDescription>
)

@Serializable
data class Main(
    @SerialName("temp") val temp: Double
)

@Serializable
data class WeatherDescription(
    @SerialName("description") val description: String,
    @SerialName("icon") val icon: String
)
