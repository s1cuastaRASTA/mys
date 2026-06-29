package com.dashboard.app

data class RouteStep(
    val instruction: String,
    val distance: String,
    val duration: String,
    val lat: Double,
    val lon: Double
)

data class RouteInfo(
    val destination: String = "",
    val totalDistance: String = "",
    val totalDuration: String = "",
    val steps: List<RouteStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val polyline: String = "",
    val isNavigating: Boolean = false,
    val status: String = "",
    val destinationLat: Double = 0.0,
    val destinationLon: Double = 0.0
)
