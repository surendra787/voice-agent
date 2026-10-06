package com.example.model

enum class ActionType(val displayName: String, val category: String) {
    WIFI_TOGGLE("Toggle Wi-Fi", "Connectivity"),
    HOTSPOT_TOGGLE("Toggle Hotspot Settings", "Connectivity"),
    SEND_SMS("Send Automated SMS", "Messaging"),
    PHONE_CALL("Trigger Phone Call", "Communication"),
    BATTERY_SAVER_MODE("Battery Saver Profile", "Power & Device"),
    SEND_NOTIFICATION("Sound/Reminder Alert", "Productivity")
}

enum class TriggerType(val displayName: String) {
    TIME_OF_DAY("Scheduled Time"),
    LOCATION("Geofence / Location"),
    CALENDAR_EVENT("Upcoming Calendar Event"),
    MANUAL("Manual Quick Action")
}

data class AutomationRule(
    val id: String,
    val title: String,
    val description: String,
    val isEnabled: Boolean = true,
    val triggerType: TriggerType,
    // Trigger params:
    val timeHour: Int = 9,
    val timeMinute: Int = 0,
    val repeatDays: List<Int> = listOf(1, 2, 3, 4, 5), // Calendar.MONDAY .. SUNDAY
    val locationName: String = "Office / Home",
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val radiusMeters: Float = 200f,
    val triggerOnEnter: Boolean = true,
    val calendarEventKeyword: String = "Meeting",

    // Action params:
    val actionType: ActionType,
    val targetState: Boolean = true, // on/off for toggles
    val targetPhoneNumber: String = "",
    val messageBody: String = "",
    val soundAlertLevel: String = "Chime", // Low, Normal, High
    val lastExecutedTimestamp: Long? = null,
    val executionCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class ExecutionLog(
    val id: String,
    val ruleId: String,
    val ruleTitle: String,
    val actionType: ActionType,
    val timestamp: Long,
    val isSuccess: Boolean,
    val statusMessage: String,
    val triggerSource: String
)

data class CalendarEventItem(
    val id: Long,
    val title: String,
    val description: String,
    val startTime: Long,
    val endTime: Long,
    val location: String,
    val isAllDay: Boolean
)

data class DeviceStatus(
    val isWifiEnabled: Boolean,
    val isHotspotActive: Boolean,
    val batteryPercent: Int,
    val isPowerSaveMode: Boolean,
    val isNetworkConnected: Boolean
)
