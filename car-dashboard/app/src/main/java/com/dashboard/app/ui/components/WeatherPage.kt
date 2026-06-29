package com.dashboard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dashboard.app.WeatherCache
import com.dashboard.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DayWeather(
    val date: String,
    val tempMax: Int,
    val tempMin: Int,
    val code: Int,
    val icon: String
)

@Composable
fun WeatherPage(onClose: () -> Unit) {
    var currentTemp by remember { mutableStateOf<Float?>(null) }
    var currentCode by remember { mutableIntStateOf(0) }
    var currentWind by remember { mutableStateOf(0f) }
    var forecast by remember { mutableStateOf<List<DayWeather>>(emptyList()) }

    val lat = 44.0
    val lon = 26.2

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            WeatherCache.fetch(lat, lon)
            if (WeatherCache.loaded) {
                currentTemp = WeatherCache.temperature.toFloat()
                currentCode = WeatherCache.weatherCode
                currentWind = WeatherCache.windSpeed
            }
            // Fetch forecast from Open-Meteo (7 days)
            try {
                val jsonStr = java.net.URL("https://api.open-meteo.com/v1/forecast?" +
                    "latitude=$lat&longitude=$lon&daily=temperature_2m_max,temperature_2m_min,weather_code" +
                    "&timezone=Europe/Bucharest&forecast_days=8").readText()
                val root = JSONObject(jsonStr)
                val daily = root.getJSONObject("daily")
                val dates = daily.getJSONArray("time")
                val maxT = daily.getJSONArray("temperature_2m_max")
                val minT = daily.getJSONArray("temperature_2m_min")
                val codes = daily.getJSONArray("weather_code")
                val days = mutableListOf<DayWeather>()
                for (i in 0 until dates.length()) {
                    days.add(DayWeather(
                        date = dates.getString(i),
                        tempMax = maxT.getDouble(i).toInt(),
                        tempMin = minT.getDouble(i).toInt(),
                        code = codes.getInt(i),
                        icon = weatherIcon(codes.getInt(i))
                    ))
                }
                forecast = days
            } catch (_: Exception) {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(DarkSurface.copy(alpha = 0.95f))) {
        // Close bar
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Închide", tint = AccentRed, modifier = Modifier.size(28.dp))
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Dunărea - Zona Oltenița/Giurgiu", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
            Spacer(Modifier.height(16.dp))

            // Current weather card
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = DarkSurfaceVariant) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ACUM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(currentTemp?.let { weatherIcon(currentCode) + "  ${it.toInt()}°C" } ?: "Se încarcă...",
                        fontSize = 48.sp, fontWeight = FontWeight.Black, color = AccentYellow)
                    Spacer(Modifier.height(4.dp))
                    Text(weatherDesc(currentCode), fontSize = 16.sp, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text("Vânt: ${currentWind.toInt()} km/h", fontSize = 13.sp, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            // 7-day forecast
            Text("Următoarele 7 zile", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))

            forecast.drop(1).forEach { day ->
                val dayName = try {
                    LocalDate.parse(day.date).format(DateTimeFormatter.ofPattern("EEE dd/MM", java.util.Locale("ro")))
                } catch (_: Exception) { day.date }

                Surface(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dayName.uppercase(), Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(day.icon, fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Text("${day.tempMin}°", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.width(35.dp), textAlign = TextAlign.End)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.width(60.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(DarkSurfaceVariant)) {
                            val frac = ((day.tempMax + 10f) / 50f).coerceIn(0f, 1f)
                            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(AccentYellow))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("${day.tempMax}°", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.width(35.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun weatherIcon(code: Int): String = when (code) {
    0 -> "\u2600\uFE0F"
    1, 2 -> "\u26C5"
    3 -> "\u2601\uFE0F"
    45, 48 -> "\uD83C\uDF2B\uFE0F"
    51, 53, 55 -> "\uD83C\uDF27\uFE0F"
    61, 63, 65 -> "\uD83C\uDF27\uFE0F"
    71, 73, 75 -> "\u2744\uFE0F"
    80, 81, 82 -> "\uD83C\uDF26\uFE0F"
    95, 96, 99 -> "\u26A1"
    else -> "\u2600\uFE0F"
}

private fun weatherDesc(code: Int): String = when (code) {
    0 -> "Senin"
    1, 2 -> "Parțial noros"
    3 -> "Înnorat"
    45, 48 -> "Ceață"
    51, 53, 55 -> "Ploaie ușoară"
    61, 63, 65 -> "Ploaie"
    71, 73, 75 -> "Ninsoare"
    80, 81, 82 -> "Averse"
    95, 96, 99 -> "Furtună"
    else -> "Variabil"
}
