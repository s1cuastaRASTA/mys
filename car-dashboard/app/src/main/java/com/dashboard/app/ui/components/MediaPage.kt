package com.dashboard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.dashboard.app.DashboardViewModel
import com.dashboard.app.ui.theme.*
import com.dashboard.app.romanianStations
import java.io.File

enum class MediaTab { RADIO, MUSIC }

@Composable
fun MediaPage(viewModel: DashboardViewModel, modifier: Modifier = Modifier) {
    var tab by remember { mutableStateOf(MediaTab.RADIO) }

    Column(modifier = modifier.fillMaxSize().padding(12.dp)) {
        // Tab buttons
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                onClick = { tab = MediaTab.RADIO },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (tab == MediaTab.RADIO) AccentBlue.copy(alpha = 0.2f) else DarkSurfaceVariant
            ) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Radio, null, tint = if (tab == MediaTab.RADIO) AccentBlue else TextSecondary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("RADIO", fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (tab == MediaTab.RADIO) AccentBlue else TextSecondary)
                }
            }
            Surface(
                onClick = { tab = MediaTab.MUSIC },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (tab == MediaTab.MUSIC) AccentGreen.copy(alpha = 0.2f) else DarkSurfaceVariant
            ) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, null, tint = if (tab == MediaTab.MUSIC) AccentGreen else TextSecondary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("MUZICĂ", fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (tab == MediaTab.MUSIC) AccentGreen else TextSecondary)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        when (tab) {
            MediaTab.RADIO -> RadioContent(viewModel)
            MediaTab.MUSIC -> MusicContent(viewModel)
        }
    }
}

