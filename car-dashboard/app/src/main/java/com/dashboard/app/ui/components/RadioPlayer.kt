package com.dashboard.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.dashboard.app.DashboardViewModel
import com.dashboard.app.RadioStation
import com.dashboard.app.ui.theme.*
import com.dashboard.app.romanianStations
import java.io.File
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun RadioPlayer(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val isPlaying by viewModel.isRadioPlaying.collectAsState()
    val currentStation by viewModel.currentStation.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val favorites by viewModel.favoriteStations.collectAsState()

    val sorted = romanianStations.sortedByDescending { if (favorites.contains(it.name)) 1 else 0 }
    val genres = sorted.groupBy { it.genre }

    Row(
        modifier = modifier.fillMaxSize().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.weight(0.5f).fillMaxHeight().clip(RoundedCornerShape(14.dp))
                .background(if (isDarkTheme) DarkSurface else LightSurface).padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying && currentStation != null) {
                RadioNowPlaying(currentStation!!, volume, { viewModel.setVolume(it) }, { viewModel.stopRadio() }, isDarkTheme)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Radio, null, tint = AccentBlue.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Alege un post radio", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                }
            }
        }

        Box(
            modifier = Modifier.weight(0.5f).fillMaxHeight().clip(RoundedCornerShape(14.dp))
                .background(if (isDarkTheme) DarkSurface else LightSurface).padding(6.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                genres.forEach { (genre, sts) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(genre.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Black, color = AccentBlue,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 2.dp))
                    }
                    items(sts, key = { it.name }) { station ->
                        val isActive = station == currentStation && isPlaying
                        StationCard(station, isActive, favorites.contains(station.name), isDarkTheme,
                            onClick = { if (isActive) viewModel.stopRadio() else viewModel.playStation(station) },
                            onToggleFavorite = { viewModel.toggleFavorite(station.name) })
                    }
                }
            }
        }
    }
}

@Composable
fun RadioNowPlaying(
    station: RadioStation,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onStop: () -> Unit,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val logoFile = stationLogoFile(station, context)

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(160.dp).clip(RoundedCornerShape(24.dp)).background(if (isDarkTheme) DarkBackground.copy(alpha = 0.5f) else Color.White), contentAlignment = Alignment.Center) {
            SubcomposeAsyncImage(
                model = if (logoFile.exists()) logoFile else station.iconUrl,
                contentDescription = station.name, modifier = Modifier.fillMaxSize().padding(16.dp), contentScale = ContentScale.Fit,
                error = { Icon(Icons.Default.Radio, null, tint = AccentBlue, modifier = Modifier.size(64.dp)) })
        }
        Spacer(Modifier.height(20.dp))
        Text(station.name, fontSize = 28.sp, fontWeight = FontWeight.Black, color = AccentBlue, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(station.genre, fontSize = 15.sp, color = TextSecondary)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(0.8f), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = AccentYellow, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Slider(value = volume, onValueChange = onVolumeChange, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = AccentYellow, activeTrackColor = AccentYellow,
                    inactiveTrackColor = if (isDarkTheme) DarkSurfaceVariant else LightSurfaceVariant))
        }
        Spacer(Modifier.height(16.dp))
        Surface(onClick = onStop, modifier = Modifier.fillMaxWidth(0.5f).height(56.dp), shape = RoundedCornerShape(14.dp), color = DarkSurfaceVariant) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Stop, null, tint = AccentYellow, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
