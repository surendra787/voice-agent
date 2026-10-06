package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AutomationRepository
import com.example.model.ActionType
import com.example.model.AutomationRule
import com.example.model.CalendarEventItem
import com.example.model.DeviceStatus
import com.example.model.ExecutionLog
import com.example.model.TriggerType
import com.example.receiver.AutomationAlarmReceiver
import com.example.service.AutomationActionExecutor
import com.example.service.CalendarSyncService
import com.example.service.DeviceMonitorService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface DashboardTab {
    object Rules : DashboardTab
    object Logs : DashboardTab
    object CalendarSync : DashboardTab
    object BatterySettings : DashboardTab
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AutomationRepository(application)

    val rules: StateFlow<List<AutomationRule>> = repository.rulesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<ExecutionLog>> = repository.logsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deviceStatus = MutableStateFlow(DeviceMonitorService.getDeviceStatus(application))
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus.asStateFlow()

    private val _calendarEvents = MutableStateFlow<List<CalendarEventItem>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEventItem>> = _calendarEvents.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        refreshDeviceStatus()
        loadCalendarEvents()
        syncAllAlarms()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun refreshDeviceStatus() {
        viewModelScope.launch {
            _deviceStatus.value = DeviceMonitorService.getDeviceStatus(getApplication())
        }
    }

    fun loadCalendarEvents() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val events = CalendarSyncService.readUpcomingCalendarEvents(getApplication())
            _calendarEvents.value = events
            _isRefreshing.value = false
        }
    }

    fun toggleRule(ruleId: String) {
        viewModelScope.launch {
            val updated = repository.toggleRuleEnabled(ruleId)
            if (updated != null) {
                if (updated.isEnabled) {
                    AutomationAlarmReceiver.scheduleRuleAlarm(getApplication(), updated)
                    _userMessage.value = "Activated \"${updated.title}\""
                } else {
                    AutomationAlarmReceiver.cancelRuleAlarm(getApplication(), updated.id)
                    _userMessage.value = "Paused \"${updated.title}\""
                }
            }
        }
    }

    fun runRuleNow(rule: AutomationRule) {
        viewModelScope.launch {
            _userMessage.value = "Executing \"${rule.title}\"..."
            val log = AutomationActionExecutor.executeRule(
                context = getApplication(),
                rule = rule,
                triggerSource = "Manual Quick Trigger"
            )
            repository.recordExecution(rule.id, log)
            refreshDeviceStatus()
            _userMessage.value = "Executed: ${log.statusMessage}"
        }
    }

    fun saveRule(rule: AutomationRule) {
        viewModelScope.launch {
            repository.upsertRule(rule)
            if (rule.isEnabled) {
                AutomationAlarmReceiver.scheduleRuleAlarm(getApplication(), rule)
            } else {
                AutomationAlarmReceiver.cancelRuleAlarm(getApplication(), rule.id)
            }
            _userMessage.value = "Rule saved: ${rule.title}"
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            AutomationAlarmReceiver.cancelRuleAlarm(getApplication(), ruleId)
            repository.deleteRule(ruleId)
            _userMessage.value = "Rule removed from automation"
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
            _userMessage.value = "Execution logs cleared"
        }
    }

    private fun syncAllAlarms() {
        viewModelScope.launch {
            val allRules = repository.getAllRules()
            for (rule in allRules) {
                if (rule.isEnabled && rule.triggerType == TriggerType.TIME_OF_DAY) {
                    AutomationAlarmReceiver.scheduleRuleAlarm(getApplication(), rule)
                }
            }
        }
    }

    fun createRuleFromCalendarEvent(event: CalendarEventItem, actionType: ActionType) {
        val newRule = AutomationRule(
            id = UUID.randomUUID().toString(),
            title = "Sync: ${event.title}",
            description = "Automated ${actionType.displayName} scheduled for calendar event at ${event.location.ifBlank { "Event Time" }}",
            isEnabled = true,
            triggerType = TriggerType.CALENDAR_EVENT,
            calendarEventKeyword = event.title,
            actionType = actionType,
            targetState = true,
            messageBody = "Reminder for upcoming event: ${event.title}"
        )
        saveRule(newRule)
        _userMessage.value = "Created automation rule from \"${event.title}\""
    }
}
