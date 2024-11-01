package com.example.apptopicos.controllers

import android.content.Context
import android.content.Intent

class ComandController (private val context: Context,){
    fun comando(mensaje: String){
        when {
            mensaje.contains("hola", ignoreCase = true) || mensaje.contains("comenzar", ignoreCase = true) -> {

            }
            mensaje.contains("terminado", ignoreCase = true) || mensaje.contains("Adios", ignoreCase = true) || mensaje.contains("pronto", ignoreCase = true) || mensaje.contains("Gracias por utilizar nuestros servicios", ignoreCase = true)-> {
                val intent = Intent("com.example.apptopicos.DESACTIVAR_ESCUCHA")
                context.sendBroadcast(intent)
            }
            mensaje.contains("cámara", ignoreCase = true) -> {
                //soController.activarCamara()
            }
            else -> {

            }
        }
    }
}