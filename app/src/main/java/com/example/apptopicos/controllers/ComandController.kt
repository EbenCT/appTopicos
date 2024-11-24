package com.example.apptopicos.controllers

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.apptopicos.R
import com.example.apptopicos.utils.GlobalVars

import com.example.apptopicos.views.CameraPreviewActivity


class ComandController(private val context: Context) {

    private val resultadoController = ResultadosController(context)
    private val gpsController = GPSController(context)
    fun ejecutarComando(respuesta: String) {
        when {
            respuesta.contains(context.getString(R.string.FRASE_START_CAMERA)   , ignoreCase = true) -> activarCamara()
            respuesta.contains(context.getString(R.string.FRASE_DESACTIVAR)     , ignoreCase = true) -> desactivarEscucha()
            respuesta.contains(context.getString(R.string.FRASE_REPIT_RESULT)   , ignoreCase = true) -> comunicarRegistros()
            respuesta.contains(context.getString(R.string.FRASE_GPS)            , ignoreCase = true) -> solicitarUbicacion()
            respuesta.contains(context.getString(R.string.FRASE_CALCULO)        , ignoreCase = true) -> realizarCalculo()
            respuesta.contains(context.getString(R.string.FRASE_NAVEGACION)     , ignoreCase = true) -> iniciarNavegacion()
            else -> Log.d("CommandController", "Comando no reconocido: $respuesta")
        }
    }

    private fun iniciarNavegacion() {
        TODO("Not yet implemented")
    }

    private fun realizarCalculo() {
        GlobalVars.command=1
    }

    private fun solicitarUbicacion() {
        gpsController.obtenerUbicacion()
    }

    private fun comunicarRegistros() {
        resultadoController.getRegistro()
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
}