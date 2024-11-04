package com.example.apptopicos.views

import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.apptopicos.R
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraPreviewActivity : AppCompatActivity() {

    private lateinit var cameraPreviewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private val REQUEST_CODE_PERMISSIONS = 10
    private var savedUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_preview_layout)
        cameraPreviewView = findViewById(R.id.camera_preview)

        Log.d("CameraPreviewActivity", "Iniciando verificación de permisos de cámara")
        checkCameraPermission()
        cameraExecutor = Executors.newSingleThreadExecutor()
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
                Handler(Looper.getMainLooper()).postDelayed({
                    takePhoto()
                }, 5000)

            } catch (exc: Exception) {
                Log.e("CameraPreviewActivity", "Error al inicializar la cámara", exc)
            }
        }, ContextCompat.getMainExecutor(this))
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

                    Toast.makeText(baseContext, "Imagen guardada en galería", Toast.LENGTH_SHORT).show()

                    // Inicia la carga de la imagen en un hilo de fondo
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

        val request = Request.Builder()
            .url("http://192.168.0.5:5000/upload_image") // Cambia la IP a la de tu servidor
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("CameraPreviewActivity", "Imagen subida exitosamente: ${response.message}")
                } else {
                    Log.e("CameraPreviewActivity", "Error al subir la imagen: ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("CameraPreviewActivity", "Error al realizar la solicitud de carga de imagen", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CameraPreviewActivity", "Liberando recursos en onDestroy")
        cameraExecutor.shutdown()
    }
}
