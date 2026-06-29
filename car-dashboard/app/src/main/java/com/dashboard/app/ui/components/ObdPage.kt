package com.dashboard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.dashboard.app.ObdData
import com.dashboard.app.ObdService
import com.dashboard.app.ui.theme.*

@Composable
fun ObdPage(modifier: Modifier = Modifier) {
    val isConnected by ObdService.isConnected.collectAsState()
    val obdData by ObdService.data.collectAsState()
    val status by ObdService.status.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Connection bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = if (isConnected) AccentGreen.copy(alpha = 0.1f) else DarkSurface.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                    null,
                    tint = if (isConnected) AccentGreen else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "OBD2 Diagnostică",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        status,
                        fontSize = 12.sp,
                        color = if (isConnected) AccentGreen else TextSecondary
                    )
                }
                Button(
                    onClick = {
                        if (isConnected) ObdService.disconnect() else ObdService.connect()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) AccentRed else AccentGreen
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        if (isConnected) "Deconectează" else "Conectează",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (isConnected) {
            // RPM + Speed row
            Row(
                modifier = Modifier.fillMaxWidth().height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ObdGauge(
                    label = "RPM",
                    value = "${obdData.rpm}",
                    sub = "rot/min",
                    color = AccentRed,
                    progress = (obdData.rpm / 8000f).coerceIn(0f, 1f),
                    modifier = Modifier.weight(1f)
                )
                ObdGauge(
                    label = "VITEZĂ",
                    value = "${obdData.speed}",
                    sub = "km/h",
                    color = AccentBlue,
                    progress = (obdData.speed / 220f).coerceIn(0f, 1f),
                    modifier = Modifier.weight(1f)
                )
            }

            // Temp + Fuel row
            Row(
                modifier = Modifier.fillMaxWidth().height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ObdGauge(
                    label = "TEMP MOTOR",
                    value = "${obdData.coolantTemp}",
                    sub = "°C",
                    color = if (obdData.coolantTemp > 100) AccentRed else AccentYellow,
                    progress = (obdData.coolantTemp / 120f).coerceIn(0f, 1f),
                    modifier = Modifier.weight(1f)
                )
                ObdGauge(
                    label = "COMBUSTIBIL",
                    value = "%.0f".format(obdData.fuelLevel),
                    sub = "%",
                    color = if (obdData.fuelLevel < 15) AccentRed else AccentGreen,
                    progress = obdData.fuelLevel / 100f,
                    modifier = Modifier.weight(1f)
                )
            }

            // Throttle + Load row
            Row(
                modifier = Modifier.fillMaxWidth().height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ObdGauge(
                    label = "ACCELERAȚIE",
                    value = "%.0f".format(obdData.throttle),
                    sub = "%",
                    color = AccentOrange,
                    progress = obdData.throttle / 100f,
                    modifier = Modifier.weight(1f)
                )
                ObdGauge(
                    label = "SARCINĂ MOTOR",
                    value = "%.0f".format(obdData.engineLoad),
                    sub = "%",
                    color = AccentBlue,
                    progress = obdData.engineLoad / 100f,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Idle message
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.BluetoothDisabled,
                        null,
                        tint = TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Conectează OBD2",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Asigură-te că tableta e conectată",
                        fontSize = 13.sp,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                    Text(
                        "la rețeaua WiFi a OBD-ului",
                        fontSize = 13.sp,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "IP implicit: 192.168.0.10:35000",
                        fontSize = 12.sp,
                        color = AccentBlue.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun ObdGauge(
    label: String,
    value: String,
    sub: String,
    color: Color,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 36.sp, fontWeight = FontWeight.Black, color = color)
            Text(sub, fontSize = 11.sp, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(DarkSurfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
        }
    }
}
