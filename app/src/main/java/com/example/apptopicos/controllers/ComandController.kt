package com.example.apptopicos.controllers

import android.content.Context
import android.util.Log

class ComandController(private val context: Context) {

    // Método que recibe la respuesta de Dialogflow y ejecuta el comando correspondiente
    fun ejecutarComando(respuesta: String) {
        when {
            respuesta.contains("activar cámara", ignoreCase = true) -> activarCamara()
            respuesta.contains("desactivar escucha", ignoreCase = true) -> desactivarEscucha()
            respuesta.contains("otra acción específica", ignoreCase = true) -> otraAccion()
            else -> Log.d("CommandController", "Comando no reconocido: $respuesta")
        }
    }

    private fun activarCamara() {
        Log.d("CommandController", "Ejecutando: Activar cámara")
        // Aquí puedes agregar el código para activar la cámara si es necesario
    }

    private fun desactivarEscucha() {
        Log.d("CommandController", "Ejecutando: Desactivar escucha")
        // Aquí puedes agregar el código para desactivar la escucha
    }

    private fun otraAccion() {
        Log.d("CommandController", "Ejecutando: Otra acción específica")
        // Código para ejecutar una acción específica
    }
}