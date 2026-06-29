package com.dashboard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dashboard.app.ui.theme.*

data class CbChannel(
    val number: Int,
    val frequency: String,
    val freqKhz: Int,
    val name: String,
    val usage: String,
    val modulation: String = "FM",
    val details: String = "",
    val serverUrl: String = "http://websdr.yo3ggx.ro:8765",
    val urlFreqParam: String = "tune"
)

val romanianCbChannels = listOf(
    CbChannel(13, "27.115 MHz", 27115, "CH13 - Pescar", "Pescari / Ambarcațiuni", "FM",
        "Canal folosit de pescari și ambarcațiuni (marin/RV).",
        "http://websdr.yo3ggx.ro:8765", "tune"),
    CbChannel(16, "27.155 MHz", 27155, "CH16 - 4x4", "Off-road / 4x4", "FM",
        "Canal oficial off-road în România, folosit de comunitatea 4x4.",
        "http://websdr.yo3ggx.ro:8765", "tune"),
    CbChannel(22, "27.225 MHz", 27225, "CH22 - BREAZA", "YO3GGX (Prahova)", "FM",
        "WebSDR România — Breaza, Prahova. Acoperă banda CB 27 MHz.",
        "http://websdr.yo3ggx.ro:8765", "tune"),
    CbChannel(22, "27.225 MHz", 27225, "CH22 - București", "YO3BN (București)", "FM",
        "KiwiSDR România — Ștefăneștii de Jos, București. Acoperă 0-30 MHz.",
        "http://www.yo3bn.ro:8073", "freq"),
)

@Composable
fun CbRadio(
    selectedChannel: Int,
    isPlaying: Boolean,
    playingServerUrl: String,
    playingFreq: Int,
    savedChannels: Set<String> = emptySet(),
    onChannelClick: (Int) -> Unit,
    onPlayChannel: (freqKhz: Int, serverUrl: String, urlParam: String) -> Unit,
    onStop: () -> Unit,
    onToggleSaved: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(6.dp)
    ) {
        Text(
            "CB Radio (27 MHz)",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (rowIdx in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (colIdx in 0..1) {
                        val idx = rowIdx * 2 + colIdx
                        val channel = romanianCbChannels[idx]
                        val isThisPlaying = isPlaying && playingServerUrl == channel.serverUrl && playingFreq == channel.freqKhz
                        val isSelected = channel.number == selectedChannel
                        val chKey = "CH${channel.number}_${channel.serverUrl}_${channel.freqKhz}"
                        val isSaved = savedChannels.contains(chKey)

                        Box(
                            modifier = Modifier
                                .weight(1f).fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isThisPlaying -> AccentGreen.copy(alpha = 0.25f)
                                        isSelected -> AccentBlue.copy(alpha = 0.2f)
                                        else -> DarkSurfaceVariant
                                    }
                                )
                                .clickable { onChannelClick(channel.number) }
                                .padding(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Spacer(Modifier.size(18.dp))
                                    Text(
                                        "CH${channel.number}",
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isThisPlaying -> AccentGreen
                                            isSelected -> AccentBlue
                                            else -> AccentYellow
                                        },
                                        fontSize = 22.sp
                                    )
                                    Icon(
                                        if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Salvează",
                                        tint = if (isSaved) AccentYellow else Color.Gray.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp).clickable { onToggleSaved(chKey) }
                                    )
                                }
                                Text(
                                    channel.frequency,
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    channel.name,
                                    color = if (isThisPlaying) AccentGreen else if (isSelected) AccentBlue else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    channel.usage,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isThisPlaying) AccentRed.copy(alpha = 0.3f)
                                            else AccentGreen.copy(alpha = 0.3f)
                                        )
                                        .clickable {
                                            if (isThisPlaying) onStop()
                                            else onPlayChannel(channel.freqKhz, channel.serverUrl, channel.urlFreqParam)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = if (isThisPlaying) "Stop" else "Play",
                                        tint = if (isThisPlaying) AccentRed else AccentGreen,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
