package com.example.apptopicos.controllers

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

class AutoDesactivityController(
    private val registerController: RegisterController,
    private val context: Context // Agregar contexto como parámetro
) {

    private var isActive: Boolean = false
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval: Long = 5000 // Verifica cada 5 segundos
    private val inactivityThreshold: Long = 30000 // 30 segundos de inactividad

    fun starAutodesactivity() {
        if (!isActive) {
            isActive = true
            Log.d("MiApp", "Actividad automática iniciada")
            startInactivityCheck()
            registerController.logEvent("Autodesactivado encendido")
        }
    }

    fun offAutodesactivity() {
        if (isActive) {
            isActive = false
            Log.d("MiApp", "Actividad automática detenida")
            handler.removeCallbacks(inactivityCheckRunnable)
            registerController.logEvent("Autodesactivado apagado")
        }
    }

    private fun startInactivityCheck() {
        handler.postDelayed(inactivityCheckRunnable, checkInterval)
    }

    private val inactivityCheckRunnable = object : Runnable {
        override fun run() {
            if (isActive) {
                val lastActivity = registerController.getLastEvent()
                if (lastActivity != null) {
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastActivity = currentTime - lastActivity.timestamp

                    if (timeSinceLastActivity > inactivityThreshold) {
                        Log.d("MiApp", "Inactividad detectada: Desactivando escucha")

                        // Enviar broadcast para desactivar escucha
                        val intent = Intent("com.example.apptopicos.DESACTIVAR_ESCUCHA")
                        context.sendBroadcast(intent) // Usar el contexto pasado al controlador
                    } else {
                        Log.d("MiApp", "Última actividad hace ${timeSinceLastActivity / 1000} segundos.")
                    }
                }
                handler.postDelayed(this, checkInterval)
            }
        }
    }
}
