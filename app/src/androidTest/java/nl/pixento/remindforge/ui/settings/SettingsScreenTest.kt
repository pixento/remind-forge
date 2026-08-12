package nl.pixento.remindforge.ui.settings

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import nl.pixento.remindforge.R
import nl.pixento.remindforge.domain.model.VibrationPatternType
import nl.pixento.remindforge.ui.settings.vibration.labelRes
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun string(resId: Int, vararg formatArgs: Any) = context.getString(resId, *formatArgs)

    private fun setScreen(
        uiState: SettingsUiState = SettingsUiState(),
        onEnabledChanged: (Boolean) -> Unit = {},
        onRequestExactAlarmPermission: () -> Unit = {},
        onRequestBatteryOptimizationExemption: () -> Unit = {},
        onDismissBatteryOptimizationBanner: () -> Unit = {},
    ) {
        composeRule.setContent {
            SettingsScreen(
                uiState = uiState,
                onEnabledChanged = onEnabledChanged,
                onIntervalChanged = {},
                onWindowStartChanged = {},
                onWindowEndChanged = {},
                onVibrationPatternSelected = {},
                onRingtoneSelected = {},
                onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                onRequestBatteryOptimizationExemption = onRequestBatteryOptimizationExemption,
                onDismissBatteryOptimizationBanner = onDismissBatteryOptimizationBanner,
            )
        }
    }

    @Test
    fun permissionBannerHiddenWhenNotNeeded() {
        setScreen(uiState = SettingsUiState(needsExactAlarmPermission = false))
        composeRule.onNodeWithText(string(R.string.exact_alarm_banner_action)).assertDoesNotExist()
    }

    @Test
    fun permissionBannerShownWhenNeeded() {
        setScreen(uiState = SettingsUiState(needsExactAlarmPermission = true))
        composeRule.onNodeWithText(string(R.string.exact_alarm_banner_action)).assertExists()
    }

    @Test
    fun grantPermissionButtonInvokesCallback() {
        var requested = false
        setScreen(
            uiState = SettingsUiState(needsExactAlarmPermission = true),
            onRequestExactAlarmPermission = { requested = true },
        )
        composeRule.onNodeWithText(string(R.string.exact_alarm_banner_action)).performClick()
        assertTrue(requested)
    }

    @Test
    fun batteryOptimizationActionInvokesCallback() {
        var requested = false
        setScreen(
            uiState = SettingsUiState(showBatteryOptimizationBanner = true),
            onRequestBatteryOptimizationExemption = { requested = true },
        )
        composeRule.onNodeWithText(string(R.string.battery_optimization_banner_action)).performClick()
        assertTrue(requested)
    }

    @Test
    fun batteryOptimizationDismissInvokesCallback() {
        // Dismissal is the only way out of this banner - the exemption is optional, so a dead
        // button would leave the nudge on screen for good.
        var dismissed = false
        setScreen(
            uiState = SettingsUiState(showBatteryOptimizationBanner = true),
            onDismissBatteryOptimizationBanner = { dismissed = true },
        )
        composeRule.onNodeWithText(string(R.string.battery_optimization_banner_dismiss)).performClick()
        assertTrue(dismissed)
    }

    @Test
    fun enabledSwitchReflectsState() {
        setScreen(uiState = SettingsUiState(enabled = true))
        composeRule.onNodeWithTag("enabledSwitch").assertIsOn()
    }

    @Test
    fun togglingEnabledSwitchInvokesCallback() {
        var enabled: Boolean? = null
        setScreen(
            uiState = SettingsUiState(enabled = false),
            onEnabledChanged = { enabled = it },
        )
        composeRule.onNodeWithTag("enabledSwitch").assertIsOff().performClick()
        assertTrue(enabled == true)
    }

    @Test
    fun bothAlertChannelsAreAlwaysShownWithTheirCurrentValue() {
        setScreen(
            uiState = SettingsUiState(
                vibrationPattern = VibrationPatternType.DOUBLE_PULSE,
                ringtoneUri = "content://media/ringtone/1",
                ringtoneTitle = "Galaxy Bells",
            ),
        )

        composeRule.onNodeWithText(string(R.string.vibration_label)).assertExists()
        composeRule.onNodeWithText(string(VibrationPatternType.DOUBLE_PULSE.labelRes())).assertExists()
        composeRule.onNodeWithText(string(R.string.sound_label)).assertExists()
        composeRule.onNodeWithText("Galaxy Bells").assertExists()
    }

    @Test
    fun soundRowReadsSilentWithNoRingtoneSelected() {
        setScreen(uiState = SettingsUiState(ringtoneUri = null, ringtoneTitle = null))
        composeRule.onNodeWithText(string(R.string.silent_label)).assertExists()
    }

    @Test
    fun warningHiddenWhileEitherChannelStillAlerts() {
        setScreen(
            uiState = SettingsUiState(
                vibrationPattern = VibrationPatternType.SILENT,
                ringtoneUri = "content://media/ringtone/1",
                ringtoneTitle = "Galaxy Bells",
            ),
        )
        composeRule.onNodeWithText(string(R.string.no_alert_warning)).assertDoesNotExist()
    }

    @Test
    fun warningShownWhenBothChannelsAreSilent() {
        setScreen(
            uiState = SettingsUiState(
                vibrationPattern = VibrationPatternType.SILENT,
                ringtoneUri = null,
            ),
        )
        // The warning is the last item of the LazyColumn, so on a short screen it is never
        // composed until scrolled to.
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText(string(R.string.no_alert_warning)))
        composeRule.onNodeWithText(string(R.string.no_alert_warning)).assertExists()
    }

    @Test
    fun nextReminderShowsThePendingAlarmTime() {
        val trigger = Instant.now().plusSeconds(600)
        setScreen(
            uiState = SettingsUiState(
                enabled = true,
                nextTriggerAtMillis = trigger.toEpochMilli(),
            ),
        )

        val expected = DateTimeFormatter.ofPattern("HH:mm")
            .format(trigger.atZone(ZoneId.systemDefault()))
        composeRule.onNodeWithText(string(R.string.next_reminder_around, expected)).assertExists()
    }

    @Test
    fun nextReminderHiddenWhenNothingIsScheduled() {
        setScreen(uiState = SettingsUiState(enabled = true, nextTriggerAtMillis = null))
        // The raw template ("Next reminder around %1$s") with the placeholder stripped, so this
        // stays a substring match regardless of where the locale puts the placeholder.
        val prefix = context.getString(R.string.next_reminder_around).substringBefore("%").trim()
        composeRule.onNodeWithText(prefix, substring = true).assertDoesNotExist()
    }
}
