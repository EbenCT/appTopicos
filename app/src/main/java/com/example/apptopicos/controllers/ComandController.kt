package com.example.apptopicos.controllers

import android.content.Context
import android.content.Intent
import android.util.Log

class ComandController(private val context: Context) {

    // Método que recibe la respuesta de Dialogflow y ejecuta el comando correspondiente
    fun ejecutarComando(respuesta: String) {
        when {
            respuesta.contains("activare la camara", ignoreCase = true) -> activarCamara()
            respuesta.contains("Hasta pronto", ignoreCase = true) -> desactivarEscucha()
            respuesta.contains("Analizando", ignoreCase = true) -> otraAccion()
            else -> Log.d("CommandController", "Comando no reconocido: $respuesta")
        }
    }

    private fun activarCamara() {
        Log.d("CommandController", "Ejecutando: Activar cámara")
        // Aquí puedes agregar el código para activar la cámara si es necesario
    }

    private fun desactivarEscucha() {
        Log.d("CommandController", "Ejecutando: Desactivar escucha")
        // Enviar broadcast para desactivar escucha
        val intent = Intent("com.example.apptopicos.DESACTIVAR_ESCUCHA")
        context.sendBroadcast(intent)
    }

    private fun otraAccion() {
        Log.d("CommandController", "Ejecutando: Otra acción específica")
        // Código para ejecutar una acción específica
    }
}