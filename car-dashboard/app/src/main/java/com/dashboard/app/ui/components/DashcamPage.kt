package com.dashboard.app.ui.components

import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import coil.request.ImageRequest
import com.dashboard.app.Camera70maiService
import com.dashboard.app.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.security.MessageDigest

private fun dashLog(msg: String) {
    Log.d("Dashcam", msg)
    try {
        val f = java.io.File("/storage/emulated/0/Documents/dash_dashcam_log.txt")
        f.parentFile?.mkdirs()
        f.appendText("${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date())} $msg\n")
    } catch (_: Exception) {}
}

@Composable
fun DashcamPage(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { Camera70maiService.init(ctx) }

    val camState by Camera70maiService.state.collectAsState()
    var isConnected by remember { mutableStateOf(camState.connected) }
    var status by remember { mutableStateOf(camState.status) }
    var snapKey by remember { mutableIntStateOf(0) }
    var testResults by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(camState) {
        isConnected = camState.connected
        status = camState.status
        if (camState.connected) snapKey++
    }

    fun testAndConnect() {
        CoroutineScope(Dispatchers.Main).launch {
            status = "Se conectează..."
            val results = mutableListOf<String>()
            withContext(Dispatchers.IO) {
                // First try the new pairing-based API
                results.add("Paring camera...")
                testResults = results.toList()

                val paired = tryPairAndGetSnapshot()
                if (paired != null) {
                    results.add("Pairing OK! Snapshot URL: ${paired.take(60)}...")
                    Camera70maiService.connect()
                    testResults = results.toList()
                    return@withContext
                }

                // Fallback to old direct URLs
                results.add("Pairing esuat, test direct URLs...")
                testResults = results.toList()

                val urls = listOf(
                    "http://192.168.0.1/cgi-bin/currentpic.cgi",
                    "http://192.168.0.1/?custom=1&cmd=2001&par=1",
                )
                for (url in urls) {
                    try {
                        val conn = URL(url).openConnection() as HttpURLConnection
                        conn.connectTimeout = 2000; conn.readTimeout = 2000
                        conn.setRequestProperty("User-Agent", "70mai/1.0")
                        val code = conn.responseCode
                        val bytes = try { conn.inputStream.readBytes() } catch (_: Exception) { ByteArray(0) }
                        val preview = if (bytes.size >= 4 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte())
                            "JPEG ${bytes.size}B" else "TEXT[${bytes.size}B]"
                        results.add("HTTP $code $url -> $preview")
                        testResults = results.toList()
                        conn.disconnect()
                    } catch (e: Exception) {
                        results.add("ERROR ${e.message?.take(30)} $url")
                        testResults = results.toList()
                    }
                }
            }
            Camera70maiService.connect()
            status = camState.status
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = if (isConnected) AccentGreen.copy(alpha = 0.1f) else DarkSurfaceVariant) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isConnected) Icons.Default.Videocam else Icons.Default.VideocamOff, null, tint = if (isConnected) AccentGreen else TextSecondary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("70mai Camera Auto", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(status, fontSize = 12.sp, color = if (isConnected) AccentGreen else TextSecondary)
                }
                Button(onClick = { testAndConnect() }, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(10.dp), modifier = Modifier.height(40.dp)) {
                    Text("Conectează", fontSize = 13.sp, color = Color.White)
                }
            }
        }

        if (isConnected && camState.snapshotUrl.isNotEmpty()) {
            Surface(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(14.dp), color = DarkSurfaceVariant) {
                key(snapKey) {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx).data(camState.snapshotUrl).crossfade(true).build(),
                        contentDescription = "Camera",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
        } else if (testResults.isNotEmpty()) {
            Surface(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(14.dp), color = DarkSurfaceVariant) {
                Column(Modifier.fillMaxSize().padding(8.dp).verticalScroll(rememberScrollState())) {
                    for (r in testResults) {
                        val color = when {
                            r.startsWith("Pairing OK") -> AccentGreen
                            r.startsWith("HTTP 4") -> AccentYellow
                            r.startsWith("ERROR") -> AccentRed.copy(alpha = 0.6f)
                            r.startsWith("HTTP 200") -> AccentGreen
                            else -> TextSecondary
                        }
                        Text(r, fontSize = 11.sp, color = color, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.VideocamOff, null, tint = TextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Camera 70mai neconectată", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text("Conectează tableta la WiFi-ul camerei", fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.5f))
                }
            }
        }
    }
}

private fun tryPairAndGetSnapshot(): String? {
    val ip = "192.168.0.1"
    val magic = "73VpsAfdety8FDd0"
    val userId = "1935132"
    fun md5(s: String) = MessageDigest.getInstance("MD5").digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    return try {
        // Step 1: Bind
        val key1 = md5(userId + magic)
        val bindUrl = "http://$ip/cgi-bin/BindByBanya.cgi?&-usr=$userId&-signkey=$key1"
        val bindResult = httpGetBytes(bindUrl) ?: return null
        val bindBody = String(bindResult.body)
        dashLog("Bind: $bindBody")
        if (!bindBody.contains("\"ResultCode\":\"0\"")) return null
        val token = bindBody.substringAfter("\"Token\":\"").substringBefore("\"")
        val tsStr = bindBody.substringAfter("\"timestamp\":\"").substringBefore("\"")
        val ts = tsStr.toLong()

        // Step 2: Skip confirm, try using token directly
        dashLog("Skipping confirm, trying token directly...")

        // Step 3: Try authenticated requests with token
        val endpoints = listOf(
            "/cgi-bin/currentpic.cgi",
            "/cgi-bin/getdeviceattr.cgi",
            "/cgi-bin/getAllMenu.cgi",
            "/cgi-bin/getwifi.cgi",
            "/cgi-bin/getsdstate.cgi",
            "/cgi-bin/getfilecount.cgi",
        )
        for (ep in endpoints) {
            val sTs = System.currentTimeMillis() / 1000
            val epWithTs = "$ep?&-timestamp=$sTs"
            val epSig = md5(epWithTs + token)
            val epFull = "http://$ip$epWithTs&-signkey=$epSig"
            val epRes = httpGetBytes(epFull)
            val body = epRes?.body?.let { String(it) } ?: "null"
            dashLog("AUTH $ep -> ${epRes?.status} (${epRes?.body?.size ?: 0}B) $body")
            if (epRes?.status == 200 && epRes.body.size > 10) {
                if (epRes.body[0] == 0xFF.toByte() && epRes.body[1] == 0xD8.toByte()) {
                    dashLog("JPEG found at $ep!")
                    return epFull
                }
            }
        }

        // If auth fails, try without auth (maybe camera is open)
        dashLog("Auth failed, trying raw...")
        for (ep in endpoints.take(1)) {
            val raw = httpGetBytes("http://$ip$ep")
            val body = raw?.body?.let { String(it) } ?: "null"
            dashLog("RAW $ep -> ${raw?.status} (${raw?.body?.size ?: 0}B) $body")
        }

        // Try without auth
        val rawResult = httpGetBytes("http://$ip/cgi-bin/currentpic.cgi")
        if (rawResult != null && rawResult.status == 200 && rawResult.body.size > 10) {
            dashLog("Snapshot raw OK: ${rawResult.body.size}B")
            return "http://$ip/cgi-bin/currentpic.cgi"
        }

        dashLog("Snapshot: empty or error")
        null
    } catch (e: Exception) {
        dashLog("Pair error: ${e.message?.take(40)}")
        null
    }
}

private fun httpGetBytes(urlString: String): PairResult? {
    return try {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        conn.connectTimeout = 3000; conn.readTimeout = 5000
        val code = conn.responseCode
        val body: ByteArray = if (code == 200) try { conn.inputStream.readBytes() } catch (_: Exception) { ByteArray(0) }
                   else try { conn.errorStream?.readBytes() ?: ByteArray(0) } catch (_: Exception) { ByteArray(0) }
        conn.disconnect()
        PairResult(code, body)
    } catch (e: Exception) { null }
}

private data class PairResult(val status: Int, val body: ByteArray)

@Composable
fun RearCamPopup(onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(Modifier.fillMaxSize()) {
            // X button
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}
