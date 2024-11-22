package com.example.apptopicos.controllers

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.io.File

class ResultadosController(private val context: Context) : TextToSpeech.OnInitListener {

    private val resultadosLog = mutableListOf<Resultado>()
    private val fileName = "resultados_log.txt"
    private var textToSpeech: TextToSpeech
    private var isTTSInitialized = false
    private var isSessionActive = false

    data class Resultado(val clase: String, val confianza: Int)

    init {
        textToSpeech = TextToSpeech(context, this)
        cargarResultadosDesdeArchivo()
    }

    // Método para inicializar el TextToSpeech
    override fun onInit(status: Int) {
        isTTSInitialized = status == TextToSpeech.SUCCESS
    }

    // Método para registrar un resultado
    fun registrarResultado(predictedClass: String, confidence: Int) {

        // Si la sesión está activa y no se ha registrado aún ningún resultado, resetear archivo
        if (!isSessionActive) {
            resetResultados()
            isSessionActive = true  // Marcar que ya se registró el primer resultado
        }

        val resultado = Resultado(predictedClass, confidence)
        resultadosLog.add(resultado)
        Log.d("ResultadosController", "Resultado registrado: $predictedClass con confianza de $confidence")
        guardarResultadosEnArchivo() // Guardar el resultado en archivo para mantenerlo persistente
    }

    // Método para obtener y comunicar los resultados registrados por voz
    fun getRegistro() {
        if (isTTSInitialized) {
            if (resultadosLog.isEmpty()) {
                textToSpeech.speak("No se han registrado detecciones en esta sesión.", TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                resultadosLog.forEach { resultado ->
                    val mensaje = "Se detectó un billete de ${resultado.clase}, con una confianza de ${resultado.confianza}"
                    textToSpeech.speak(mensaje, TextToSpeech.QUEUE_ADD, null, null)
                }
            }
        } else {
            Log.e("ResultadosController", "TextToSpeech no está inicializado")
        }
    }

    // Método para guardar la lista de resultados en un archivo
    private fun guardarResultadosEnArchivo() {
        try {
            val file = File(context.filesDir, fileName)
            file.printWriter().use { writer ->
                resultadosLog.forEach { resultado ->
                    writer.println("${resultado.clase},${resultado.confianza}")
                }
            }
            Log.d("ResultadosController", "Resultados guardados en $fileName")
        } catch (e: Exception) {
            Log.e("ResultadosController", "Error al guardar los resultados: ${e.message}")
        }
    }

    // Método para cargar los resultados desde el archivo al iniciar la clase
    private fun cargarResultadosDesdeArchivo() {
        try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                file.forEachLine { line ->
                    val data = line.split(",")
                    if (data.size == 2) {
                        val clase = data[0]
                        val confianza = data[1].toDoubleOrNull() ?: 0.0
                        resultadosLog.add(Resultado(clase, confianza.toInt()))
                    }
                }
                Log.d("ResultadosController", "Resultados cargados desde el archivo")
            }
        } catch (e: Exception) {
            Log.e("ResultadosController", "Error al cargar los resultados: ${e.message}")
        }
    }

    // Método para resetear los resultados cuando se inicia una nueva sesión de detección
    fun resetResultados() {
        resultadosLog.clear()
        guardarResultadosEnArchivo() // Limpiar el archivo para la nueva sesión
        Log.d("ResultadosController", "Resultados reseteados para una nueva sesión")
    }

    // Limpiar recursos de TextToSpeech
    fun release() {
        if (isTTSInitialized) {
            textToSpeech.shutdown()
        }
    }

    fun iniciarNuevaSesion() {
        isSessionActive = false  // Resetear el indicador de sesión
    }
}
