package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AutomationRepository
import com.example.model.ActionType
import com.example.model.AutomationRule
import com.example.model.TriggerType
import com.example.service.AutomationActionExecutor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AutoPulse", appName)
    }

    @Test
    fun `verify repository creates and retrieves rules`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AutomationRepository(context)

        val newRule = AutomationRule(
            id = UUID.randomUUID().toString(),
            title = "Office Wi-Fi Auto",
            description = "Toggle Wi-Fi upon arrival",
            isEnabled = true,
            triggerType = TriggerType.TIME_OF_DAY,
            timeHour = 10,
            timeMinute = 30,
            actionType = ActionType.WIFI_TOGGLE,
            targetState = true
        )

        repo.upsertRule(newRule)
        val retrieved = repo.getRuleById(newRule.id)
        assertNotNull(retrieved)
        assertEquals("Office Wi-Fi Auto", retrieved?.title)

        val toggled = repo.toggleRuleEnabled(newRule.id)
        assertEquals(false, toggled?.isEnabled)
    }

    @Test
    fun `verify rule execution generates audit log`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rule = AutomationRule(
            id = "test_reminder_rule",
            title = "Test Reminder Alert",
            description = "Notification test",
            triggerType = TriggerType.MANUAL,
            actionType = ActionType.SEND_NOTIFICATION,
            messageBody = "Meeting starts in 5 minutes"
        )

        val log = AutomationActionExecutor.executeRule(context, rule, "Robolectric Unit Test")
        assertNotNull(log)
        assertTrue(log.isSuccess)
        assertEquals("test_reminder_rule", log.ruleId)
    }
}
