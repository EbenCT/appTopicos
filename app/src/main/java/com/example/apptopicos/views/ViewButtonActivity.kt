package com.example.apptopicos.views

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.apptopicos.R

class ViewButtonActivity : AppCompatActivity() {

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()  // Cierra el Activity
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        setContentView(R.layout.activity_view_button)
        // Obtener referencia del botón
        val button = findViewById<Button>(R.id.fullscreen_button)

        // Configurar el listener para detectar cuando se presiona y se suelta el botón
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Se presionó el botón
                    Log.d("MiApp", "Presionaste el botón")
                }
                MotionEvent.ACTION_UP -> {
                    // Se soltó el botón
                    Log.d("MiApp", "Dejaste de presionar el botón")
                }
            }
            true
        }
        // Registrar el receptor de broadcast para cerrar el Activity
        registerReceiver(closeReceiver, IntentFilter("com.example.apptopicos.CLOSE_ACTIVITY"))
    }

    override fun onDestroy() {
        super.onDestroy()
        // Desregistrar el receptor para evitar fugas de memoria
        unregisterReceiver(closeReceiver)
    }
}
