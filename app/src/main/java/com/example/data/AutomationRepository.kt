package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ActionType
import com.example.model.AutomationRule
import com.example.model.ExecutionLog
import com.example.model.TriggerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AutomationRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("autopulse_rules_prefs", Context.MODE_PRIVATE)

    private val _rulesFlow = MutableStateFlow<List<AutomationRule>>(emptyList())
    val rulesFlow: StateFlow<List<AutomationRule>> = _rulesFlow.asStateFlow()

    private val _logsFlow = MutableStateFlow<List<ExecutionLog>>(emptyList())
    val logsFlow: StateFlow<List<ExecutionLog>> = _logsFlow.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val rulesJson = prefs.getString(KEY_RULES, null)
        val logsJson = prefs.getString(KEY_LOGS, null)

        val loadedRules = if (rulesJson.isNullOrBlank()) {
            getDefaultRules()
        } else {
            deserializeRules(rulesJson)
        }
        _rulesFlow.value = loadedRules

        val loadedLogs = if (logsJson.isNullOrBlank()) {
            getDefaultLogs()
        } else {
            deserializeLogs(logsJson)
        }
        _logsFlow.value = loadedLogs

        // If newly seeded default rules, persist them
        if (rulesJson.isNullOrBlank()) {
            saveRules(loadedRules)
        }
        if (logsJson.isNullOrBlank()) {
            saveLogs(loadedLogs)
        }
    }

    fun getAllRules(): List<AutomationRule> = _rulesFlow.value

    fun getRuleById(id: String): AutomationRule? = _rulesFlow.value.find { it.id == id }

    fun upsertRule(rule: AutomationRule) {
        val current = _rulesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == rule.id }
        if (index >= 0) {
            current[index] = rule
        } else {
            current.add(0, rule)
        }
        _rulesFlow.value = current
        saveRules(current)
    }

    fun toggleRuleEnabled(id: String): AutomationRule? {
        val current = _rulesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = current[index]
            val updated = old.copy(isEnabled = !old.isEnabled)
            current[index] = updated
            _rulesFlow.value = current
            saveRules(current)
            return updated
        }
        return null
    }

    fun deleteRule(id: String) {
        val current = _rulesFlow.value.filter { it.id != id }
        _rulesFlow.value = current
        saveRules(current)
    }

    fun recordExecution(ruleId: String, log: ExecutionLog) {
        // Update execution count on rule
        val currentRules = _rulesFlow.value.toMutableList()
        val index = currentRules.indexOfFirst { it.id == ruleId }
        if (index >= 0) {
            val rule = currentRules[index]
            currentRules[index] = rule.copy(
                lastExecutedTimestamp = log.timestamp,
                executionCount = rule.executionCount + 1
            )
            _rulesFlow.value = currentRules
            saveRules(currentRules)
        }

        val currentLogs = _logsFlow.value.toMutableList()
        currentLogs.add(0, log)
        // Keep max 50 logs for low memory & battery usage
        val trimmed = if (currentLogs.size > 50) currentLogs.take(50) else currentLogs
        _logsFlow.value = trimmed
        saveLogs(trimmed)
    }

    fun clearAllLogs() {
        _logsFlow.value = emptyList()
        saveLogs(emptyList())
    }

    private fun saveRules(rules: List<AutomationRule>) {
        val jsonArray = JSONArray()
        for (r in rules) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("description", r.description)
                put("isEnabled", r.isEnabled)
                put("triggerType", r.triggerType.name)
                put("timeHour", r.timeHour)
                put("timeMinute", r.timeMinute)
                put("locationName", r.locationName)
                put("latitude", r.latitude)
                put("longitude", r.longitude)
                put("radiusMeters", r.radiusMeters.toDouble())
                put("triggerOnEnter", r.triggerOnEnter)
                put("calendarEventKeyword", r.calendarEventKeyword)
                put("actionType", r.actionType.name)
                put("targetState", r.targetState)
                put("targetPhoneNumber", r.targetPhoneNumber)
                put("messageBody", r.messageBody)
                put("soundAlertLevel", r.soundAlertLevel)
                put("lastExecutedTimestamp", r.lastExecutedTimestamp ?: -1L)
                put("executionCount", r.executionCount)
                put("createdAt", r.createdAt)

                val daysArray = JSONArray()
                r.repeatDays.forEach { daysArray.put(it) }
                put("repeatDays", daysArray)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_RULES, jsonArray.toString()).apply()
    }

    private fun deserializeRules(json: String): List<AutomationRule> {
        val list = mutableListOf<AutomationRule>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val repeatList = mutableListOf<Int>()
                val daysArr = obj.optJSONArray("repeatDays")
                if (daysArr != null) {
                    for (d in 0 until daysArr.length()) {
                        repeatList.add(daysArr.getInt(d))
                    }
                }
                val lastTs = obj.optLong("lastExecutedTimestamp", -1L)
                list.add(
                    AutomationRule(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        triggerType = TriggerType.valueOf(obj.optString("triggerType", TriggerType.TIME_OF_DAY.name)),
                        timeHour = obj.optInt("timeHour", 9),
                        timeMinute = obj.optInt("timeMinute", 0),
                        repeatDays = if (repeatList.isEmpty()) listOf(1, 2, 3, 4, 5) else repeatList,
                        locationName = obj.optString("locationName", "Office"),
                        latitude = obj.optDouble("latitude", 37.7749),
                        longitude = obj.optDouble("longitude", -122.4194),
                        radiusMeters = obj.optDouble("radiusMeters", 200.0).toFloat(),
                        triggerOnEnter = obj.optBoolean("triggerOnEnter", true),
                        calendarEventKeyword = obj.optString("calendarEventKeyword", "Meeting"),
                        actionType = ActionType.valueOf(obj.optString("actionType", ActionType.WIFI_TOGGLE.name)),
                        targetState = obj.optBoolean("targetState", true),
                        targetPhoneNumber = obj.optString("targetPhoneNumber", ""),
                        messageBody = obj.optString("messageBody", ""),
                        soundAlertLevel = obj.optString("soundAlertLevel", "Chime"),
                        lastExecutedTimestamp = if (lastTs == -1L) null else lastTs,
                        executionCount = obj.optInt("executionCount", 0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {
            return getDefaultRules()
        }
        return list
    }

    private fun saveLogs(logs: List<ExecutionLog>) {
        val jsonArray = JSONArray()
        for (l in logs) {
            val obj = JSONObject().apply {
                put("id", l.id)
                put("ruleId", l.ruleId)
                put("ruleTitle", l.ruleTitle)
                put("actionType", l.actionType.name)
                put("timestamp", l.timestamp)
                put("isSuccess", l.isSuccess)
                put("statusMessage", l.statusMessage)
                put("triggerSource", l.triggerSource)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_LOGS, jsonArray.toString()).apply()
    }

    private fun deserializeLogs(json: String): List<ExecutionLog> {
        val list = mutableListOf<ExecutionLog>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ExecutionLog(
                        id = obj.getString("id"),
                        ruleId = obj.getString("ruleId"),
                        ruleTitle = obj.getString("ruleTitle"),
                        actionType = ActionType.valueOf(obj.optString("actionType", ActionType.WIFI_TOGGLE.name)),
                        timestamp = obj.getLong("timestamp"),
                        isSuccess = obj.getBoolean("isSuccess"),
                        statusMessage = obj.getString("statusMessage"),
                        triggerSource = obj.optString("triggerSource", "System")
                    )
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return list
    }

    private fun getDefaultRules(): List<AutomationRule> {
        return listOf(
            AutomationRule(
                id = "rule_morning_wifi",
                title = "Work Mode Wi-Fi Boost",
                description = "Turn ON Wi-Fi at 09:00 AM weekdays for office work tasks",
                isEnabled = true,
                triggerType = TriggerType.TIME_OF_DAY,
                timeHour = 9,
                timeMinute = 0,
                repeatDays = listOf(2, 3, 4, 5, 6),
                actionType = ActionType.WIFI_TOGGLE,
                targetState = true
            ),
            AutomationRule(
                id = "rule_hotspot_work",
                title = "Laptop Hotspot Quick Toggle",
                description = "Open and set mobile hotspot for field laptop tethering",
                isEnabled = true,
                triggerType = TriggerType.LOCATION,
                locationName = "Work Co-Working Space",
                latitude = 37.7891,
                longitude = -122.4014,
                radiusMeters = 300f,
                actionType = ActionType.HOTSPOT_TOGGLE,
                targetState = true
            ),
            AutomationRule(
                id = "rule_sms_checkin",
                title = "Daily Evening Work Check-in SMS",
                description = "Automated status text message sent at 06:30 PM",
                isEnabled = true,
                triggerType = TriggerType.TIME_OF_DAY,
                timeHour = 18,
                timeMinute = 30,
                repeatDays = listOf(2, 3, 4, 5, 6),
                actionType = ActionType.SEND_SMS,
                targetPhoneNumber = "+15550192834",
                messageBody = "Hey! Work tasks completed for today. Heading home now."
            ),
            AutomationRule(
                id = "rule_night_battery",
                title = "Night Eco & Battery Saver Profile",
                description = "Power-efficiency mode at 11:00 PM to eliminate overnight battery drain",
                isEnabled = true,
                triggerType = TriggerType.TIME_OF_DAY,
                timeHour = 23,
                timeMinute = 0,
                repeatDays = listOf(1, 2, 3, 4, 5, 6, 7),
                actionType = ActionType.BATTERY_SAVER_MODE,
                targetState = true
            ),
            AutomationRule(
                id = "rule_calendar_sync_standup",
                title = "Calendar Standup Alert & Wi-Fi Check",
                description = "Trigger Wi-Fi check when 'Standup' or 'Sprint' event starts",
                isEnabled = true,
                triggerType = TriggerType.CALENDAR_EVENT,
                calendarEventKeyword = "Standup",
                actionType = ActionType.SEND_NOTIFICATION,
                messageBody = "Daily Standup starting in 5 minutes! Sync tasks and notes."
            )
        )
    }

    private fun getDefaultLogs(): List<ExecutionLog> {
        val now = System.currentTimeMillis()
        return listOf(
            ExecutionLog(
                id = UUID.randomUUID().toString(),
                ruleId = "rule_morning_wifi",
                ruleTitle = "Work Mode Wi-Fi Boost",
                actionType = ActionType.WIFI_TOGGLE,
                timestamp = now - (2 * 3600 * 1000),
                isSuccess = true,
                statusMessage = "Wi-Fi toggle executed seamlessly",
                triggerSource = "Scheduled Time (09:00 AM)"
            ),
            ExecutionLog(
                id = UUID.randomUUID().toString(),
                ruleId = "rule_sms_checkin",
                ruleTitle = "Daily Evening Work Check-in SMS",
                actionType = ActionType.SEND_SMS,
                timestamp = now - (14 * 3600 * 1000),
                isSuccess = true,
                statusMessage = "Automated SMS dispatched to +15550192834",
                triggerSource = "Scheduled Time (06:30 PM)"
            )
        )
    }

    companion object {
        private const val KEY_RULES = "saved_automation_rules_json"
        private const val KEY_LOGS = "saved_execution_logs_json"
    }
}
