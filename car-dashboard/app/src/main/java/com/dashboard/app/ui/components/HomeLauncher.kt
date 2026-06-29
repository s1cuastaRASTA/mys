package com.dashboard.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dashboard.app.ui.theme.*
import java.io.File

data class HomeMenuItem(
    val label: String,
    val icon: ImageVector,
    val pageIndex: Int,
    val tint: Color = AccentBlue,
    val iconFile: File? = null,
)

private val homeMenuItems = listOf(
    HomeMenuItem("Acasă", Icons.Default.Home, 3),
    HomeMenuItem("YouTube", Icons.Default.PlayArrow, 99, Color(0xFFFF0000)),
    HomeMenuItem("Meteo", Icons.Default.WbSunny, 98, AccentYellow),
    HomeMenuItem("Pescuit", Icons.Default.Water, 97, AccentBlue, iconFile = File("/storage/emulated/0/Pictures/Icons/pescuit.png")),
    HomeMenuItem("4x4", Icons.Default.Landscape, 4, AccentGreen, iconFile = File("/storage/emulated/0/Pictures/Icons/4x4.png")),
    HomeMenuItem("NAVI", Icons.Default.Navigation, 5, Color(0xFF33CCFF)),
    HomeMenuItem("Radio", Icons.Default.Radio, 1, AccentYellow),
    HomeMenuItem("Muzică", Icons.Default.MusicNote, 1, AccentBlue),
    HomeMenuItem("OBD", Icons.Default.Info, 7, AccentGreen),
    HomeMenuItem("Cam 70mai", Icons.Default.Videocam, 8, AccentRed),
    HomeMenuItem("Setări", Icons.Default.Settings, 6),
)

@Composable
fun HomeLauncher(
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(8.dp)
    ) {
        items(homeMenuItems) { item ->
            Surface(
                onClick = { onNavigate(item.pageIndex) },
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceVariant
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                ) {
                    if (item.iconFile != null && item.iconFile.exists()) {
                        AsyncImage(model = item.iconFile, contentDescription = item.label, modifier = Modifier.fillMaxSize(0.6f), contentScale = ContentScale.Fit)
                    } else {
                        Icon(item.icon, contentDescription = item.label, tint = item.tint, modifier = Modifier.fillMaxSize(0.7f))
                    }
                    Text(item.label, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
