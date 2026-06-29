package com.dashboard.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dashboard.app.WeatherInfo
import com.dashboard.app.WeatherCache
import com.dashboard.app.ui.theme.*
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WeatherWidget(
    latitude: Double,
    longitude: Double,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(latitude, longitude) {
        if (latitude == 0.0 && longitude == 0.0) return@LaunchedEffect
        withContext(Dispatchers.IO) { WeatherCache.fetch(latitude, longitude) }
        if (WeatherCache.loaded) {
            weather = WeatherInfo(
                temperature = WeatherCache.temperature.toFloat(),
                weatherCode = WeatherCache.weatherCode,
                windSpeed = WeatherCache.windSpeed
            )
            error = false
        } else {
            error = true
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(if (compact) 4.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (latitude == 0.0 && longitude == 0.0) {
            Text(
                if (compact) "GPS..." else "Așteptăm semnal GPS...",
                color = TextSecondary,
                fontSize = if (compact) 12.sp else 16.sp
            )
        } else if (error) {
            Text(
                if (compact) "Error" else "Nu s-a putut încărca",
                color = AccentRed,
                fontSize = if (compact) 12.sp else 16.sp
            )
        } else if (weather != null) {
            val w = weather!!

            if (compact) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(w.iconChar, fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${w.temperature.toInt()}°C",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentYellow
                    )
                }
            } else {
                Text(
                    text = w.iconChar,
                    fontSize = 72.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = w.condition,
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "${w.temperature.toInt()}°C",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        w.temperature > 30 -> AccentRed
                        w.temperature < 5 -> AccentBlue
                        else -> AccentYellow
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Real Feel", color = TextSecondary, fontSize = 12.sp)
                        Text("${w.feelsLike.toInt()}°C", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Umiditate", color = TextSecondary, fontSize = 12.sp)
                        Text("${w.humidity}%", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Vânt", color = TextSecondary, fontSize = 12.sp)
                        Text("${w.windSpeed.toInt()} km/h", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        } else {
            Text(
                "Se încarcă...",
                color = TextSecondary,
                fontSize = 16.sp
            )
        }
    }
}
