package com.dashboard.app.ui.components

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.dashboard.app.WazeNavInfo
import com.dashboard.app.ui.theme.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

@Composable
fun WazeSection(
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    wazeNavInfo: WazeNavInfo = WazeNavInfo(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isWazeNavigating = wazeNavInfo.isNavigating
    var showWazeMap by remember { mutableStateOf(false) }
    var showMapsMap by remember { mutableStateOf(false) }

    // Fullscreen Waze
    if (showWazeMap) {
        Box(modifier = modifier.fillMaxSize().padding(8.dp)) {
            AndroidView(factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    settings.javaScriptEnabled = true; settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl("https://www.waze.com/live-map")
                }
            }, modifier = Modifier.fillMaxSize().padding(top = 36.dp))
            IconButton(onClick = { showWazeMap = false }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                Icon(Icons.Default.Close, "Close", tint = AccentRed, modifier = Modifier.size(32.dp))
            }
        }
        return
    }

    // Fullscreen OSM
    if (showMapsMap) {
        Box(modifier = modifier.fillMaxSize().padding(8.dp)) {
            AndroidView(factory = { ctx ->
                org.osmdroid.config.Configuration.getInstance().apply {
                    userAgentValue = "DashboardApp/1.0"
                    osmdroidBasePath = ctx.cacheDir.resolve("osmdroid")
                    osmdroidTileCache = ctx.cacheDir.resolve("osmdroid/tiles")
                }
                MapView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    if (latitude != 0.0 || longitude != 0.0) {
                        val point = GeoPoint(latitude, longitude)
                        controller.setCenter(point)
                        val marker = Marker(this)
                        marker.position = point
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = "Eu"
                        overlays.add(marker)
                    }
                }
            }, modifier = Modifier.fillMaxSize().padding(top = 36.dp))
            IconButton(onClick = { showMapsMap = false }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                Icon(Icons.Default.Close, "Close", tint = AccentRed, modifier = Modifier.size(32.dp))
            }
        }
        return
    }

    // Normal mode: two panels side by side
    Row(modifier = modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // LEFT - WAZE
        Box(Modifier.weight(0.5f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(DarkSurface).padding(10.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                val wazeIcon = File("/storage/emulated/0/Pictures/Icons/waze.png")
                Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                    if (wazeIcon.exists()) {
                        AsyncImage(model = wazeIcon, contentDescription = "Waze", modifier = Modifier.fillMaxSize(0.85f), contentScale = ContentScale.Fit)
                    } else {
                        Icon(Icons.Default.Navigation, null, tint = Color(0xFF33CCFF).copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Waze", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF33CCFF))
                Spacer(Modifier.height(8.dp))
                Surface(onClick = {
                    val wazeUri = Uri.parse("https://waze.com/ul?navigate=yes")
                    val intent = Intent(Intent.ACTION_VIEW, wazeUri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    try { context.startActivity(intent) } catch (_: Exception) { }
                }, shape = RoundedCornerShape(14.dp), color = Color(0xFF33CCFF),
                    modifier = Modifier.fillMaxWidth(0.85f).height(72.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("Deschide Waze", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(10.dp))
                Surface(onClick = { showWazeMap = true }, shape = RoundedCornerShape(14.dp), color = Color(0xFF33CCFF).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth(0.85f).height(56.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("Hartă Waze", color = Color(0xFF33CCFF), fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                }
                if (isWazeNavigating) {
                    Spacer(Modifier.height(12.dp)); Text("Navigație activă", fontSize = 13.sp, color = AccentGreen)
                    if (wazeNavInfo.remainingDistance.isNotEmpty()) Text(wazeNavInfo.remainingDistance, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    if (wazeNavInfo.eta.isNotEmpty()) Text("ETA: ${wazeNavInfo.eta}", fontSize = 14.sp, color = AccentYellow)
                }
            }
        }

        // RIGHT - GOOGLE MAPS
        Box(Modifier.weight(0.5f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(DarkSurface).padding(10.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                val mapsIcon = File("/storage/emulated/0/Pictures/Icons/googlemaps.png")
                Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                    if (mapsIcon.exists()) {
                        AsyncImage(model = mapsIcon, contentDescription = "Maps", modifier = Modifier.fillMaxSize(0.85f), contentScale = ContentScale.Fit)
                    } else {
                        Icon(Icons.Default.Map, null, tint = AccentGreen.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Google Maps", fontSize = 24.sp, fontWeight = FontWeight.Black, color = AccentGreen)
                Spacer(Modifier.height(8.dp))
                Surface(onClick = {
                    try {
                        val gmmUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmUri).apply {
                            setPackage("com.google.android.apps.maps")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(mapIntent)
                    } catch (_: Exception) {
                        val webUri = Uri.parse("https://www.google.com/maps?q=$latitude,$longitude")
                        val intent = Intent(Intent.ACTION_VIEW, webUri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        try { context.startActivity(intent) } catch (_: Exception) { }
                    }
                }, shape = RoundedCornerShape(14.dp), color = AccentGreen,
                    modifier = Modifier.fillMaxWidth(0.85f).height(72.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("Deschide Maps", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(10.dp))
                Surface(onClick = { showMapsMap = true }, shape = RoundedCornerShape(14.dp), color = AccentGreen.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth(0.85f).height(56.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("Hartă Live", color = AccentGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
