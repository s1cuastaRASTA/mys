package com.dashboard.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dashboard.app.ui.theme.*

data class SpeedZone(val limit: Float, val label: String, val color: androidx.compose.ui.graphics.Color)

val speedZones = listOf(
    SpeedZone(50f, "50", SpeedGreen),
    SpeedZone(90f, "90", SpeedYellow),
    SpeedZone(130f, "130", SpeedRed),
)

@Composable
fun SpeedometerGauge(
    speedKmh: Float,
    displaySpeed: Float,
    maxDisplaySpeed: Float = 220f,
    unit: String = "km/h",
    modifier: Modifier = Modifier
) {
    val speedKmhClamped = speedKmh.coerceAtLeast(0f)
    val frac = (displaySpeed / maxDisplaySpeed).coerceIn(0f, 1f)

    val displayColor by animateColorAsState(
        targetValue = when {
            speedKmhClamped > 130 -> SpeedRed
            speedKmhClamped > 90 -> AccentOrange
            speedKmhClamped > 50 -> SpeedYellow
            else -> SpeedGreen
        }
    )

    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse)
    )
    val isDanger = speedKmhClamped > 130

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .then(
                if (isDanger) Modifier.border(3.dp, SpeedRed.copy(alpha = pulseAlpha), RoundedCornerShape(20.dp))
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isDanger) {
                Text(
                    text = "DEPĂȘIT",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = SpeedRed.copy(alpha = pulseAlpha),
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = "${displaySpeed.toInt()}",
                fontSize = 200.sp,
                fontWeight = FontWeight.Bold,
                color = displayColor,
                textAlign = TextAlign.Center,
                letterSpacing = 4.sp
            )

            Text(
                text = unit,
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Zone indicator
            Row(
                modifier = Modifier.fillMaxWidth(0.5f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                speedZones.forEach { zone ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = zone.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (speedKmhClamped >= zone.limit && zone.limit > 50) zone.color.copy(alpha = 0.6f) else TextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (speedKmhClamped > zone.limit) zone.color
                                    else DarkSurfaceVariant
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Multi-zone progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(DarkSurfaceVariant)
            ) {
                // Colored segments
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.weight(50f / maxDisplaySpeed).fillMaxHeight().background(SpeedGreen.copy(alpha = 0.35f)))
                    Box(Modifier.weight(40f / maxDisplaySpeed).fillMaxHeight().background(SpeedYellow.copy(alpha = 0.35f)))
                    Box(Modifier.weight(40f / maxDisplaySpeed).fillMaxHeight().background(AccentOrange.copy(alpha = 0.35f)))
                    Box(Modifier.weight((maxDisplaySpeed - 130) / maxDisplaySpeed).fillMaxHeight().background(SpeedRed.copy(alpha = 0.2f)))
                }

                // Progress fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth(frac)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(displayColor)
                )
            }
        }
    }
}
