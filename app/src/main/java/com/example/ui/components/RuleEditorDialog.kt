package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ActionType
import com.example.model.AutomationRule
import com.example.model.TriggerType
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RuleEditorDialog(
    initialRule: AutomationRule?,
    onDismiss: () -> Unit,
    onSave: (AutomationRule) -> Unit
) {
    var title by remember { mutableStateOf(initialRule?.title ?: "") }
    var description by remember { mutableStateOf(initialRule?.description ?: "") }
    var triggerType by remember { mutableStateOf(initialRule?.triggerType ?: TriggerType.TIME_OF_DAY) }
    var actionType by remember { mutableStateOf(initialRule?.actionType ?: ActionType.WIFI_TOGGLE) }
    var targetState by remember { mutableStateOf(initialRule?.targetState ?: true) }
    var hour by remember { mutableIntStateOf(initialRule?.timeHour ?: 9) }
    var minute by remember { mutableIntStateOf(initialRule?.timeMinute ?: 0) }
    var locationName by remember { mutableStateOf(initialRule?.locationName ?: "Office / Home") }
    var targetPhone by remember { mutableStateOf(initialRule?.targetPhoneNumber ?: "") }
    var messageBody by remember { mutableStateOf(initialRule?.messageBody ?: "") }
    var calendarKeyword by remember { mutableStateOf(initialRule?.calendarEventKeyword ?: "Meeting") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .testTag("rule_editor_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialRule == null) "Create Automation Rule" else "Edit Automation Rule",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Rule Title") },
                    placeholder = { Text("e.g. Work Arrival Hotspot, Morning Wi-Fi") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_title_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes") },
                    placeholder = { Text("Brief notes on when & why this executes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Trigger Selector
                Text(
                    text = "1. TRIGGER CONDITION",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TriggerType.values().forEach { t ->
                        FilterChip(
                            selected = triggerType == t,
                            onClick = { triggerType = t },
                            label = { Text(t.displayName) },
                            leadingIcon = {
                                when (t) {
                                    TriggerType.TIME_OF_DAY -> Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                    TriggerType.LOCATION -> Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    else -> null
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Trigger Details
                when (triggerType) {
                    TriggerType.TIME_OF_DAY -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Scheduled Execution Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = hour.toString(),
                                        onValueChange = { hour = it.toIntOrNull()?.coerceIn(0, 23) ?: 0 },
                                        label = { Text("Hour (0-23)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = minute.toString(),
                                        onValueChange = { minute = it.toIntOrNull()?.coerceIn(0, 59) ?: 0 },
                                        label = { Text("Minute (0-59)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    TriggerType.LOCATION -> {
                        OutlinedTextField(
                            value = locationName,
                            onValueChange = { locationName = it },
                            label = { Text("Geofence / Location Name") },
                            placeholder = { Text("e.g. Office, Home, Co-Working Studio") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TriggerType.CALENDAR_EVENT -> {
                        OutlinedTextField(
                            value = calendarKeyword,
                            onValueChange = { calendarKeyword = it },
                            label = { Text("Event Keyword Filter") },
                            placeholder = { Text("e.g. Standup, Meeting, Presentation") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TriggerType.MANUAL -> {
                        Text(
                            text = "Manual Trigger allows instant 1-tap test execution directly from your dashboard.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Selector
                Text(
                    text = "2. ACTION TO EXECUTE",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionType.values().forEach { a ->
                        FilterChip(
                            selected = actionType == a,
                            onClick = { actionType = a },
                            label = { Text(a.displayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Parameters
                when (actionType) {
                    ActionType.WIFI_TOGGLE, ActionType.HOTSPOT_TOGGLE, ActionType.BATTERY_SAVER_MODE -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Set target state to ${if (targetState) "ON" else "OFF"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = targetState,
                                onCheckedChange = { targetState = it }
                            )
                        }
                    }
                    ActionType.SEND_SMS -> {
                        OutlinedTextField(
                            value = targetPhone,
                            onValueChange = { targetPhone = it },
                            label = { Text("Recipient Phone Number") },
                            placeholder = { Text("e.g. +1234567890") },
                            modifier = Modifier.fillMaxWidth().testTag("sms_phone_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = messageBody,
                            onValueChange = { messageBody = it },
                            label = { Text("Automated Message Text") },
                            placeholder = { Text("Status update, daily reminder message...") },
                            modifier = Modifier.fillMaxWidth().testTag("sms_body_input"),
                            maxLines = 3
                        )
                    }
                    ActionType.PHONE_CALL -> {
                        OutlinedTextField(
                            value = targetPhone,
                            onValueChange = { targetPhone = it },
                            label = { Text("Call Recipient Phone Number") },
                            placeholder = { Text("e.g. +1234567890") },
                            modifier = Modifier.fillMaxWidth().testTag("call_phone_input")
                        )
                    }
                    ActionType.SEND_NOTIFICATION -> {
                        OutlinedTextField(
                            value = messageBody,
                            onValueChange = { messageBody = it },
                            label = { Text("Reminder Message / Alert Note") },
                            placeholder = { Text("Alert sound & notification contents") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save button
                Button(
                    onClick = {
                        val finalTitle = title.ifBlank { "${actionType.displayName} Rule" }
                        val newRule = AutomationRule(
                            id = initialRule?.id ?: UUID.randomUUID().toString(),
                            title = finalTitle,
                            description = description,
                            isEnabled = initialRule?.isEnabled ?: true,
                            triggerType = triggerType,
                            timeHour = hour,
                            timeMinute = minute,
                            locationName = locationName,
                            calendarEventKeyword = calendarKeyword,
                            actionType = actionType,
                            targetState = targetState,
                            targetPhoneNumber = targetPhone,
                            messageBody = messageBody,
                            executionCount = initialRule?.executionCount ?: 0,
                            lastExecutedTimestamp = initialRule?.lastExecutedTimestamp,
                            createdAt = initialRule?.createdAt ?: System.currentTimeMillis()
                        )
                        onSave(newRule)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_rule_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (initialRule == null) "Create Automation" else "Save Changes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
