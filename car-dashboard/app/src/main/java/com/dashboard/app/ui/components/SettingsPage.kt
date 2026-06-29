package com.dashboard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dashboard.app.DashboardViewModel
import com.dashboard.app.ui.theme.*
import java.io.File

@Composable
fun SettingsPage(viewModel: DashboardViewModel, modifier: Modifier = Modifier) {
    val useMph by viewModel.useMph.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val screenTimeout by viewModel.screenTimeoutMinutes.collectAsState()
    val speedLimit by viewModel.speedLimit.collectAsState()
    val speedLimitEnabled by viewModel.speedLimitEnabled.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    val brightnessValue by viewModel.brightness.collectAsState()
    val fuelRateValue by viewModel.fuelRate.collectAsState()
    val currentBg by viewModel.backgroundFile.collectAsState()
    var selectedTab by remember { mutableIntStateOf(-1) }

    val tabs = listOf("GENERAL", "VITEZĂ/TRIP", "START")

    Row(modifier = modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // LEFT MENU
        Column(
            modifier = Modifier.weight(0.35f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(DarkSurface.copy(alpha = 0.5f)).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEachIndexed { i, name ->
                Surface(
                    onClick = { selectedTab = i },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTab == i) AccentBlue.copy(alpha = 0.2f) else Color.Transparent
                ) {
                    Text(
                        name,
                        fontSize = 15.sp, fontWeight = FontWeight.Black,
                        color = if (selectedTab == i) AccentBlue else TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth()
                    )
                }
            }
        }

        // RIGHT CONTENT
        Column(
            modifier = Modifier.weight(0.65f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(DarkSurface.copy(alpha = 0.5f)).padding(12.dp).verticalScroll(rememberScrollState())
        ) {
            when (selectedTab) {
                -1 -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Settings, null, tint = AccentBlue.copy(alpha = 0.3f), modifier = Modifier.size(256.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(viewModel.displayName.collectAsState().value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = AccentBlue.copy(alpha = 0.5f))
                        }
                    }
                }
                0 -> { // GENERAL
                    val use24h by viewModel.use24hClock.collectAsState()
                    val autoRadio by viewModel.autoPlayRadio.collectAsState()
                    val overlayA by viewModel.overlayAlpha.collectAsState()
                    SectionTitle("Temă")
                    SettingRow("Temă închisă") { Switch(checked = isDarkTheme, onCheckedChange = { viewModel.toggleTheme() }) }
                    SectionTitle("Luminozitate")
                    Slider(value = brightnessValue, onValueChange = { viewModel.setBrightness(it) }, modifier = Modifier.fillMaxWidth())
                    SectionTitle("Ceas")
                    SettingRow("Format 24h") { Switch(checked = use24h, onCheckedChange = { viewModel.toggle24hClock() }) }
                    SectionTitle("Radio")
                    SettingRow("Pornire automată radio") { Switch(checked = autoRadio, onCheckedChange = { viewModel.toggleAutoPlayRadio() }) }
                    SectionTitle("Fundal")
                    Text("Opacitate overlay: ${(overlayA * 100).toInt()}%", fontSize = 14.sp, color = TextSecondary)
                    Slider(value = overlayA, onValueChange = { viewModel.setOverlayAlpha(it) }, valueRange = 0.15f..0.85f, modifier = Modifier.fillMaxWidth())
                    SectionTitle("Background")
                    BackgroundGrid(currentBg, onSelect = { viewModel.setBackground(it) })
                }
                1 -> { // VITEZĂ/TRIP
                    SectionTitle("Alertă viteză")
                    SettingRow("Activează") { Switch(checked = speedLimitEnabled, onCheckedChange = { viewModel.toggleSpeedLimit() }) }
                    if (speedLimitEnabled) {
                        Text("Limită: ${speedLimit.toInt()} km/h", fontSize = 14.sp, color = AccentYellow)
                        Slider(value = speedLimit, onValueChange = { viewModel.setSpeedLimit(it) }, valueRange = 30f..200f, modifier = Modifier.fillMaxWidth())
                    }
                    SectionTitle("Unități")
                    SettingRow("Unitate") {
                        Button(onClick = { viewModel.toggleUnit() },
                            colors = ButtonDefaults.buttonColors(containerColor = if (useMph) AccentBlue else DarkSurfaceVariant),
                            modifier = Modifier.width(80.dp)) { Text(if (useMph) "mph" else "km/h", fontSize = 13.sp, color = TextPrimary) }
                    }
                    SectionTitle("Consum")
                    Text("${fuelRateValue.toInt()} L/100km", fontSize = 14.sp, color = AccentYellow)
                    Slider(value = fuelRateValue, onValueChange = { viewModel.setFuelRate(it) }, valueRange = 3f..20f, modifier = Modifier.fillMaxWidth())
                    SectionTitle("Ecran aprins")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0 to "Niciodată", 1 to "1 min", 5 to "5 min", 10 to "10 min").forEach { (v, l) ->
                            Button(onClick = { viewModel.setScreenTimeout(v) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (screenTimeout == v) AccentBlue else DarkSurfaceVariant),
                                modifier = Modifier.weight(1f).height(36.dp)) { Text(l, fontSize = 11.sp) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showResetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        modifier = Modifier.fillMaxWidth()) { Text("Resetează Trip", color = Color.White) }
                }
                2 -> { // START PAGE
                    val showClock by viewModel.showClockOnStart.collectAsState()
                    val showWeather by viewModel.showWeatherOnStart.collectAsState()
                    SectionTitle("Nume afișat")
                    val displayName by viewModel.displayName.collectAsState()
                    var editName by remember { mutableStateOf(displayName) }
                    OutlinedTextField(value = editName, onValueChange = { editName = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = Color.Gray),
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = { viewModel.setDisplayName(editName) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        modifier = Modifier.fillMaxWidth().height(36.dp)) { Text("Salvează", fontSize = 13.sp) }
                    SectionTitle("Ce să apară pe pagina Start")
                    SettingRow("Ceas mare") { Switch(checked = showClock, onCheckedChange = { viewModel.toggleClockOnStart() }) }
                    SettingRow("Meteo") { Switch(checked = showWeather, onCheckedChange = { viewModel.toggleWeatherOnStart() }) }
                    SectionTitle("Pagină start implicită")
                    val startPage by viewModel.startPage.collectAsState()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0 to "Apps", 1 to "Media", 2 to "START", 3 to "Vitezom.", 4 to "4x4", 5 to "NAVI", 6 to "Setări", 7 to "OBD", 8 to "Dashcam").forEach { (v, l) ->
                            Button(onClick = { viewModel.setStartPage(v) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (startPage == v) AccentBlue else DarkSurfaceVariant),
                                modifier = Modifier.weight(1f).height(36.dp)) { Text(l, fontSize = 9.sp) }
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Confirmare", color = TextPrimary) },
            text = { Text("Resetezi distanța, viteza maximă și altitudinea?", color = TextSecondary) },
            confirmButton = { TextButton(onClick = { viewModel.resetTrip(); showResetDialog = false }) { Text("Da", color = AccentRed) } },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Nu", color = TextPrimary) } },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentBlue,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

@Composable
fun SettingRow(label: String, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = TextPrimary, fontSize = 14.sp)
        control()
    }
}

@Composable
fun BackgroundGrid(currentBg: String, onSelect: (String) -> Unit) {
    val dir = File("/storage/emulated/0/Pictures/background")
    val files = if (dir.exists() && dir.isDirectory) {
        dir.listFiles()?.filter { it.extension.lowercase() in listOf("png", "jpg", "jpeg") } ?: emptyList()
    } else emptyList()

    if (files.isEmpty()) {
        Text("Nicio imagine", fontSize = 12.sp, color = TextSecondary)
    } else {
        val rows = files.chunked(3)
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { file ->
                    Surface(
                        onClick = { onSelect(file.name) },
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceVariant,
                        modifier = Modifier.weight(1f).aspectRatio(3f / 2f),
                        tonalElevation = if (currentBg == file.name) 4.dp else 0.dp
                    ) {
                        Box(Modifier.fillMaxSize().then(
                            if (currentBg == file.name) Modifier.border(2.dp, AccentGreen, RoundedCornerShape(8.dp)) else Modifier)) {
                            AsyncImage(model = file, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f).aspectRatio(3f / 2f)) }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
