package com.example.apptopicos.controllers

import android.util.Log

class RegisterController {

    private var isRegistering: Boolean = false

    // Método para iniciar el registro
    fun starRegister() {
        if (!isRegistering) {
            isRegistering = true
            Log.d("RegisterController", "Registro iniciado")
        }
    }

    // Método para detener el registro
    fun offRegister() {
        if (isRegistering) {
            isRegistering = false
            Log.d("RegisterController", "Registro detenido")
        }
    }
}
