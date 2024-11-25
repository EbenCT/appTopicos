package com.example.apptopicos.helpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.apptopicos.R
import com.example.apptopicos.entity.NavegationStep
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class NavegationHelper(private val context: Context,
                       private val textToSpeech: TextToSpeech?
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var navigationSteps = mutableListOf<NavegationStep>()
    private var currentStepIndex = 0
    private var isNavigating = false
    private var locationCallback: LocationCallback? = null

    fun stopNavigation() {
        isNavigating = false
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
        currentStepIndex = 0
        navigationSteps.clear()
    }

    fun startNavigation(origin: LatLng, destination: String, onError: (String) -> Unit) {
        isNavigating = true

        // Configurar la solicitud de ubicación
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000 // 5 segundos
            fastestInterval = 3000
        }

        // Crear el callback de ubicación
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (!isNavigating) return

                val location = locationResult.lastLocation ?: return
                val currentLatLng = LatLng(location.latitude, location.longitude)

                checkProgressAndProvideInstructions(currentLatLng)
            }
        }

        // Iniciar la obtención de la ruta
        getRouteFromGoogleDirections(origin, destination) { steps, error ->
            if (error != null) {
                onError(error)
                return@getRouteFromGoogleDirections
            }

            navigationSteps = steps.toMutableList()
            if (navigationSteps.isNotEmpty()) {
                // Iniciar actualizaciones de ubicación
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback!!,
                        Looper.getMainLooper()
                    )
                    // Proporcionar primera instrucción
                    provideNextInstruction()
                }
            }
        }
    }

    private fun getRouteFromGoogleDirections(origin: LatLng, destination: String, callback: (List<NavegationStep>, String?) -> Unit) {
        val directionsApi = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${URLEncoder.encode(destination, "UTF-8")}" +
                "&mode=walking" +
                "&language=es" +
                "&key=${context.getString(R.string.google_maps_key)}"

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL(directionsApi)
                val connection = url.openConnection() as HttpURLConnection

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                Log.d("Navegador", "Respuesta de Google: $jsonResponse")
                if (jsonResponse.getString("status") == "OK") {
                    val route = jsonResponse.getJSONArray("routes").getJSONObject(0)
                    val legs = route.getJSONArray("legs").getJSONObject(0)
                    val steps = legs.getJSONArray("steps")

                    val navigationSteps = mutableListOf<NavegationStep>()

                    for (i in 0 until steps.length()) {
                        val step = steps.getJSONObject(i)
                        val endLoc = step.getJSONObject("end_location")
                        val instruction = step.getString("html_instructions")
                            .replace(Regex("<[^>]*>"), "") // Remover tags HTML

                        navigationSteps.add(
                            NavegationStep(
                                instruction = instruction,
                                distance = step.getJSONObject("distance").getDouble("value"),
                                duration = step.getJSONObject("duration").getDouble("value"),
                                endLocation = LatLng(
                                    endLoc.getDouble("lat"),
                                    endLoc.getDouble("lng")
                                ),
                                maneuver = if (step.has("maneuver")) step.getString("maneuver") else null
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        callback(navigationSteps, null)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(emptyList(), "No se pudo obtener la ruta")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(emptyList(), "Error al obtener la ruta: ${e.message}")
                }
            }
        }
    }

    private fun checkProgressAndProvideInstructions(currentLocation: LatLng) {
        if (currentStepIndex >= navigationSteps.size) return

        val currentStep = navigationSteps[currentStepIndex]
        val distanceToNextPoint = calculateDistance(
            currentLocation,
            currentStep.endLocation
        )

        // Si estamos cerca del siguiente punto (10 metros)
        if (distanceToNextPoint < 10) {
            currentStepIndex++
            if (currentStepIndex < navigationSteps.size) {
                provideNextInstruction()
            } else {
                announceArrival()
            }
        }
    }

    private fun calculateDistance(point1: LatLng, point2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0]
    }

    private fun provideNextInstruction() {
        if (currentStepIndex < navigationSteps.size) {
            val instruction = navigationSteps[currentStepIndex].instruction
            textToSpeech?.speak(instruction, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun announceArrival() {
        textToSpeech?.speak(
            "Has llegado a tu destino",
            TextToSpeech.QUEUE_FLUSH,
            null,
            null
        )
        stopNavigation()
    }
}