fun StationCard(
    station: RadioStation, isActive: Boolean, isFav: Boolean, isDarkTheme: Boolean,
    onClick: () -> Unit, onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val logoFile = stationLogoFile(station, context)

    Card(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = if (isActive) AccentBlue.copy(alpha = 0.2f) else if (isDarkTheme) DarkSurfaceVariant else LightSurfaceVariant),
        border = if (isActive) ButtonDefaults.outlinedButtonBorder else null
    ) {
        Box(Modifier.fillMaxSize()) {
            SubcomposeAsyncImage(model = if (logoFile.exists()) logoFile else station.iconUrl, contentDescription = station.name,
                modifier = Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit,
                error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Radio, null, tint = if (isActive) AccentBlue else AccentYellow, modifier = Modifier.size(32.dp)) } })
            Icon(imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorite",
                tint = if (isFav) AccentRed else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.TopEnd).size(18.dp).padding(2.dp).clickable(onClick = onToggleFavorite))
            if (isActive) {
                Box(Modifier.align(Alignment.BottomEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(AccentGreen), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

fun stationLogoFile(station: RadioStation, context: android.content.Context): File {
    val clean = station.name.lowercase().replace(" ", "_").replace("-", "_")
        .replace("ă","a").replace("â","a").replace("î","i").replace("ș","s").replace("ț","t")
        .replace("&","si").replace("é","e").replace(Regex("[^a-z0-9_]"), "")

    val primary = File("/storage/emulated/0/Pictures/IMG radio")
    val secondary = context.getExternalFilesDir("IMG radio")

    fun find(dir: File?): File? {
        if (dir == null || !dir.exists()) return null
        val exact = File(dir, "${clean}.png")
        if (exact.exists()) return exact
        val c = clean.replace("_", "")
        for (f in dir.listFiles() ?: emptyArray()) {
            val ext = f.extension.lowercase()
            if (ext !in listOf("png", "jpg", "jpeg")) continue
            val fn = f.name.lowercase().replace(" ", "").replace("-", "").replace("_", "").replace(Regex("[^a-z0-9.]"), "")
            if (fn == "$c.$ext") return f
            if (fn.contains(c)) return f
            // Reverse: does station name contain the filename (without ext)?
            val fnNoExt = fn.substringBeforeLast(".")
            if (c.contains(fnNoExt) && fnNoExt.length > 3) return f
        }
        return null
    }
    find(primary)?.let { return it }
    find(secondary)?.let { return it }
    return File(context.filesDir, "${clean}.png")
}

@Composable
fun EqBarsFullscreen(modifier: Modifier = Modifier) {
    val count = 50
    val phases = remember { FloatArray(count) { Random.nextFloat() * 6.28f } }
    val freqs = remember { FloatArray(count) { 0.5f + Random.nextFloat() * 2.5f } }
    var t by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) { while (true) { t += 0.08f; kotlinx.coroutines.delay(30) } }
    Canvas(modifier) {
        val sp = 4.dp.toPx(); val tsp = sp * (count - 1); val bw = (size.width - tsp) / count
        for (i in 0 until count) {
            val r = (sin((phases[i] + t * freqs[i]).toDouble()) * 0.48f + 0.52f).toFloat()
            val h = r * size.height
            drawRoundRect(when { r > 0.85f -> AccentRed.copy(alpha=0.6f); r > 0.65f -> AccentYellow.copy(alpha=0.5f); r > 0.45f -> AccentGreen.copy(alpha=0.4f); else -> AccentBlue.copy(alpha=0.3f) },
                Offset(i * (bw + sp), size.height - h), Size(bw, h), CornerRadius(bw / 2))
        }
    }
}

@Composable
fun EqBars(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val count = 16
    val phases = remember { FloatArray(count) { Random.nextFloat() * 6.28f } }
    val freqs = remember { FloatArray(count) { 1.5f + Random.nextFloat() * 3.5f } }
    var t by remember { mutableStateOf(0f) }
    LaunchedEffect(isPlaying) { if (isPlaying) while (true) { t += 0.12f; kotlinx.coroutines.delay(35) } }
    Canvas(modifier) {
        val sp = 3.dp.toPx(); val tsp = sp * (count - 1); val bw = (size.width - tsp) / count
        for (i in 0 until count) {
            val r = if (isPlaying) (sin((phases[i] + t * freqs[i]).toDouble()) * 0.45f + 0.55f).toFloat() else 0.08f
            val h = (r * size.height).coerceAtLeast(3.dp.toPx())
            drawRoundRect(when { r > 0.8f -> AccentRed; r > 0.55f -> AccentYellow; r > 0.3f -> AccentGreen; else -> AccentBlue },
                Offset(i * (bw + sp), size.height - h), Size(bw, h), CornerRadius(bw / 2))
        }
    }
}
