package com.example.apptopicos.controllers

import android.content.Context
import android.content.Intent
import android.util.Log

import com.example.apptopicos.views.CameraPreviewActivity


class ComandController(private val context: Context) {

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

        val intent = Intent(context, CameraPreviewActivity::class.java)
        Log.d("CommandController", "creada la intencion, inicando desde un contexto")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Para iniciar desde un contexto de aplicación
        context.startActivity(intent)
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