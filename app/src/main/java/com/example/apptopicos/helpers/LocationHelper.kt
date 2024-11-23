package com.example.apptopicos.helpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log

import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

class LocationHelper(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun getCurrentLocation(onLocationResult: (Location?) -> Unit) {
        // Solicitar permisos de ubicación si es necesario
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permisos al usuario
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                onLocationResult(location)
            }
            .addOnFailureListener { exception ->
                // Manejar errores al obtener la ubicación
                Log.e("LocationHelper", "Error al obtener la ubicación: ${exception.message}")
            }
    }
}