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
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
fun FishingPage(onClose: () -> Unit) {
    var temp by remember { mutableIntStateOf(0) }
    var wind by remember { mutableStateOf(0f) }
    var pressure by remember { mutableIntStateOf(0) }
    var humidity by remember { mutableIntStateOf(0) }
    var giurgiuLevel by remember { mutableStateOf("...") }
    var giurgiuTrend by remember { mutableStateOf("...") }
    var oltenitaLevel by remember { mutableStateOf("...") }
    var oltenitaTrend by remember { mutableStateOf("...") }
    var calarasiLevel by remember { mutableStateOf("...") }
    var calarasiTrend by remember { mutableStateOf("...") }
    var turnuLevel by remember { mutableStateOf("...") }
    var turnuTrend by remember { mutableStateOf("...") }

    val lat = 44.0; val lon = 26.2

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            WeatherCache.fetch(lat, lon)
            if (WeatherCache.loaded) {
                temp = WeatherCache.temperature; wind = WeatherCache.windSpeed
                pressure = WeatherCache.pressure; humidity = WeatherCache.humidity
            }
            try {
                val html = java.net.URL("https://www.cotele-dunarii.ro/").readText()
                val jsData = Regex("var localitati = (\\[[\\s\\S]*?\\]);").find(html)
                if (jsData != null) {
                    val jsonArr = org.json.JSONArray(jsData.groupValues[1])
                    for (i in 0 until jsonArr.length()) {
                        val obj = jsonArr.getJSONObject(i)
                        when (obj.getString("nume")) {
                            "Giurgiu" -> giurgiuLevel = "${obj.getString("cota")} cm"
                            "Oltenița" -> oltenitaLevel = "${obj.getString("cota")} cm"
                            "Călărași" -> calarasiLevel = "${obj.getString("cota")} cm"
                            "Turnu Măgurele" -> turnuLevel = "${obj.getString("cota")} cm"
                        }
                    }
                }
                val gRow = Regex("Giurgiu[\\s\\S]*?data-label='Variație'[^>]*>([^<]+)<").find(html)
                if (gRow != null) giurgiuTrend = gRow.groupValues[1].trim()
                val oRow = Regex("Oltenița[\\s\\S]*?data-label='Variație'[^>]*>([^<]+)<").find(html)
                if (oRow != null) oltenitaTrend = oRow.groupValues[1].trim()
                val cRow = Regex("Călărași[\\s\\S]*?data-label='Variație'[^>]*>([^<]+)<").find(html)
                if (cRow != null) calarasiTrend = cRow.groupValues[1].trim()
                val tRow = Regex("Turnu Măgurele[\\s\\S]*?data-label='Variație'[^>]*>([^<]+)<").find(html)
                if (tRow != null) turnuTrend = tRow.groupValues[1].trim()
            } catch (_: Exception) {}
        }
    }

    val moon = moonPhase(LocalDate.now())
    val rating = fishRating(temp, pressure, wind, moon, LocalTime.now())
    val sr = LocalTime.of(5, 35); val ss = LocalTime.of(21, 5)

    Column(modifier = Modifier.fillMaxSize().background(DarkSurface.copy(alpha = 0.95f))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Închide", tint = AccentRed, modifier = Modifier.size(28.dp)) }
        }

        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // LEFT PANEL
            Box(Modifier.weight(0.5f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(DarkSurface).padding(10.dp)) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
                    // RATING - full width centered
                    Text("RATING PESCUIT", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 4.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(ratingBar(rating), fontSize = 56.sp)
                    Text(ratingText(rating), fontSize = 28.sp, fontWeight = FontWeight.Black, color = ratingColor(rating))

                    Spacer(Modifier.height(12.dp))

                    // Meteo + Astronomy on same row
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        // Weather
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("METEO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 2.sp)
                            Spacer(Modifier.height(4.dp))
                            StatItem("🌡", "${temp}°C", "", AccentYellow)
                            StatItem("💨", "${wind.toInt()} km/h", "", AccentBlue)
                            StatItem("🎈", "$pressure hPa", "", AccentGreen)
                            StatItem("💧", "$humidity%", "", AccentBlue)
                        }
                        // Astronomy
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ASTRONOMIE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 2.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(moonIcon(moon), fontSize = 34.sp)
                            Text(moonName(moon), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                            Spacer(Modifier.height(4.dp))
                            Text("🌅 ${sr.format(DateTimeFormatter.ofPattern("HH:mm"))}", fontSize = 14.sp, color = TextPrimary)
                            Text("🌇 ${ss.format(DateTimeFormatter.ofPattern("HH:mm"))}", fontSize = 14.sp, color = TextPrimary)
                            Spacer(Modifier.height(2.dp))
                            Text("🎣 05-08 | 17-20", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentYellow)
                        }
                    }
                }
            }

            // RIGHT PANEL - Danube levels
            Box(Modifier.weight(0.5f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(DarkSurface).padding(10.dp)) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("COTELE DUNĂRII", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 3.sp)
                    Spacer(Modifier.height(8.dp))

                    LevelCard("OLTENIȚA", "km 430", oltenitaLevel, oltenitaTrend)
                    Spacer(Modifier.height(6.dp))
                    LevelCard("GIURGIU", "km 493", giurgiuLevel, giurgiuTrend)
                    Spacer(Modifier.height(6.dp))
                    LevelCard("CĂLĂRAȘI", "km 370", calarasiLevel, calarasiTrend)
                    Spacer(Modifier.height(6.dp))
                    LevelCard("TR. MĂGURELE", "km 597", turnuLevel, turnuTrend)
                }
            }
        }
    }
}

