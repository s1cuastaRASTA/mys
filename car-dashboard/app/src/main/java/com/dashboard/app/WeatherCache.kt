package com.dashboard.app

import android.util.Log
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

object WeatherCache {
    private var lastFetch = 0L
    var temperature: Int = 0
    var windSpeed: Float = 0f
    var pressure: Int = 0
    var humidity: Int = 0
    var weatherCode: Int = 0
    var loaded = false
    var error: String? = null

    fun fetch(lat: Double, lon: Double) {
        val now = System.currentTimeMillis()
        if (loaded && now - lastFetch < 3_600_000) return
        try {
            val url = URL("https://wttr.in/${lat},${lon}?format=j1")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "CarDashboard/1.0")
            conn.connectTimeout = 10000; conn.readTimeout = 10000
            val code = conn.responseCode
            if (code != 200) { error = "HTTP $code"; Log.e("WeatherCache", "HTTP $code"); return }
            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val cur = org.json.JSONObject(json).getJSONArray("current_condition").getJSONObject(0)
            temperature = cur.getInt("temp_C")
            windSpeed = cur.getInt("windspeedKmph").toFloat()
            pressure = cur.getInt("pressure")
            humidity = cur.getInt("humidity")
            weatherCode = cur.getInt("weatherCode")
            loaded = true; error = null; lastFetch = now
            Log.d("WeatherCache", "Loaded: ${temperature}°C from wttr.in")
        } catch (e: Exception) {
            error = e.message?.take(40)
            Log.e("WeatherCache", "Error", e)
        }
    }
}
