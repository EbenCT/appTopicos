package com.example.apptopicos.controllers

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import com.example.apptopicos.helpers.GeocoderHelper
import com.example.apptopicos.helpers.LocationHelper
import com.example.apptopicos.helpers.NavegationHelper
import com.example.apptopicos.helpers.PlacesHelper
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale

class GPSController(private val context: Context) : TextToSpeech.OnInitListener {
    private val locationHelper = LocationHelper(context)
    private val geocoderHelper = GeocoderHelper(context)
    private val placesHelper = PlacesHelper(context)
    private var textToSpeech: TextToSpeech? = null
    private var navigationManager: NavegationHelper? = null

    init {
        textToSpeech = TextToSpeech(context, this)
        navigationManager = NavegationHelper(context, textToSpeech)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Configurar idioma a español
            val result = textToSpeech?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(context, "Idioma no soportado para TTS", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Error al inicializar Text-to-Speech", Toast.LENGTH_SHORT).show()
        }
    }

    fun obtenerUbicacion() {
        locationHelper.getCurrentLocation { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude

                GlobalScope.launch(Dispatchers.Main) {
                    try {
                        val addressDetails = geocoderHelper.getAddressFromLocation(latitude, longitude)
                        val mensaje = geocoderHelper.buildVoiceMessage(addressDetails)
                        comunicarPorVoz(mensaje)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        comunicarPorVoz("Ocurrió un error al obtener la ubicación. Intente otra vez")
                    }
                }
            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    comunicarPorVoz("No se pudo obtener la ubicación. Hay problemas de conexión, vualva a intentarlo")
                }
            }
        }
    }

    private fun comunicarPorVoz(mensaje: String) {
        textToSpeech?.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun iniciarNavegacion(mensaje: String) {
        val destino = extraerDestino(mensaje)
        Log.d("GPSController", "Destino recibido: $destino")

        placesHelper.buscarLugares(destino, { lugares ->
            if (lugares.isNotEmpty()) {
                comunicarOpcionesDestino(lugares) { lugarSeleccionado ->
                    obtenerUbicacionActual { ubicacionActual ->
                        navigationManager?.startNavigation(
                            origin = ubicacionActual,
                            destination = lugarSeleccionado.latLng,
                            onError = { error ->
                                comunicarPorVoz("Error al iniciar la navegación: $error")
                                Log.d("GPSController", "Error: $error")
                            }
                        )
                    }
                }
            } else {
                comunicarPorVoz("No se encontraron lugares para '$destino'. Intenta con otro destino.")
            }
        }, { error ->
            comunicarPorVoz("Hubo un problema al buscar lugares: $error")
        })
    }

    private fun comunicarOpcionesDestino(
        lugares: List<PlacesHelper.PlaceResult>,
        onLugarSeleccionado: (PlacesHelper.PlaceResult) -> Unit
    ) {
        val nombres = lugares.map { it.nombre }
        comunicarPorVoz("Se encontraron los siguientes lugares: ${nombres.joinToString(", ")}")

        // Aquí puedes implementar la lógica para mostrar una lista al usuario y permitir la selección.
        // Simularemos que el usuario selecciona el primer lugar.
        val lugarSeleccionado = lugares.first()
        comunicarPorVoz("Seleccionaste: ${lugarSeleccionado.nombre}")
        onLugarSeleccionado(lugarSeleccionado)
    }

    private fun extraerDestino(mensaje: String): String {
        return mensaje.lowercase().replace("quiero ir a ", "")
            .replace("llevame a ", "")
            .replace("como llego a ", "")
            .trim()
    }

    private fun obtenerUbicacionActual(onUbicacionObtenida: (LatLng) -> Unit) {
        locationHelper.getCurrentLocation { location ->
            if (location != null) {
                onUbicacionObtenida(LatLng(location.latitude, location.longitude))
            } else {
                comunicarPorVoz("No se pudo obtener tu ubicación actual. Verifica el GPS.")
            }
        }
    }

    fun liberarRecursos() {
        navigationManager?.stopNavigation()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
