package com.example.apptopicos.views

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.apptopicos.R
import com.example.apptopicos.controllers.ResultadosController
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraPreviewActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var resultadoController: ResultadosController
    private lateinit var cameraPreviewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private val REQUEST_CODE_PERMISSIONS = 10
    private var savedUri: Uri? = null
    private lateinit var textToSpeech: TextToSpeech
    private var retryCounter = 1  // Contador de intentos

    private val closeCameraReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()  // Cierra el CameraPreviewActivity
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_preview_layout)
        cameraPreviewView = findViewById(R.id.camera_preview)
        textToSpeech = TextToSpeech(this, this)
        resultadoController = ResultadosController(this)
        // Registrar el receptor de broadcast para cerrar el CameraPreviewActivity
        registerReceiver(closeCameraReceiver, IntentFilter("com.example.apptopicos.CLOSE_CAMERA_ACTIVITY"))

        Log.d("CameraPreviewActivity", "Iniciando verificación de permisos de cámara")
        checkCameraPermission()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.getDefault()
        } else {
            Log.e("CameraPreviewActivity", "Error al inicializar TextToSpeech")
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            Log.d("CameraPreviewActivity", "Permiso de cámara no otorgado, solicitando permisos")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CODE_PERMISSIONS
            )
        } else {
            Log.d("CameraPreviewActivity", "Permiso de cámara otorgado, iniciando cámara")
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("CameraPreviewActivity", "Permiso de cámara concedido por el usuario")
                startCamera()
            } else {
                Log.e("CameraPreviewActivity", "Permiso de cámara denegado por el usuario")
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = cameraPreviewView.surfaceProvider
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

                Log.d("CameraPreviewActivity", "Cámara iniciada, configurando captura de imagen tras 5 segundos")
                iniciarCicloCaptura()

            } catch (exc: Exception) {
                Log.e("CameraPreviewActivity", "Error al inicializar la cámara", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun iniciarCicloCaptura() {
        Handler(Looper.getMainLooper()).postDelayed({
            takePhoto()
        }, 8000) // Espera 8 segundos antes de capturar la imagen
    }

    private fun takePhoto() {
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Images")
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

        Log.d("CameraPreviewActivity", "Iniciando captura de imagen")
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    savedUri = outputFileResults.savedUri
                    Log.d("CameraPreviewActivity", "Imagen guardada en galería: $savedUri")
                    textToSpeech.speak("Imagen capturada. Analizando", TextToSpeech.QUEUE_FLUSH, null, null)
                    Toast.makeText(baseContext, "Imagen guardada en galería", Toast.LENGTH_SHORT).show()

                    cameraExecutor.execute {
                        savedUri?.let { uri ->
                            val imageFile = uriToFile(uri)
                            if (imageFile != null) {
                                Log.d("CameraPreviewActivity", "Archivo de imagen creado, iniciando carga")
                                uploadImage(imageFile)
                            } else {
                                Log.e("CameraPreviewActivity", "No se pudo obtener el archivo de imagen desde URI")
                            }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraPreviewActivity", "Error al capturar imagen", exception)
                }
            }
        )
    }

    private fun uploadImage(imageFile: File) {
        Log.d("CameraPreviewActivity", "Intentando cargar la imagen: $imageFile")
        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image", imageFile.name,
                imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()
        val url = getString(R.string.IP_CONNECTION)
        val request = Request.Builder()
            .url(url) // Cambia la IP a la de tu servidor
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        val jsonResponse = JSONObject(responseBody)
                        val predictedClass = jsonResponse.getString("predicted_class")
                        val confidence = jsonResponse.getDouble("confidence")

                        procesarResultado(predictedClass, confidence)
                    }
                } else {
                    Log.e("CameraPreviewActivity", "Error al subir la imagen: ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("CameraPreviewActivity", "Error al realizar la solicitud de carga de imagen", e)
            runOnUiThread {
                textToSpeech.speak("No se pudo establecer conexión", TextToSpeech.QUEUE_FLUSH, null, null)
                Toast.makeText(this, "No se pudo establecer conexión", Toast.LENGTH_LONG).show()

            }
            // Esperar un breve momento para que se reproduzca el mensaje antes de cerrar
            Handler(Looper.getMainLooper()).postDelayed({
                finish()  // Cierra la actividad tras el mensaje
            }, 3000)
        }
    }
    private fun procesarResultado(predictedClass: String, confidence: Double) {
        if (confidence >= 70) {
            val confidencePercentage = confidence.toInt()
            val message = "Se detectó que el billete es de $predictedClass pesos con una confianza de $confidencePercentage%"
            resultadoController.registrarResultado(predictedClass, confidencePercentage)
            runOnUiThread {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
            }
            Handler(Looper.getMainLooper()).postDelayed({
                finish()  // Cierra la actividad
            }, 6000)
        } else {
            retryCounter++
            if (retryCounter > 3) {
                val message = "Se rebaso el límite de intentos, vuelve a intentarlo más tarde"
                runOnUiThread {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
                }
                val intent = Intent("com.example.apptopicos.DESACTIVAR_ESCUCHA")
                sendBroadcast(intent)
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()  // Cierra la actividad tras el mensaje
                }, 8000)
            } else {
                val message = "No se reconoció billete. Vuelva a intentarlo."
                runOnUiThread {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
                }
                iniciarCicloCaptura()  // Reinicia el ciclo de captura
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(uri, filePathColumn, null, null, null)
            cursor?.moveToFirst()
            val columnIndex = cursor?.getColumnIndex(filePathColumn[0])
            val filePath = columnIndex?.let { cursor.getString(it) }
            cursor?.close()
            filePath?.let { File(it) }
        } catch (e: Exception) {
            Log.e("CameraPreviewActivity", "Error al convertir URI a archivo", e)
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(closeCameraReceiver)
        cameraExecutor.shutdown()
        textToSpeech.shutdown()
    }
}