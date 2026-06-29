package com.dashboard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.dashboard.app.DashboardViewModel
import com.dashboard.app.romanianStations
import com.dashboard.app.ui.theme.*

@Composable
fun RadioMiniPlayer(viewModel: DashboardViewModel, modifier: Modifier = Modifier) {
    val isRadioPlaying by viewModel.isRadioPlaying.collectAsState()
    val currentStation by viewModel.currentStation.collectAsState()
    val isMusicPlaying by viewModel.isMusicPlaying.collectAsState()
    val currentMusicName by viewModel.currentMusicName.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val context = LocalContext.current

    val logoFile = if (currentStation != null) stationLogoFile(currentStation!!, context) else null
    val anyPlaying = isRadioPlaying || isMusicPlaying

    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(if (isDarkTheme) DarkSurface.copy(alpha = 0.85f) else LightSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Previous
        Surface(
            onClick = { if (isRadioPlaying) viewModel.playPreviousStation() else if (isMusicPlaying) viewModel.playPrevMusic() },
            modifier = Modifier.weight(0.2f).height(100.dp), shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme) DarkSurfaceVariant else LightSurfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.SkipPrevious, "Anterior", tint = AccentYellow, modifier = Modifier.size(48.dp))
            }
        }

        // Play/Stop center
        Surface(
            onClick = {
                when {
                    isRadioPlaying -> viewModel.stopRadio()
                    isMusicPlaying -> viewModel.toggleMusic()
                    else -> viewModel.playStation(romanianStations.random())
                }
            },
            modifier = Modifier.weight(0.6f).height(100.dp), shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme) DarkSurfaceVariant else LightSurfaceVariant
        ) {
            if (anyPlaying) {
                Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isRadioPlaying && currentStation != null) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(DarkBackground.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                            if (logoFile != null && logoFile.exists()) SubcomposeAsyncImage(model = logoFile, contentDescription = null, modifier = Modifier.fillMaxSize().padding(3.dp), contentScale = ContentScale.Fit, error = { Icon(Icons.Default.Radio, null, tint = AccentYellow, modifier = Modifier.size(22.dp)) })
                            else Icon(Icons.Default.Radio, null, tint = AccentYellow, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(currentStation!!.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentYellow, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            EqBars(isPlaying = true, modifier = Modifier.fillMaxWidth().height(14.dp))
                        }
                    } else if (isMusicPlaying) {
                        Icon(Icons.Default.MusicNote, null, tint = AccentGreen, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(currentMusicName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            EqBars(isPlaying = true, modifier = Modifier.fillMaxWidth().height(14.dp))
                        }
                    }
                }
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, "Play", tint = AccentYellow, modifier = Modifier.size(52.dp))
                }
            }
        }

        // Next
        Surface(
            onClick = { if (isRadioPlaying) viewModel.playNextStation() else if (isMusicPlaying) viewModel.playNextMusic() },
            modifier = Modifier.weight(0.2f).height(100.dp), shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme) DarkSurfaceVariant else LightSurfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.SkipNext, "Următorul", tint = AccentYellow, modifier = Modifier.size(48.dp))
            }
        }
    }
}
