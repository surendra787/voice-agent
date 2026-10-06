package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.model.AutomationRule
import com.example.model.TriggerType
import com.example.service.AutomationActionExecutor
import java.util.Calendar

class AutomationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val ruleId = intent.getStringExtra(EXTRA_RULE_ID) ?: return
        val ruleTitle = intent.getStringExtra(EXTRA_RULE_TITLE) ?: "Scheduled Task"

        // Perform local rule execution
        val repository = com.example.data.AutomationRepository(context)
        val rule = repository.getRuleById(ruleId)
        if (rule != null && rule.isEnabled) {
            val log = AutomationActionExecutor.executeRule(context, rule, "AlarmManager Scheduler")
            repository.recordExecution(ruleId, log)

            // Reschedule next recurrence if applicable
            scheduleNextAlarm(context, rule)
        }
    }

    companion object {
        const val ACTION_TRIGGER = "com.example.autopulse.ACTION_TRIGGER_AUTOMATION"
        const val EXTRA_RULE_ID = "extra_rule_id"
        const val EXTRA_RULE_TITLE = "extra_rule_title"

        fun scheduleRuleAlarm(context: Context, rule: AutomationRule) {
            if (!rule.isEnabled || rule.triggerType != TriggerType.TIME_OF_DAY) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, AutomationAlarmReceiver::class.java).apply {
                action = ACTION_TRIGGER
                putExtra(EXTRA_RULE_ID, rule.id)
                putExtra(EXTRA_RULE_TITLE, rule.title)
            }

            val requestCode = rule.id.hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, rule.timeHour)
                set(Calendar.MINUTE, rule.timeMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If scheduled time has already passed today, advance to next occurrence
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // Power-efficient exact or inexact alarm based on Android versions
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (se: SecurityException) {
                // Fallback to inexact set if exact alarm permission not yet granted
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }

        fun cancelRuleAlarm(context: Context, ruleId: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, AutomationAlarmReceiver::class.java).apply {
                action = ACTION_TRIGGER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ruleId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        private fun scheduleNextAlarm(context: Context, rule: AutomationRule) {
            scheduleRuleAlarm(context, rule)
        }
    }
}
