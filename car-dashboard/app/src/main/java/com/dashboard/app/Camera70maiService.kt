package com.dashboard.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class CameraState(
    val paired: Boolean = false,
    val connected: Boolean = false,
    val token: String = "",
    val status: String = "Deconectat",
    val snapshotUrl: String = "",
    val previewUrl: String = "",
)

object Camera70maiService {
    private const val TAG = "Camera70mai"
    private const val CAMERA_IP = "192.168.0.1"
    private const val MAGIC_KEY = "73VpsAfdety8FDd0"
    private const val USER_ID = "1935132"

    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    private var prefs: SharedPreferences? = null
    private var job: Job? = null

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("camera70mai", Context.MODE_PRIVATE)
        val savedToken = prefs?.getString("token", "") ?: ""
        if (savedToken.isNotEmpty()) {
            _state.value = _state.value.copy(paired = true, token = savedToken, status = "Token salvat")
        }
    }

    fun connect() {
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                _state.value = _state.value.copy(status = "Se conectează...")

                // Step 1: Check if we're paired
                val token = _state.value.token
                if (token.isEmpty()) {
                    val newToken = pairCamera()
                    if (newToken == null) {
                        _state.value = _state.value.copy(status = "Pairing eșuat")
                        return@launch
                    }
                    prefs?.edit()?.putString("token", newToken)?.apply()
                    _state.value = _state.value.copy(paired = true, token = newToken)
                }

                // Step 2: Try to get a snapshot
                val ts = System.currentTimeMillis() / 1000
                val snapUrl = "http://$CAMERA_IP/cgi-bin/currentpic.cgi?&-timestamp=$ts"
                val snapSig = md5(snapUrl + _state.value.token)
                val fullSnapUrl = "$snapUrl&-signkey=$snapSig"
                Log.d(TAG, "Snapshot URL: $fullSnapUrl")

                val snapResult = httpGet(fullSnapUrl)
                if (snapResult != null && snapResult.status == 200 && snapResult.body.isNotEmpty()) {
                    _state.value = _state.value.copy(
                        connected = true,
                        status = "Conectat!",
                        snapshotUrl = fullSnapUrl
                    )
                    Log.d(TAG, "Snapshot OK, body size=${snapResult.body.size}")
                } else {
                    Log.d(TAG, "Snapshot failed: ${snapResult?.status}")
                    // Try without auth
                    val rawResult = httpGet("http://$CAMERA_IP/cgi-bin/currentpic.cgi")
                    if (rawResult != null && rawResult.status == 200 && rawResult.body.isNotEmpty()) {
                        _state.value = _state.value.copy(
                            connected = true,
                            status = "Conectat (fara auth)",
                            snapshotUrl = "http://$CAMERA_IP/cgi-bin/currentpic.cgi"
                        )
                    } else {
                        _state.value = _state.value.copy(
                            connected = true,
                            status = "Conectat, imagine indisponibila"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connect error", e)
                _state.value = _state.value.copy(status = "Eroare: ${e.message?.take(40)}")
            }
        }
    }

    private suspend fun pairCamera(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val key1 = md5(USER_ID + MAGIC_KEY)
                val bindUrl = "http://$CAMERA_IP/cgi-bin/BindByBanya.cgi?&-usr=$USER_ID&-signkey=$key1"
                Log.d(TAG, "Binding: $bindUrl")
                val result = httpGet(bindUrl)
                if (result == null || result.status != 200) {
                    Log.e(TAG, "Bind failed: ${result?.status} ${result?.body?.let { String(it) }}")
                    return@withContext null
                }
                val body = String(result.body)
                Log.d(TAG, "Bind response: $body")
                // Parse token from JSON response: {"ResultCode":"0","Result":{"Token":"xxx","timestamp":"xxx"}}
                val tokenStart = body.indexOf("\"Token\":\"")
                if (tokenStart == -1) return@withContext null
                val tokenEnd = body.indexOf("\"", tokenStart + 9)
                val token = body.substring(tokenStart + 9, tokenEnd)

                // Confirm pairing
                val tsStr = body.substringAfter("\"timestamp\":\"").substringBefore("\"")
                val ts = tsStr.toLong()
                val key2 = md5("$ts$MAGIC_KEY")
                val confirmUrl = "http://$CAMERA_IP/cgi-bin/UserconfirmByBanya.cgi?&-timestamp=$ts&-signkey=$key2"
                Log.d(TAG, "Confirm URL: $confirmUrl")

                // Poll for confirmation (camera might need button press)
                for (i in 1..10) {
                    delay(500)
                    val confResult = httpGet(confirmUrl)
                    val confBody = confResult?.body?.let { String(it) }
                    Log.d(TAG, "Confirm attempt $i: $confBody")
                    if (confBody?.contains("\"ResultCode\":\"0\"") == true) {
                        Log.d(TAG, "Paired successfully! Token: $token")
                        return@withContext token
                    }
                }
                Log.e(TAG, "Pairing confirm timeout")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Pairing error", e)
                null
            }
        }
    }

    private fun httpGet(urlString: String): HttpResult? {
        return try {
            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 5000
            val code = conn.responseCode
            val body: ByteArray = if (code == 200) {
                try { conn.inputStream.readBytes() } catch (_: Exception) { ByteArray(0) }
            } else {
                try { conn.errorStream?.readBytes() ?: ByteArray(0) } catch (_: Exception) { ByteArray(0) }
            }
            conn.disconnect()
            HttpResult(code, body)
        } catch (e: Exception) {
            Log.d(TAG, "HTTP error for $urlString: ${e.message?.take(40)}")
            null
        }
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun disconnect() {
        job?.cancel()
        _state.value = _state.value.copy(connected = false, status = "Deconectat")
    }

    data class HttpResult(val status: Int, val body: ByteArray)
}
