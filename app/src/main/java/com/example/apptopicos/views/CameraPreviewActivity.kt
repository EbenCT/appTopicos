package com.example.apptopicos.views

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.apptopicos.R
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView

class CameraPreviewActivity : AppCompatActivity() {

    private lateinit var cameraPreviewView: PreviewView
    private val REQUEST_CODE_PERMISSIONS = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("CommandController", "Creando el activity")
        super.onCreate(savedInstanceState)
        Log.d("CommandController", "Creando la instancia")
        setContentView(R.layout.camera_preview_layout)
        Log.d("CommandController", "colocando el content")
        cameraPreviewView = findViewById(R.id.camera_preview)
        Log.d("CommandController", "Activando camara desde el activity")

        // Verifica y solicita permisos de cámara
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            Log.d("CommandController", "Permiso de cámara no otorgado, solicitando permisos")
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.CAMERA), REQUEST_CODE_PERMISSIONS)
        } else {
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("CommandController", "Permiso de cámara otorgado")
                startCamera()
            } else {
                Log.e("CommandController", "Permiso de cámara denegado.")
            }
        }
    }

    private fun startCamera() {
        try {
            Log.d("CommandController", "Dentro de startCamera")
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            cameraProviderFuture.addListener({
                Log.d("CommandController", "cameraProviderFuture.addListener")
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = cameraPreviewView.surfaceProvider
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview)
                } catch (exc: Exception) {
                    Log.e("CommandController", "Failed to bind camera use case", exc)
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            Log.e("CommandController", "Error en startCamera", e)
        }
    }
}
