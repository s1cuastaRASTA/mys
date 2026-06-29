package com.dashboard.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WazeNavInfo(
    val instruction: String = "",
    val distance: String = "",
    val eta: String = "",
    val remainingTime: String = "",
    val remainingDistance: String = "",
    val isNavigating: Boolean = false,
    val rawText: String = ""
)

object WazeNavigationHolder {
    private val _navInfo = MutableStateFlow(WazeNavInfo())
    val navInfo: StateFlow<WazeNavInfo> = _navInfo.asStateFlow()

    fun update(info: WazeNavInfo) { _navInfo.value = info }

    fun checkAccess(context: android.content.Context): Boolean {
        val enabled = android.provider.Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabled?.contains(context.packageName) == true
    }
}
