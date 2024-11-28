package com.example.apptopicos.helpers

import android.content.Context
import android.location.Location
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.apptopicos.R
import com.example.apptopicos.entity.NavegationStep
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class NavegationHelper(
    private val context: Context,
    private val textToSpeech: TextToSpeech?
) {
    private var isNavigating = false
    private var navigationSteps = mutableListOf<NavegationStep>()
    private var currentStepIndex = 0
    private var destinationLatLng: LatLng? = null

    fun stopNavigation() {
        isNavigating = false
        navigationSteps.clear()
        currentStepIndex = 0
    }

    fun startNavigation(
        origin: LatLng,
        destination: LatLng,
        onError: (String) -> Unit
    ) {
        isNavigating = true
        destinationLatLng = destination

        getRouteFromGoogleDirections(origin, destination) { steps, error ->
            if (error != null) {
                onError(error)
                return@getRouteFromGoogleDirections
            }

            if (steps.isNotEmpty()) {
                navigationSteps = steps.toMutableList()
                currentStepIndex = 0
                provideNextInstruction()
            } else {
                onError("No se encontraron pasos para la navegación.")
            }
        }
    }

    private fun provideNextInstruction() {
        if (currentStepIndex < navigationSteps.size) {
            val currentStep = navigationSteps[currentStepIndex]
            comunicarPorVoz("Instrucción: ${currentStep.instruction}")
        } else {
            comunicarPorVoz("Has llegado a tu destino.")
            stopNavigation()
        }
    }

    fun updateLocation(currentLocation: LatLng) {
        if (!isNavigating || currentStepIndex >= navigationSteps.size) return

        val currentStep = navigationSteps[currentStepIndex]
        val distanceToNextPoint = calculateDistance(currentLocation, currentStep.endLocation)

        if (distanceToNextPoint < 10) {
            // Usuario alcanzó el punto actual
            currentStepIndex++
            provideNextInstruction()
        } else if (distanceToNextPoint > 50) {
            // Usuario se desvió del camino, recalcular ruta
            destinationLatLng?.let { destination ->
                recalcularRuta(currentLocation, destination)
            }
        }
    }

    private fun recalcularRuta(currentLocation: LatLng, destination: LatLng) {
        getRouteFromGoogleDirections(currentLocation, destination) { steps, error ->
            if (error == null && steps.isNotEmpty()) {
                navigationSteps = steps.toMutableList()
                currentStepIndex = 0
                comunicarPorVoz("Ruta recalculada. Por favor, sigue la nueva dirección.")
                provideNextInstruction()
            } else {
                comunicarPorVoz("No se pudo recalcular la ruta.")
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

    private fun comunicarPorVoz(mensaje: String) {
        textToSpeech?.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun getRouteFromGoogleDirections(
        origin: LatLng,
        destinationLatLng: LatLng,
        callback: (List<NavegationStep>, String?) -> Unit
    ) {
        val directionsApi = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destinationLatLng.latitude},${destinationLatLng.longitude}" +
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
                    val steps = parseSteps(jsonResponse)
                    withContext(Dispatchers.Main) {
                        callback(steps, null)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(emptyList(), "No se pudo calcular la ruta.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(emptyList(), "Error al obtener la ruta: ${e.message}")
                }
            }
        }
    }

    private fun parseSteps(jsonResponse: JSONObject): List<NavegationStep> {
        val steps = mutableListOf<NavegationStep>()

        val routes = jsonResponse.getJSONArray("routes")
        if (routes.length() > 0) {
            val legs = routes.getJSONObject(0).getJSONArray("legs")
            val stepsJson = legs.getJSONObject(0).getJSONArray("steps")

            for (i in 0 until stepsJson.length()) {
                val step = stepsJson.getJSONObject(i)

                // Instrucción
                val instruction = step.getString("html_instructions").replace("<[^>]*>".toRegex(), "")

                // Distancia
                val distance = step.getJSONObject("distance").getDouble("value") // En metros

                // Duración
                val duration = step.getJSONObject("duration").getDouble("value") // En segundos

                // Ubicación final
                val endLocationJson = step.getJSONObject("end_location")
                val endLocation = LatLng(
                    endLocationJson.getDouble("lat"),
                    endLocationJson.getDouble("lng")
                )

                // Maniobra (opcional)
                val maneuver = if (step.has("maneuver")) step.getString("maneuver") else null

                steps.add(NavegationStep(instruction, distance, duration, endLocation, maneuver))
            }
        }

        return steps
    }
}

