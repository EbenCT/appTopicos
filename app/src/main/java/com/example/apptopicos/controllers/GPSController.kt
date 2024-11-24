package com.example.apptopicos.controllers

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import com.example.apptopicos.helpers.GeocoderHelper
import com.example.apptopicos.helpers.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale

class GPSController(private val context: Context) : TextToSpeech.OnInitListener {

    private val locationHelper = LocationHelper(context)
    private val geocoderHelper = GeocoderHelper(context)
    private var textToSpeech: TextToSpeech? = null

    init {
        textToSpeech = TextToSpeech(context, this) // Inicializar Text-to-Speech
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

    fun liberarRecursos() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    fun iniciarNavegacion(mensaje: String) {

    }
}
