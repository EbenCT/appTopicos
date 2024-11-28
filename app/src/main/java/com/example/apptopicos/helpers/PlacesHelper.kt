package com.example.apptopicos.helpers

import android.content.Context
import android.util.Log
import com.example.apptopicos.R
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class PlacesHelper(private val context: Context) {
    data class PlaceResult(val nombre: String, val latLng: LatLng)

    fun buscarLugares(
        query: String,
        onSuccess: (List<PlaceResult>) -> Unit,
        onError: (String) -> Unit
    ) {
        val placesApi = "https://maps.googleapis.com/maps/api/place/textsearch/json?" +
                "query=${URLEncoder.encode(query, "UTF-8")}" +
                "&key=${context.getString(R.string.google_maps_key)}"

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL(placesApi)
                val connection = url.openConnection() as HttpURLConnection

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                Log.d("PlacesHelper", "Respuesta de Places: $jsonResponse")

                if (jsonResponse.getString("status") == "OK") {
                    val resultados = jsonResponse.getJSONArray("results")
                    val lugares = mutableListOf<PlaceResult>()
                    for (i in 0 until resultados.length()) {
                        val lugar = resultados.getJSONObject(i)
                        val nombre = lugar.getString("name")
                        val location = lugar.getJSONObject("geometry").getJSONObject("location")
                        val lat = location.getDouble("lat")
                        val lng = location.getDouble("lng")
                        lugares.add(PlaceResult(nombre, LatLng(lat, lng)))
                    }
                    withContext(Dispatchers.Main) {
                        onSuccess(lugares)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("No se encontraron resultados.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Error al buscar lugares: ${e.message}")
                }
            }
        }
    }
}