@Composable
private fun RadioContent(viewModel: DashboardViewModel) {
    val isPlaying by viewModel.isRadioPlaying.collectAsState()
    val currentStation by viewModel.currentStation.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val favorites by viewModel.favoriteStations.collectAsState()

    val sorted = romanianStations.sortedByDescending { if (favorites.contains(it.name)) 1 else 0 }
    val genres = sorted.groupBy { it.genre }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // LEFT - Now playing
        Box(
            Modifier.weight(0.5f).fillMaxHeight().clip(RoundedCornerShape(14.dp))
                .background(DarkSurface).padding(10.dp), contentAlignment = Alignment.Center
        ) {
            if (isPlaying && currentStation != null) {
                RadioNowPlaying(currentStation!!, volume, { viewModel.setVolume(it) }, { viewModel.stopRadio() }, true)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Radio, null, tint = AccentBlue.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Alege un post radio", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                }
            }
        }

        // RIGHT - Station grid
        Box(
            Modifier.weight(0.5f).fillMaxHeight().clip(RoundedCornerShape(14.dp))
                .background(DarkSurface).padding(6.dp)
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
                        StationCard(station, isActive, favorites.contains(station.name), true,
                            onClick = { if (isActive) viewModel.stopRadio() else viewModel.playStation(station) },
                            onToggleFavorite = { viewModel.toggleFavorite(station.name) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicContent(viewModel: DashboardViewModel) {
    val rootDir = File("/storage/emulated/0/Music")
    var currentDir by remember { mutableStateOf(rootDir) }
    var songs by remember { mutableStateOf(loadMusicFolder(currentDir)) }
    val isPlaying by viewModel.isMusicPlaying.collectAsState()
    val currentName by viewModel.currentMusicName.collectAsState()

    fun navigateTo(dir: File) { currentDir = dir; songs = loadMusicFolder(dir) }
    fun goUp() { if (currentDir != rootDir) { currentDir = currentDir.parentFile ?: rootDir; songs = loadMusicFolder(currentDir) } }

    val folders = remember { rootDir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.sortedBy { it.name } ?: emptyList() }
    val isRoot = currentDir == rootDir

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // LEFT - Player
        Box(Modifier.weight(0.5f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(DarkSurface).padding(8.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (isPlaying) {
                    Box(Modifier.size(100.dp).clip(RoundedCornerShape(18.dp)).background(DarkBackground.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, null, tint = AccentGreen.copy(alpha = 0.5f), modifier = Modifier.size(50.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(currentName, fontSize = 18.sp, fontWeight = FontWeight.Black, color = AccentBlue, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    var musicVol by remember { mutableFloatStateOf(0.8f) }
                    Row(Modifier.fillMaxWidth(0.8f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, null, tint = AccentYellow, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Slider(value = musicVol, onValueChange = { musicVol = it; viewModel.setMusicVolume(it) }, modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = AccentYellow, activeTrackColor = AccentYellow, inactiveTrackColor = DarkSurfaceVariant))
                    }
                } else {
                    Icon(Icons.Default.LibraryMusic, null, tint = TextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Music Player", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                }

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(0.9f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(onClick = { viewModel.playPrevMusic() }, modifier = Modifier.weight(1f).height(64.dp), shape = RoundedCornerShape(14.dp), color = DarkSurfaceVariant) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.SkipPrevious, null, tint = AccentYellow, modifier = Modifier.size(36.dp)) }
                    }
                    Surface(onClick = { if (isPlaying) viewModel.toggleMusic() else viewModel.playRandomMusic(currentDir) }, modifier = Modifier.weight(1f).height(64.dp), shape = RoundedCornerShape(14.dp), color = DarkSurfaceVariant) {
                        Box(contentAlignment = Alignment.Center) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = AccentYellow, modifier = Modifier.size(32.dp)) }
                    }
                    Surface(onClick = { viewModel.stopMusic() }, modifier = Modifier.weight(1f).height(64.dp), shape = RoundedCornerShape(14.dp), color = DarkSurfaceVariant) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Stop, null, tint = AccentYellow, modifier = Modifier.size(32.dp)) }
                    }
                    Surface(onClick = { viewModel.playNextMusic() }, modifier = Modifier.weight(1f).height(64.dp), shape = RoundedCornerShape(14.dp), color = DarkSurfaceVariant) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.SkipNext, null, tint = AccentYellow, modifier = Modifier.size(36.dp)) }
                    }
                }
            }
        }

        // RIGHT - Song list + folders
        Box(Modifier.weight(0.5f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(DarkSurface).padding(6.dp)) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!isRoot) {
                        IconButton(onClick = { goUp() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                        }
                    }
                    Text(if (isRoot) "Muzică" else currentDir.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { songs = loadMusicFolder(currentDir) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Refresh, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                if (isRoot && folders.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("Foldere", fontSize = 13.sp, fontWeight = FontWeight.Black, color = AccentBlue,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 4.dp))
                        val rows = folders.chunked(4)
                        rows.forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                row.forEach { folder ->
                                    Surface(onClick = { navigateTo(folder) },
                                        modifier = Modifier.weight(1f).padding(vertical = 2.dp).height(80.dp),
                                        shape = RoundedCornerShape(12.dp), color = DarkSurfaceVariant
                                    ) {
                                        Box(Modifier.fillMaxSize().padding(4.dp), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Folder, null, tint = AccentYellow, modifier = Modifier.size(28.dp))
                                                Spacer(Modifier.height(2.dp))
                                                Text(folder.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                                                    maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(songs.filter { !it.isFolder }) { song ->
                        Surface(onClick = { viewModel.playMusic(song.file, song.name) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                            color = if (currentName == song.name) AccentBlue.copy(alpha = 0.15f) else Color.Transparent
                        ) {
                            Row(Modifier.padding(horizontal = 6.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(DarkBackground.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                                    Icon(if (isPlaying && currentName == song.name) Icons.Default.MusicNote else Icons.Default.AudioFile, null,
                                        tint = if (currentName == song.name) AccentBlue else TextSecondary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(song.name, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                        color = if (currentName == song.name) AccentBlue else TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (song.artist.isNotEmpty()) Text(song.artist, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(formatMusicDuration(song.file), fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class MusicFileData(val file: File, val name: String, val artist: String = "", val isFolder: Boolean = false)

private fun loadMusicFolder(dir: File): List<MusicFileData> {
    if (!dir.exists()) return emptyList()
    return dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))?.filter {
        it.isDirectory || it.extension.lowercase() in listOf("mp3", "mp4", "wav", "flac", "aac", "ogg", "m4a", "wma")
    }?.map { f ->
        if (f.isDirectory) MusicFileData(f, f.name, isFolder = true)
        else MusicFileData(f, f.nameWithoutExtension, try {
            val mmr = android.media.MediaMetadataRetriever(); mmr.setDataSource(f.absolutePath)
            val a = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""; mmr.release(); a
        } catch (_: Exception) { "" })
    } ?: emptyList()
}

private fun formatMusicDuration(file: File): String = try {
    val mmr = android.media.MediaMetadataRetriever(); mmr.setDataSource(file.absolutePath)
    val dur = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
    mmr.release(); val sec = dur / 1000; "%d:%02d".format(sec / 60, sec % 60)
} catch (_: Exception) { "" }
