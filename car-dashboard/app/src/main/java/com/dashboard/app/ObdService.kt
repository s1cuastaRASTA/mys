package com.dashboard.app

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.*

data class ObdData(
    val rpm: Int = 0,
    val speed: Int = 0,
    val coolantTemp: Int = 0,
    val fuelLevel: Float = 0f,
    val throttle: Float = 0f,
    val engineLoad: Float = 0f,
)

object ObdService {
    private const val TAG = "ObdService"
    private val OBD_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val logFile = java.io.File("/storage/emulated/0/Documents/dash_obd_log.txt")

    private fun logToFile(msg: String) {
        Log.d(TAG, msg)
        try {
            logFile.parentFile?.mkdirs()
            logFile.appendText("${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date())} $msg\n")
        } catch (_: Exception) {}
    }

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _data = MutableStateFlow(ObdData())
    val data: StateFlow<ObdData> = _data.asStateFlow()

    private val _status = MutableStateFlow("Deconectat")
    val status: StateFlow<String> = _status.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var reader: BufferedReader? = null
    private var writer: OutputStreamWriter? = null
    private var job: Job? = null

    fun connect() {
        job?.cancel()
        _status.value = "Se caută OBD..."
        logToFile("=== OBD CONNECT STARTED ===")
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter == null) {
                    _status.value = "Bluetooth indisponibil"
                    logToFile("No Bluetooth adapter")
                    return@launch
                }
                if (!adapter.isEnabled) {
                    _status.value = "Pornește Bluetooth"
                    logToFile("Bluetooth not enabled")
                    return@launch
                }

                _status.value = "Căutare dispozitive..."
                logToFile("Bonded devices:")
                for (d in adapter.bondedDevices) {
                    logToFile("  ${d.name} - ${d.address}")
                }
                val device = findObdDevice(adapter)
                if (device == null) {
                    _status.value = "OBD negăsit. E împerecheat?"
                    logToFile("OBD not found in bonded devices")
                    return@launch
                }

                _status.value = "Conectare la ${device.name}..."
                logToFile("Connecting to ${device.name} (${device.address})")
                socket = device.createRfcommSocketToServiceRecord(OBD_UUID)
                socket?.connect()
                logToFile("Socket connected!")
                reader = BufferedReader(InputStreamReader(socket!!.inputStream))
                writer = OutputStreamWriter(socket!!.outputStream)

                // Init ELM327 with retry
                for (attempt in 1..3) {
                    val atz = sendCommand("ATZ"); logToFile("ATZ -> $atz")
                    if (atz != null) break
                    delay(1000)
                    logToFile("ATZ retry $attempt/3")
                }
                delay(600)
                logToFile("ATE0 -> ${sendCommand("ATE0")}"); delay(200)
                logToFile("ATL0 -> ${sendCommand("ATL0")}"); delay(200)
                logToFile("ATSP0 -> ${sendCommand("ATSP0")}"); delay(300)

                // Flush any buffered data
                while (reader?.ready() == true) reader?.readLine()
                logToFile("Flushed buffered data")

                // Quick connectivity test
                val testResponse = sendCommand("0100")
                logToFile("PID 0100 test -> $testResponse")
                if (testResponse == null) {
                    _status.value = "OBD nu răspunde la PID. Verifică contactul AUTO!"
                    logToFile("OBD not responding to PIDs - ignition off?")
                    return@launch
                }

                _isConnected.value = true
                _status.value = "Conectat: ${device.name}"
                logToFile("=== OBD CONNECTED SUCCESSFULLY ===")

                while (isActive) {
                    try {
                        val rpm = queryPid("010C")?.let { ((it[0].toInt() and 0xFF) * 256 + (it[1].toInt() and 0xFF)) / 4 } ?: 0
                        val speed = queryPid("010D")?.let { it[0].toInt() and 0xFF } ?: 0
                        val temp = queryPid("0105")?.let { (it[0].toInt() and 0xFF) - 40 } ?: 0
                        val fuel = queryPid("012F")?.let { ((it[0].toInt() and 0xFF) * 100f / 255f) } ?: 0f
                        val throttle = queryPid("0111")?.let { ((it[0].toInt() and 0xFF) * 100f / 255f) } ?: 0f
                        val load = queryPid("0104")?.let { ((it[0].toInt() and 0xFF) * 100f / 255f) } ?: 0f

                        _data.value = ObdData(rpm = rpm, speed = speed, coolantTemp = temp, fuelLevel = fuel, throttle = throttle, engineLoad = load)
                    } catch (e: Exception) { logToFile("Poll error: ${e.message}") }
                    delay(600)
                }
            } catch (e: Exception) {
                logToFile("BT connect failed: ${e.message}")
                _status.value = "Eroare: ${e.message?.take(40) ?: "conexiune"}"
                disconnect()
            }
        }
    }

    private fun findObdDevice(adapter: BluetoothAdapter): BluetoothDevice? {
        for (device in adapter.bondedDevices) {
            val name = device.name?.lowercase(Locale.ROOT) ?: continue
            if (name.contains("obd") || name.contains("clkdevices") || name.contains("v-link") || name.contains("elm")) return device
        }
        return adapter.bondedDevices.firstOrNull()
    }

    fun disconnect() {
        job?.cancel(); job = null
        try { writer?.close() } catch (_: Exception) {}
        try { reader?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        socket = null; reader = null; writer = null
        _isConnected.value = false; _status.value = "Deconectat"
    }

    private suspend fun sendCommand(cmd: String): String? {
        return try {
            writer?.write("$cmd\r\n"); writer?.flush()
            withTimeout(3000L) { readResponse() }
        } catch (e: Exception) { null }
    }

    private fun readResponse(): String {
        val sb = StringBuilder()
        val r = reader ?: return ""
        var line = r.readLine() ?: return ""
        while (line != null && line != ">" && !line.startsWith(">")) {
            val t = line.trim()
            if (t.isNotEmpty() && t != "SEARCHING..." && !t.startsWith("AT") && !t.startsWith("OK")) sb.append(t)
            line = r.readLine()
        }
        val result = sb.toString()
        if (result.isNotEmpty()) logToFile("response: $result")
        return result
    }

    private suspend fun queryPid(pid: String): ByteArray? {
        try {
            val resp = sendCommand(pid) ?: return null
            val hex = resp.replace(" ", "").uppercase()
            logToFile("PID $pid -> raw='$resp' hex='$hex'")
            if (hex == "NODATA") { logToFile("PID $pid: NO DATA from ECU"); return null }
            if (hex.length < 6 || !hex.startsWith("41")) { logToFile("PID $pid: invalid response: $hex"); return null }
            val len = (hex.length - 4) / 2; val data = ByteArray(len)
            for (i in 0 until len) {
                val s = 4 + i * 2
                if (s + 2 > hex.length) break
                data[i] = hex.substring(s, s + 2).toInt(16).toByte()
            }
            return data
        } catch (e: Exception) { logToFile("PID $pid parse error: ${e.message}"); return null }
    }
}
