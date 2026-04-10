package com.example.sports_court_rater.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class LocationResult(
    val city: String?,
    val neighborhood: String?
)

@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val geocoder = Geocoder(context, Locale("iw", "IL"))

    suspend fun reverseGeocode(latitude: Double, longitude: Double): LocationResult? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                try {
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            continuation.resume(processAddresses(addresses))
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resume(null)
                        }
                    })
                } catch (e: Exception) {
                    continuation.resume(null)
                }
            }
        } else {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                processAddresses(addresses)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun processAddresses(addresses: List<Address>?): LocationResult? {
        if (addresses.isNullOrEmpty()) return null
        val address = addresses[0]
        return LocationResult(
            city = address.locality,
            neighborhood = address.subLocality ?: address.thoroughfare
        )
    }
}
