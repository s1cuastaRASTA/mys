package com.dashboard.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dashboard.app.R
import com.dashboard.app.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun InclinometerGauge(
    pitch: Float,
    roll: Float,
    maxPitch: Float,
    maxRoll: Float,
    altitude: Double = 0.0,
    heading: Float = 0f,
    isDarkTheme: Boolean,
    onCalibrate: () -> Unit = {},
    modifier: Modifier = Modifier
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

    Row(
        modifier = modifier.fillMaxSize().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Chenar Pitch
        Box(modifier = Modifier.weight(1.2f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(if (isDarkTheme) DarkSurface else LightSurface).padding(8.dp)) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                PitchViewExtreme(pitch = pitch, modifier = Modifier.weight(1f))
                Text("MAX: ${maxPitch.roundToInt()}°", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentRed)
            }
        }
        
        // CENTRU: Altitude + Heading + Calibration
        Box(modifier = Modifier.weight(0.8f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(if (isDarkTheme) DarkSurface else LightSurface).padding(8.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Altitude
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ALTITUDINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                    Text("${altitude.roundToInt()} m", fontSize = 28.sp, fontWeight = FontWeight.Black, color = AccentYellow)
                }

                // Heading
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CAP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                    Text("$cardinal ${heading.roundToInt()}°", fontSize = 28.sp, fontWeight = FontWeight.Black, color = AccentBlue)
                }

                // Calibration button
                Surface(
                    onClick = onCalibrate,
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(0.8f).height(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "CALIBRARE ZERO",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentBlue,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Chenar Roll
        Box(modifier = Modifier.weight(1.2f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(if (isDarkTheme) DarkSurface else LightSurface).padding(8.dp)) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                RollViewExtreme(roll = roll, modifier = Modifier.weight(1f))
                Text("MAX: ${maxRoll.roundToInt()}°", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentRed)
            }
        }
    }
}

@Composable
private fun PitchViewExtreme(pitch: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.3f))
        Text("${pitch.roundToInt()}°", fontSize = 64.sp, fontWeight = FontWeight.Black, color = if (abs(pitch) > 20) AccentRed else AccentGreen)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(color = GaugeTrack, start = Offset(0f, size.height / 2f), end = Offset(size.width, size.height / 2f), strokeWidth = 2f)
            }
            Image(
                painter = painterResource(R.drawable.lateral_rav),
                contentDescription = "pitch",
                modifier = Modifier.fillMaxSize().rotate(-pitch),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun RollViewExtreme(roll: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.3f))
        Text("${roll.roundToInt()}°", fontSize = 64.sp, fontWeight = FontWeight.Black, color = if (abs(roll) > 20) AccentRed else AccentGreen)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(color = GaugeTrack, start = Offset(0f, size.height / 2f), end = Offset(size.width, size.height / 2f), strokeWidth = 2f)
            }
            Image(
                painter = painterResource(R.drawable.spate_rav),
                contentDescription = "roll",
                modifier = Modifier.fillMaxSize().rotate(roll),
                contentScale = ContentScale.Fit
            )
        }
    }
}
