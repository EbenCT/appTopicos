package com.example.apptopicos.controllers

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class RegisterController {

    private var isRegistering: Boolean = false
    private val eventLog = mutableListOf<Event>()

    // Clase interna para representar un evento con descripción y timestamp
    data class Event(val description: String, val timestamp: Long)

    // Método para iniciar el registro
    fun starRegister() {
        if (!isRegistering) {
            isRegistering = true
            Log.d("RegisterController", "Registro iniciado")
            logEvent("Registro iniciado")
        }
    }

    // Método para detener el registro
    fun offRegister() {
        if (isRegistering) {
            logEvent("Registro detenido")
            isRegistering = false
            Log.d("RegisterController", "Registro detenido")
        }
    }

    // Método para registrar un evento si el registro está activo
    fun logEvent(description: String) {
        if (isRegistering) {
            val timestamp = System.currentTimeMillis()
            eventLog.add(Event(description, timestamp))
            Log.d("RegisterController", "Evento registrado: $description a las ${formatDate(timestamp)}")
        }
    }

    // Obtener el último evento registrado
    fun getLastEvent(): Event? {
        return eventLog.lastOrNull()
    }

    // Método para formatear el timestamp a una fecha legible
    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