@Composable
fun StatItem(icon: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 20.sp)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = color)
        Text(unit, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
fun LevelCard(name: String, km: String, level: String, trend: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = DarkSurfaceVariant) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 18.sp, fontWeight = FontWeight.Black, color = AccentBlue)
                Text(km, fontSize = 14.sp, color = TextSecondary)
            }
            Text(
                if (trend != "...") "$level | $trend" else level,
                fontSize = 28.sp, fontWeight = FontWeight.Black, color = AccentYellow
            )
        }
    }
}

private fun moonPhase(date: LocalDate): Double {
    val knownNewMoon = LocalDate.of(2026, 5, 31)
    val days = date.toEpochDay() - knownNewMoon.toEpochDay()
    return ((days % 29.53059) / 29.53059).let { if (it < 0) it + 1 else it }
}

private fun moonIcon(phase: Double) = when {
    phase < 0.03 || phase > 0.97 -> "🌑"; phase < 0.22 -> "🌒"; phase < 0.28 -> "🌓"
    phase < 0.47 -> "🌔"; phase < 0.53 -> "🌕"; phase < 0.72 -> "🌖"; phase < 0.78 -> "🌗"
    else -> "🌘"
}

private fun moonName(phase: Double) = when {
    phase < 0.03 || phase > 0.97 -> "Lună Nouă"; phase < 0.25 -> "Primul Pătrar"
    phase < 0.50 -> "Lună Plină"; phase < 0.53 -> "Lună Plină"
    phase < 0.75 -> "Ultimul Pătrar"; else -> "Lună Nouă"
}

private fun fishRating(temp: Int, pressure: Int, wind: Float, moon: Double, now: LocalTime): Int {
    var score = 5
    if (pressure < 1005 || pressure > 1030) score--
    if (pressure < 995 || pressure > 1040) score -= 2
    if (wind > 15) score--
    if (wind > 30) score -= 2
    if (moon in 0.03..0.25 || moon in 0.28..0.47 || moon in 0.53..0.72 || moon in 0.78..0.97) score--
    val hour = now.hour
    if (hour in 6..9 || hour in 17..20) score = minOf(score + 1, 5)
    if (hour in 12..15) score--
    return score.coerceIn(1, 5)
}

private fun ratingBar(r: Int) = when (r) { 5 -> "★★★★★"; 4 -> "★★★★☆"; 3 -> "★★★☆☆"; 2 -> "★★☆☆☆"; else -> "★☆☆☆☆" }
private fun ratingText(r: Int) = when (r) { 5 -> "Excelent"; 4 -> "Foarte Bun"; 3 -> "Bun"; 2 -> "Slab"; else -> "Foarte Slab" }
private fun ratingColor(r: Int) = when (r) { 5 -> AccentGreen; 4 -> Color(0xFF8BC34A); 3 -> AccentYellow; else -> AccentRed }
