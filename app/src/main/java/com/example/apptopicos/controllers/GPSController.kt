package com.example.apptopicos.controllers

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import com.example.apptopicos.helpers.GeocoderHelper
import com.example.apptopicos.helpers.LocationHelper
import com.example.apptopicos.helpers.NavegationHelper
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale

class GPSController(private val context: Context) : TextToSpeech.OnInitListener {
    private val locationHelper = LocationHelper(context)
    private val geocoderHelper = GeocoderHelper(context)
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
        // Extraer el destino del mensaje
        val destino = extraerDestino(mensaje)
        Log.d("NavegadorGPS", "Destino: $destino")
        // Obtener la ubicación actual
        locationHelper.getCurrentLocation { location ->
            if (location != null) {
                val origin = LatLng(location.latitude, location.longitude)

                // Iniciar la navegación
                navigationManager?.startNavigation(
                    origin = origin,
                    destination = destino,
                    onError = { error ->
                        GlobalScope.launch(Dispatchers.Main) {
                            comunicarPorVoz("Lo siento, hubo un error: $error")
                            Log.d("NavegadorGPS", "Lo siento, hubo un error: $error")
                        }
                    }
                )
            } else {
                comunicarPorVoz("No se pudo obtener tu ubicación actual. Por favor, verifica que el GPS esté activado")
                Log.d("NavegadorGPS", "No se pudo obtener tu ubicación actual. Por favor, verifica que el GPS esté activado")
            }
        }
    }

    private fun extraerDestino(mensaje: String): String {
        // Aquí puedes implementar la lógica para extraer el destino del mensaje
        // Por ejemplo, si el mensaje es "quiero ir a Plaza Mayor"
        return mensaje.toLowerCase().replace("quiero ir a ", "")
            .replace("llevame a ", "")
            .replace("como llego a ", "")
            .trim()
    }

    fun liberarRecursos() {
        navigationManager?.stopNavigation()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
