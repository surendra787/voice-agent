package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActionType
import com.example.model.AutomationRule
import com.example.model.TriggerType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RuleCard(
    rule: AutomationRule,
    onToggle: (String) -> Unit,
    onRunNow: (AutomationRule) -> Unit,
    onEdit: (AutomationRule) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actionColor = getActionColor(rule.actionType)
    val actionIcon = getActionIcon(rule.actionType)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rule_card_${rule.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isEnabled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (rule.isEnabled) 3.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Icon, Title & Enabled Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(actionColor.copy(alpha = if (rule.isEnabled) 0.18f else 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = rule.actionType.displayName,
                        tint = if (rule.isEnabled) actionColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (rule.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = rule.actionType.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = actionColor.copy(alpha = if (rule.isEnabled) 1f else 0.6f)
                    )
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onToggle(rule.id) },
                    modifier = Modifier.testTag("switch_rule_${rule.id}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            if (rule.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = rule.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Trigger Badge & Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TriggerBadge(triggerType = rule.triggerType, rule = rule)

                if (rule.executionCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = "${rule.executionCount} runs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Target Parameters (Phone / SMS / Settings)
            if (rule.actionType == ActionType.SEND_SMS && rule.targetPhoneNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = "To: ${rule.targetPhoneNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (rule.messageBody.isNotBlank()) {
                            Text(
                                text = "\"${rule.messageBody}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else if (rule.actionType == ActionType.PHONE_CALL && rule.targetPhoneNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Calls: ${rule.targetPhoneNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions row: Quick Run Now, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalButton(
                    onClick = { onRunNow(rule) },
                    modifier = Modifier.testTag("run_now_btn_${rule.id}"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = actionColor.copy(alpha = 0.15f),
                        contentColor = actionColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run Now",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Run Now",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onEdit(rule) },
                        modifier = Modifier.testTag("edit_rule_${rule.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Rule",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onDelete(rule.id) },
                        modifier = Modifier.testTag("delete_rule_${rule.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Rule",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TriggerBadge(triggerType: TriggerType, rule: AutomationRule) {
    val (icon, label) = when (triggerType) {
        TriggerType.TIME_OF_DAY -> {
            val hourStr = rule.timeHour.toString().padStart(2, '0')
            val minStr = rule.timeMinute.toString().padStart(2, '0')
            Icons.Default.Schedule to "Daily at $hourStr:$minStr"
        }
        TriggerType.LOCATION -> {
            Icons.Default.LocationOn to "Location: ${rule.locationName}"
        }
        TriggerType.CALENDAR_EVENT -> {
            Icons.Default.CalendarMonth to "Calendar: \"${rule.calendarEventKeyword}\""
        }
        TriggerType.MANUAL -> {
            Icons.Default.PlayArrow to "Quick Action"
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

fun getActionIcon(actionType: ActionType): ImageVector = when (actionType) {
    ActionType.WIFI_TOGGLE -> Icons.Default.Wifi
    ActionType.HOTSPOT_TOGGLE -> Icons.Default.WifiTethering
    ActionType.SEND_SMS -> Icons.Default.Sms
    ActionType.PHONE_CALL -> Icons.Default.Call
    ActionType.BATTERY_SAVER_MODE -> Icons.Default.BatteryChargingFull
    ActionType.SEND_NOTIFICATION -> Icons.Default.Notifications
}

fun getActionColor(actionType: ActionType): Color = when (actionType) {
    ActionType.WIFI_TOGGLE -> Color(0xFF0284C7) // Sky blue
    ActionType.HOTSPOT_TOGGLE -> Color(0xFFEA580C) // Orange
    ActionType.SEND_SMS -> Color(0xFF16A34A) // Green
    ActionType.PHONE_CALL -> Color(0xFF7C3AED) // Purple
    ActionType.BATTERY_SAVER_MODE -> Color(0xFF0D9488) // Teal
    ActionType.SEND_NOTIFICATION -> Color(0xFFD97706) // Amber
}
