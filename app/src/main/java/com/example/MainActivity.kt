package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AutomationRule
import com.example.ui.components.RuleEditorDialog
import com.example.ui.screens.BatterySettingsScreen
import com.example.ui.screens.CalendarSyncScreen
import com.example.ui.screens.DashboardRulesScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.theme.AutoPulseTheme
import com.example.viewmodel.DashboardTab
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AutoPulseTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    var currentTab by remember { mutableStateOf<DashboardTab>(DashboardTab.Rules) }
    var editingRule by remember { mutableStateOf<AutomationRule?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }

    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val deviceStatus by viewModel.deviceStatus.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val isRefreshingCalendar by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Request permissions for SMS, Calls, Calendar, Notifications, Location
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.refreshDeviceStatus()
        viewModel.loadCalendarEvents()
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(permissionsToRequest.toTypedArray())
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab is DashboardTab.Rules,
                    onClick = { currentTab = DashboardTab.Rules },
                    icon = {
                        Icon(
                            imageVector = if (currentTab is DashboardTab.Rules) Icons.Filled.AutoMode else Icons.Outlined.AutoMode,
                            contentDescription = "Automation Rules"
                        )
                    },
                    label = { Text("Rules") },
                    modifier = Modifier.testTag("nav_tab_rules")
                )

                NavigationBarItem(
                    selected = currentTab is DashboardTab.Logs,
                    onClick = { currentTab = DashboardTab.Logs },
                    icon = {
                        Icon(
                            imageVector = if (currentTab is DashboardTab.Logs) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "Logs"
                        )
                    },
                    label = { Text("Logs") },
                    modifier = Modifier.testTag("nav_tab_logs")
                )

                NavigationBarItem(
                    selected = currentTab is DashboardTab.CalendarSync,
                    onClick = {
                        currentTab = DashboardTab.CalendarSync
                        viewModel.loadCalendarEvents()
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentTab is DashboardTab.CalendarSync) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = "Calendar Sync"
                        )
                    },
                    label = { Text("Calendar") },
                    modifier = Modifier.testTag("nav_tab_calendar")
                )

                NavigationBarItem(
                    selected = currentTab is DashboardTab.BatterySettings,
                    onClick = { currentTab = DashboardTab.BatterySettings },
                    icon = {
                        Icon(
                            imageVector = if (currentTab is DashboardTab.BatterySettings) Icons.Filled.BatteryChargingFull else Icons.Outlined.BatteryChargingFull,
                            contentDescription = "Eco Battery"
                        )
                    },
                    label = { Text("Eco Power") },
                    modifier = Modifier.testTag("nav_tab_battery")
                )
            }
        },
        floatingActionButton = {
            if (currentTab is DashboardTab.Rules) {
                FloatingActionButton(
                    onClick = {
                        editingRule = null
                        showEditorDialog = true
                    },
                    modifier = Modifier.testTag("create_rule_fab"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add New Rule",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                is DashboardTab.Rules -> {
                    DashboardRulesScreen(
                        rules = rules,
                        deviceStatus = deviceStatus,
                        onToggleRule = { viewModel.toggleRule(it) },
                        onRunRule = { viewModel.runRuleNow(it) },
                        onEditRule = {
                            editingRule = it
                            showEditorDialog = true
                        },
                        onDeleteRule = { viewModel.deleteRule(it) },
                        onAddNewRule = {
                            editingRule = null
                            showEditorDialog = true
                        },
                        onRefreshStatus = { viewModel.refreshDeviceStatus() }
                    )
                }
                is DashboardTab.Logs -> {
                    LogsScreen(
                        logs = logs,
                        onClearLogs = { viewModel.clearLogs() }
                    )
                }
                is DashboardTab.CalendarSync -> {
                    CalendarSyncScreen(
                        events = calendarEvents,
                        isRefreshing = isRefreshingCalendar,
                        onRefresh = { viewModel.loadCalendarEvents() },
                        onRequestCalendarPermission = {
                            permissionsLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR))
                        },
                        onCreateRuleForEvent = { event, actionType ->
                            viewModel.createRuleFromCalendarEvent(event, actionType)
                        }
                    )
                }
                is DashboardTab.BatterySettings -> {
                    BatterySettingsScreen(
                        deviceStatus = deviceStatus,
                        onRefresh = { viewModel.refreshDeviceStatus() }
                    )
                }
            }

            if (showEditorDialog) {
                RuleEditorDialog(
                    initialRule = editingRule,
                    onDismiss = { showEditorDialog = false },
                    onSave = { rule ->
                        viewModel.saveRule(rule)
                        showEditorDialog = false
                    }
                )
            }
        }
    }
}
