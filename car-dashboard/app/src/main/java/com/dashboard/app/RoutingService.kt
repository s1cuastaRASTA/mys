package com.dashboard.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object RoutingService {

    data class GeocodingResult(val lat: Double, val lon: Double, val displayName: String)

    suspend fun geocode(query: String): GeocodingResult? = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "CarDashboard/1.0")
            conn.connectTimeout = 10000
            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val arr = JSONArray(json)
            if (arr.length() > 0) {
                val obj = arr.getJSONObject(0)
                val lat = obj.getDouble("lat")
                val lon = obj.getDouble("lon")
                val name = obj.optString("display_name", query)
                GeocodingResult(lat, lon, name)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRoute(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): RouteInfo? = withContext(Dispatchers.IO) {
        val coordStr = "$fromLon,$fromLat;$toLon,$toLat"
        val servers = listOf(
            "https://routing.openstreetmap.de/routed-car/route/v1/driving/$coordStr",
            "https://router.project-osrm.org/route/v1/driving/$coordStr",
        )
        var lastError = "Niciun server disponibil"
        for (baseUrl in servers) {
            try {
                val url = URL("$baseUrl?steps=true&overview=full&geometries=polyline")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "CarDashboard/1.0")
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                val responseCode = conn.responseCode
                if (responseCode != 200) {
                    val errorBody = try { conn.errorStream?.bufferedReader()?.readText() } catch (_: Exception) { "" }
                    lastError = "HTTP $responseCode: $errorBody"
                    Log.e("RoutingService", "Server $baseUrl returned $responseCode: $errorBody")
                    conn.disconnect()
                    continue
                }
                val json = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                Log.d("RoutingService", "Route from $baseUrl: OK")

                val root = JSONObject(json)
                if (root.getString("code") != "Ok") {
                    lastError = "Code: ${root.getString("code")}"
                    continue
                }

                val route = root.getJSONArray("routes").getJSONObject(0)
                val totalDist = route.getDouble("distance")
                val totalDur = route.getDouble("duration")
                val polyline = route.getString("geometry")

                val legs = route.getJSONArray("legs").getJSONObject(0)
                val stepsArr = legs.getJSONArray("steps")
                val steps = mutableListOf<RouteStep>()
                for (i in 0 until stepsArr.length()) {
                    val s = stepsArr.getJSONObject(i)
                    val maneuver = s.getJSONObject("maneuver")
                    val loc = maneuver.getJSONArray("location")
                    steps.add(
                        RouteStep(
                            instruction = s.optString("instruction", s.optString("name", "")),
                            distance = formatDistance(s.getDouble("distance")),
                            duration = formatDuration(s.getDouble("duration")),
                            lat = loc.getDouble(1),
                            lon = loc.getDouble(0)
                        )
                    )
                }

                return@withContext RouteInfo(
                    totalDistance = formatDistance(totalDist),
                    totalDuration = formatDuration(totalDur),
                    steps = steps,
                    polyline = polyline,
                    isNavigating = true,
                    status = "Ok",
                    destinationLat = toLat,
                    destinationLon = toLon
                )
            } catch (e: Exception) {
                lastError = e.message?.take(80) ?: "conexiune"
                Log.e("RoutingService", "Route error on $baseUrl", e)
            }
        }
        Log.e("RoutingService", "All servers failed: $lastError")
        RouteInfo(status = "Eroare: $lastError", isNavigating = false)
    }

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) "%.1f km".format(meters / 1000)
        else "%.0f m".format(meters)
    }

    private fun formatDuration(seconds: Double): String {
        val totalMin = (seconds / 60).toInt()
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "${h}h ${m}min" else "${m}min"
    }

    fun decodePolyline(encoded: String): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        var index = 0
        var lat = 0
        var lon = 0
        val len = encoded.length

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlon = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lon += dlon

            points.add(Pair(lat / 1e5, lon / 1e5))
        }
        return points
    }
}
