package com.example.apptopicos.controllers

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.apptopicos.R
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Locale

class GPTController(private val context: Context) : TextToSpeech.OnInitListener {

    private val apiService: GeminiApiService
    private var textToSpeech: TextToSpeech? = null

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(context.getString(R.string.Url_API_GeminiIA))
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(GeminiApiService::class.java)

        // Inicializar TextToSpeech
        textToSpeech = TextToSpeech(context, this)
    }

    // Configuración inicial de TextToSpeech
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("GPTController", "Idioma no soportado para TextToSpeech")
            }
        } else {
            Log.e("GPTController", "Error al inicializar TextToSpeech")
        }
    }

    // Método para enviar cálculos y reproducir el resultado
    fun enviarCalculo(userMessage: String) {
        // Formato JSON correcto para la API
        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$userMessage, quiero que en tu respuesta solo digas la operacion y su resultado")
                        })
                    })
                })
            })
        }

        // Crear el RequestBody usando la nueva sintaxis
        val mediaType = "application/json".toMediaType()
        val body = requestBody.toString().toRequestBody(mediaType)

        apiService.sendCalculation(
            key = context.getString(R.string.API_GEMINI_IA), // Reemplaza con tu clave de API
            body = body
        ).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val result = response.body()?.string() ?: "No se obtuvo respuesta"
                    Log.d("GPTController", "Respuesta recibida: $result")

                    try {
                        // Parsear el JSON correctamente
                        val jsonResponse = JSONObject(result)
                        val textoRespuesta = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .trim() // Limpiar texto de saltos de línea u espacios innecesarios

                        // Reproducir el texto usando TextToSpeech
                        reproducirTexto(textoRespuesta)

                    } catch (e: Exception) {
                        Log.e("GPTController", "Error al procesar el JSON: ${e.message}")
                    }
                } else {
                    Log.e("GPTController", "Error en la respuesta: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("GPTController", "Fallo en la conexión: ${t.message}")
            }
        })
    }

    // Reproducir texto con TextToSpeech
    fun reproducirTexto(texto: String) {
        textToSpeech?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // Interfaz de la API Gemini
    interface GeminiApiService {
        @Headers("Content-Type: application/json")
        @POST("v1beta/models/gemini-1.5-flash-latest:generateContent")
        fun sendCalculation(
            @Query("key") key: String,
            @Body body: RequestBody
        ): Call<ResponseBody>
    }
}
