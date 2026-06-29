package com.dashboard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dashboard.app.DashboardViewModel
import com.dashboard.app.TripRecord
import com.dashboard.app.ui.theme.*

@Composable
fun TripPage(viewModel: DashboardViewModel, onClose: () -> Unit) {
    val speed by viewModel.speed.collectAsState()
    val maxSpeed by viewModel.maxSpeed.collectAsState()
    val avgSpeed by viewModel.avgSpeed.collectAsState()
    val distance by viewModel.tripDistance.collectAsState()
    val time by viewModel.tripElapsedTime.collectAsState()
    val isActive by viewModel.isTripActive.collectAsState()
    val isPaused by viewModel.isTripPaused.collectAsState()
    val fuelRate by viewModel.fuelRate.collectAsState()
    val history by viewModel.tripHistory.collectAsState()

    val fuelUsed = if (fuelRate > 0 && distance > 0) (fuelRate / 100) * (distance / 1000) else 0f

    Column(
        modifier = Modifier.fillMaxSize().background(DarkSurface.copy(alpha = 0.95f)).verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("TRIP COMPUTER", fontSize = 18.sp, fontWeight = FontWeight.Black, color = AccentBlue, letterSpacing = 3.sp)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Închide", tint = AccentRed, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        // Live trip data
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = DarkSurfaceVariant) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (isActive) "TRIP ACTIV" else "FĂRĂ TRIP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isActive) AccentGreen else TextSecondary, letterSpacing = 2.sp)
                if (isPaused) { Text("PAUZAT", fontSize = 14.sp, fontWeight = FontWeight.Black, color = AccentYellow) }
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TripStat("VITEZĂ", "${speed.toInt()}", "km/h", AccentBlue)
                    TripStat("MAX", "${maxSpeed.toInt()}", "km/h", AccentRed)
                    TripStat("AVG", "${avgSpeed.toInt()}", "km/h", AccentGreen)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TripStat("DISTANȚĂ", if (distance < 1000) "${distance.toInt()}m" else "%.1f".format(distance / 1000), if (distance < 1000) "m" else "km", AccentYellow)
                    TripStat("TIMP", "%02d:%02d".format(time / 3600, (time % 3600) / 60), "h:m", AccentYellow)
                    TripStat("CONSUM", "%.1f".format(fuelUsed), "L", AccentOrange)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Controls
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!isActive) {
                Surface(onClick = { viewModel.startTrip() }, modifier = Modifier.weight(1f).height(64.dp), shape = RoundedCornerShape(14.dp), color = AccentGreen) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                }
            } else {
                if (isPaused) {
                    Surface(onClick = { viewModel.resumeTrip() }, modifier = Modifier.weight(1f).height(64.dp), shape = RoundedCornerShape(14.dp), color = AccentGreen) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                    }
                } else {
                    Surface(onClick = { viewModel.pauseTrip() }, modifier = Modifier.weight(1f).height(64.dp), shape = RoundedCornerShape(14.dp), color = AccentYellow) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Pause, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                    }
                }
                Surface(onClick = { viewModel.stopTrip() }, modifier = Modifier.weight(1f).height(64.dp), shape = RoundedCornerShape(14.dp), color = AccentRed) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Trip history
        if (history.isNotEmpty()) {
            Text("ISTORIC (ultimele ${history.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            history.forEach { trip ->
                Surface(Modifier.fillMaxWidth().padding(vertical = 2.dp), shape = RoundedCornerShape(10.dp), color = DarkSurfaceVariant.copy(alpha = 0.5f)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(trip.date, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("%.1f km".format(trip.distance / 1000), fontSize = 12.sp, color = AccentBlue)
                                Text("%02d:%02d".format(trip.duration / 3600, (trip.duration % 3600) / 60), fontSize = 12.sp, color = AccentYellow)
                                Text("${trip.maxSpeed.toInt()} max", fontSize = 12.sp, color = AccentRed)
                            }
                        }
                        Text("%.1f L".format(trip.fuelUsed), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                    }
                }
            }
        }
    }
}

@Composable
fun TripStat(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Black, color = color)
        Text(unit, fontSize = 11.sp, color = TextSecondary)
    }
}
