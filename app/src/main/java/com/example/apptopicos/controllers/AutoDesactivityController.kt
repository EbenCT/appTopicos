package com.example.apptopicos.controllers
import android.util.Log

class AutoDesactivityController {

    private var isActive: Boolean = false

    // Método para iniciar la actividad automática
    fun starAutodesactivity() {
        if (!isActive) {
            isActive = true
            Log.d("AutodesactivityController", "Actividad automática iniciada")
        }
    }

    // Método para detener la actividad automática
    fun offAutodesactivity() {
        if (isActive) {
            isActive = false
            Log.d("AutodesactivityController", "Actividad automática detenida")
        }
    }
}