package com.example.apptopicos.helpers

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.example.apptopicos.R
import com.example.apptopicos.entity.AddressDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient

class GeocoderHelper(private val context: Context) {
    private val placesClient: PlacesClient

    init {
        Places.initialize(context, context.getString(R.string.google_maps_key))
        placesClient = Places.createClient(context)
    }

    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): AddressDetails? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latitude, longitude, 2) ?: emptyList()
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 2) ?: emptyList()
                }
                Log.d("GeocoderHelper", "Resultados obtenidos: $addresses")
                if (addresses.isNotEmpty()) {
                    extractAddressDetails(addresses[1]) // Retorna el segundo resultado si existe
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun extractAddressDetails(address: Address): AddressDetails {
        return AddressDetails(
            country = address.countryName,
            department = address.adminArea,
            province = address.subAdminArea,
            locality = address.locality,
            street = address.thoroughfare,
            feature = address.featureName
        )
    }

    fun buildVoiceMessage(addressDetails: AddressDetails?): String {
        return addressDetails?.let {
            buildString {
                append("Te encuentras en ")
                it.country?.let { country -> append("$country, ") }
                it.department?.let { department -> append("en el $department, ") }
                it.province?.let { province -> append("$province, ") }
                it.locality?.let { locality -> append("localidad de $locality, ") }
                it.street?.let { street -> append("en la calle o avenida $street.") }
                it.feature?.let { feature -> append("punto de interés $feature.") }
            }.ifEmpty {
                "No se pudo determinar la ubicación exacta, por favor intente de nuevo."
            }
        } ?: "No se pudo obtener información de la ubicación. Hay problemas con la coenxión, intente de nuevo"
    }
}
