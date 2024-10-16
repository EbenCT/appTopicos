package com.example.apptopicos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.apptopicos.controllers.RegisterController
import com.example.apptopicos.views.ViewButtonActivity

class MyService : Service() {

    private lateinit var volumeObserver: ContentObserver
    private var previousVolume: Int = 0
    private var Modo: Boolean = false
    private lateinit var registerController: RegisterController

    override fun onCreate() {
        super.onCreate()
        registerController = RegisterController()
        Log.d("MiApp", "Servicio creado") // Log para servicio creado

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        // Configuramos el ContentObserver para monitorear los cambios en el volumen
        volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                detectarPulsacion()  // Llamamos a la función para manejar el cambio de volumen
            }
        }

        // Registramos el observer
        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI, true, volumeObserver
        )
    }

    private fun detectarPulsacion() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        // Si el volumen ha cambiado, invertimos el estado de "Modo"
        if (currentVolume != previousVolume) {
            Modo = !Modo  // Cambiamos el estado de la variable Modo
            Log.d("MiApp", "Modo cambiado a: $Modo") // Log para indicar el cambio de modo

            if (currentVolume > previousVolume) {
                Log.d("MiApp", "Subir volumen detectado") // Log para subir volumen
            } else if (currentVolume < previousVolume) {
                Log.d("MiApp", "Bajar volumen detectado") // Log para bajar volumen
            }
        }
        previousVolume = currentVolume

        verificar_swich()
    }

    private fun verificar_swich() {
        if (Modo) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                activar_escucha() // Solo ejecuta si tienes permiso
            }
        } else {
            desactivar_escucha() // Llama a desactivar_escucha si Modo está en false
        }
    }

    private fun activar_escucha() {
        Log.d("MiApp", "Activando escucha...")

        val intent = Intent(this, ViewButtonActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            pendingIntent.send()  // Inicia el Activity usando el PendingIntent
            Log.d("MiApp", "Activity lanzada con PendingIntent")
        } catch (e: PendingIntent.CanceledException) {
            Log.e("MiApp", "Error al lanzar Activity: ${e.message}")
        }
        registerController.starRegister()
    }

    private fun desactivar_escucha() {
        Log.d("MiApp", "Desactivando escucha...") // Implementación futura
        // Aquí se agregará la lógica para desactivar el reconocimiento o escucha
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MiApp", "onStartCommand llamado") // Log para inicio del servicio
        createNotificationChannel()
        startServiceForeground()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("running_channel",
                "Mi Canal", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startServiceForeground() {
        Log.d("MiApp", "Iniciando servicio en primer plano") // Log para servicio en primer plano
        val notification = NotificationCompat.Builder(this, "running_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Servicio activo")
            .setContentText("Monitoreando el volumen")
            .build()
        startForeground(1, notification)
        Log.d("MiApp", "Notificación mostrada") // Log para notificación
    }
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}