package com.dashboard.app

data class WeatherInfo(
    val temperature: Float = 0f,
    val feelsLike: Float = 0f,
    val humidity: Int = 0,
    val windSpeed: Float = 0f,
    val weatherCode: Int = 0,
    val locationName: String = ""
) {
    val condition: String get() = when (weatherCode) {
        in listOf(113) -> "Senin"
        in listOf(116) -> "Parțial noros"
        in listOf(119, 122) -> "Înnorat"
        in listOf(143, 248, 260) -> "Ceață"
        in listOf(176, 263, 266, 293, 296, 299, 302, 305, 308, 311, 314, 353, 356, 359) -> "Ploaie"
        in listOf(179, 227, 230, 323, 326, 329, 332, 335, 338, 371) -> "Ninsoare"
        in listOf(200, 386, 389) -> "Furtună"
        // Also support old Open-Meteo WMO codes
        0 -> "Senin"; 1, 2 -> "Parțial noros"; 3 -> "Înnorat"
        45, 48 -> "Ceață"; 51, 53, 55, 61, 63, 65 -> "Ploaie"
        71, 73, 75 -> "Ninsoare"; 95, 96, 99 -> "Furtună"
        else -> "Variabil"
    }

    val iconChar: String get() = when (weatherCode) {
        in listOf(113) -> "\u2600\uFE0F"
        in listOf(116) -> "\u26C5"
        in listOf(119, 122) -> "\u2601\uFE0F"
        in listOf(143, 248, 260) -> "\uD83C\uDF2B\uFE0F"
        in listOf(176, 263, 266, 293, 296, 299, 302, 305, 308, 311, 314, 353, 356, 359) -> "\uD83C\uDF27\uFE0F"
        in listOf(179, 227, 230, 323, 326, 329, 332, 335, 338, 371) -> "\u2744\uFE0F"
        in listOf(200, 386, 389) -> "\u26A1"
        0 -> "\u2600\uFE0F"; 1, 2 -> "\u26C5"; 3 -> "\u2601\uFE0F"
        45, 48 -> "\uD83C\uDF2B\uFE0F"; 51, 53, 55, 61, 63, 65 -> "\uD83C\uDF27\uFE0F"
        71, 73, 75 -> "\u2744\uFE0F"; 95, 96, 99 -> "\u26A1"
        else -> "\u2600\uFE0F"
    }
}
