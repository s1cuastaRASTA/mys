package com.dashboard.app

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

import com.dashboard.app.ui.screens.DashboardScreen
import com.dashboard.app.ui.theme.DashboardTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener, ImageLoaderFactory {

    private lateinit var viewModel: DashboardViewModel
    private lateinit var imageLoader: ImageLoader
    
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .build()
    }
    private lateinit var sensorManager: SensorManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var lastLatLon: Pair<Double, Double>? = null

    private var filteredPitch = 0f
    private var filteredRoll = 0f
    private val pitchAlpha = 0.02f
    private val rollAlpha = 0.008f
    private val rollDeadZone = 0.5f

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        checkPermissions()

        if (viewModel.autoPlayRadio.value) {
            viewModel.playStation(romanianStations.random())
        }

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val brightness by viewModel.brightness.collectAsState()
            
            // Aplicam luminozitatea pe fereastra
            LaunchedEffect(brightness) {
                val params = window.attributes
                params.screenBrightness = brightness
                window.attributes = params
            }

            DashboardTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        updateKeepScreenOn()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                computePitchRoll(event.values)
                viewModel.updateGForce(event.values[0] / 9.81f, event.values[1] / 9.81f)
                computeOrientation()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                computeOrientation()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun computePitchRoll(values: FloatArray) {
        val ax = values[0].toDouble()
        val ay = values[1].toDouble()
        val az = values[2].toDouble()
        val rawPitch = Math.toDegrees(
            atan2(-ax, sqrt(ay * ay + az * az))
        ).toFloat()
        val rawRoll = Math.toDegrees(
            atan2(ay, sqrt(ax * ax + az * az))
        ).toFloat()
        filteredPitch += (rawPitch - filteredPitch) * pitchAlpha
        val delta = rawRoll - filteredRoll
        if (abs(delta) > rollDeadZone) {
            filteredRoll += delta * rollAlpha
        }
        viewModel.updateInclination(filteredPitch, filteredRoll)
    }

    private fun computeOrientation() {
        if (!SensorManager.getRotationMatrix(
                rotationMatrix, null,
                accelerometerReading, magnetometerReading
            )
        ) return
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val heading = (azimuth + 360) % 360
        viewModel.updateCompassHeading(heading)
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }

    private fun checkPermissions() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED -> startLocationUpdates()
            else ->         locationPermissionRequest.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
        if (Build.VERSION.SDK_INT <= 32) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED -> { /* OK */ }
                else -> requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 101
                )
            }
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 500L
        ).apply {
            setMinUpdateIntervalMillis(200L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (viewModel.useObdSpeed.value && ObdService.isConnected.value) {
                        viewModel.updateSpeed(ObdService.data.value.speed.toFloat())
                    } else {
                        val speedMps = location.speed
                        val speedKmh = if (speedMps >= 0) speedMps * 3.6f else 0f
                        viewModel.updateSpeed(speedKmh)
                    }

                    val lat = location.latitude
                    val lon = location.longitude
                    val alt = location.altitude
                    viewModel.updateGps(lat, lon, alt)
                    viewModel.updateGpsAccuracy(location.accuracy)

                    if (lastLatLon != null) {
                        val distance = haversine(
                            lastLatLon!!.first, lastLatLon!!.second, lat, lon
                        )
                        viewModel.addTripDistance(distance)
                    }
                    lastLatLon = Pair(lat, lon)
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        }
    }

    private fun updateKeepScreenOn() {
        val timeout = viewModel.screenTimeoutMinutes.value
        if (timeout == 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2.0).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2.0).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return (R * c).toFloat()
    }
}
