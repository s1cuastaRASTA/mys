package com.dashboard.app

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

data class RadioStation(
    val name: String,
    val url: String,
    val genre: String = "",
    val iconUrl: String = ""
)

data class TripRecord(
    val date: String,
    val distance: Float,
    val duration: Long,
    val maxSpeed: Float,
    val avgSpeed: Float,
    val fuelUsed: Float
)

val romanianStations = listOf(
    RadioStation("Kiss FM", "https://live.kissfm.ro/kissfm.aacp", "Pop"),
    RadioStation("Virgin Radio", "https://astreaming.edi.ro:8443/VirginRadio_aac", "Pop"),
    RadioStation("Magic FM", "https://live.magicfm.ro/magicfm.aacp", "Pop"),
    RadioStation("Radio ZU", "http://zuicast.digitalag.ro:9420/zu", "Pop"),
    RadioStation("Europa FM", "https://astreaming.edi.ro:8443/EuropaFM_aac", "Pop"),
    RadioStation("Radio Impuls", "https://live.radio-impuls.ro/stream", "Pop"),
    RadioStation("Radio Tequila", "http://89.238.227.6:8022/stream", "Pop"),
    RadioStation("Romantic FM", "http://zuicast.digitalag.ro:9420/romanticfm", "Pop"),
    RadioStation("Vibe FM", "https://astreaming.vibefm.ro:8443/vibe_ro64", "Pop"),
    RadioStation("Smart Radio", "https://live.smartradio.ro:8443/live", "Pop"),
    RadioStation("Rock FM", "https://live.rockfm.ro/rockfm.aacp", "Rock"),
    RadioStation("Radio Guerrilla", "https://live.guerrillaradio.ro:8443/guerrilla.aac", "Rock"),
    RadioStation("Radio Deep", "http://live.radiodeep.ro:7500/", "Rock"),
    RadioStation("Radio GaGa", "http://rc.radiogaga.ro:8000/live", "Rock"),
    RadioStation("Gold FM", "http://80.86.106.110:8002/", "Oldies"),
    RadioStation("RFI România", "http://asculta.rfi.ro:9128/live.aac", "Știri"),
    RadioStation("Dance FM", "https://edge126.rcs-rds.ro/profm/dancefm.mp3", "Dance"),
    RadioStation("One FM", "https://live.onefm.ro/onefm.aacp", "Dance"),
    RadioStation("One World Radio", "https://playerservices.streamtheworld.com/api/livestream-redirect/OWR_INTERNATIONAL_ADP", "Dance"),
    RadioStation("Pro FM", "http://edge126.rdsnet.ro:84/profm/profm.mp3", "Hits"),
    RadioStation("Digi FM", "http://edge76.rdsnet.ro:84/digifm/digifm.mp3", "Hits"),
    RadioStation("Național FM", "http://live2.nationalfm.ro:8001/", "Hits"),
    RadioStation("Radio România Actualități", "http://89.238.227.6:8006/", "Public"),
    RadioStation("Radio Trinitas", "http://live.radiotrinitas.ro:8000/", "Public"),
    RadioStation("Radio România Muzical", "http://stream2.srr.ro:8022/", "Clasic"),
    RadioStation("Radio Clasic", "https://live.radioclasic.com/listen/radio_clasic_bach/live.mp3", "Clasic"),
    RadioStation("Manele FM", "http://a.fmradiomanele.ro:8054/stream", "Manele"),
    RadioStation("Radio Manele", "https://play.wrhradios.com/8044/stream", "Manele"),
    RadioStation("Radio Taraf", "http://asculta.radiotaraf.ro:7100/", "Manele"),
    RadioStation("Radio Stil Manele", "https://mp3.radiostill.ro:8888/stream", "Manele"),
    RadioStation("Sport Total FM", "https://livesptfm.com/SPTFM/Live/chunklist.m3u8", "Sport"),
    RadioStation("Itsy Bitsy", "http://live.itsybitsy.ro:8000/itsybitsy", "Copii"),
    RadioStation("Radio Doina", "http://89.43.138.116:8000/radiodoina.mp3", "Etno"),
    RadioStation("Radio Lăutaru", "http://live.radiolautaru.ro:9000/", "Etno"),
    RadioStation("Radio Etno", "https://radio.sonicpanel.ro:9300/", "Etno"),
    RadioStation("Antena Satelor", "http://89.238.227.6:8042/", "Etno"),
    RadioStation("Lăutaru Populara", "http://live.radiolautaru.ro:9000/", "Etno"),
    RadioStation("Liberty Populara", "https://hs1.radiolibertymp.ro/listen/lmppopulara/stream.mp3", "Etno"),
    RadioStation("Clasic Popular", "https://live.radioclasic.com/listen/radio_clasic_popular/live.mp3", "Etno"),
    RadioStation("Nostalgia FM", "https://live.radionostalgia.ro:8443/nostalgia.aac", "Oldies"),
    RadioStation("Urban FM", "https://urbanfm.ro:8777/live", "Urban"),
    RadioStation("Vocea Evangheliei", "http://91.216.75.116:8618/stream", "Religios"),
    RadioStation("București FM", "https://stream4.srr.ro:8443/bucuresti-fm", "Local"),
    RadioStation("Radio Cultural", "http://stream2.srr.ro:8012/", "Cultural"),
    RadioStation("Radio Seven", "http://80.86.106.32:8000/radio7.mp3", "Rock"),
    RadioStation("Digi 24 FM", "https://edge76.rcs-rds.ro/digifm/digi24fm.mp3", "Știri"),
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "DashboardVM"

    private val _speed = MutableStateFlow(0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _maxSpeed = MutableStateFlow(0f)
    val maxSpeed: StateFlow<Float> = _maxSpeed.asStateFlow()

    private val _avgSpeed = MutableStateFlow(0f)
    val avgSpeed: StateFlow<Float> = _avgSpeed.asStateFlow()

    private val _useObdSpeed = MutableStateFlow(false)
    val useObdSpeed: StateFlow<Boolean> = _useObdSpeed.asStateFlow()

    fun toggleSpeedSource() { _useObdSpeed.value = !_useObdSpeed.value }

    private val _pitch = MutableStateFlow(0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _roll = MutableStateFlow(0f)
    val roll: StateFlow<Float> = _roll.asStateFlow()

    private val _maxPitch = MutableStateFlow(0f)
    val maxPitch: StateFlow<Float> = _maxPitch.asStateFlow()

    private val _maxRoll = MutableStateFlow(0f)
    val maxRoll: StateFlow<Float> = _maxRoll.asStateFlow()

    private val _gForceX = MutableStateFlow(0f)
    val gForceX: StateFlow<Float> = _gForceX.asStateFlow()

    private val _gForceY = MutableStateFlow(0f)
    val gForceY: StateFlow<Float> = _gForceY.asStateFlow()

    private var pitchOffset = 0f
    private var rollOffset = 0f

    private val _currentStation = MutableStateFlow<RadioStation?>(null)
    val currentStation: StateFlow<RadioStation?> = _currentStation.asStateFlow()

    private val _isRadioPlaying = MutableStateFlow(false)
    val isRadioPlaying: StateFlow<Boolean> = _isRadioPlaying.asStateFlow()

    private val _compassHeading = MutableStateFlow(0f)
    val compassHeading: StateFlow<Float> = _compassHeading.asStateFlow()

    private val _gpsLatitude = MutableStateFlow(0.0)
    val gpsLatitude: StateFlow<Double> = _gpsLatitude.asStateFlow()

    private val _gpsLongitude = MutableStateFlow(0.0)
    val gpsLongitude: StateFlow<Double> = _gpsLongitude.asStateFlow()

    private val _gpsAltitude = MutableStateFlow(0.0)
    val gpsAltitude: StateFlow<Double> = _gpsAltitude.asStateFlow()

    private val _gpsAccuracy = MutableStateFlow(0f)
    val gpsAccuracy: StateFlow<Float> = _gpsAccuracy.asStateFlow()

    private val _maxAltitude = MutableStateFlow(0.0)
    val maxAltitude: StateFlow<Double> = _maxAltitude.asStateFlow()

    private val _fuelRate = MutableStateFlow(8f)
    val fuelRate: StateFlow<Float> = _fuelRate.asStateFlow()

    private val _tripDistance = MutableStateFlow(0f)
    val tripDistance: StateFlow<Float> = _tripDistance.asStateFlow()

    private val _tripElapsedTime = MutableStateFlow(0L)
    val tripElapsedTime: StateFlow<Long> = _tripElapsedTime.asStateFlow()

    private val _useMph = MutableStateFlow(false)
    val useMph: StateFlow<Boolean> = _useMph.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _brightness = MutableStateFlow(1.0f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _backgroundFile = MutableStateFlow("")
    val backgroundFile: StateFlow<String> = _backgroundFile.asStateFlow()

    private val _screenTimeoutMinutes = MutableStateFlow(0)
    val screenTimeoutMinutes: StateFlow<Int> = _screenTimeoutMinutes.asStateFlow()

    private val _volume = MutableStateFlow(0.8f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _speedLimit = MutableStateFlow(100f)
    val speedLimit: StateFlow<Float> = _speedLimit.asStateFlow()

    private val _speedLimitEnabled = MutableStateFlow(false)
    val speedLimitEnabled: StateFlow<Boolean> = _speedLimitEnabled.asStateFlow()

    private val _isYouTubeActive = MutableStateFlow(false)
    val isYouTubeActive: StateFlow<Boolean> = _isYouTubeActive.asStateFlow()

    private val _isYouTubeMinimized = MutableStateFlow(false)
    val isYouTubeMinimized: StateFlow<Boolean> = _isYouTubeMinimized.asStateFlow()

    private val _showWeather = MutableStateFlow(false)
    val showWeather: StateFlow<Boolean> = _showWeather.asStateFlow()

    private val _showFishing = MutableStateFlow(false)
    val showFishing: StateFlow<Boolean> = _showFishing.asStateFlow()

    private val _showTrip = MutableStateFlow(false)
    val showTrip: StateFlow<Boolean> = _showTrip.asStateFlow()
    private val _showRearCam = MutableStateFlow(false)
    val showRearCam: StateFlow<Boolean> = _showRearCam.asStateFlow()

    private val _startPage = MutableStateFlow(3)
    val startPage: StateFlow<Int> = _startPage.asStateFlow()
    private val _autoStartTrip = MutableStateFlow(false)
    val autoStartTrip: StateFlow<Boolean> = _autoStartTrip.asStateFlow()
    private val _displayName = MutableStateFlow("B 404 DKO")
    val displayName: StateFlow<String> = _displayName.asStateFlow()
    private val _showClockOnStart = MutableStateFlow(false)
    val showClockOnStart: StateFlow<Boolean> = _showClockOnStart.asStateFlow()
    private val _showWeatherOnStart = MutableStateFlow(false)
    val showWeatherOnStart: StateFlow<Boolean> = _showWeatherOnStart.asStateFlow()
    private val _use24hClock = MutableStateFlow(true)
    val use24hClock: StateFlow<Boolean> = _use24hClock.asStateFlow()
    private val _autoPlayRadio = MutableStateFlow(false)
    val autoPlayRadio: StateFlow<Boolean> = _autoPlayRadio.asStateFlow()
    private val _overlayAlpha = MutableStateFlow(0.55f)
    val overlayAlpha: StateFlow<Float> = _overlayAlpha.asStateFlow()

    // Music player state (all using exoPlayer now)
    private val _isMusicPlaying = MutableStateFlow(false)
    val isMusicPlaying: StateFlow<Boolean> = _isMusicPlaying.asStateFlow()
    private val _currentMusicFile = MutableStateFlow<java.io.File?>(null)
    val currentMusicFile: StateFlow<java.io.File?> = _currentMusicFile.asStateFlow()
    private val _currentMusicName = MutableStateFlow("")
    val currentMusicName: StateFlow<String> = _currentMusicName.asStateFlow()

    fun setMusicVolume(vol: Float) { exoPlayer.volume = vol }

    fun playMusic(file: java.io.File, name: String) {
        try {
            stopRadio()
            exoPlayer.stop()
            val mediaItem = MediaItem.fromUri(file.toURI().toString())
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
            _isMusicPlaying.value = true
            _currentMusicFile.value = file
            _currentMusicName.value = name
        } catch (e: Exception) {
            Log.e(TAG, "playMusic failed: ${e.message}")
            _isMusicPlaying.value = false; _currentMusicName.value = "Eroare: ${file.name}"
        }
    }

    fun toggleMusic() {
        if (_isMusicPlaying.value) { exoPlayer.pause(); _isMusicPlaying.value = false }
        else { exoPlayer.play(); _isMusicPlaying.value = true }
    }

    fun playRandomMusic(folder: java.io.File? = null) {
        val root = java.io.File("/storage/emulated/0/Music")
        val dir = folder ?: root
        val songs = if (dir == root) findAllSongs(dir) else loadSongsFromDir(dir)
        if (songs.isNotEmpty()) {
            val song = songs.random()
            playMusic(song, song.nameWithoutExtension)
        }
    }

    private fun loadSongsFromDir(dir: java.io.File): List<java.io.File> {
        return dir.listFiles()?.filter { !it.isDirectory && it.extension.lowercase() in listOf("mp3", "mp4", "wav", "flac", "aac", "ogg", "m4a", "wma") }?.toList() ?: emptyList()
    }

    private fun findAllSongs(dir: java.io.File): List<java.io.File> {
        val result = mutableListOf<java.io.File>()
        dir.listFiles()?.forEach { f ->
            if (f.isDirectory && !f.name.startsWith(".")) result.addAll(findAllSongs(f))
            else if (f.extension.lowercase() in listOf("mp3", "mp4", "wav", "flac", "aac", "ogg", "m4a", "wma")) result.add(f)
        }
        return result
    }

    fun stopMusic() {
        exoPlayer.stop()
        _isMusicPlaying.value = false; _currentMusicFile.value = null; _currentMusicName.value = ""
    }

    fun playNextMusic() {
        val current = _currentMusicFile.value ?: return
        val parent = current.parentFile ?: java.io.File("/storage/emulated/0/Music")
        // Try current folder first, fall back to all songs
        var songs = loadSongsFromDir(parent)
        if (songs.isEmpty()) songs = findAllSongs(java.io.File("/storage/emulated/0/Music"))
        if (songs.isEmpty()) return
        val idx = songs.indexOfFirst { it.absolutePath == current.absolutePath }
        val next = if (idx >= 0 && idx < songs.size - 1) songs[idx + 1] else songs.first()
        playMusic(next, next.nameWithoutExtension)
    }

    fun playPrevMusic() {
        val current = _currentMusicFile.value ?: return
        val parent = current.parentFile ?: java.io.File("/storage/emulated/0/Music")
        var songs = loadSongsFromDir(parent)
        if (songs.isEmpty()) songs = findAllSongs(java.io.File("/storage/emulated/0/Music"))
        if (songs.isEmpty()) return
        val idx = songs.indexOfFirst { it.absolutePath == current.absolutePath }
        val prev = if (idx > 0) songs[idx - 1] else songs.last()
        playMusic(prev, prev.nameWithoutExtension)
    }

    private val _selectedCbChannel = MutableStateFlow(0)
    val selectedCbChannel: StateFlow<Int> = _selectedCbChannel.asStateFlow()

    private val _isCbPlaying = MutableStateFlow(false)
    val isCbPlaying: StateFlow<Boolean> = _isCbPlaying.asStateFlow()

    private val _cbPlayingFreq = MutableStateFlow(0)
    val cbPlayingFreq: StateFlow<Int> = _cbPlayingFreq.asStateFlow()

    private val _cbPlayingServerUrl = MutableStateFlow("")
    val cbPlayingServerUrl: StateFlow<String> = _cbPlayingServerUrl.asStateFlow()

    private val _cbPlayingUrlParam = MutableStateFlow("tune")
    val cbPlayingUrlParam: StateFlow<String> = _cbPlayingUrlParam.asStateFlow()

    private val _favoriteStations = MutableStateFlow<Set<String>>(emptySet())
    val favoriteStations: StateFlow<Set<String>> = _favoriteStations.asStateFlow()

    private val _savedCbChannels = MutableStateFlow<Set<String>>(emptySet())
    val savedCbChannels: StateFlow<Set<String>> = _savedCbChannels.asStateFlow()

    private val prefs = application.getSharedPreferences("dash_prefs", Context.MODE_PRIVATE)

    private val _tripHistory = MutableStateFlow<List<TripRecord>>(emptyList())
    val tripHistory: StateFlow<List<TripRecord>> = _tripHistory.asStateFlow()

    init {
        val savedStations = prefs.getStringSet("fav_stations", emptySet())
        if (savedStations != null) _favoriteStations.value = savedStations
        val savedCb = prefs.getStringSet("saved_cb", emptySet())
        if (savedCb != null) _savedCbChannels.value = savedCb
        _backgroundFile.value = prefs.getString("bg_file", "") ?: ""
        pitchOffset = prefs.getFloat("pitch_offset", 0f)
        rollOffset = prefs.getFloat("roll_offset", 0f)
        _startPage.value = prefs.getInt("start_page", 3)
        _autoStartTrip.value = prefs.getBoolean("auto_trip", false)
        _displayName.value = prefs.getString("display_name", "B 404 DKO") ?: "B 404 DKO"
        _showClockOnStart.value = prefs.getBoolean("clock_start", false)
        _showWeatherOnStart.value = prefs.getBoolean("weather_start", false)
        _use24hClock.value = prefs.getBoolean("24h_clock", true)
        _autoPlayRadio.value = prefs.getBoolean("autoplay_radio", false)
        _overlayAlpha.value = prefs.getFloat("overlay_alpha", 0.55f)
        _brightness.value = prefs.getFloat("brightness", 1.0f)
        val tripsJson = prefs.getString("trip_history", null)
        if (tripsJson != null) {
            try {
                val arr = org.json.JSONArray(tripsJson)
                val list = mutableListOf<TripRecord>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(TripRecord(o.getString("date"), o.getDouble("dist").toFloat(),
                        o.getLong("dur"), o.getDouble("max").toFloat(),
                        o.getDouble("avg").toFloat(), o.getDouble("fuel").toFloat()))
                }
                _tripHistory.value = list
            } catch (_: Exception) {}
        }
    }

    fun toggleFavorite(name: String) {
        val current = _favoriteStations.value.toMutableSet()
        if (current.contains(name)) current.remove(name) else current.add(name)
        _favoriteStations.value = current
        prefs.edit().putStringSet("fav_stations", current).apply()
    }

    fun toggleSavedCbChannel(key: String) {
        val current = _savedCbChannels.value.toMutableSet()
        if (current.contains(key)) current.remove(key) else current.add(key)
        _savedCbChannels.value = current
        prefs.edit().putStringSet("saved_cb", current).apply()
    }

    private var tripStarted = false
    private var tripPaused = false

    private val _isTripActive = MutableStateFlow(false)
    val isTripActive: StateFlow<Boolean> = _isTripActive.asStateFlow()
    private val _isTripPaused = MutableStateFlow(false)
    val isTripPaused: StateFlow<Boolean> = _isTripPaused.asStateFlow()

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build().apply {
        playWhenReady = true
        addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                _isRadioPlaying.value = false; _currentStation.value = null
                _isMusicPlaying.value = false
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && _isMusicPlaying.value) {
                    _isMusicPlaying.value = false
                    playNextMusic()
                }
            }
        })
    }

    fun updateSpeed(speed: Float) {
        _speed.value = speed
        if (speed > _maxSpeed.value) _maxSpeed.value = speed
        if (_tripElapsedTime.value > 0) {
            _avgSpeed.value = (_tripDistance.value / 1000f) / (_tripElapsedTime.value / 3600f)
        }
    }

    fun updateInclination(pitch: Float, roll: Float) {
        val adjustedPitch = pitch - pitchOffset
        val adjustedRoll = roll - rollOffset
        _pitch.value = adjustedPitch
        _roll.value = adjustedRoll
        if (abs(adjustedPitch) > _maxPitch.value) _maxPitch.value = abs(adjustedPitch)
        if (abs(adjustedRoll) > _maxRoll.value) _maxRoll.value = abs(adjustedRoll)
    }

    fun updateGForce(x: Float, y: Float) {
        _gForceX.value = x
        _gForceY.value = y
    }

    fun calibrateInclination() {
        pitchOffset += _pitch.value
        rollOffset += _roll.value
        _maxPitch.value = 0f
        _maxRoll.value = 0f
        prefs.edit().putFloat("pitch_offset", pitchOffset).putFloat("roll_offset", rollOffset).apply()
    }

    fun updateGps(lat: Double, lon: Double, alt: Double = 0.0) {
        _gpsLatitude.value = lat
        _gpsLongitude.value = lon
        _gpsAltitude.value = alt
        if (alt > _maxAltitude.value) _maxAltitude.value = alt
    }

    fun addTripDistance(meters: Float) {
        _tripDistance.value += meters
    }

    fun startTrip() {
        if (!tripStarted) {
            tripStarted = true; tripPaused = false
            _isTripActive.value = true; _isTripPaused.value = false
            viewModelScope.launch {
                while (tripStarted) {
                    delay(1000)
                    if (!tripPaused) _tripElapsedTime.value++
                }
            }
        }
    }
    fun pauseTrip() { tripPaused = true; _isTripPaused.value = true }
    fun resumeTrip() { tripPaused = false; _isTripPaused.value = false }
    fun stopTrip() {
        if (tripStarted) {
            saveTripToHistory()
            _tripDistance.value = 0f; _tripElapsedTime.value = 0L
            _maxSpeed.value = 0f; _avgSpeed.value = 0f; _maxAltitude.value = 0.0
            tripStarted = false; tripPaused = false
            _isTripActive.value = false; _isTripPaused.value = false
        }
    }
    fun resetTrip() {
        if (tripStarted) stopTrip()
        _tripDistance.value = 0f; _tripElapsedTime.value = 0L
        _maxSpeed.value = 0f; _avgSpeed.value = 0f; _maxAltitude.value = 0.0
        tripStarted = false; tripPaused = false
        _isTripActive.value = false; _isTripPaused.value = false
    }

    private fun saveTripToHistory() {
        val fuel = if (_fuelRate.value > 0 && _tripDistance.value > 0) (_fuelRate.value / 100) * (_tripDistance.value / 1000) else 0f
        val record = TripRecord(
            date = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm")),
            distance = _tripDistance.value,
            duration = _tripElapsedTime.value,
            maxSpeed = _maxSpeed.value,
            avgSpeed = _avgSpeed.value,
            fuelUsed = fuel
        )
        val history = _tripHistory.value.toMutableList()
        history.add(0, record)
        if (history.size > 5) history.removeAt(5)
        _tripHistory.value = history
        val json = org.json.JSONArray()
        history.forEach { t ->
            json.put(org.json.JSONObject().apply {
                put("date", t.date); put("dist", t.distance.toDouble())
                put("dur", t.duration); put("max", t.maxSpeed.toDouble())
                put("avg", t.avgSpeed.toDouble()); put("fuel", t.fuelUsed.toDouble())
            })
        }
        prefs.edit().putString("trip_history", json.toString()).apply()
    }

    fun updateGpsAccuracy(accuracy: Float) { _gpsAccuracy.value = accuracy }
    fun setFuelRate(value: Float) { _fuelRate.value = value }
    fun setBrightness(value: Float) { _brightness.value = value; prefs.edit().putFloat("brightness", value).apply() }
    fun setBackground(filename: String) {
        _backgroundFile.value = filename
        prefs.edit().putString("bg_file", filename).apply()
    }
    fun setScreenTimeout(minutes: Int) { _screenTimeoutMinutes.value = minutes }
    fun setVolume(value: Float) {
        _volume.value = value
        exoPlayer.volume = value
    }
    fun setSpeedLimit(value: Float) { _speedLimit.value = value }
    fun toggleSpeedLimit() { _speedLimitEnabled.value = !_speedLimitEnabled.value }

    val wazeNavInfo: StateFlow<WazeNavInfo> = WazeNavigationHolder.navInfo

    fun playStation(station: RadioStation) {
        try {
            stopMusic()
            exoPlayer.stop()
            val mediaItem = MediaItem.fromUri(station.url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
            _isRadioPlaying.value = true
            _currentStation.value = station
        } catch (e: Exception) {
            _isRadioPlaying.value = false
            _currentStation.value = null
        }
    }
    fun stopRadio() {
        exoPlayer.stop()
        _isRadioPlaying.value = false
        _currentStation.value = null
    }
    fun playNextStation() {
        val current = _currentStation.value
        val idx = romanianStations.indexOf(current)
        val next = if (idx >= 0 && idx < romanianStations.size - 1) romanianStations[idx + 1] else romanianStations[0]
        playStation(next)
    }
    fun playPreviousStation() {
        val current = _currentStation.value
        val idx = romanianStations.indexOf(current)
        val prev = if (idx > 0) romanianStations[idx - 1] else romanianStations.last()
        playStation(prev)
    }
    fun updateCompassHeading(heading: Float) { _compassHeading.value = heading }
    fun toggleUnit() { _useMph.value = !_useMph.value }
    fun toggleTheme() { _isDarkTheme.value = !_isDarkTheme.value }
    fun toggleYouTube(active: Boolean) {
        _isYouTubeActive.value = active
        if (!active) _isYouTubeMinimized.value = false
    }
    fun minimizeYouTube() { _isYouTubeMinimized.value = true }
    fun restoreYouTube() { _isYouTubeMinimized.value = false }
    fun toggleWeather() { _showWeather.value = !_showWeather.value }
    fun toggleFishing() { _showFishing.value = !_showFishing.value }
    fun toggleTrip() { _showTrip.value = !_showTrip.value }
    fun toggleRearCam() { _showRearCam.value = !_showRearCam.value }
    fun open70maiApp() {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                setClassName("com.banyac.midrive.app.eu", "com.banyac.midrive.app.start.splash.SplashActivity")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<android.app.Application>().startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "open70maiApp failed: ${e.message}")
        }
    }
    fun setStartPage(page: Int) { _startPage.value = page.coerceIn(0, 8); prefs.edit().putInt("start_page", _startPage.value).apply() }
    fun toggleAutoTrip() { _autoStartTrip.value = !_autoStartTrip.value; prefs.edit().putBoolean("auto_trip", _autoStartTrip.value).apply() }
    fun setDisplayName(name: String) { _displayName.value = name; prefs.edit().putString("display_name", name).apply() }
    fun toggleClockOnStart() { _showClockOnStart.value = !_showClockOnStart.value; prefs.edit().putBoolean("clock_start", _showClockOnStart.value).apply() }
    fun toggleWeatherOnStart() { _showWeatherOnStart.value = !_showWeatherOnStart.value; prefs.edit().putBoolean("weather_start", _showWeatherOnStart.value).apply() }
    fun toggle24hClock() { _use24hClock.value = !_use24hClock.value; prefs.edit().putBoolean("24h_clock", _use24hClock.value).apply() }
    fun toggleAutoPlayRadio() { _autoPlayRadio.value = !_autoPlayRadio.value; prefs.edit().putBoolean("autoplay_radio", _autoPlayRadio.value).apply() }
    fun setOverlayAlpha(a: Float) { _overlayAlpha.value = a; prefs.edit().putFloat("overlay_alpha", a).apply() }
    fun selectCbChannel(index: Int) { _selectedCbChannel.value = index }
    fun playCbChannel(freqKhz: Int, serverUrl: String, urlParam: String) {
        _cbPlayingFreq.value = freqKhz
        _cbPlayingServerUrl.value = serverUrl
        _cbPlayingUrlParam.value = urlParam
        _isCbPlaying.value = true
    }
    fun stopCb() { _isCbPlaying.value = false }
}
