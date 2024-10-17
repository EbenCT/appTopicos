package com.example.apptopicos.controllers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.core.content.ContextCompat

class SOController(private val context: Context) {

    private var isCameraInUse: Boolean = false

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
}
