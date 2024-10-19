package com.example.apptopicos.controllers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Locale

class SOController(private val context: Context) {

    private var isCameraInUse: Boolean = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening: Boolean = false
    private var capturedText: StringBuilder = StringBuilder()
    private var registerController: RegisterController = RegisterController(context)
    private var comunicadorController: ComunicadorController = ComunicadorController(context)

    // Verifica si la cámara está activa, pero primero comprueba si tiene permiso
    fun isCameraActive(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e("SOController", "Permiso de cámara no concedido")
            return false
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0] // Usar el primer ID de cámara disponible
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d("SOController", "Cámara abierta correctamente.")
                    camera.close() // Cerramos la cámara después de verificar que se puede abrir
                    isCameraInUse = false
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.d("SOController", "Cámara desconectada.")
                    isCameraInUse = false
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e("SOController", "Error al abrir la cámara: $error")
                    if (error == CameraDevice.StateCallback.ERROR_CAMERA_IN_USE) {
                        Log.e("SOController", "La cámara está en uso por otra aplicación o servicio.")
                        isCameraInUse = true
                    }
                }
            }, null)
        } catch (e: CameraAccessException) {
            Log.e("SOController", "Error al acceder a la cámara: ${e.message}")
        } catch (e: SecurityException) {
            Log.e("SOController", "Excepción de seguridad: ${e.message}")
        }
        return isCameraInUse
    }

    // Método para desactivar la cámara si está activa
    fun disableCameraIfActive() {
        if (isCameraActive()) {
            Log.d("SOController", "Desactivando la cámara...")
            // Aquí se puede realizar la lógica para desactivar la cámara.
        } else {
            Log.d("SOController", "La cámara no está activa, no se requiere desactivación.")
        }
    }
    fun activarMicrofono() {

        registerController.starRegister()
        registerController.logEvent("Microfono activado")

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("SOController", "Micrófono listo para hablar")
                }

                override fun onBeginningOfSpeech() {
                    Log.d("SOController", "Comenzando a hablar")
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d("SOController", "Fin de la conversación")
                }

                override fun onError(error: Int) {
                    Log.e("SOController", "Error durante el reconocimiento de voz: $error")
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.let {
                        capturedText.append(it[0]).append(" ") // Acumular el texto capturado
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
            isListening = true
            Log.d("SOController", "Micrófono activado")
        } else {
            Log.e("SOController", "El reconocimiento de voz no está disponible")
        }

    }

    // Desactivar el micrófono y detener la conversión de voz a texto
    fun desactivarMicrofono() {

        registerController.starRegister()
        registerController.logEvent("Microfono desactivado")

        if (isListening) {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            isListening = false
            Log.d("SOController", "Micrófono desactivado")

            // Texto capturado que será enviado al ComunicadorController
            val mensajeCapturado = capturedText.toString().trim()
            Log.d("SOController", "Texto capturado: $mensajeCapturado")
            // Llamar a la función escuchar del ComunicadorController y enviarle el mensaje capturado
            comunicadorController.escuchar(mensajeCapturado)

            capturedText.clear() // Limpiar el buffer de texto para la próxima vez

        }
    }
}
