package com.dashboard.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dashboard.app.DashboardViewModel
import com.dashboard.app.ui.components.*
import java.io.File
import com.dashboard.app.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import kotlin.math.roundToInt

@Composable
fun GlassHeader(currentDate: String, currentTime: String, latitude: Double, longitude: Double, isDarkTheme: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val bg = if (isDarkTheme) DarkSurface.copy(alpha = 0.8f) else LightSurface
        val txt = if (isDarkTheme) TextPrimary else LightTextPrimary
        
        Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) {
            Text(currentDate.uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = txt, textAlign = TextAlign.Center)
        }
        Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) {
            WeatherWidget(latitude = latitude, longitude = longitude, compact = true, modifier = Modifier.fillMaxSize())
        }
        Box(Modifier.weight(1.2f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) {
            Text(currentTime, fontSize = 24.sp, fontWeight = FontWeight.Black, color = txt)
        }
    }
}

@Composable
fun BottomHeaderInfo(
    altitude: Double,
    heading: Float,
    latitude: Double,
    longitude: Double,
    isDarkTheme: Boolean,
    is4x4Page: Boolean = false,
    onCalibrate: () -> Unit = {}
) {
    val cardinal = when {
        heading < 22.5f || heading >= 337.5f -> "N"
        heading < 67.5f -> "NE"
        heading < 112.5f -> "E"
        heading < 157.5f -> "SE"
        heading < 202.5f -> "S"
        heading < 247.5f -> "SV"
        heading < 292.5f -> "V"
        else -> "NV"
    }
    Row(modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        val bg = if (isDarkTheme) DarkSurface.copy(alpha = 0.8f) else LightSurface
        Box(Modifier.weight(1f).fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ALTITUDINE", fontSize = 10.sp, color = TextSecondary)
                Text("${altitude.roundToInt()} m", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AccentYellow)
            }
        }
        
        // Mijloc: Buton Calibrare pe pagina 4x4, altfel Directie
        Box(Modifier.weight(1f).fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
            if (is4x4Page) {
                Surface(
                    onClick = onCalibrate,
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text("CALIBRARE (ZERO)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DIRECȚIE", fontSize = 10.sp, color = TextSecondary)
                    Text("$cardinal ${heading.roundToInt()}°", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                }
            }
        }
        
        Box(Modifier.weight(1f).fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
            if (is4x4Page) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CAP", fontSize = 10.sp, color = TextSecondary)
                    Text("$cardinal ${heading.roundToInt()}°", fontSize = 24.sp, fontWeight = FontWeight.Black, color = AccentYellow)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("COORDONATE GPS", fontSize = 10.sp, color = TextSecondary)
                    Text("%.4f, %.4f".format(latitude, longitude), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) TextPrimary else LightTextPrimary)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val speedKmh by viewModel.speed.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val roll by viewModel.roll.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val latitude by viewModel.gpsLatitude.collectAsState()
    val longitude by viewModel.gpsLongitude.collectAsState()
    val altitude by viewModel.gpsAltitude.collectAsState()
    val heading by viewModel.compassHeading.collectAsState()
    val maxPitch by viewModel.maxPitch.collectAsState()
    val maxRoll by viewModel.maxRoll.collectAsState()
    val useMph by viewModel.useMph.collectAsState()
    val isYtActive by viewModel.isYouTubeActive.collectAsState()
    val showWeather by viewModel.showWeather.collectAsState()
    val showFishing by viewModel.showFishing.collectAsState()
    val showTrip by viewModel.showTrip.collectAsState()
    val showRearCam by viewModel.showRearCam.collectAsState()
    val useObdSpeed by viewModel.useObdSpeed.collectAsState()

    val displaySpeed = remember { derivedStateOf { if (useMph) speedKmh * 0.621371f else speedKmh } }.value
    val unitLabel = remember { derivedStateOf { if (useMph) "mph" else "km/h" } }.value

    var currentTime by remember { mutableStateOf("") }
    val currentDate = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE - dd MMMM", java.util.Locale("ro"))) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalTime.now()
            currentTime = if (viewModel.use24hClock.value) now.format(DateTimeFormatter.ofPattern("HH:mm"))
                else now.format(DateTimeFormatter.ofPattern("hh:mm a"))
            delay(if (viewModel.use24hClock.value) 1000L else 30000L)
        }
    }

    val pagerState = rememberPagerState(pageCount = { 9 }, initialPage = viewModel.startPage.collectAsState().value)
    val scope = rememberCoroutineScope()

    val selBg by viewModel.backgroundFile.collectAsState()
    val bgFile = remember(selBg) {
        val dir = java.io.File("/storage/emulated/0/Pictures/background")
        if (selBg.isNotEmpty()) {
            val sel = java.io.File(dir, selBg)
            if (sel.exists()) sel else null
        } else {
            val png = java.io.File(dir, "bg-rav.png")
            val jpg = java.io.File(dir, "bg-rav.jpg")
            when { png.exists() -> png; jpg.exists() -> jpg; else -> null }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (bgFile != null) {
            AsyncImage(
                model = bgFile,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        // Semi-transparent overlay for readability
        val overlayA by viewModel.overlayAlpha.collectAsState()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isDarkTheme) Color.Black.copy(alpha = overlayA) else Color.White.copy(alpha = overlayA * 0.5f)
        ) {}
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. HEADER FIX (Mai subtil si elegant)
            GlassHeader(currentDate, currentTime, latitude, longitude, isDarkTheme)
            
            Box(modifier = Modifier.weight(1f)) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (page) {
                        0 -> { // APPS GRID
                            HomeLauncher(onNavigate = { p -> 
                                when (p) {
                                    99 -> viewModel.toggleYouTube(true)
                                    98 -> viewModel.toggleWeather()
                                    97 -> viewModel.toggleFishing()
                                    else -> scope.launch { try { pagerState.animateScrollToPage(p.coerceIn(0, 8)) } catch (_: Exception) {} }
                                }
                            })
                        }
                        1 -> { MediaPage(viewModel = viewModel, modifier = Modifier.fillMaxSize()) }
                        2 -> { // START page - customizable
                            val showClock by viewModel.showClockOnStart.collectAsState()
                            val showWeatherStart by viewModel.showWeatherOnStart.collectAsState()
                            val displayName by viewModel.displayName.collectAsState()
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (showClock && showWeatherStart) {
                                    Row(Modifier.fillMaxWidth(0.8f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Surface(Modifier.weight(1f).aspectRatio(1.3f), shape = RoundedCornerShape(20.dp), color = DarkSurface.copy(alpha = 0.5f)) {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(currentTime, fontSize = 44.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                                                    Text(currentDate.replace("-", "\n"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Center)
                                                }
                                            }
                                        }
                                        Surface(Modifier.weight(1f).aspectRatio(1.3f), shape = RoundedCornerShape(20.dp), color = DarkSurface.copy(alpha = 0.5f)) {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                WeatherWidget(latitude = latitude, longitude = longitude, compact = false, modifier = Modifier.fillMaxSize(0.75f))
                                            }
                                        }
                                    }
                                } else if (showClock) {
                                    Surface(Modifier.fillMaxWidth(0.45f).aspectRatio(1.3f), shape = RoundedCornerShape(20.dp), color = DarkSurface.copy(alpha = 0.5f)) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(currentTime, fontSize = 50.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                                                Text(currentDate.replace("-", "\n"), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                } else if (showWeatherStart) {
                                    Surface(Modifier.fillMaxWidth(0.45f).aspectRatio(1.3f), shape = RoundedCornerShape(20.dp), color = DarkSurface.copy(alpha = 0.5f)) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            WeatherWidget(latitude = latitude, longitude = longitude, compact = false, modifier = Modifier.fillMaxSize(0.65f))
                                        }
                                    }
                                } else {
                                    Text(displayName, fontSize = 48.sp, fontWeight = FontWeight.Black, color = AccentBlue.copy(alpha = 0.4f))
                                }
                            }
                        }
                        3 -> { // HOME - vitezometru
                            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    modifier = Modifier.weight(1f).fillMaxWidth(0.85f),
                                    shape = RoundedCornerShape(20.dp),
                                    color = DarkSurface.copy(alpha = 0.7f)
                                ) {
                                    SpeedometerGauge(
                                        speedKmh = speedKmh,
                                        displaySpeed = displaySpeed,
                                        maxDisplaySpeed = 220f,
                                        unit = unitLabel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(0.85f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // TRIP button
                                    Surface(
                                        onClick = { viewModel.toggleTrip() },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = DarkSurfaceVariant.copy(alpha = 0.6f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("TRIP", fontSize = 16.sp, fontWeight = FontWeight.Black, color = AccentBlue, letterSpacing = 4.sp)
                                        }
                                    }
                                    // GPS/OBD toggle
                                    Surface(
                                        onClick = { viewModel.toggleSpeedSource() },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = DarkSurfaceVariant.copy(alpha = 0.6f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.BluetoothSearching, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(if (useObdSpeed) "OBD" else "GPS", fontSize = 15.sp, fontWeight = FontWeight.Black, color = AccentGreen)
                                            }
                                        }
                                    }
                                    // 70mai App button
                                    Surface(
                                        onClick = { viewModel.open70maiApp() },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = DarkSurfaceVariant.copy(alpha = 0.6f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Videocam, null, tint = AccentRed, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("SPATE", fontSize = 15.sp, fontWeight = FontWeight.Black, color = AccentRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        4 -> { // 4x4 EXTREME
                            InclinometerGauge(
                                pitch = pitch,
                                roll = roll,
                                maxPitch = maxPitch,
                                maxRoll = maxRoll,
                                altitude = altitude,
                                heading = heading,
                                isDarkTheme = isDarkTheme,
                                onCalibrate = { viewModel.calibrateInclination() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        5 -> { // NAVIGATION
                            val wazeNav by viewModel.wazeNavInfo.collectAsState()
                            WazeSection(
                                latitude = latitude, longitude = longitude,
                                wazeNavInfo = wazeNav,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        6 -> { SettingsPage(viewModel = viewModel, modifier = Modifier.fillMaxSize()) }
                        7 -> { ObdPage(modifier = Modifier.fillMaxSize()) }
                        8 -> { DashcamPage(modifier = Modifier.fillMaxSize()) }
                    }
                }

                // YouTube Popup
                if (isYtActive) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                        YouTubeSection(
                            onClose = { viewModel.toggleYouTube(false) },
                            onMinimize = { viewModel.toggleYouTube(false) }
                        )
                    }
                }

                // Weather Popup
                if (showWeather) {
                    WeatherPage(onClose = { viewModel.toggleWeather() })
                }

                // Fishing Popup
                if (showFishing) {
                    FishingPage(onClose = { viewModel.toggleFishing() })
                }

                // Trip Popup
                if (showTrip) {
                    TripPage(viewModel = viewModel, onClose = { viewModel.toggleTrip() })
                }

                // Rear Camera Popup
                if (showRearCam) {
                    RearCamPopup(onClose = { viewModel.toggleRearCam() })
                }
            }
            
            // MEDIA MINI DOCK (Jos, persistent pe toate paginile)
            RadioMiniPlayer(viewModel = viewModel, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth())
        }
    }
    }
}
