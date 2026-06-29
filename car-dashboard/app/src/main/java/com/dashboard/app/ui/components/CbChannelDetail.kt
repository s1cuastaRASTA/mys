package com.dashboard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dashboard.app.ui.theme.*

@Composable
fun CbChannelDetail(channelNumber: Int, modifier: Modifier = Modifier) {
    val channel = romanianCbChannels.find { it.number == channelNumber }

    Column(
        modifier = modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (channel == null) {
            Text(
                "Alege un canal\nși apasă ▶\npentru a asculta",
                color = TextSecondary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                channel.name,
                fontWeight = FontWeight.Bold,
                color = AccentBlue,
                fontSize = 28.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                channel.frequency,
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentBlue.copy(alpha = 0.1f))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    channel.usage,
                    color = AccentYellow,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Modulație: ${channel.modulation}",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                if (channel.details.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        channel.details,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
