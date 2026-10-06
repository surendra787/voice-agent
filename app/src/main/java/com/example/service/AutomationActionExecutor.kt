package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.model.ActionType
import com.example.model.AutomationRule
import com.example.model.ExecutionLog
import java.util.UUID

object AutomationActionExecutor {

    private const val CHANNEL_ID = "autopulse_notifications_channel"
    private const val CHANNEL_NAME = "AutoPulse Automation Alerts"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications and triggers for AutoPulse local automation rules"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun executeRule(context: Context, rule: AutomationRule, triggerSource: String): ExecutionLog {
        ensureNotificationChannel(context)
        var success = true
        var statusMsg = ""

        try {
            when (rule.actionType) {
                ActionType.WIFI_TOGGLE -> {
                    val result = handleWifiToggle(context, rule.targetState)
                    statusMsg = result
                }
                ActionType.HOTSPOT_TOGGLE -> {
                    val result = handleHotspotToggle(context, rule.targetState)
                    statusMsg = result
                }
                ActionType.SEND_SMS -> {
                    val result = handleSendSms(context, rule.targetPhoneNumber, rule.messageBody)
                    statusMsg = result
                }
                ActionType.PHONE_CALL -> {
                    val result = handlePhoneCall(context, rule.targetPhoneNumber)
                    statusMsg = result
                }
                ActionType.BATTERY_SAVER_MODE -> {
                    val result = handleBatterySaverGuide(context, rule.targetState)
                    statusMsg = result
                }
                ActionType.SEND_NOTIFICATION -> {
                    val result = handleReminderNotification(context, rule.title, rule.messageBody)
                    statusMsg = result
                }
            }
        } catch (e: Exception) {
            success = false
            statusMsg = "Failed: ${e.localizedMessage ?: "Unknown error"}"
        }

        // Post execution notification
        postExecutionNotification(context, rule, statusMsg, success)

        return ExecutionLog(
            id = UUID.randomUUID().toString(),
            ruleId = rule.id,
            ruleTitle = rule.title,
            actionType = rule.actionType,
            timestamp = System.currentTimeMillis(),
            isSuccess = success,
            statusMessage = statusMsg,
            triggerSource = triggerSource
        )
    }

    private fun handleWifiToggle(context: Context, targetState: Boolean): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && wifiManager != null) {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = targetState
                "Wi-Fi set to ${if (targetState) "ON" else "OFF"} via local Wi-Fi controller"
            } else {
                // On Android Q+, direct programmatic toggling without user dialog is restricted by modern Android security.
                // We dispatch the system Wi-Fi panel / settings intent smoothly.
                val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Prompted system Wi-Fi toggle panel to switch Wi-Fi ${if (targetState) "ON" else "OFF"}"
            }
        } catch (e: Exception) {
            // Fallback to Settings.ACTION_WIFI_SETTINGS
            val fallback = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            "Opened Wi-Fi settings for ${if (targetState) "enabling" else "disabling"} connection"
        }
    }

    private fun handleHotspotToggle(context: Context, targetState: Boolean): String {
        return try {
            // Hotspot settings / Tethering page
            val intent = Intent().apply {
                action = "android.settings.TETHER_SETTINGS"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Navigated directly to Tethering & Hotspot controls (Target: ${if (targetState) "ON" else "OFF"})"
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            "Opened Wireless Network settings to manage Mobile Hotspot"
        }
    }

    private fun handleSendSms(context: Context, phoneNumber: String, message: String): String {
        if (phoneNumber.isBlank()) {
            return "No recipient phone number specified"
        }
        val safeMessage = if (message.isBlank()) "Automated reminder from AutoPulse" else message

        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(safeMessage)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            "Automated SMS successfully transmitted to $phoneNumber: \"$safeMessage\""
        } catch (se: SecurityException) {
            // Fallback: Open SMS compose with pre-filled content
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber")).apply {
                putExtra("sms_body", safeMessage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "SMS permissions pending: Pre-filled SMS opened for $phoneNumber"
        } catch (e: Exception) {
            "SMS Error: ${e.message}"
        }
    }

    private fun handlePhoneCall(context: Context, phoneNumber: String): String {
        if (phoneNumber.isBlank()) {
            return "No contact phone number provided"
        }

        return try {
            // Direct call if CALL_PHONE is granted, else DIAL intent
            val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(callIntent)
            "Direct call initiated to $phoneNumber"
        } catch (se: SecurityException) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            "Permission pending: Dial pad opened with $phoneNumber ready"
        }
    }

    private fun handleBatterySaverGuide(context: Context, targetState: Boolean): String {
        return try {
            val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Battery Saver settings opened (Optimal Low Battery Profile: ${if (targetState) "ON" else "OFF"})"
        } catch (e: Exception) {
            "Battery Profile settings accessed"
        }
    }

    private fun handleReminderNotification(context: Context, title: String, message: String): String {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("AutoPulse Reminder: $title")
            .setContentText(message.ifBlank { "Scheduled automated trigger fired" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.ifBlank { "Scheduled automated task successfully executed locally on device." }))
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notifManager.notify(System.currentTimeMillis().toInt(), notification)
        return "Sound/Reminder alert dispatched to user"
    }

    private fun postExecutionNotification(context: Context, rule: AutomationRule, statusMsg: String, success: Boolean) {
        try {
            val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                rule.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val iconRes = if (success) android.R.drawable.stat_notify_sync else android.R.drawable.stat_notify_error
            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(iconRes)
                .setContentTitle(if (success) "Executed: ${rule.title}" else "Task Failed: ${rule.title}")
                .setContentText(statusMsg)
                .setSubText("AutoPulse Automation")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notifManager.notify((rule.id.hashCode() and 0x7FFFFFFF), notif)
        } catch (_: Exception) {
            // Ignore if notification permission not yet given
        }
    }
}
