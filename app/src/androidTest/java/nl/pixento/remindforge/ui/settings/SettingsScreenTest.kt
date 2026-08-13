package nl.pixento.remindforge.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import nl.pixento.remindforge.R
import nl.pixento.remindforge.domain.model.ActiveWindowMode
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
    ) {
        composeRule.setContent {
            // The screen is a LazyColumn, so an item below the fold is never composed and an
            // assertDoesNotExist on it would pass for the wrong reason (and an assertExists fail
            // for the wrong reason) depending only on the device's screen height. Give it a
            // viewport taller than the content so presence and absence both mean what they say.
            Box(modifier = Modifier.requiredHeight(2000.dp)) {
                SettingsScreen(
                    uiState = uiState,
                    onEnabledChanged = onEnabledChanged,
                    onIntervalChanged = {},
                    onActiveWindowModeChanged = {},
                    onWindowStartChanged = {},
                    onWindowEndChanged = {},
                    onVibrationPatternSelected = {},
                    onRingtoneSelected = {},
                    onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                )
            }
        }
    }

    @Test
    fun permissionBannerHiddenWhenNotNeeded() {
        setScreen(uiState = SettingsUiState(needsExactAlarmPermission = false))
        composeRule.onNodeWithText(string(R.string.exact_alarm_banner_action)).assertDoesNotExist()
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
        composeRule.onNodeWithText(string(R.string.no_alert_warning)).assertExists()
    }

    @Test
    fun pausedNoticeShownWhileFollowingDoNotDisturbAndItIsOn() {
        // Without this the enabled row would still promise a reminder at a specific time while
        // every tick silently skips its alert.
        setScreen(
            uiState = SettingsUiState(
                enabled = true,
                activeWindowMode = ActiveWindowMode.DO_NOT_DISTURB_OFF,
                doNotDisturbActive = true,
            ),
        )
        composeRule.onNodeWithTag("doNotDisturbPausedNotice").assertExists()
    }

    @Test
    fun pausedNoticeHiddenOnCustomTimesEvenWhileDoNotDisturbIsOn() {
        setScreen(
            uiState = SettingsUiState(
                enabled = true,
                activeWindowMode = ActiveWindowMode.CUSTOM_TIMES,
                doNotDisturbActive = true,
            ),
        )
        composeRule.onNodeWithTag("doNotDisturbPausedNotice").assertDoesNotExist()
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
