package com.dashboard.app

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class WazeNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.waze") return
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE, "")
        val text = extras.getString(Notification.EXTRA_TEXT, "")
        val bigText = extras.getString(Notification.EXTRA_BIG_TEXT, "")
        val combined = listOfNotNull(title, text, bigText).joinToString(" | ")

        var remainingDistance = ""
        var remainingTime = ""
        var eta = ""
        var instruction = ""

        for (part in listOfNotNull(title, text, bigText)) {
            val cleaned = part.trim()
            when {
                cleaned.matches(Regex(".*\\d+(\\.\\d+)?\\s*(km|Km|KM|m)\\b.*")) && remainingDistance.isEmpty() ->
                    remainingDistance = cleaned
                (cleaned.contains("min", true) || cleaned.contains("oră", true) || cleaned.contains("hr", true)) && remainingTime.isEmpty() ->
                    remainingTime = cleaned
                cleaned.matches(Regex(".*\\d{1,2}:\\d{2}.*")) && eta.isEmpty() ->
                    eta = cleaned.trim()
                instruction.isEmpty() && cleaned.length > 2 && !cleaned.contains("Waze", true) ->
                    instruction = cleaned
            }
        }

        val hasNavData = remainingDistance.isNotEmpty() || remainingTime.isNotEmpty() || eta.isNotEmpty()

        WazeNavigationHolder.update(
            WazeNavInfo(
                instruction = instruction,
                remainingDistance = remainingDistance,
                remainingTime = remainingTime,
                eta = eta.trimStart('•', '·', ' ', '|'),
                isNavigating = hasNavData,
                rawText = combined
            )
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == "com.waze") {
            WazeNavigationHolder.update(WazeNavInfo())
        }
    }
}
